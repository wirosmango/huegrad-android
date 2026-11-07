package ru.huegrad.app.ui.screens

import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Имя видео в app/src/main/res/raw (расширение не пишется): положите файл easter_video_q4x7.mp4 в эту папку.
 * Ищется по имени во время работы, поэтому проект собирается и без файла, а экран тогда покажет подсказку.
 */
private const val EASTER_VIDEO = "easter_video_q4x7"

/** Пасхалка: вертикальное видео на весь экран по центру, нажатие ставит на паузу, видео зациклено. */
@Composable
fun EasterEggScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resId = remember { context.resources.getIdentifier(EASTER_VIDEO, "raw", context.packageName) }
    var failed by remember { mutableStateOf(resId == 0) }
    var playing by remember { mutableStateOf(true) }
    val video = remember { VideoView(context) }

    // Видео не должно играть в фоне: пауза при сворачивании, освобождение при выходе с экрана
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, video) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && video.isPlaying) {
                video.pause()
                playing = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            video.stopPlayback()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (failed) {
            Text(
                "Видео не найдено.\nПоложите файл $EASTER_VIDEO.mp4 в app/src/main/res/raw/ и пересоберите проект.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        // VideoView сам вписывает видео по пропорциям, FrameLayout ставит его по центру
                        video.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER,
                        )
                        video.setVideoURI(Uri.parse("android.resource://${ctx.packageName}/$resId"))
                        video.setOnPreparedListener { mp ->
                            mp.isLooping = true
                            video.start()
                        }
                        video.setOnErrorListener { _, _, _ ->
                            failed = true
                            true
                        }
                        video.setOnClickListener {
                            if (video.isPlaying) {
                                video.pause()
                                playing = false
                            } else {
                                video.start()
                                playing = true
                            }
                        }
                        addView(video)
                    }
                },
            )
            if (!playing) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Продолжить",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.Center).size(72.dp),
                )
            }
        }

        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") }
    }
}
