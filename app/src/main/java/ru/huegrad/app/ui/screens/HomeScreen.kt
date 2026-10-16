@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.MediaImage
import kotlinx.coroutines.launch
import ru.huegrad.app.data.Content
import ru.huegrad.app.data.Admin
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.animation.animateContentSize
import ru.huegrad.app.data.Site
import ru.huegrad.app.ui.NoInsets
import ru.huegrad.app.ui.AppSettings

@Composable
fun HomeScreen() {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = NoInsets,
        topBar = {
            LargeTopAppBar(
                title = { Text("Хуеград") },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (AppSettings.navStyle == "drawer") {
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.SwipeRight, contentDescription = null)
                            Text(
                                text = "Пролистните вправо, чтобы открыть меню",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
            item { ServerCard(Modifier.padding(horizontal = 16.dp)) }

            item {
                Text(
                    "Администрация",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                ExpressiveListGroup(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Content.admins.forEachIndexed { index, admin ->
                        AdminListItem(
                            admin = admin,
                            showDivider = index != Content.admins.lastIndex,
                        )
                    }
                }
            }

            item {
                Text(
                    "Красоты Хуеграда",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { Gallery() }
        }
    }
}

@Composable
private fun ServerCard(modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Приветствуем!", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Официальная страница лучшего сервера на свете",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(Site.SERVER_HOST, style = MaterialTheme.typography.titleMedium)
            Text(
                "${Site.SERVER_IP} · Версия: ${Site.SERVER_VERSION}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("server", Site.SERVER_HOST))
                        )
                        copied = true
                    }
                },
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Text(
                    if (copied) "Скопировано" else "Скопировать адрес",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Gallery() {
    val items = Content.gallery
    val state = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 220.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { index ->
        MediaImage(
            file = items[index].file,
            contentDescription = items[index].caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge),
        )
    }
}

/** Строка администрации в стиле сгруппированных настроек Android 16/17.
 * Нажатие раскрывает полное описание прямо внутри общего списка.
 */
@Composable
private fun AdminListItem(admin: Admin, showDivider: Boolean) {
    var expanded by rememberSaveable(admin.nick) { mutableStateOf(false) }

    Column(Modifier.animateContentSize()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(admin.nick, style = MaterialTheme.typography.titleMedium)
                if (!expanded) {
                    Text(
                        admin.role.replace('\n', ' ').let {
                            if (it.length > 100) it.take(100).trimEnd() + "…" else it
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Свернуть описание" else "Развернуть описание",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (expanded) {
            Text(
                text = admin.role,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}
