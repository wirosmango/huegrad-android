package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.runtime.remember
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.MediaImage
import ru.huegrad.app.data.Content
import ru.huegrad.app.data.Site

@Composable
fun DrochkaScreen(onBack: () -> Unit) {
    SubPageScaffold("Дрочка", onBack) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Выберите игрока, с которым можно подрочить на что-то",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Content.drochka) { e ->
                ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                    DrochkaTopCroppedImage(
                        file = e.image,
                        contentDescription = e.nick,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(e.nick, style = MaterialTheme.typography.titleLarge)
                        Text(e.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}


@Composable
private fun DrochkaTopCroppedImage(
    file: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val resId = remember(file, resources) {
        resources.getIdentifier(Site.resName(file), "drawable", context.packageName)
    }
    if (resId != 0) {
        AsyncImage(
            model = resId,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = modifier,
        )
    } else {
        MediaImage(
            file = file,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
