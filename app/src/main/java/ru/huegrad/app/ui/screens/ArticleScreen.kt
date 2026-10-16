@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material3.CardDefaults
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.huegrad.app.data.ArticleBlock
import ru.huegrad.app.data.ArticleParser
import ru.huegrad.app.data.ArticleRef

private sealed interface ArticleState {
    data object Loading : ArticleState
    data class Ready(val blocks: List<ArticleBlock>) : ArticleState
    data class Failed(val message: String) : ArticleState
}

@Composable
fun ArticleScreen(ref: ArticleRef, onBack: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<ArticleState>(ArticleState.Loading, ref.url, attempt) {
        value = ArticleState.Loading
        value = withContext(Dispatchers.IO) { ArticleParser.load(ref.url) }.fold(
            onSuccess = { if (it.isEmpty()) ArticleState.Failed("Страница пуста") else ArticleState.Ready(it) },
            onFailure = { ArticleState.Failed(it.localizedMessage ?: "Не удалось загрузить") },
        )
    }
    val uriHandler = LocalUriHandler.current

    SubPageScaffold(ref.title, onBack) { pad ->
        Box(Modifier.fillMaxSize()) {
            when (val s = state) {
                ArticleState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is ArticleState.Failed -> Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        s.message,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { attempt++ }) { Text("Повторить") }
                    OutlinedButton(onClick = { uriHandler.openUri(ref.url) }) { Text("Открыть на сайте") }
                }
                is ArticleState.Ready -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(s.blocks) { _, block -> BlockView(block) }

                    if (ref.id == "edu-ptu" || ref.id == "edu-htu") {
                        item {
                            AdmissionCard(
                                onOpen = { uriHandler.openUri(ref.url) },
                            )
                        }
                    } else {
                        item {
                            TextButton(onClick = { uriHandler.openUri(ref.url) }) {
                                Text("Открыть на сайте")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdmissionCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Анкета для поступления",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Заполните анкету на сайте учебного заведения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Открыть на сайте")
            }
        }
    }
}

@Composable
private fun BlockView(block: ArticleBlock) {
    when (block) {
        is ArticleBlock.Heading -> Text(
            block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            modifier = Modifier.padding(top = 8.dp),
        )
        is ArticleBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
        is ArticleBlock.Bullet -> Row {
            Text("•  ", style = MaterialTheme.typography.bodyLarge)
            Text(block.text, style = MaterialTheme.typography.bodyLarge)
        }
        is ArticleBlock.Image -> AsyncImage(
            model = block.url,
            contentDescription = block.alt.ifBlank { null },
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
        )
    }
}
