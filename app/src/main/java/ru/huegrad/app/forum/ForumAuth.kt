package ru.huegrad.app.forum

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Анонимный вход, как в forum.js (signInAnonymously). Правила Firestore ждут request.auth. */
object ForumAuth {
    fun isConfigured(ctx: Context) = FirebaseApp.getApps(ctx).isNotEmpty()

    suspend fun signIn(): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().await().user?.uid ?: error("Нет uid после анонимного входа")
    }

    fun describe(e: Throwable) =
        "Не удалось войти анонимно: ${e.localizedMessage}. Проверь, что в Firebase → Authentication включён провайдер «Anonymous»."
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { t ->
        if (t.isSuccessful) cont.resume(t.result) else cont.resumeWithException(t.exception ?: Exception("Task failed"))
    }
}
