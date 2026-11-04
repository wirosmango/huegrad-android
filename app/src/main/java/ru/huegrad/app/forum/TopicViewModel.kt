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
)

/** Результат загрузки фото в Cloudinary. */
sealed interface UploadEvent {
    data class Done(val url: String) : UploadEvent
    data class Failed(val message: String) : UploadEvent
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

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

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
                            )
                        }
                        lastPosts = posts
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

    /**
     * onDone получает null при успехе или текст ошибки.
     * Вошедший пользователь пишет под своим закреплённым ником, гость под введённым (если он не занят).
     */
    fun send(nick: String, message: String, imageUrl: String?, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val registered = ForumSession.nickname.value
            val nickname = (registered ?: nick.trim()).take(30)
            // Прикреплённое фото уходит в конце сообщения строкой ![](url), как на сайте
            val suffix = imageUrl?.let { "\n![]($it)" }.orEmpty()
            val text = message.trim().take((2000 - suffix.length).coerceAtLeast(0)).trim()
            val full = (text + suffix).trim()
            if (full.isEmpty()) {
                onDone("Пустое сообщение")
                return@launch
            }
            try {
                // После выхода из аккаунта вход анонимный нужно восстановить, иначе правила запретят запись
                val uid = ForumAuth.signIn()

                // Гость не может писать под ником, который занят зарегистрированным пользователем
                if (registered == null && ForumIdentity.isNickTaken(nickname)) {
                    onDone("Этот ник занят зарегистрированным пользователем. Выберите другой или войдите.")
                    return@launch
                }

                val user = FirebaseAuth.getInstance().currentUser
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

                val ref = db.collection("posts").add(data).await()
                onDone(null)

                ForumSession.notifyRecipients(
                    postId = ref.id,
                    message = text.ifBlank { "[фото]" },
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

    /** Загружает фото в Cloudinary; ссылка приходит в [uploadEvents] и показывается превью над полем, к сообщению добавляется при отправке. */
    fun uploadImage(uri: Uri) {
        if (_uploading.value) return
        _uploading.value = true
        viewModelScope.launch {
            try {
                val url = CloudinaryUploader.upload(getApplication<Application>(), uri)
                uploadChannel.send(UploadEvent.Done(url))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uploadChannel.send(UploadEvent.Failed(e.localizedMessage ?: "Не удалось загрузить фото"))
            } finally {
                _uploading.value = false
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
        super.onCleared()
    }
}

private val notImageKeys = setOf("message", "nickname", "photoURL", "avatarUrl", "authorUid", "topicId")

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
