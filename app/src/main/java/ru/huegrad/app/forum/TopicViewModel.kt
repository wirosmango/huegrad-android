package ru.huegrad.app.forum

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

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
    fun send(nick: String, message: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val registered = ForumSession.nickname.value
            val nickname = (registered ?: nick.trim()).take(30)
            val text = message.trim().take(2000)
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
                    "message" to text,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "likes" to 0,
                )
                if (!photo.isNullOrBlank()) data["photoURL"] = photo
                if (registered != null) data["authorUid"] = uid

                val ref = db.collection("posts").add(data).await()
                onDone(null)

                ForumSession.notifyRecipients(
                    postId = ref.id,
                    message = text,
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
