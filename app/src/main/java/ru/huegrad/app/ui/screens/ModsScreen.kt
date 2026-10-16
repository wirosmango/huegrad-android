package ru.huegrad.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ru.huegrad.app.data.Content
import ru.huegrad.app.data.ModFile

@Composable
fun ModsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SubPageScaffold("Доп. ресурсы", onBack) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 12.dp, bottom = pad.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Моды для сервера", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Forge 1.20.1 · ${Content.mods.size} файлов. Файлы скачаются в директорию Загрузки/huegrad-mods.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = {
                            Content.mods.forEach { enqueue(ctx, it) }
                            Toast.makeText(ctx, "Загрузка ${Content.mods.size} файлов начата", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Text("Скачать всё", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            item {
                ExpressiveListGroup {
                    Content.mods.forEachIndexed { index, mod ->
                        ExpressiveListItem(
                            title = mod.name,
                            subtitle = mod.size,
                            icon = Icons.Rounded.Download,
                            trailing = {
                                IconButton(onClick = {
                                    enqueue(ctx, mod)
                                    Toast.makeText(ctx, "Загрузка начата", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Rounded.Download, contentDescription = "Скачать") }
                            },
                            showDivider = index != Content.mods.lastIndex,
                        )
                    }
                }
            }
            item {
                ExpressiveListGroup {
                    ExpressiveListItem(
                        title = "world/",
                        subtitle = "Директория с бэкапами мира, открывать в браузере",
                        icon = Icons.Rounded.FolderOpen,
                        onClick = { uriHandler.openUri(Content.MODS_WORLD_URL) },
                    )
                }
            }
        }
    }
}

private fun enqueue(ctx: Context, mod: ModFile) {
    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(mod.url))
        .setTitle(mod.name)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "huegrad-mods/${mod.name}")
    dm.enqueue(request)
}
