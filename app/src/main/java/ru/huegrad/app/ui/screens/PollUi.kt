package ru.huegrad.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.huegrad.app.forum.Poll
import ru.huegrad.app.forum.PollResult
import kotlin.math.roundToInt

/** Опрос в сообщении: до голосования варианты-кнопки, после голосования полосы с процентами. */
@Composable
internal fun PollCard(poll: Poll, result: PollResult?, onVote: (Int) -> Unit) {
    val counts = result?.counts ?: List(poll.options.size) { 0 }
    val total = counts.sum()
    val myVote = result?.myVote
    val voted = myVote != null

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("📊 ${poll.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        poll.options.forEachIndexed { i, option ->
            val share = if (total == 0) 0f else counts.getOrElse(i) { 0 }.toFloat() / total
            val mine = myVote == i
            Surface(
                onClick = { onVote(i) },
                enabled = !voted,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = if (mine) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box {
                    if (voted) {
                        Box(Modifier.matchParentSize()) {
                            Box(
                                Modifier
                                    .fillMaxWidth(share)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (mine) Icon(Icons.Rounded.Check, contentDescription = "Ваш голос", modifier = Modifier.size(18.dp))
                        Text(option, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (voted) {
                            Text(
                                "${(share * 100).roundToInt()}% · ${counts.getOrElse(i) { 0 }}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
        Text(
            if (voted) "Всего голосов: $total" else "Голосов: $total. Выберите вариант, чтобы проголосовать",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Создание опроса: вопрос и от 2 до 6 вариантов. */
@Composable
internal fun PollDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var question by rememberSaveable { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    val filled = options.map { it.trim() }.filter { it.isNotEmpty() }
    val valid = question.isNotBlank() && filled.distinct().size >= 2
    val removable = options.size > 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый опрос") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it.take(150) },
                    label = { Text("Вопрос") },
                    modifier = Modifier.fillMaxWidth(),
                )
                options.forEachIndexed { i, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { options[i] = it.take(60) },
                        label = { Text("Вариант ${i + 1}") },
                        singleLine = true,
                        trailingIcon = if (removable) {
                            @Composable {
                                IconButton(onClick = { options.removeAt(i) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Убрать вариант")
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (options.size < 6) {
                    TextButton(onClick = { options.add("") }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить вариант")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(question.trim(), filled) }, enabled = valid) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
