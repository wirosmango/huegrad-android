package ru.huegrad.app.forum

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Уведомление из коллекции notifications: type = "mention" (упомянули) или "reply" (ответили через #N). */
data class AppNotification(
    val id: String,
    val type: String,
    val fromNickname: String,
    val fromPhoto: String?,
    val topicId: String,
    val topicTitle: String,
    val postId: String,
    val excerpt: String,
    val timeMs: Long?,
    val read: Boolean,
) {
    val isReply get() = type == "reply"

    /** Короткий текст для всплывающего сообщения. */
    val toastText get() = "$fromNickname ${if (isReply) "ответил(а) на ваше сообщение" else "упомянул(а) вас"}"
}

/**
 * Состояние входа на форуме на уровне всего приложения (как onAuthStateChanged в forum.js):
 * закреплённый ник вошедшего пользователя и его уведомления об упоминаниях и ответах.
 */
object ForumSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val db get() = FirebaseFirestore.getInstance()

    private var started = false
    private lateinit var appContext: Context

    /** Пока идёт регистрация по почте, ник закрепляет сама регистрация, а не слушатель входа. */
    @Volatile
    var registrationInProgress = false

    private val _nickname = MutableStateFlow<String?>(null)

    /** Ник вошедшего (не анонимного) пользователя, иначе null. */
    val nickname: StateFlow<String?> = _nickname.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _incoming = MutableSharedFlow<AppNotification>(extraBufferCapacity = 8)

    /** Новые непрочитанные уведомления, пришедшие во время работы приложения (для всплывающего сообщения). */
    val incoming: SharedFlow<AppNotification> = _incoming.asSharedFlow()

    private var registration: ListenerRegistration? = null
    private var notificationsUid: String? = null
    private var loaded = false

    fun start(app: Application) {
        if (started || !ForumAuth.isConfigured(app)) return
        started = true
        appContext = app.applicationContext
        MentionNotifier.ensureChannel(appContext)
        FirebaseAuth.getInstance().addAuthStateListener { onAuth(it.currentUser) }
    }

    private fun onAuth(user: FirebaseUser?) {
        if (user == null || user.isAnonymous) {
            _nickname.value = null
            stopNotifications()
            MentionService.sync(appContext)
            return
        }
        _nickname.value = user.displayName?.takeIf { it.isNotBlank() } ?: "Пользователь"
        startNotifications(user.uid)
        MentionService.sync(appContext)
        if (!registrationInProgress) scope.launch { applyUsername(user) }
    }

    /** Закрепляет ник за аккаунтом и обновляет его в приложении. */
    suspend fun applyUsername(user: FirebaseUser) {
        try {
            val nick = ForumIdentity.ensureUsername(user)
            if (nick != null && nick != _nickname.value) _nickname.value = nick
        } catch (e: Exception) {
            Log.e("ForumSession", "Не удалось закрепить ник", e)
        }
    }

    /**
     * Регистрация по почте с ником: проверяет ник, создаёт аккаунт и закрепляет ник (usernames).
     * Выполняется в своём scope, чтобы закрытие страницы входа не оборвало закрепление ника.
     * onDone получает null при успехе или текст ошибки.
     */
    fun registerWithEmail(email: String, password: String, nickname: String, onDone: (String?) -> Unit) {
        scope.launch {
            val nick = nickname.trim()
            if (!ForumIdentity.NICK_RE.matches(nick)) {
                onDone("Ник: 2–30 символов, буквы, цифры, пробел и _ . - (не в конце)")
                return@launch
            }
            if (ForumIdentity.isNickTaken(nick)) {
                onDone("Этот ник уже занят")
                return@launch
            }
            registrationInProgress = true // чтобы слушатель входа не закрепил ник раньше времени
            try {
                val user = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email.trim(), password).await().user
                    ?: error("Не удалось создать аккаунт")
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nick).build()).await()
                applyUsername(user)
                onDone(null)
            } catch (e: Exception) {
                Log.e("ForumSession", "Ошибка регистрации", e)
                onDone(ForumIdentity.translateAuthError(e))
            } finally {
                registrationInProgress = false
            }
        }
    }

    /** Выход из аккаунта: дальше форум снова работает анонимно, как на сайте. */
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        scope.launch { runCatching { ForumAuth.signIn() } }
    }

    // ---------- Уведомления ----------

    private fun startNotifications(uid: String) {
        if (notificationsUid == uid && registration != null) return
        stopNotifications()
        notificationsUid = uid
        loaded = false

        registration = db.collection("notifications").whereEqualTo("toUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    Log.e("ForumSession", "Ошибка уведомлений", err)
                    return@addSnapshotListener
                }
                val all = snap.documents.map { it.toAppNotification() }
                // Ещё не подтверждённое сервером время считаем самым новым, как на сайте
                _notifications.value = all.sortedByDescending { it.timeMs ?: Long.MAX_VALUE }.take(30)

                // Прочитанное (здесь или на сайте) убираем из шторки
                all.filter { it.read }.forEach { MentionNotifier.cancel(appContext, it.id) }

                if (!loaded) {
                    // Первая загрузка: старое не показываем, но запоминаем, чтобы фоновая проверка его не повторила
                    MentionNotifier.markNotified(appContext, all.filter { !it.read }.map { it.id })
                } else {
                    val added = snap.documentChanges
                        .filter { it.type == DocumentChange.Type.ADDED }
                        .map { it.document.toAppNotification() }
                        .filter { !it.read }
                    if (AppForeground.visible) {
                        // Приложение открыто: короткое сообщение внутри приложения, как на сайте
                        MentionNotifier.markNotified(appContext, added.map { it.id })
                        added.forEach { _incoming.tryEmit(it) }
                    } else {
                        // Приложение в фоне, но процесс жив: уведомление в шторке
                        MentionNotifier.notifyNew(appContext, added)
                    }
                }
                loaded = true
            }
    }

    private fun stopNotifications() {
        registration?.remove()
        registration = null
        notificationsUid = null
        _notifications.value = emptyList()
    }

    fun markRead(n: AppNotification) {
        if (n.read) return
        markReadById(n.id)
    }

    /** Отмечает прочитанным и убирает из шторки (в том числе при переходе по уведомлению из шторки). */
    fun markReadById(id: String) {
        MentionNotifier.cancel(appContext, id)
        db.collection("notifications").document(id).update("read", true)
            .addOnFailureListener { Log.e("ForumSession", "Не удалось отметить прочитанным", it) }
    }

    fun markAllRead() {
        val unread = _notifications.value.filter { !it.read }
        if (unread.isEmpty()) return
        unread.forEach { MentionNotifier.cancel(appContext, it.id) }
        val batch = db.batch()
        unread.forEach { batch.update(db.collection("notifications").document(it.id), "read", true) }
        batch.commit().addOnFailureListener { Log.e("ForumSession", "Не удалось отметить прочитанными", it) }
    }

    /**
     * Отправляет уведомления после публикации поста: упомянутым через @ник и авторам постов, на которые
     * ответили через #N. [authorUids] — авторы постов темы по порядку (индекс 0 = пост #1), у гостей null.
     * Сбой уведомлений не должен ломать публикацию, поэтому всё выполняется отдельно и только пишется в лог.
     */
    fun notifyRecipients(
        postId: String,
        message: String,
        authorNick: String,
        fromUid: String,
        fromPhoto: String?,
        topicId: String,
        topicTitle: String,
        authorUids: List<String?>,
    ) {
        scope.launch {
            try {
                val recipients = LinkedHashMap<String, String>() // uid получателя -> "mention" | "reply"
                val clean = message.replace(Regex("```[\\s\\S]*?```|`[^`\\n]*`"), "")

                // --- упоминания @ник ---
                val nicks = ForumIdentity.extractMentions(message).take(5)
                nicks.map { n ->
                    async { runCatching { db.collection("usernames").document(n).get().await() }.getOrNull() }
                }.awaitAll().forEach { s ->
                    if (s != null && s.exists()) s.getString("uid")?.let { recipients[it] = "mention" }
                }

                // --- ответы #N (номер = позиция поста в теме) ---
                Regex("#(\\d+)").findAll(clean).take(5).forEach { m ->
                    val index = (m.groupValues[1].toIntOrNull() ?: return@forEach) - 1
                    authorUids.getOrNull(index)?.let { recipients[it] = "reply" }
                }

                recipients.remove(fromUid) // себя не уведомляем
                val list = recipients.entries.take(10)
                if (list.isEmpty()) return@launch

                val batch = db.batch()
                list.forEach { (toUid, type) ->
                    batch.set(
                        db.collection("notifications").document("${postId}_$toUid"),
                        mapOf(
                            "type" to type,
                            "toUid" to toUid,
                            "fromUid" to fromUid,
                            "fromNickname" to authorNick.take(30),
                            "fromPhoto" to fromPhoto,
                            "topicId" to topicId,
                            "topicTitle" to topicTitle.take(100),
                            "postId" to postId,
                            "excerpt" to message.replace(Regex("\\s+"), " ").take(140),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "read" to false,
                        ),
                    )
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("ForumSession", "Не удалось отправить уведомления", e)
            }
        }
    }

}

/** Документ коллекции notifications в виде AppNotification (общий для слушателя и фонового воркера). */
internal fun DocumentSnapshot.toAppNotification() = AppNotification(
    id = id,
    type = getString("type") ?: "mention",
    fromNickname = getString("fromNickname") ?: "Кто-то",
    fromPhoto = getString("fromPhoto"),
    topicId = getString("topicId").orEmpty(),
    topicTitle = getString("topicTitle").orEmpty(),
    postId = getString("postId").orEmpty(),
    excerpt = getString("excerpt").orEmpty(),
    timeMs = getTimestamp("createdAt")?.toDate()?.time,
    read = getBoolean("read") ?: false,
)
