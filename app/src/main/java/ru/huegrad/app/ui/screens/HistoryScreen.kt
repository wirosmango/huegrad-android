@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.MediaImage
import ru.huegrad.app.data.Content
import ru.huegrad.app.data.GalleryItem
import ru.huegrad.app.data.HistoryBlock

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    SubPageScaffold("История Хуеграда", onBack) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Content.history.forEach { section ->
                item(key = "h-${section.title}") {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                    )
                }
                section.blocks.forEachIndexed { i, block ->
                    item(key = "b-${section.title}-$i") { BlockCard(block) }
                }
            }
        }
    }
}

@Composable
private fun BlockCard(block: HistoryBlock) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            block.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        when (block.images.size) {
            0 -> Unit
            1 -> MediaImage(
                file = block.images[0].file,
                contentDescription = block.images[0].caption.ifBlank { null },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
            )
            else -> ImageCarousel(block.images)
        }
    }
}

@Composable
private fun ImageCarousel(items: List<GalleryItem>) {
    val state = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 260.dp,
        modifier = Modifier.fillMaxWidth().height(200.dp),
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { index ->
        MediaImage(
            file = items[index].file,
            contentDescription = items[index].caption.ifBlank { null },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().maskClip(MaterialTheme.shapes.extraLarge),
        )
    }
}
