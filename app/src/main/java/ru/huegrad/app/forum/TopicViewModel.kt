package ru.huegrad.app.forum

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Post(
    val id: String,
    val number: Int,
    val nickname: String,
    val avatarUrl: String?,
    val message: String,
    val timeMs: Long?,
    val likes: Long,
    val authorUid: String? = null, // только у вошедших, нужен для уведомлений об ответах
    val images: List<String> = emptyList(), // картинки из полей документа, которых нет в тексте (старые посты)
    val poll: Poll? = null,
    val attachments: List<Attachment> = emptyList(), // вложения как на сайте: поле attachments [{url, type}]
)

/** Опрос лежит в посте полем poll: {question, options}. Голоса хранятся отдельно, в posts/{id}/pollVotes/{uid}. */
data class Poll(val question: String, val options: List<String>)

/** Итоги опроса: голоса по вариантам и выбор текущего пользователя (null, если не голосовал). */
data class PollResult(val counts: List<Int>, val myVote: Int?)

private fun parsePoll(raw: Any?): Poll? {
    val m = raw as? Map<*, *> ?: return null
    val question = m["question"] as? String ?: return null
    val options = (m["options"] as? List<*>)?.filterIsInstance<String>() ?: return null
    return if (question.isNotBlank() && options.size >= 2) Poll(question, options) else null
}

/** Результат загрузки фото в Cloudinary. */
sealed interface UploadEvent {
    data class Failed(val message: String) : UploadEvent
}

const val MAX_ATTACHMENTS = 4

enum class UploadStatus { UPLOADING, DONE }

/** Файл, выбранный для сообщения: сначала загружается в Cloudinary, затем уходит вместе с постом. */
data class PendingAttachment(
    val id: Int,
    val previewUri: Uri,
    val isVideo: Boolean,
    val progress: Float = 0f,
    val status: UploadStatus = UploadStatus.UPLOADING,
    val result: Attachment? = null,
)

/** Вложения поста; принимаются только ссылки Cloudinary (на сайте такая же проверка). */
private fun parseAttachments(raw: Any?): List<Attachment> =
    (raw as? List<*>).orEmpty().take(MAX_ATTACHMENTS).mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        val url = m["url"] as? String ?: return@mapNotNull null
        if (!url.startsWith("https://res.cloudinary.com/")) return@mapNotNull null
        Attachment(url, m["type"] == "video")
    }

sealed interface PostsState {
    data object NotConfigured : PostsState
    data object Loading : PostsState
    data class Ready(val posts: List<Post>) : PostsState // новые сверху, как на сайте
    data class Error(val message: String) : PostsState
}

/**
 * Посты темы: коллекция posts {topicId, nickname, message, createdAt, likes},
 * лайки: posts/{postId}/likes/{uid}. Номер поста = позиция по createdAt (старейший = #1).
 */
class TopicViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    val topicId: String = checkNotNull(handle["id"])

    /** Пост, к которому нужно прокрутить при открытии (переход из уведомления). */
    val initialPostId: String? = handle.get<String>("post")
    private val db get() = FirebaseFirestore.getInstance()

    private val _title = MutableStateFlow("Тема")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _state = MutableStateFlow<PostsState>(PostsState.Loading)
    val state: StateFlow<PostsState> = _state.asStateFlow()

    private val _liked = MutableStateFlow<Set<String>>(emptySet())
    val liked: StateFlow<Set<String>> = _liked.asStateFlow()

    private val _pollResults = MutableStateFlow<Map<String, PollResult>>(emptyMap())
    val pollResults: StateFlow<Map<String, PollResult>> = _pollResults.asStateFlow()
    private val pollListeners = mutableMapOf<String, ListenerRegistration>()

    /** Выбранные для сообщения файлы (до 4, как на сайте). */
    private val _attachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    val attachments: StateFlow<List<PendingAttachment>> = _attachments.asStateFlow()
    private var attachSeq = 0

    private val uploadChannel = Channel<UploadEvent>(Channel.BUFFERED)

    /** Каждое событие приходит один раз, даже если экран пересоздался во время загрузки. */
    val uploadEvents: Flow<UploadEvent> = uploadChannel.receiveAsFlow()

    private var uid: String? = null
    private val likeChecked = mutableSetOf<String>()
    private var lastPosts: List<Post> = emptyList() // по возрастанию, индекс 0 = пост #1
    private var registration: ListenerRegistration? = null

    init {
        if (!ForumAuth.isConfigured(app)) {
            _state.value = PostsState.NotConfigured
        } else {
            viewModelScope.launch {
                uid = try {
                    ForumAuth.signIn()
                } catch (e: Exception) {
                    _state.value = PostsState.Error(ForumAuth.describe(e))
                    return@launch
                }
                db.collection("topics").document(topicId).get()
                    .addOnSuccessListener { _title.value = it.getString("title") ?: "Тема" }

                registration = db.collection("posts")
                    .whereEqualTo("topicId", topicId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            _state.value = PostsState.Error(err.localizedMessage ?: "Ошибка загрузки постов")
                            return@addSnapshotListener
                        }
                        val posts = snap?.documents.orEmpty().mapIndexed { i, d ->
                            Post(
                                id = d.id,
                                number = i + 1,
                                nickname = d.getString("nickname") ?: "Аноним",
                                avatarUrl = d.getString("photoURL") ?: d.getString("avatarUrl"),
                                message = d.getString("message") ?: "",
                                timeMs = d.getTimestamp("createdAt")?.toDate()?.time,
                                likes = d.getLong("likes") ?: 0L,
                                authorUid = d.getString("authorUid"),
                                images = extractImages(d.data, d.getString("message") ?: ""),
                                poll = parsePoll(d.get("poll")),
                                attachments = parseAttachments(d.get("attachments")),
                            )
                        }
                        lastPosts = posts
                        watchPolls(posts)
                        _state.value = PostsState.Ready(posts.reversed())
                        checkLikes(posts.map { it.id })
                    }
            }
        }
    }

    private fun checkLikes(ids: List<String>) {
        val me = uid ?: return
        ids.filter { likeChecked.add(it) }.forEach { id ->
            db.collection("posts").document(id).collection("likes").document(me).get()
                .addOnSuccessListener { if (it.exists()) _liked.update { s -> s + id } }
        }
    }

    private fun patchAttachment(id: Int, change: (PendingAttachment) -> PendingAttachment) {
        _attachments.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    /** Начинает загрузку выбранных файлов: пока они грузятся, можно писать текст, а отправка ждёт окончания. */
    fun addAttachments(uris: List<Uri>) {
        if (ForumSession.nickname.value == null) {
            viewModelScope.launch { uploadChannel.send(UploadEvent.Failed("Прикреплять файлы могут только вошедшие в аккаунт")) }
            return
        }
        val app = getApplication<Application>()
        for (uri in uris) {
            if (_attachments.value.size >= MAX_ATTACHMENTS) {
                viewModelScope.launch { uploadChannel.send(UploadEvent.Failed("Не больше $MAX_ATTACHMENTS файлов в одном сообщении")) }
                break
            }
            val isVideo = app.contentResolver.getType(uri)?.startsWith("video/") == true
            val id = ++attachSeq
            _attachments.update { it + PendingAttachment(id, uri, isVideo) }
            viewModelScope.launch {
                try {
                    val result = CloudinaryUploader.upload(app, uri) { p -> patchAttachment(id) { it.copy(progress = p) } }
                    patchAttachment(id) { it.copy(status = UploadStatus.DONE, progress = 1f, result = result) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _attachments.update { list -> list.filterNot { it.id == id } }
                    uploadChannel.send(UploadEvent.Failed(e.localizedMessage ?: "Не удалось загрузить файл"))
                }
            }
        }
    }

    fun removeAttachment(id: Int) {
        _attachments.update { list -> list.filterNot { it.id == id } }
    }

    /**
     * onDone получает null при успехе или текст ошибки.
     * Писать могут только вошедшие в аккаунт. Файлы уходят в поле attachments, как на сайте.
     */
    fun send(nick: String, message: String, onDone: (String?) -> Unit) {
        val pending = _attachments.value
        if (pending.any { it.status == UploadStatus.UPLOADING }) {
            onDone("Дождитесь окончания загрузки файлов")
            return
        }
        val ready = pending.mapNotNull { it.result }
        val text = message.trim().take(2000)
        if (text.isEmpty() && ready.isEmpty()) {
            onDone("Пустое сообщение")
            return
        }
        val extra = if (ready.isNotEmpty()) mapOf("attachments" to ready.map { it.toMap() }) else emptyMap()
        publish(nick, text, text.ifBlank { "[вложение]" }, extra) { error ->
            if (error == null) _attachments.value = emptyList()
            onDone(error)
        }
    }

    /**
     * Опрос публикуется обычным постом с полем poll. В message он дублируется текстом, чтобы сайт,
     * который опросы не рисует, показал хотя бы вопрос и варианты.
     */
    fun sendPoll(nick: String, question: String, options: List<String>, onDone: (String?) -> Unit) {
        val q = question.trim().take(150)
        val opts = options.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(6)
        if (q.isEmpty() || opts.size < 2) {
            onDone("Нужен вопрос и минимум два разных варианта")
            return
        }
        val message = buildString {
            append("📊 ").append(q)
            opts.forEachIndexed { i, o -> append("\n${i + 1}. ").append(o) }
        }
        publish(nick, message, "📊 $q", mapOf("poll" to mapOf("question" to q, "options" to opts)), onDone)
    }

    private fun publish(nick: String, full: String, notifyText: String, extra: Map<String, Any?>, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val registered = ForumSession.nickname.value
            val user = FirebaseAuth.getInstance().currentUser
            // Писать могут только вошедшие в аккаунт: анонимный вход остаётся для чтения и лайков
            if (registered == null || user == null || user.isAnonymous) {
                onDone("Писать могут только вошедшие в аккаунт")
                return@launch
            }
            val nickname = registered.take(30)
            try {
                val uid = ForumAuth.signIn()

                // Как на сайте: photoURL пишется только если у вошедшего пользователя есть фото,
                // а authorUid только у вошедших
                val photo = user?.takeIf { !it.isAnonymous }?.photoUrl?.toString()
                val data = mutableMapOf<String, Any?>(
                    "topicId" to topicId,
                    "nickname" to nickname,
                    "message" to full,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "likes" to 0,
                )
                if (!photo.isNullOrBlank()) data["photoURL"] = photo
                if (registered != null) data["authorUid"] = uid
                data.putAll(extra)

                val ref = db.collection("posts").add(data).await()
                onDone(null)

                ForumSession.notifyRecipients(
                    postId = ref.id,
                    message = notifyText,
                    authorNick = nickname,
                    fromUid = uid,
                    fromPhoto = photo?.takeIf { it.isNotBlank() },
                    topicId = topicId,
                    topicTitle = _title.value,
                    authorUids = lastPosts.map { it.authorUid },
                )
            } catch (e: Exception) {
                onDone(e.localizedMessage ?: "Не удалось отправить")
            }
        }
    }

    /** На каждый опрос в теме вешается один слушатель голосов; итоги считаются на устройстве. */
    private fun watchPolls(posts: List<Post>) {
        for (p in posts) {
            val poll = p.poll ?: continue
            if (pollListeners.containsKey(p.id)) continue
            pollListeners[p.id] = db.collection("posts").document(p.id).collection("pollVotes")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val me = FirebaseAuth.getInstance().currentUser?.uid ?: uid
                    val counts = IntArray(poll.options.size)
                    var mine: Int? = null
                    for (d in snap.documents) {
                        val o = d.getLong("option")?.toInt() ?: continue
                        if (o !in counts.indices) continue
                        counts[o]++
                        if (d.id == me) mine = o
                    }
                    _pollResults.update { it + (p.id to PollResult(counts.toList(), mine)) }
                }
        }
    }

    /** Один голос на пользователя: документ pollVotes/{uid} создаётся один раз, голос изменить нельзя. */
    fun vote(postId: String, option: Int, onError: (String) -> Unit) {
        if (_pollResults.value[postId]?.myVote != null) return
        viewModelScope.launch {
            try {
                val me = ForumAuth.signIn()
                db.collection("posts").document(postId).collection("pollVotes").document(me)
                    .set(mapOf("option" to option, "createdAt" to FieldValue.serverTimestamp()))
                    .await()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Не удалось проголосовать")
            }
        }
    }

    fun like(postId: String) {
        val me = uid ?: return
        if (postId in _liked.value) return
        _liked.update { it + postId }
        val postRef = db.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(me)
        db.runTransaction(Transaction.Function<Void?> { tx ->
            if (!tx.get(likeRef).exists()) {
                tx.set(likeRef, mapOf("likedAt" to FieldValue.serverTimestamp()))
                tx.update(postRef, "likes", FieldValue.increment(1))
            }
            null
        }).addOnFailureListener { _liked.update { s -> s - postId } }
    }

    override fun onCleared() {
        registration?.remove()
        pollListeners.values.forEach { it.remove() }
        super.onCleared()
    }
}

private val notImageKeys = setOf("attachments", "message", "nickname", "photoURL", "avatarUrl", "authorUid", "topicId")

/**
 * Старые посты могли хранить картинку отдельным полем (имя поля неизвестно), поэтому просматриваем
 * все поля документа и берём ссылки на картинки, которых ещё нет в тексте сообщения.
 */
internal fun extractImages(data: Map<String, Any?>?, message: String): List<String> {
    if (data == null) return emptyList()
    val out = LinkedHashSet<String>()
    fun add(v: Any?) {
        when (v) {
            is String -> {
                val u = v.trim()
                if (' ' !in u && isImageUrl(u) && u !in message) out += secureImageUrl(u)
            }
            is List<*> -> v.forEach { add(it) }
            is Map<*, *> -> v.values.forEach { add(it) }
        }
    }
    data.forEach { (key, value) -> if (key !in notImageKeys) add(value) }
    return out.toList()
}
