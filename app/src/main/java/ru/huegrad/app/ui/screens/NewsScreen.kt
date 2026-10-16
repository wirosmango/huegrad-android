@file:OptIn(ExperimentalMaterial3Api::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.huegrad.app.data.Content
import ru.huegrad.app.ui.NoInsets

@Composable
fun NewsScreen(onOpen: (String) -> Unit) {
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Новости Хуеграда") }) },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 12.dp, bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ExpressiveListGroup {
                    Content.news.forEachIndexed { index, item ->
                        ExpressiveListItem(
                            title = item.ref.title,
                            subtitle = item.time,
                            icon = Icons.Rounded.Article,
                            onClick = { onOpen(item.ref.id) },
                            showDivider = index != Content.news.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
