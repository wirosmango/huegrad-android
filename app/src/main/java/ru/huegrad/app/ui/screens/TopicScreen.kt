@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import ru.huegrad.app.ui.AppSettings
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import ru.huegrad.app.forum.AppNotification
import ru.huegrad.app.forum.ForumIdentity
import ru.huegrad.app.forum.ForumSession
import ru.huegrad.app.forum.MentionToken
import ru.huegrad.app.forum.Post
import ru.huegrad.app.forum.PostsState
import ru.huegrad.app.forum.TopicViewModel
import ru.huegrad.app.ui.ForumAvatar
import ru.huegrad.app.ui.MarkdownText
import ru.huegrad.app.ui.NoInsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopicScreen(
    onBack: () -> Unit,
    onOpenNotification: (AppNotification) -> Unit,
    vm: TopicViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    // Ник вошедшего закреплён за аккаунтом, гость вводит ник сам
    val myNick by ForumSession.nickname.collectAsStateWithLifecycle()

    var nick by rememberSaveable { mutableStateOf("Аноним") }
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val text = field.text
    var sending by remember { mutableStateOf(false) }
    var highlighted by remember { mutableStateOf<Int?>(null) }
    // Пост, к которому надо прокрутить (переход из уведомления)
    var pendingPostId by rememberSaveable { mutableStateOf(vm.initialPostId) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var mentionToken by remember { mutableStateOf<MentionToken?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Подсказки ников при вводе @ник
    LaunchedEffect(field.text, field.selection) {
        val token = if (field.selection.collapsed) ForumIdentity.findMentionToken(field.text, field.selection.start) else null
        mentionToken = token
        if (token == null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(150)
        suggestions = ForumIdentity.searchNicks(token.prefix)
    }

    fun pickMention(picked: String) {
        val token = mentionToken ?: return
        val insert = (if (picked.any { it.isWhitespace() }) "@[$picked]" else "@$picked") + " "
        val newText = field.text.substring(0, token.start) + insert + field.text.substring(token.end)
        field = TextFieldValue(newText, TextRange(token.start + insert.length))
        suggestions = emptyList()
        mentionToken = null
    }

    fun scrollToPost(index: Int, number: Int) {
        scope.launch {
            listState.animateScrollToItem(index)
            highlighted = number
            delay(1500)
            highlighted = null
        }
    }

    LaunchedEffect(pendingPostId, state) {
        val id = pendingPostId ?: return@LaunchedEffect
        val ready = state as? PostsState.Ready ?: return@LaunchedEffect
        val index = ready.posts.indexOfFirst { it.id == id }
        if (index < 0) return@LaunchedEffect
        pendingPostId = null
        scrollToPost(index, ready.posts[index].number)
    }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = {
                        if (AppSettings.navStyle == "drawer") AppSettings.openDrawer?.invoke() else onBack()
                    }) {
                        if (AppSettings.navStyle == "drawer") Icon(Icons.Rounded.Menu, contentDescription = "Открыть меню")
                        else Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    NotificationBell(onOpen = { n ->
                        // Уведомление из этой же темы просто прокручивает к посту, из другой открывает её
                        if (n.topicId == vm.topicId) pendingPostId = n.postId else onOpenNotification(n)
                    })
                },
            )
        },
        bottomBar = {
            if (state is PostsState.Ready) {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        Modifier
                            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (suggestions.isNotEmpty()) {
                            MentionSuggestions(suggestions, onPick = ::pickMention)
                        }
                        if (myNick == null) {
                            OutlinedTextField(
                                value = nick,
                                onValueChange = { nick = it.take(30) },
                                label = { Text("Ваш ник") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            OutlinedTextField(
                                value = field,
                                onValueChange = {
                                    val t = it.text.take(2000)
                                    field = if (t.length == it.text.length) it else it.copy(text = t, selection = TextRange(t.length))
                                },
                                label = { Text(if (myNick != null) "Сообщение как $myNick" else "Сообщение") },
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                            )
                            FilledIconButton(
                                onClick = {
                                    sending = true
                                    vm.send(myNick ?: nick, text) { error ->
                                        sending = false
                                        if (error == null) field = TextFieldValue("")
                                        else Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = (myNick != null || nick.isNotBlank()) && text.isNotBlank() && !sending,
                                shape = MaterialShapes.Cookie12Sided.toShape(),
                                modifier = Modifier.padding(start = 8.dp).size(56.dp),
                            ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить") }
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (val s = state) {
                PostsState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                PostsState.NotConfigured -> Hint(
                    "Firebase не настроен. Положи google-services.json в папку app/ и пересобери проект.",
                    Modifier.align(Alignment.Center),
                )
                is PostsState.Error -> Hint(s.message, Modifier.align(Alignment.Center))
                is PostsState.Ready -> {
                    if (s.posts.isEmpty()) {
                        Hint("Пока нет сообщений", Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.posts, key = { it.id }) { p ->
                                PostCard(
                                    post = p,
                                    liked = p.id in liked,
                                    highlighted = highlighted == p.number,
                                    myNick = myNick,
                                    onNumberClick = {
                                        val newText = "#${p.number} $text"
                                        field = TextFieldValue(newText, TextRange(newText.length))
                                    },
                                    onLike = { vm.like(p.id) },
                                    onRefClick = { n ->
                                        val idx = s.posts.indexOfFirst { it.number == n }
                                        if (idx >= 0) scrollToPost(idx, n)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Подсказки ников над полем ввода: нажатие подставляет @ник в текст. */
@Composable
private fun MentionSuggestions(nicks: List<String>, onPick: (String) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            nicks.forEach { n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(n) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ForumAvatar(nickname = n, photoUrl = null, size = 28.dp)
                    Text(n, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    liked: Boolean,
    highlighted: Boolean,
    myNick: String?,
    onNumberClick: () -> Unit,
    onLike: () -> Unit,
    onRefClick: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForumAvatar(nickname = post.nickname, photoUrl = post.avatarUrl, size = 36.dp)
                Column(Modifier.weight(1f)) {
                    Text(post.nickname, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        post.timeMs?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
                            ?: "только что",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "#${post.number}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onNumberClick),
                )
            }
            MarkdownText(post.message, onRefClick, myNick = myNick)
            // Поставленный лайк заливается цветом акцента (и не выцветает, хотя кнопка уже неактивна)
            Button(
                onClick = onLike,
                enabled = !liked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (liked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            ) {
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Лайк",
                )
                Text("${post.likes}", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
