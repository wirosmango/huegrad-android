package ru.huegrad.app.forum

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.random.Random

/** Токен «@...» перед курсором в поле ввода: [start, end) в тексте и введённое после @ начало ника. */
data class MentionToken(val start: Int, val end: Int, val prefix: String)

/**
 * Ники и упоминания, как на сайте (forum.js): коллекция usernames/{ник в нижнем регистре}
 * = {uid, nickname, createdAt} закрепляет ник за аккаунтом, уникальность гарантируют правила Firestore.
 */
object ForumIdentity {
    private val db get() = FirebaseFirestore.getInstance()

    /** Ник: 2–30 символов, буквы/цифры/пробел/_ . -, без . - пробела в конце. */
    val NICK_RE = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N}_. \\-]{0,28}[\\p{L}\\p{N}_]$")

    /** @ник или @[Ник с пробелом]: группа 1 — ник в скобках, группа 2 — ник без скобок. */
    val MENTION_RE = Regex("(?<![\\p{L}\\p{N}_])@(?:\\[([^\\]\\n]{2,30})]|([\\p{L}\\p{N}_.\\-]{2,30}))")

    private val CODE_RE = Regex("```[\\s\\S]*?```|`[^`\\n]*`")
    private val TOKEN_BRACKET_RE = Regex("(?<![\\p{L}\\p{N}_])@\\[([^\\]\\n]{0,30})$")
    private val TOKEN_PLAIN_RE = Regex("(?<![\\p{L}\\p{N}_])@([\\p{L}\\p{N}_.\\-]{0,30})$")

    fun sanitizeNick(raw: String?): String {
        val s = (raw ?: "")
            .replace(Regex("[^\\p{L}\\p{N}_. \\-]"), "")
            .replace(Regex("^[^\\p{L}\\p{N}]+"), "")
            .take(30)
            .replace(Regex("[. \\-]+$"), "")
        return if (s.length >= 2) s else "Пользователь"
    }

    /** Ники, упомянутые в тексте: в нижнем регистре, без дублей и без кода. */
    fun extractMentions(text: String): List<String> {
        val clean = text.replace(CODE_RE, "")
        val found = LinkedHashSet<String>()
        for (m in MENTION_RE.findAll(clean)) {
            val raw = m.groupValues[1].ifEmpty { m.groupValues[2] }
            if (raw.isEmpty()) continue
            val nick = raw.trim().trimEnd('.', '-').lowercase()
            if (nick.length >= 2) found += nick
        }
        return found.toList()
    }

    /** Ищет незавершённое упоминание сразу перед курсором (для подсказок ников). */
    fun findMentionToken(text: String, caret: Int): MentionToken? {
        if (caret < 0 || caret > text.length) return null
        val before = text.substring(0, caret)
        val m = TOKEN_BRACKET_RE.find(before) ?: TOKEN_PLAIN_RE.find(before) ?: return null
        val prefix = m.groupValues[1]
        if (prefix.isEmpty()) return null
        return MentionToken(start = m.range.first, end = caret, prefix = prefix)
    }

    /** Закрепляет ник за аккаунтом, возвращает итоговый ник или null, если подобрать не удалось. */
    suspend fun ensureUsername(user: FirebaseUser): String? {
        val mine = db.collection("usernames").whereEqualTo("uid", user.uid).limit(1).get().await()
        if (!mine.isEmpty) return mine.documents[0].getString("nickname")

        val base = sanitizeNick(user.displayName)
        for (attempt in 0 until 5) {
            val nick = if (attempt == 0) base else base.take(26) + Random.nextInt(1000, 10000)
            try {
                db.collection("usernames").document(nick.lowercase()).set(
                    mapOf("uid" to user.uid, "nickname" to nick, "createdAt" to FieldValue.serverTimestamp())
                ).await()
                if (user.displayName != nick) {
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nick).build()).await()
                }
                return nick
            } catch (e: FirebaseFirestoreException) {
                // ник занят (правила Firestore запрещают перезапись) — пробуем другой
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        return null
    }

    /** Занят ли ник зарегистрированным пользователем. Недопустимый для id ник считаем свободным. */
    suspend fun isNickTaken(nick: String): Boolean =
        runCatching { db.collection("usernames").document(nick.trim().lowercase()).get().await().exists() }
            .getOrDefault(false)

    /** Подсказки ников по началу (id документа = ник в нижнем регистре). */
    suspend fun searchNicks(prefix: String): List<String> {
        val p = prefix.lowercase()
        return runCatching {
            db.collection("usernames")
                .orderBy(FieldPath.documentId())
                .startAt(p)
                .endAt(p + "")
                .limit(5)
                .get().await()
                .documents.mapNotNull { it.getString("nickname") }
        }.getOrDefault(emptyList())
    }

    fun translateAuthError(e: Throwable): String = when ((e as? FirebaseAuthException)?.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Этот email уже зарегистрирован"
        "ERROR_INVALID_EMAIL" -> "Некорректный email"
        "ERROR_WEAK_PASSWORD" -> "Пароль слишком простой (минимум 6 символов)"
        "ERROR_USER_NOT_FOUND" -> "Пользователь не найден"
        "ERROR_WRONG_PASSWORD" -> "Неверный пароль"
        "ERROR_INVALID_CREDENTIAL" -> "Неверный email или пароль"
        "ERROR_OPERATION_NOT_ALLOWED" -> "Этот способ входа не включён в Firebase Console"
        else -> e.localizedMessage ?: "Ошибка входа"
    }
}
