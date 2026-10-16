package ru.huegrad.app.ui

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.huegrad.app.forum.MentionNotifier

/** Куда открыть приложение по нажатию на уведомление: тема форума и пост, к которому прокрутить. */
object DeepLink {
    data class Target(val topicId: String, val postId: String, val notificationId: String?)

    /** Ждёт, пока HuegradApp откроет нужный экран, и сбрасывается после этого. */
    var pending by mutableStateOf<Target?>(null)

    /** Читает адрес из Intent уведомления и убирает его оттуда, чтобы поворот экрана не открыл тему повторно. */
    fun handle(intent: Intent?) {
        val topic = intent?.getStringExtra(MentionNotifier.EXTRA_TOPIC_ID)?.takeIf { it.isNotBlank() } ?: return
        pending = Target(
            topicId = topic,
            postId = intent.getStringExtra(MentionNotifier.EXTRA_POST_ID).orEmpty(),
            notificationId = intent.getStringExtra(MentionNotifier.EXTRA_NOTIFICATION_ID),
        )
        intent.removeExtra(MentionNotifier.EXTRA_TOPIC_ID)
        intent.removeExtra(MentionNotifier.EXTRA_POST_ID)
        intent.removeExtra(MentionNotifier.EXTRA_NOTIFICATION_ID)
    }
}
