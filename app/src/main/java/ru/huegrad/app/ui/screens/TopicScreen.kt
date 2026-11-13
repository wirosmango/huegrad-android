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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Poll
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.AnimatedVisibility
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
import ru.huegrad.app.forum.PollResult
import ru.huegrad.app.forum.TopicViewModel
import ru.huegrad.app.forum.UploadEvent
import ru.huegrad.app.ui.DeepLink
import ru.huegrad.app.ui.VideoCard
import ru.huegrad.app.forum.MAX_ATTACHMENTS
import ru.huegrad.app.forum.feedImageUrl
import ru.huegrad.app.forum.UploadStatus
import ru.huegrad.app.ui.postMedia
import ru.huegrad.app.ui.PlayBadge
import ru.huegrad.app.ui.MediaViewer
import ru.huegrad.app.ui.ForumMedia
import ru.huegrad.app.forum.videoThumbnailUrl
import ru.huegrad.app.forum.isVideoUrl
import androidx.compose.material.icons.rounded.Videocam
import ru.huegrad.app.ui.frostedGlass
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
    val attachments by vm.attachments.collectAsStateWithLifecycle()
    val uploading = attachments.any { it.status == UploadStatus.UPLOADING }
    // Полноэкранный просмотр медиа: список медиа сообщения и номер открытого
    var viewer by remember { mutableStateOf<Pair<List<ForumMedia>, Int>?>(null) }
    val pollResults by vm.pollResults.collectAsStateWithLifecycle()
    // Ник вошедшего закреплён за аккаунтом, гость вводит ник сам
    val myNick by ForumSession.nickname.collectAsStateWithLifecycle()
    val signedIn = rememberSignedIn()
    var showAccount by remember { mutableStateOf(false) }

    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val text = field.text
    var sending by remember { mutableStateOf(false) }
    // Выставляется при отправке своего сообщения: список прокрутится к нему, даже если читали старые
    var stickToEnd by remember { mutableStateOf(false) }
    // Матовое стекло: лента сообщений уходит под плавающую панель ввода и размывается под ней
    val hazeState = rememberHazeState()
    val glassInput = AppSettings.inputStyle == "floating" && AppSettings.navGlass
    // Загруженное фото ждёт отправки: превью над полем вместо ссылки в тексте
    var highlighted by remember { mutableStateOf<Int?>(null) }
    // Пост, к которому надо прокрутить (переход из уведомления)
    var pendingPostId by rememberSaveable { mutableStateOf(vm.initialPostId) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var mentionToken by remember { mutableStateOf<MentionToken?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Системный выбор фото (не требует разрешений)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_ATTACHMENTS)) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }

    // Готовое фото показывается превью над полем и уходит вместе с сообщением
    LaunchedEffect(Unit) {
        vm.uploadEvents.collect { event ->
            when (event) {
                is UploadEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
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

    // Новое сообщение внизу: прокручиваем к нему, если читаем самый низ или только что отправили своё
    val newestId = (state as? PostsState.Ready)?.posts?.firstOrNull()?.id
    LaunchedEffect(newestId) {
        if (newestId != null && (stickToEnd || listState.firstVisibleItemIndex <= 1)) {
            listState.animateScrollToItem(0)
        }
        stickToEnd = false
    }

    LaunchedEffect(pendingPostId, state) {
        val id = pendingPostId ?: return@LaunchedEffect
        val ready = state as? PostsState.Ready ?: return@LaunchedEffect
        val index = ready.posts.indexOfFirst { it.id == id }
        if (index < 0) return@LaunchedEffect
        pendingPostId = null
        scrollToPost(index, ready.posts[index].number)
    }

    if (showAccount) AccountDialog(onDismiss = { showAccount = false })
    viewer?.let { (items, start) -> MediaViewer(items, start, onDismiss = { viewer = null }) }

    // Создание опроса из меню вложений
    var showPoll by remember { mutableStateOf(false) }
    if (showPoll) {
        PollDialog(
            onDismiss = { showPoll = false },
            onCreate = { question, options ->
                if (myNick == null) {
                    Toast.makeText(context, "Писать могут только вошедшие в аккаунт", Toast.LENGTH_SHORT).show()
                } else {
                    showPoll = false
                    sending = true
                    stickToEnd = true
                    vm.sendPoll(myNick.orEmpty(), question, options) { error ->
                        sending = false
                        if (error != null) {
                            stickToEnd = false
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
        )
    }

    // Профиль автора: открывается по нажатию на аватар или ник в сообщении
    var profileOf by remember { mutableStateOf<Post?>(null) }
    profileOf?.let { author ->
        ForumProfileSheet(
            nickname = author.nickname,
            avatarUrl = author.avatarUrl,
            authorUid = author.authorUid,
            onDismiss = { profileOf = null },
            onOpenPost = { target ->
                profileOf = null
                val posts = (state as? PostsState.Ready)?.posts.orEmpty()
                val idx = posts.indexOfFirst { it.id == target.postId }
                if (target.topicId == vm.topicId && idx >= 0) {
                    scrollToPost(idx, posts[idx].number)
                } else {
                    DeepLink.pending = DeepLink.Target(target.topicId, target.postId, null)
                }
            },
        )
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
            if (state is PostsState.Ready && !signedIn) {
                GuestPromptBar(onSignIn = { showAccount = true })
            } else if (state is PostsState.Ready) {
                // "floating": кнопки и поле парят над содержимым по отдельности; "minimal": плоская панель без рамок
                val floating = AppSettings.inputStyle == "floating"
                // В AMOLED-теме панель чисто чёрная: тональная подсветка поверхности (3dp) осветлила бы её до серого
                val amoled = AppSettings.themeMode == "amoled"
                Surface(
                    color = if (floating) Color.Transparent else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (floating || amoled) 0.dp else 3.dp,
                ) {
                    Column(
                        Modifier
                            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                            .padding(start = 12.dp, end = 12.dp, top = if (floating) 8.dp else 6.dp, bottom = if (floating) 8.dp else 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (suggestions.isNotEmpty()) {
                            MentionSuggestions(suggestions, onPick = ::pickMention)
                        }
                        if (attachments.isNotEmpty()) {
                            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                attachments.forEach { att ->
                                    Box(Modifier.size(72.dp)) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(MaterialTheme.shapes.medium)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            val done = att.result
                                            val model: Any? = when {
                                                done != null -> if (done.isVideo) videoThumbnailUrl(done.url) else feedImageUrl(done.url)
                                                !att.isVideo -> att.previewUri
                                                else -> null
                                            }
                                            if (model != null) {
                                                AsyncImage(
                                                    model = model,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                            if (att.isVideo) PlayBadge(Modifier.size(32.dp))
                                            if (att.status == UploadStatus.UPLOADING) {
                                                Box(
                                                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    CircularProgressIndicator(
                                                        progress = { att.progress },
                                                        modifier = Modifier.size(28.dp),
                                                        strokeWidth = 2.5.dp,
                                                        color = Color.White,
                                                    )
                                                }
                                            }
                                        }
                                        FilledTonalIconButton(
                                            onClick = { vm.removeAttachment(att.id) },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(24.dp),
                                        ) { Icon(Icons.Rounded.Close, contentDescription = "Убрать вложение", Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }
                        val gap = if (floating) 8.dp else 4.dp
                        Row(verticalAlignment = if (floating) Alignment.Bottom else Alignment.CenterVertically) {
                            ComposerBox(floating, hazeState, CircleShape, Modifier.size(if (floating) 56.dp else 40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    run {
                                        // Меню вложений: фото, видео (только вошедшим) и опрос
                                        var attachMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { attachMenu = true }) {
                                                Icon(
                                                    // В минималистичной панели иконки Solar (outline), в плавающей прежние
                                                    painterResource(if (floating) R.drawable.ic_attach_plus else R.drawable.ic_solar_paperclip),
                                                    contentDescription = "Прикрепить",
                                                    modifier = Modifier.size(if (floating) 24.dp else 22.dp),
                                                    tint = if (floating) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text("Фото")
                                                            if (myNick == null) {
                                                                Text(
                                                                    "только после входа в аккаунт",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                )
                                                            }
                                                        }
                                                    },
                                                    leadingIcon = {
                                                        if (floating) {
                                                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                                                        } else {
                                                            Icon(painterResource(R.drawable.ic_solar_gallery_add), contentDescription = null)
                                                        }
                                                    },
                                                    enabled = myNick != null && attachments.size < MAX_ATTACHMENTS,
                                                    onClick = {
                                                        attachMenu = false
                                                        if (CloudinaryUploader.isConfigured) {
                                                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Cloudinary не настроен: добавьте cloudinary.cloudName и cloudinary.uploadPreset в local.properties",
                                                                Toast.LENGTH_LONG,
                                                            ).show()
                                                        }
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Видео") },
                                                    leadingIcon = {
                                                        if (floating) {
                                                            Icon(Icons.Rounded.Videocam, contentDescription = null)
                                                        } else {
                                                            Icon(painterResource(R.drawable.ic_solar_clapperboard_play), contentDescription = null)
                                                        }
                                                    },
                                                    enabled = myNick != null && attachments.size < MAX_ATTACHMENTS,
                                                    onClick = {
                                                        attachMenu = false
                                                        if (CloudinaryUploader.isConfigured) {
                                                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Cloudinary не настроен: добавьте cloudinary.cloudName и cloudinary.uploadPreset в local.properties",
                                                                Toast.LENGTH_LONG,
                                                            ).show()
                                                        }
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Опрос") },
                                                    leadingIcon = {
                                                        if (floating) {
                                                            Icon(Icons.Rounded.Poll, contentDescription = null)
                                                        } else {
                                                            Icon(painterResource(R.drawable.ic_solar_chart_square), contentDescription = null)
                                                        }
                                                    },
                                                    onClick = {
                                                        attachMenu = false
                                                        showPoll = true
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            ComposerBox(floating, hazeState, RoundedCornerShape(12.dp), Modifier.weight(1f).padding(start = gap)) {
                                val onFieldChange: (TextFieldValue) -> Unit = {
                                    val t = it.text.take(2000)
                                    field = if (t.length == it.text.length) it else it.copy(text = t, selection = TextRange(t.length))
                                }
                                val hint = if (myNick != null) "Сообщение" else "Текст..."
                                if (floating) {
                                    TextField(
                                        value = field,
                                        onValueChange = onFieldChange,
                                        placeholder = { Text(hint) },
                                        colors = composerFieldColors(),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 4,
                                    )
                                } else {
                                    CompactField(field, onFieldChange, hint, Modifier.fillMaxWidth(), maxLines = 4)
                                }
                            }
                            val send: () -> Unit = {
                                sending = true
                                stickToEnd = true
                                vm.send(myNick.orEmpty(), text) { error ->
                                    sending = false
                                    if (error == null) {
                                        field = TextFieldValue("")
                                    }
                                    else {
                                        stickToEnd = false
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            val canSend = myNick != null && (text.isNotBlank() || attachments.isNotEmpty()) && !sending && !uploading
                            // Кнопка отправки скрыта, пока в поле нет текста (и нет фото), и плавно появляется с первым символом
                            val hasInput = text.isNotBlank() || attachments.isNotEmpty()
                            AnimatedVisibility(
                                visible = hasInput,
                                enter = fadeIn() + scaleIn(initialScale = 0.6f) + expandHorizontally(expandFrom = Alignment.Start),
                                exit = fadeOut() + scaleOut(targetScale = 0.6f) + shrinkHorizontally(shrinkTowards = Alignment.Start),
                            ) {
                                Row {
                                    Spacer(Modifier.width(gap))
                                    if (floating) {
                                        FilledIconButton(
                                            onClick = send,
                                            enabled = canSend,
                                            shape = MaterialShapes.Cookie12Sided.toShape(),
                                            modifier = Modifier.size(56.dp),
                                        ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить") }
                                    } else {
                                        FilledIconButton(
                                            onClick = send,
                                            enabled = canSend,
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.ic_solar_plain),
                                                contentDescription = "Отправить",
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(
            Modifier
                .padding(top = pad.calculateTopPadding(), bottom = if (glassInput) 0.dp else pad.calculateBottomPadding())
                .fillMaxSize()
                .then(if (glassInput) Modifier.hazeSource(hazeState) else Modifier),
        ) {
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
                            // Как в мессенджерах: самые новые сообщения снизу, прокрутка вверх открывает более старые
                            reverseLayout = true,
                            // Под стеклянной панелью список идёт до самого низа, поэтому последний пост приподнят на её высоту
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp + if (glassInput) pad.calculateBottomPadding() else 0.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.posts, key = { it.id }) { p ->
                                PostCard(
                                    post = p,
                                    liked = p.id in liked,
                                    highlighted = highlighted == p.number,
                                    myNick = myNick,
                                    pollResult = pollResults[p.id],
                                    onVote = { option ->
                                        // Голосовать могут только вошедшие: гостю открывается окно входа
                                        if (!signedIn) {
                                            showAccount = true
                                        } else {
                                            vm.vote(p.id, option) { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                                        }
                                    },
                                    onNumberClick = {
                                        val newText = "#${p.number} $text"
                                        field = TextFieldValue(newText, TextRange(newText.length))
                                    },
                                    onOpenMedia = { items, index -> viewer = items to index },
                                    onProfileClick = { profileOf = p },
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

/** Вместо панели ввода для гостя: писать могут только вошедшие в аккаунт. */
@Composable
private fun GuestPromptBar(onSignIn: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (AppSettings.themeMode == "amoled") 0.dp else 3.dp,
    ) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Войдите, чтобы писать сообщения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSignIn) { Text("Войти") }
        }
    }
}

/** Подложка элемента панели ввода: у "плавающей" панели белая плашка с тенью, у "минимализма" её нет. */
@Composable
private fun ComposerBox(
    floating: Boolean,
    haze: HazeState,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (floating && AppSettings.navGlass) {
        // Матовое стекло: фон ленты размывается под плашкой (см. frostedGlass)
        Box(modifier.frostedGlass(haze, shape)) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
        }
    } else if (floating) {
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

/** Низкое поле ввода для «минимализма»: без внутренних отступов M3 TextField (у него минимум 56dp). */
@Composable
private fun CompactField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        maxLines = maxLines,
        decorationBox = { inner -> CompactDecoration(value.text.isEmpty(), placeholder, inner) },
    )
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = singleLine,
        decorationBox = { inner -> CompactDecoration(value.isEmpty(), placeholder, inner) },
    )
}

@Composable
private fun CompactDecoration(empty: Boolean, placeholder: String, inner: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (empty) {
            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        inner()
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
    pollResult: PollResult?,
    onVote: (Int) -> Unit,
    onOpenMedia: (List<ForumMedia>, Int) -> Unit,
    onNumberClick: () -> Unit,
    onProfileClick: () -> Unit,
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
                Row(
                    Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(onClick = onProfileClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                }
                Text(
                    "#${post.number}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onNumberClick),
                )
            }
            // Все картинки и видео сообщения: по нажатию открывается галерея на нужном месте
            val media = remember(post.message, post.images, post.attachments) { postMedia(post.message, post.images, post.attachments) }
            val openMedia: (String) -> Unit = { url -> onOpenMedia(media, media.indexOfFirst { it.url == url }.coerceAtLeast(0)) }
            // У опроса message дублирует вопрос и варианты для сайта, в приложении рисуется карточка опроса
            if (post.poll != null) {
                PollCard(post.poll, pollResult, onVote)
            } else if (post.message.isNotBlank()) {
                MarkdownText(post.message, onRefClick, myNick = myNick, onMediaClick = openMedia)
            }
            post.images.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { openMedia(url) },
                )
            }
            // Вложения как на сайте: фото уменьшенной копией, видео карточкой с превью
            post.attachments.forEach { a ->
                if (a.isVideo) {
                    VideoCard(a.url, onClick = { openMedia(a.url) })
                } else {
                    AsyncImage(
                        model = feedImageUrl(a.url),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { openMedia(a.url) },
                    )
                }
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
