@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.huegrad.app.data.Content
import ru.huegrad.app.playback.RadioViewModel
import ru.huegrad.app.ui.NoInsets

@Composable
fun RadioTracksScreen(onBack: () -> Unit, vm: RadioViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Треки Huegrad FM") },
                navigationIcon = {
                    IconButton(onClick = onBack, colors = IconButtonDefaults.filledTonalIconButtonColors()) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 12.dp, bottom = 16.dp,
            ),
        ) {
            item {
                ExpressiveListGroup {
                    Content.tracks.forEachIndexed { index, track ->
                        ExpressiveListItem(
                            title = track.title,
                            subtitle = track.artist,
                            icon = if (index == state.currentIndex) Icons.Rounded.PlayArrow else Icons.Rounded.MusicNote,
                            selected = index == state.currentIndex,
                            onClick = { vm.playTrack(index) },
                            showDivider = index != Content.tracks.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
