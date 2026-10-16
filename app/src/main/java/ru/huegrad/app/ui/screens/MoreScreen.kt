@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.NoInsets

private data class MoreItem(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

private val moreItems = listOf(
    MoreItem("drochka", "Дрочка", "Кто у нас шалунишка маленький?", Icons.Rounded.Favorite),
    MoreItem("mods", "Доп. ресурсы", "Файлы сервера", Icons.Rounded.Extension),
    MoreItem("history", "История Хуеграда", "От «го в майн на телефонах» до виртуального государства", Icons.Rounded.History),
    MoreItem("education", "Образование", "ПТУ, ХТУ и школа", Icons.Rounded.School),
    MoreItem("pizdjuk", "Пиздюк", "Нико ваншоь пиздюк офишиал", Icons.Rounded.Image),
    MoreItem("settings", "Настройки", "Панель навигации, тема и акцентный цвет", Icons.Rounded.Settings),
)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Другое") }) },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ExpressiveListGroup {
                    moreItems.forEachIndexed { index, item ->
                        ExpressiveListItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            onClick = { onNavigate(item.route) },
                            showDivider = index != moreItems.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
