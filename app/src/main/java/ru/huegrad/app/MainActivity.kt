package ru.huegrad.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import ru.huegrad.app.forum.AppForeground
import ru.huegrad.app.forum.ForumSession
import ru.huegrad.app.forum.MentionService
import ru.huegrad.app.ui.AppSettings
import ru.huegrad.app.ui.DeepLink
import ru.huegrad.app.ui.HuegradApp
import ru.huegrad.app.ui.theme.HuegradTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppSettings.load(this)
        ForumSession.start(application)
        // Запуск по нажатию на уведомление; при повороте экрана Intent приходит снова, его повторно не читаем
        if (savedInstanceState == null) DeepLink.handle(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            HuegradTheme(
                themeMode = AppSettings.themeMode,
                accent = AppSettings.accent,
                fontMode = AppSettings.fontMode,
            ) {
                HuegradApp()
            }
        }
    }

    /** Приложение уже открыто, а нажали на уведомление: открываем нужную тему. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLink.handle(intent)
    }

    override fun onStart() {
        super.onStart()
        AppForeground.visible = true
        MentionService.sync(this) // из видимого приложения запуск foreground-сервиса разрешён
    }

    override fun onStop() {
        AppForeground.visible = false
        super.onStop()
    }
}
