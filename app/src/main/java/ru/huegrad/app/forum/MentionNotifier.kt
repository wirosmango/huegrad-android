package ru.huegrad.app.forum

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.huegrad.app.MainActivity
import ru.huegrad.app.R

/**
 * Находится ли приложение на экране. Пока оно открыто, о новом упоминании сообщает всплывающее
 * сообщение внутри приложения, в фоне это делает уведомление в шторке.
 */
object AppForeground {
    @Volatile
    var visible = false
}

/** Системные уведомления об упоминаниях и ответах на форуме (шторка уведомлений). */
object MentionNotifier {
    const val CHANNEL_ID = "mentions"

    // Что кладём в Intent уведомления, чтобы по нажатию открыть нужную тему и пост
    const val EXTRA_TOPIC_ID = "mention_topic_id"
    const val EXTRA_POST_ID = "mention_post_id"
    const val EXTRA_NOTIFICATION_ID = "mention_notification_id"

    private const val PREFS = "huegrad_mention_notifier"
    private const val KEY_NOTIFIED = "notified"
    private const val MAX_REMEMBERED = 300
    private const val MAX_PER_RUN = 5
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000 // старше недели не напоминаем

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Упоминания и ответы", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Когда на форуме вас упоминают через @ник или отвечают на ваше сообщение" }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // ---------- Какие уведомления уже показывали, чтобы не повторять ----------

    private fun remembered(ctx: Context): List<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NOTIFIED, "")
            .orEmpty().split('\n').filter { it.isNotEmpty() }

    /** Запоминает id уведомлений форума (порядок сохраняется, хранятся последние [MAX_REMEMBERED]). */
    fun markNotified(ctx: Context, ids: Collection<String>) {
        if (ids.isEmpty()) return
        val all = LinkedHashSet(remembered(ctx)).apply { addAll(ids) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_NOTIFIED, all.toList().takeLast(MAX_REMEMBERED).joinToString("\n"))
            .apply()
    }

    /**
     * Показывает в шторке то, о чём ещё не напоминали, и запоминает все непрочитанные из [all],
     * чтобы следующая проверка (из приложения или из фона) их не повторила.
     * За один раз показывается не больше [MAX_PER_RUN] самых свежих.
     */
    fun notifyNew(ctx: Context, all: List<AppNotification>) {
        val unread = all.filter { !it.read }
        if (unread.isEmpty()) return
        if (!MentionService.isEnabled(ctx)) {
            // Проверка отключена в аккаунте: ничего не показываем и запоминаем, чтобы старое не всплыло после включения
            markNotified(ctx, unread.map { it.id })
            return
        }
        val done = remembered(ctx).toSet()
        val now = System.currentTimeMillis()
        val fresh = unread
            .filter { it.id !in done && (it.timeMs == null || now - it.timeMs <= MAX_AGE_MS) }
            .sortedBy { it.timeMs ?: Long.MAX_VALUE }
            .takeLast(MAX_PER_RUN)
        fresh.forEach { show(ctx, it) }
        markNotified(ctx, unread.map { it.id })
    }

    // ---------- Само уведомление ----------

    /** false, если уведомления для приложения выключены (в настройках телефона или нет разрешения). */
    @SuppressLint("MissingPermission")
    fun show(ctx: Context, n: AppNotification): Boolean {
        val manager = NotificationManagerCompat.from(ctx)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(ctx)

        val open = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TOPIC_ID, n.topicId)
            putExtra(EXTRA_POST_ID, n.postId)
            putExtra(EXTRA_NOTIFICATION_ID, n.id)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            n.id.hashCode(), // свой requestCode, чтобы уведомления не подменяли друг другу Intent
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = n.excerpt.ifBlank { n.topicTitle }
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_city)
            .setContentTitle(n.toastText)
            .setContentText(text)
            .setSubText(n.topicTitle.ifBlank { null })
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(n.timeMs ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .build()

        return try {
            manager.notify(n.id.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /** Убирает уведомление из шторки (например, когда упоминание уже прочитано). */
    fun cancel(ctx: Context, id: String) {
        NotificationManagerCompat.from(ctx).cancel(id.hashCode())
    }
}
