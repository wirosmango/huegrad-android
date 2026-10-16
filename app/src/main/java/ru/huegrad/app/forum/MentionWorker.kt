package ru.huegrad.app.forum

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/**
 * Фоновая проверка уведомлений для случая, когда приложение закрыто и слушатель Firestore не работает.
 * Раз в ~15 минут (меньше система не позволяет, а в режиме сна телефона бывает реже) спрашивает
 * у Firestore непрочитанные упоминания и показывает новые в шторке.
 * Мгновенные уведомления при полностью закрытом приложении возможны только через push с сервера (FCM).
 */
class MentionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        // Пока приложение открыто, этим занимается слушатель в ForumSession
        if (AppForeground.visible || !ForumAuth.isConfigured(ctx)) return Result.success()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) return Result.success()

        try {
            val snap = FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("toUid", user.uid)
                .get().await()
            MentionNotifier.notifyNew(ctx, snap.documents.map { it.toAppNotification() })
        } catch (e: Exception) {
            // Не повторяем сразу: следующая проверка по расписанию
            Log.w(TAG, "Фоновая проверка уведомлений не удалась", e)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "MentionWorker"
        private const val WORK_NAME = "mention-poll"

        fun schedule(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<MentionWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
        }
    }
}
