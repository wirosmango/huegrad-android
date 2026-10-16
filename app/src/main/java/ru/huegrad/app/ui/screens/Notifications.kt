@file:OptIn(ExperimentalMaterial3Api::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.huegrad.app.forum.AppNotification
import ru.huegrad.app.forum.ForumSession
import ru.huegrad.app.ui.ForumAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Колокольчик уведомлений для верхней панели: значок с числом непрочитанных, по нажатию открывает список.
 * Показывается только вошедшему пользователю (анонимам уведомления не приходят).
 */
@Composable
fun NotificationBell(onOpen: (AppNotification) -> Unit) {
    val nickname by ForumSession.nickname.collectAsStateWithLifecycle()
    if (nickname == null) return

    val items by ForumSession.notifications.collectAsStateWithLifecycle()
    val unread = items.count { !it.read }
    var show by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { show = true }) {
        BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) } }) {
            Icon(
                if (unread > 0) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                contentDescription = "Уведомления",
            )
        }
    }

    if (show) {
        NotificationsSheet(
            items = items,
            onDismiss = { show = false },
            onOpen = { n ->
                ForumSession.markRead(n)
                show = false
                onOpen(n)
            },
        )
    }
}

@Composable
private fun NotificationsSheet(
    items: List<AppNotification>,
    onDismiss: () -> Unit,
    onOpen: (AppNotification) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Уведомления",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { ForumSession.markAllRead() },
                    enabled = items.any { !it.read },
                ) { Text("Прочитать все") }
            }

            if (items.isEmpty()) {
                Text(
                    "Пока нет уведомлений",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items, key = { it.id }) { n -> NotificationRow(n) { onOpen(n) } }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val action = if (n.isReply) "ответил(а) на ваше сообщение в" else "упомянул(а) вас в"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (n.read) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ForumAvatar(nickname = n.fromNickname, photoUrl = n.fromPhoto, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${n.fromNickname} $action «${n.topicTitle}»",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                n.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                n.timeMs?.let { SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault()).format(Date(it)) } ?: "только что",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
