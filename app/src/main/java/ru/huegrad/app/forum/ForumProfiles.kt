package ru.huegrad.app.forum

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Сообщение в списке «Последние сообщения» профиля. */
data class ProfilePost(
    val postId: String,
    val topicId: String,
    val topicTitle: String,
    val excerpt: String,
    val timeMs: Long?,
)

data class ForumProfile(
    val nickname: String,
    val sinceMs: Long?, // дата закрепления ника (usernames.createdAt), то есть «на форуме с»
    val postCount: Long,
    val likes: Long,
    val likesPartial: Boolean, // лайки посчитаны только по последним сообщениям
    val recent: List<ProfilePost>,
)

/**
 * Профиль автора собирается из уже существующих данных, новых коллекций не нужно:
 * usernames (ник и дата), posts с authorUid (сообщения и лайки), topics (названия тем).
 * authorUid есть только у вошедших в аккаунт: анонимные гости профиля не имеют.
 */
object ForumProfiles {
    private val db get() = FirebaseFirestore.getInstance()
    private val IMAGE_MD = Regex("!\\[[^\\]]*]\\([^)]*\\)")
    private const val FETCH_LIMIT = 100L

    suspend fun load(nickname: String, authorUid: String): ForumProfile = coroutineScope {
        val since = async {
            runCatching {
                db.collection("usernames").whereEqualTo("uid", authorUid).limit(1).get().await()
                    .documents.firstOrNull()?.getTimestamp("createdAt")?.toDate()?.time
            }.getOrNull()
        }
        // count() считается на сервере и не читает документы; если недоступно, берём число загруженных
        val total = async {
            runCatching {
                db.collection("posts").whereEqualTo("authorUid", authorUid)
                    .count().get(AggregateSource.SERVER).await().count
            }.getOrNull()
        }
        // Без orderBy, чтобы не требовался составной индекс: последние пять выбираем на устройстве
        val docs = db.collection("posts").whereEqualTo("authorUid", authorUid).limit(FETCH_LIMIT).get().await().documents

        val newestFirst = docs.sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE }
        val latest = newestFirst.take(5)
        val titles = latest.mapNotNull { it.getString("topicId") }.distinct().map { id ->
            async { id to runCatching { db.collection("topics").document(id).get().await().getString("title") }.getOrNull() }
        }.awaitAll().toMap()

        val recent = latest.mapNotNull { d ->
            val topic = d.getString("topicId") ?: return@mapNotNull null
            ProfilePost(
                postId = d.id,
                topicId = topic,
                topicTitle = titles[topic] ?: "Тема",
                excerpt = (d.getString("message") ?: "")
                    .replace(IMAGE_MD, "[фото]").replace(Regex("\\s+"), " ").trim().take(140),
                timeMs = d.getTimestamp("createdAt")?.toDate()?.time,
            )
        }
        val count = total.await() ?: docs.size.toLong()
        ForumProfile(
            nickname = nickname,
            sinceMs = since.await(),
            postCount = count,
            likes = docs.sumOf { it.getLong("likes") ?: 0L },
            likesPartial = count > docs.size,
            recent = recent,
        )
    }
}
