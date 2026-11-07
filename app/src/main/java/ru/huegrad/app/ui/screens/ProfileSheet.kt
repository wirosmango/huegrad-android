@file:OptIn(ExperimentalMaterial3Api::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import ru.huegrad.app.forum.ForumProfile
import ru.huegrad.app.forum.ForumProfiles
import ru.huegrad.app.forum.ProfilePost
import ru.huegrad.app.ui.ForumAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Профиль автора: открывается по нажатию на аватар или ник в сообщении.
 * У гостей (authorUid == null) профиля нет, показываем только аватар и ник.
 */
@Composable
internal fun ForumProfileSheet(
    nickname: String,
    avatarUrl: String?,
    authorUid: String?,
    onDismiss: () -> Unit,
    onOpenPost: (ProfilePost) -> Unit,
    onManageAccount: (() -> Unit)? = null, // только у собственного профиля: вход, выход и настройки аккаунта
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var profile by remember(nickname, authorUid) { mutableStateOf<ForumProfile?>(null) }
    var failed by remember(nickname, authorUid) { mutableStateOf(false) }

    LaunchedEffect(nickname, authorUid) {
        if (authorUid == null) return@LaunchedEffect
        try {
            profile = ForumProfiles.load(nickname, authorUid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ForumAvatar(nickname = nickname, photoUrl = avatarUrl, size = 96.dp)
            Text(nickname, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)

            val p = profile
            when {
                authorUid == null -> Text(
                    "Гость: пишет без аккаунта, поэтому профиля и истории сообщений нет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                failed -> Text(
                    "Не удалось загрузить профиль. Проверьте соединение и попробуйте ещё раз.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                p == null -> CircularProgressIndicator(Modifier.padding(24.dp))
                else -> ProfileContent(p, onOpenPost)
            }

            if (onManageAccount != null) {
                OutlinedButton(onClick = onManageAccount, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("Управление аккаунтом")
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(p: ForumProfile, onOpenPost: (ProfilePost) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(p.postCount.toString(), plural(p.postCount, "сообщение", "сообщения", "сообщений"))
            Stat(if (p.likesPartial) "${p.likes}+" else p.likes.toString(), "лайков")
            Stat(p.sinceMs?.let { formatDate(it) } ?: "—", "на форуме с")
        }
    }

    if (p.recent.isNotEmpty()) {
        Text(
            "Последние сообщения",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        p.recent.forEach { post ->
            Surface(
                onClick = { onOpenPost(post) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        post.topicTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        post.excerpt.ifBlank { "(без текста)" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    post.timeMs?.let {
                        Text(
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDate(ms: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(ms))

/** 1 сообщение, 2 сообщения, 5 сообщений. */
private fun plural(n: Long, one: String, few: String, many: String): String {
    val m100 = n % 100
    val m10 = n % 10
    return when {
        m100 in 11..14 -> many
        m10 == 1L -> one
        m10 in 2..4 -> few
        else -> many
    }
}
