@file:OptIn(ExperimentalMaterial3Api::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.NoInsets
import ru.huegrad.app.ui.AppSettings

/** Экран вложенного уровня: верхняя панель со стрелкой «назад», без нижней навигации. */
@Composable
fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (AppSettings.navStyle == "drawer") {
                        FilledTonalIconButton(onClick = { AppSettings.openDrawer?.invoke() }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Открыть меню")
                        }
                    } else {
                        FilledTonalIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
            )
        },
    ) { pad ->
        content(PaddingValues(top = pad.calculateTopPadding(), bottom = navBottom + 16.dp))
    }
}
