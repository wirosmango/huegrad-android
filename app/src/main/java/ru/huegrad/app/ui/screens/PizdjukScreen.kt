package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.MediaImage
import ru.huegrad.app.data.Site

@Composable
fun PizdjukScreen(onBack: () -> Unit) {
    SubPageScaffold("Пиздюк", onBack) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            MediaImage(
                file = "pizdjuk2.jpg",
                contentDescription = "Пиздюк",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge),
            )
        }
    }
}
