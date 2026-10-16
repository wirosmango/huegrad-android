package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.huegrad.app.data.Content

@Composable
fun EducationScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    SubPageScaffold("Образование", onBack) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 12.dp, bottom = pad.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text("Добро пожаловать!", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Ознакомьтесь с нашими замечательными образовательными учреждениями:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                ExpressiveListGroup {
                    Content.education.forEachIndexed { index, ref ->
                        ExpressiveListItem(
                            title = ref.title,
                            subtitle = ref.subtitle,
                            icon = Icons.Rounded.School,
                            onClick = { onOpen(ref.id) },
                            showDivider = index != Content.education.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
