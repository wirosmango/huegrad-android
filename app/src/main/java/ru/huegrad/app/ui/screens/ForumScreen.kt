@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.huegrad.app.forum.AppNotification
import ru.huegrad.app.forum.ForumViewModel
import ru.huegrad.app.forum.TopicsState
import ru.huegrad.app.ui.AppSettings
import ru.huegrad.app.ui.FloatingBarSideInset
import ru.huegrad.app.ui.ForumAvatar
import ru.huegrad.app.ui.LocalBottomBarInset
import ru.huegrad.app.ui.NoInsets
import ru.huegrad.app.ui.floatingActionBottom

@Composable
fun ForumScreen(
    onOpenTopic: (String) -> Unit,
    onOpenNotification: (AppNotification) -> Unit,
    vm: ForumViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showAccount by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Форум") },
                actions = {
                    NotificationBell(onOpen = onOpenNotification)
                    IconButton(onClick = { showAccount = true }) {
                        // Перечитываем пользователя после закрытия диалога входа или выхода
                        val me = remember(showAccount) { runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() }
                        val photo = me?.photoUrl?.toString()
                        val name = me?.displayName.orEmpty()
                        if (me != null && !me.isAnonymous && (photo != null || name.isNotBlank())) {
                            ForumAvatar(nickname = name.ifBlank { me.email.orEmpty() }, photoUrl = photo, size = 32.dp)
                        } else {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = "Аккаунт")
                        }
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (val s = state) {
                TopicsState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                TopicsState.NotConfigured -> Hint(
                    "Ебень ты googleservices.json проибав",
                    Modifier.align(Alignment.Center),
                )
                is TopicsState.Error -> Hint(s.message, Modifier.align(Alignment.Center))
                is TopicsState.Ready -> {
                    if (s.topics.isEmpty()) {
                        Hint("Ещё никто не соизволил создать тему", Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = LocalBottomBarInset.current + 88.dp),
                        ) {
                            item {
                                ExpressiveListGroup {
                                    s.topics.forEachIndexed { index, t ->
                                        ExpressiveListItem(
                                            title = t.title,
                                            icon = Icons.Rounded.Forum,
                                            onClick = { onOpenTopic(t.id) },
                                            showDivider = index != s.topics.lastIndex,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Кнопка стоит относительно плавающей панели навигации, а не по фиксированному отступу:
            // низ считается от реальной высоты панели, правый край совпадает с правым краем пилюли
            if (state is TopicsState.Ready) {
                val fabBottom = floatingActionBottom(AppSettings.navStyle, LocalBottomBarInset.current)
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = FloatingBarSideInset, bottom = fabBottom),
                    onClick = { showDialog = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Новая тема", maxLines = 1) },
                )
            }
        }
    }

    if (showAccount) AccountDialog(onDismiss = { showAccount = false })

    if (showDialog) NewTopicDialog(
        onDismiss = { showDialog = false },
        onCreate = { title ->
            vm.createTopic(title) { id -> if (id != null) onOpenTopic(id) }
            showDialog = false
        },
    )
}

@Composable
private fun NewTopicDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая тема") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text("Название") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title) }, enabled = title.isNotBlank()) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
internal fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(32.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
