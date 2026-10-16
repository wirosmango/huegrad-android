@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)

package ru.huegrad.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import ru.huegrad.app.data.Content
import ru.huegrad.app.playback.RadioViewModel
import ru.huegrad.app.ui.LocalBottomBarInset
import ru.huegrad.app.ui.NoInsets

/** Всё, что в плеере занимает место кроме обложки (отступы, название, ползунок, кнопки), в dp. */
private val PlayerChromeHeight = 356.dp

@Composable
fun RadioScreen(onOpenTracks: () -> Unit, vm: RadioViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs) else 0f
    // Позиция приходит часто, линейная анимация между обновлениями убирает «дёргание»
    val smoothFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "progress",
    )
    // Волна «дышит», пока играет музыка, и выпрямляется на паузе или во время перемотки
    val amplitude by animateFloatAsState(
        targetValue = if (s.isPlaying && dragging == null) 1f else 0f,
        label = "waveAmplitude",
    )
    val trackUri = Content.tracks.getOrNull(s.currentIndex)?.uri
    val isFavorite = trackUri != null && trackUri in favorites
    val position = if (dragging != null && s.durationMs > 0) (s.durationMs * dragging!!).toLong() else s.positionMs

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Сейчас играет",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Huegrad FM", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                },
            )
        },
    ) { pad ->
        BoxWithConstraints(Modifier.padding(pad).fillMaxSize()) {
            val bottomInset = LocalBottomBarInset.current
            // Обложка занимает то, что осталось от экрана, но не шире экрана и не меньше 160 dp
            val coverSize = (maxHeight - bottomInset - PlayerChromeHeight)
                .coerceAtMost(maxWidth - 48.dp)
                .coerceAtLeast(160.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = bottomInset + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Cover(artwork = s.artwork, playing = s.isPlaying, modifier = Modifier.size(coverSize))

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.title.ifBlank { "Загрузка…" },
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            // Длинное название прокручивается, а не обрезается
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(
                            s.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { trackUri?.let(vm::toggleFavorite) }) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                val interaction = remember { MutableInteractionSource() }
                Slider(
                    value = dragging ?: smoothFraction,
                    onValueChange = { dragging = it },
                    onValueChangeFinished = {
                        dragging?.let(vm::seekToFraction)
                        dragging = null
                    },
                    enabled = s.durationMs > 0,
                    interactionSource = interaction,
                    // Ползунок в виде узкой вертикальной пилюли, как в референсе
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interaction,
                            thumbSize = DpSize(4.dp, 36.dp),
                        )
                    },
                    track = { state ->
                        val range = state.valueRange
                        val progress = ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                        LinearWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            amplitude = { amplitude },
                        )
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        formatTime(position),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "−" + formatTime((s.durationMs - position).coerceAtLeast(0)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = vm::toggleShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Перемешать",
                            tint = if (s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalIconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, "Предыдущий", Modifier.size(28.dp))
                    }
                    // Кнопка play/pause: на паузе круглая, во время игры скруглённый квадрат
                    val corner by animateDpAsState(if (s.isPlaying) 28.dp else 40.dp, label = "playCorner")
                    FilledIconButton(
                        onClick = vm::togglePlay,
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(corner),
                    ) {
                        Icon(
                            if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (s.isPlaying) "Пауза" else "Играть",
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    FilledTonalIconButton(onClick = vm::next, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.SkipNext, "Следующий", Modifier.size(28.dp))
                    }
                    IconButton(onClick = vm::cycleRepeat) {
                        Icon(
                            if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Повтор",
                            tint = if (s.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    PillButton(
                        icon = Icons.Rounded.Speed,
                        label = speedLabel(s.speed),
                        onClick = vm::cycleSpeed,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 28.dp),
                        modifier = Modifier.weight(1f),
                    )
                    PillButton(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Далее",
                        onClick = onOpenTracks,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 6.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Обложка трека: картинка, если она есть, иначе «печенье» с нотой, которое пульсирует во время игры. */
@Composable
private fun Cover(artwork: Uri?, playing: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(36.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        PulsingCookie(playing = playing, modifier = Modifier.fillMaxSize(0.8f))
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun PulsingCookie(playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cookiePulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cookiePulseScale",
    )
    // На паузе пульсация плавно затухает
    val blend by animateFloatAsState(if (playing) 1f else 0f, label = "cookieBlend")
    val scaleValue = 1f + (pulse - 1f) * blend

    // Цвета берём заранее, вне Canvas, потому что MaterialTheme — @Composable.
    val cookieFill = MaterialTheme.colorScheme.surfaceContainerHighest
    val noteColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.5f
            val path = Path()
            val lobes = 12
            val pointsPerLobe = 12
            val totalPoints = lobes * pointsPerLobe

            // Плавная радиальная волна: 12 округлых выступов без острых зубцов (Cookie12Sided)
            for (i in 0..totalPoints) {
                val angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i / totalPoints)
                val wave = (0.5 + 0.5 * kotlin.math.cos(lobes * angle)).toFloat()
                val smoothWave = wave * wave * (3f - 2f * wave)
                val r = radius * (0.92f + 0.08f * smoothWave)
                val point = Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * r,
                    y = center.y + kotlin.math.sin(angle).toFloat() * r,
                )
                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()

            scale(scaleValue, pivot = center) { drawPath(path = path, color = cookieFill) }
        }
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = noteColor,
            modifier = Modifier.size(72.dp),
        )
    }
}

private fun speedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')
    return "$text×"
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
