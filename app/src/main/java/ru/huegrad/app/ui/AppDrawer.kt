package ru.huegrad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import ru.huegrad.app.data.Content

private data class DrawerEntry(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badge: String? = null,
)

private val OuterCorner = 24.dp
private val InnerCorner = 6.dp

/**
 * Боковое меню: шапка (аватар, кнопки темы, имя) и три группы пунктов,
 * каждая из которых состоит из отдельных скруглённых плиток с узкими зазорами.
 */
@Composable
fun AppDrawerSheet(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val drawerWidth = (screenWidth * 0.76f).coerceAtMost(360.dp)

    // Если Firebase не настроен, getInstance() бросает исключение: меню должно открываться и так.
    val user: FirebaseUser? = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

    val systemDark = isSystemInDarkTheme()
    val isDark = when (AppSettings.themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> systemDark
    }
    // Кнопка в шапке крутит три состояния: светлая → тёмная → AMOLED → светлая
    val themeKind = when {
        AppSettings.themeMode == "amoled" -> "amoled"
        isDark -> "dark"
        else -> "light"
    }

    val settings = listOf(DrawerEntry("settings", "Настройки", Icons.Outlined.Settings))
    val main = listOf(
        DrawerEntry("home", "Главная", Icons.Outlined.Home),
        DrawerEntry("radio", "Радио", Icons.Outlined.Radio),
        DrawerEntry("forum", "Форум", Icons.Outlined.Forum),
        DrawerEntry("news", "Новости", Icons.Outlined.Newspaper, badge = Content.news.size.toString()),
    )
    val more = listOf(
        DrawerEntry("drochka", "Дрочка", Icons.Outlined.Image),
        DrawerEntry("pizdjuk", "Пиздюк", Icons.Outlined.Favorite),
        DrawerEntry("mods", "Доп. ресурсы", Icons.Outlined.Extension, badge = Content.mods.size.toString()),
        DrawerEntry("history", "История Хуеграда", Icons.Outlined.History),
        DrawerEntry("education", "Образование", Icons.Outlined.School),
    )

    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerShape = RectangleShape,
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        ) {
            DrawerHeader(
                user = user,
                themeKind = themeKind,
                onOpenForum = { onNavigate("forum") },
                onCycleTheme = {
                    val next = when (themeKind) {
                        "light" -> "dark"
                        "dark" -> "amoled"
                        else -> "light"
                    }
                    AppSettings.setThemeMode(context, next)
                },
            )
            Spacer(Modifier.size(28.dp))
            DrawerGroup(settings, currentRoute, onNavigate)
            Spacer(Modifier.size(28.dp))
            DrawerGroup(main, currentRoute, onNavigate)
            Spacer(Modifier.size(28.dp))
            DrawerGroup(more, currentRoute, onNavigate)
        }
    }
}

@Composable
private fun DrawerHeader(
    user: FirebaseUser?,
    themeKind: String,
    onOpenForum: () -> Unit,
    onCycleTheme: () -> Unit,
) {
    val name = user?.displayName?.takeIf { it.isNotBlank() } ?: "Форум Huegrad"
    val subtitle = user?.email?.takeIf { it.isNotBlank() }
        ?: if (user != null) "Профиль форума" else "Нажмите, чтобы открыть форум"
    val photo = user?.photoUrl

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // Аватар: скруглённый квадрат, как на референсе
            val avatarShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(avatarShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClickLabel = "Открыть форум", role = Role.Button, onClick = onOpenForum),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = "Аватар форума",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = user?.displayName?.trim()?.firstOrNull()?.uppercase() ?: "Х",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Кнопка темы справа сверху: иконка показывает текущую тему, нажатие переключает на следующую
            HeaderButton(
                icon = when (themeKind) {
                    "amoled" -> Icons.Outlined.Contrast
                    "dark" -> Icons.Outlined.DarkMode
                    else -> Icons.Outlined.LightMode
                },
                description = when (themeKind) {
                    "light" -> "Светлая тема. Включить тёмную"
                    "dark" -> "Тёмная тема. Включить AMOLED"
                    else -> "AMOLED-тема. Включить светлую"
                },
                shape = RoundedCornerShape(22.dp),
                onClick = onCycleTheme,
            )
        }

        Spacer(Modifier.size(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = "Открыть форум", role = Role.Button, onClick = onOpenForum),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).size(32.dp),
            )
        }
    }
}

@Composable
private fun HeaderButton(
    icon: ImageVector,
    description: String,
    shape: Shape,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 44.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Группа плиток: у крайних плиток внешние углы большие, у внутренних маленькие, между плитками узкий зазор. */
@Composable
private fun DrawerGroup(
    entries: List<DrawerEntry>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        entries.forEachIndexed { index, entry ->
            val top: Dp = if (index == 0) OuterCorner else InnerCorner
            val bottom: Dp = if (index == entries.lastIndex) OuterCorner else InnerCorner
            DrawerRow(
                entry = entry,
                // "forum/topic/{id}" и "radio/tracks" подсвечивают родительский раздел
                selected = currentRoute == entry.route || currentRoute?.startsWith(entry.route + "/") == true,
                shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                onClick = { onNavigate(entry.route) },
            )
        }
    }
}

@Composable
private fun DrawerRow(
    entry: DrawerEntry,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (entry.badge != null) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = entry.badge,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.surface,
                )
            }
        }
    }
}
