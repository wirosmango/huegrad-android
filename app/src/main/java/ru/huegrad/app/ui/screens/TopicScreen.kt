@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import ru.huegrad.app.R
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
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
import ru.huegrad.app.forum.CloudinaryUploader
import ru.huegrad.app.forum.ForumIdentity
import ru.huegrad.app.forum.ForumSession
import ru.huegrad.app.forum.MentionToken
import ru.huegrad.app.forum.Post
import ru.huegrad.app.forum.PostsState
import ru.huegrad.app.forum.TopicViewModel
import ru.huegrad.app.forum.UploadEvent
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
    val uploading by vm.uploading.collectAsStateWithLifecycle()
    // Ник вошедшего закреплён за аккаунтом, гость вводит ник сам
    val myNick by ForumSession.nickname.collectAsStateWithLifecycle()

    var nick by rememberSaveable { mutableStateOf("Аноним") }
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val text = field.text
    var sending by remember { mutableStateOf(false) }
    // Загруженное фото ждёт отправки: превью над полем вместо ссылки в тексте
    var attachedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var highlighted by remember { mutableStateOf<Int?>(null) }
    // Пост, к которому надо прокрутить (переход из уведомления)
    var pendingPostId by rememberSaveable { mutableStateOf(vm.initialPostId) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var mentionToken by remember { mutableStateOf<MentionToken?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Системный выбор фото (не требует разрешений)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.uploadImage(uri)
    }

    // Готовое фото показывается превью над полем и уходит вместе с сообщением
    LaunchedEffect(Unit) {
        vm.uploadEvents.collect { event ->
            when (event) {
                is UploadEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is UploadEvent.Done -> attachedUrl = event.url
            }
        }
    }

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
                // "floating": кнопки и поле парят над содержимым по отдельности; "minimal": плоская панель без рамок
                val floating = AppSettings.inputStyle == "floating"
                Surface(
                    color = if (floating) Color.Transparent else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (floating) 0.dp else 3.dp,
                ) {
                    Column(
                        Modifier
                            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                            .padding(horizontal = 12.dp, vertical = if (floating) 8.dp else 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (suggestions.isNotEmpty()) {
                            MentionSuggestions(suggestions, onPick = ::pickMention)
                        }
                        if (myNick == null) {
                            ComposerBox(floating, RoundedCornerShape(24.dp), Modifier.fillMaxWidth()) {
                                TextField(
                                    value = nick,
                                    onValueChange = { nick = it.take(30) },
                                    placeholder = { Text("Ваш ник") },
                                    singleLine = true,
                                    colors = composerFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        attachedUrl?.let { url ->
                            Box(Modifier.padding(top = 4.dp)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Прикреплённое фото",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.medium),
                                )
                                FilledTonalIconButton(
                                    onClick = { attachedUrl = null },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                                ) { Icon(Icons.Rounded.Close, contentDescription = "Убрать фото", Modifier.size(16.dp)) }
                            }
                        }
                        Row(
                            verticalAlignment = if (floating) Alignment.Bottom else Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (floating) 8.dp else 4.dp),
                        ) {
                            ComposerBox(floating, CircleShape, Modifier.size(if (floating) 56.dp else 40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (uploading) {
                                        CircularProgressIndicator(Modifier.size(if (floating) 24.dp else 20.dp), strokeWidth = 2.5.dp)
                                    } else {
                                        IconButton(onClick = {
                                            if (CloudinaryUploader.isConfigured) {
                                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Cloudinary не настроен: добавьте cloudinary.cloudName и cloudinary.uploadPreset в local.properties",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        }) {
                                            Icon(
                                                painterResource(R.drawable.ic_attach_plus),
                                                contentDescription = "Прикрепить фото",
                                                modifier = Modifier.size(if (floating) 24.dp else 18.dp),
                                                tint = if (floating) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                            ComposerBox(floating, RoundedCornerShape(24.dp), Modifier.weight(1f)) {
                                TextField(
                                    value = field,
                                    onValueChange = {
                                        val t = it.text.take(2000)
                                        field = if (t.length == it.text.length) it else it.copy(text = t, selection = TextRange(t.length))
                                    },
                                    placeholder = { Text(if (myNick != null) "Сообщение" else "Текст...") },
                                    colors = composerFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(x = if (floating) 0.dp else (-12).dp),
                                    maxLines = 4,
                                )
                            }
                            val send: () -> Unit = {
                                sending = true
                                vm.send(myNick ?: nick, text, attachedUrl) { error ->
                                    sending = false
                                    if (error == null) {
                                        field = TextFieldValue("")
                                        attachedUrl = null
                                    }
                                    else Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            }
                            val canSend = (myNick != null || nick.isNotBlank()) && (text.isNotBlank() || attachedUrl != null) && !sending && !uploading
                            if (floating) {
                                FilledIconButton(
                                    onClick = send,
                                    enabled = canSend,
                                    shape = MaterialShapes.Cookie12Sided.toShape(),
                                    modifier = Modifier.size(56.dp),
                                ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить") }
                            } else {
                                IconButton(
                                    onClick = send,
                                    enabled = canSend,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Отправить",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
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

/** Подложка элемента панели ввода: у "плавающей" панели белая плашка с тенью, у "минимализма" её нет. */
@Composable
private fun ComposerBox(floating: Boolean, shape: Shape, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (floating) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
            modifier = modifier,
            content = content,
        )
    } else {
        Box(modifier) { content() }
    }
}

/** Поле ввода без рамки и подчёркивания: подложку рисует [ComposerBox]. */
@Composable
private fun composerFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
)

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
            if (post.message.isNotBlank()) MarkdownText(post.message, onRefClick, myNick = myNick)
            post.images.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                )
            }
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
