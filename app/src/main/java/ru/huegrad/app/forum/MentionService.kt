package ru.huegrad.app.forum

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import ru.huegrad.app.MainActivity
import ru.huegrad.app.R

/**
 * Фоновый сервис: пока он работает, процесс приложения не замораживается, и слушатель уведомлений
 * из ForumSession получает новые упоминания даже при свёрнутом приложении. Сам он ничего не слушает.
 * Работает только для вошедшего аккаунта и только если включён переключатель в аккаунте.
 */
class MentionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // После перезапуска системой процесс стартует без Activity, поэтому поднимаем форум-сессию здесь
        ForumSession.start(application)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_city)
            .setContentTitle("Хуеград: слежу за упоминаниями")
            .setContentText("Чтобы отключить, выключи «Проверка уведомлений» в аккаунте")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось перейти в режим foreground", e)
            stopSelf()
        }
        return START_STICKY
    }

    companion object {
        private const val TAG = "MentionService"
        private const val CHANNEL_ID = "mention_service"
        private const val NOTIFICATION_ID = 4101
        private const val PREFS = "huegrad_mention_notifier"
        private const val KEY_ENABLED = "background_service"

        /** По умолчанию включено: без сервиса Android быстро замораживает свёрнутое приложение. */
        fun isEnabled(ctx: Context) =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

        fun setEnabled(ctx: Context, enabled: Boolean) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
            sync(ctx)
        }

        /** Запускает или останавливает сервис по состоянию входа и переключателя. */
        fun sync(ctx: Context) {
            val app = ctx.applicationContext
            val user = if (ForumAuth.isConfigured(app)) FirebaseAuth.getInstance().currentUser else null
            val want = isEnabled(app) && user != null && !user.isAnonymous
            // Фоновая проверка раз в ~15 минут тоже подчиняется переключателю
            if (want) MentionWorker.schedule(app) else MentionWorker.cancel(app)
            try {
                if (want) ContextCompat.startForegroundService(app, Intent(app, MentionService::class.java))
                else app.stopService(Intent(app, MentionService::class.java))
            } catch (e: Exception) {
                // Например, запуск из фона запрещён системой: повторится при следующем открытии приложения
                Log.w(TAG, "Не удалось изменить состояние сервиса", e)
            }
        }

        private fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(CHANNEL_ID, "Работа в фоне", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Постоянное уведомление, пока приложение следит за упоминаниями" }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
