package ru.huegrad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ru.huegrad.app.data.Site

/**
 * Картинка из res/drawable-nodpi по имени файла сайта ("PTU.png" -> m_ptu).
 * Если ресурса нет, показывается заглушка с иконкой: сразу видно, какого файла не хватает.
 */
@Composable
fun MediaImage(
    file: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val packageName = LocalContext.current.packageName
    val resources = LocalResources.current
    val resId = remember(file, resources) {
        resources.getIdentifier(Site.resName(file), "drawable", packageName)
    }
    if (resId != 0) {
        AsyncImage(
            model = resId,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .heightIn(min = 120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.BrokenImage, contentDescription = "Нет файла ${Site.resName(file)}")
        }
    }
}
