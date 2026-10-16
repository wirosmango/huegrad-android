package ru.huegrad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ru.huegrad.app.forum.AppNotification
import ru.huegrad.app.forum.ForumSession
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.huegrad.app.data.Content
import ru.huegrad.app.ui.screens.ArticleScreen
import ru.huegrad.app.ui.screens.DrochkaScreen
import ru.huegrad.app.ui.screens.EducationScreen
import ru.huegrad.app.ui.screens.ForumScreen
import ru.huegrad.app.ui.screens.HistoryScreen
import ru.huegrad.app.ui.screens.HomeScreen
import ru.huegrad.app.ui.screens.ModsScreen
import ru.huegrad.app.ui.screens.MoreScreen
import ru.huegrad.app.ui.screens.NewsScreen
import ru.huegrad.app.ui.screens.PizdjukScreen
import ru.huegrad.app.ui.screens.RadioScreen
import ru.huegrad.app.ui.screens.RadioTracksScreen
import ru.huegrad.app.ui.screens.SettingsScreen
import ru.huegrad.app.ui.screens.TopicScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Главная", Icons.Rounded.Home),
    Radio("radio", "Радио", Icons.Rounded.Radio),
    Forum("forum", "Форум", Icons.Rounded.Forum),
    News("news", "Новости", Icons.Rounded.Newspaper),
    More("more", "Ещё", Icons.Rounded.MoreHoriz),
}

val NoInsets = WindowInsets(0, 0, 0, 0)

/**
 * Сколько снизу занимает панель навигации, которую экран должен учесть сам.
 * Плавающая панель лежит поверх контента, поэтому экраны берут её реальную высоту отсюда
 * (она включает отступ под системную навигацию), а не фиксированные значения.
 * В режиме md3 контент уже отступает сам, там значение 0; в режиме drawer панели нет, там отступ под системную навигацию.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

@Composable
fun HuegradApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = Dest.entries.any { it.route == route }
    val back: () -> Unit = { nav.popBackStack() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerMode = AppSettings.navStyle == "drawer"
    AppSettings.openDrawer = { scope.launch { drawerState.open() } }
    val openArticle: (String) -> Unit = { id -> nav.navigate("article/$id") }
    val openNotification: (AppNotification) -> Unit = { n ->
        nav.navigate("forum/topic/${n.topicId}?post=${n.postId}")
    }

    // Нажали на уведомление в шторке: открываем тему с постом и отмечаем упоминание прочитанным
    val deepLink = DeepLink.pending
    LaunchedEffect(deepLink) {
        val target = deepLink ?: return@LaunchedEffect
        DeepLink.pending = null
        target.notificationId?.let(ForumSession::markReadById)
        nav.navigate("forum/topic/${target.topicId}?post=${target.postId}") { launchSingleTop = true }
    }

    // Новое уведомление об упоминании или ответе, пока приложение открыто: короткое сообщение, как на сайте
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ForumSession.incoming.collect { n -> Toast.makeText(context, n.toastText, Toast.LENGTH_LONG).show() }
    }

    fun navigateTo(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerMode,
        drawerContent = {
            AppDrawerSheet(
                currentRoute = route,
                onNavigate = { target ->
                    navigateTo(target)
                    scope.launch { drawerState.close() }
                },
            )
        }
    ) {
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {},
        bottomBar = {
            if (showBar && !drawerMode) {
                val panelColor = if (AppSettings.navPanelBackground) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent
                if (AppSettings.navStyle == "md3") {
                    // Standard docked Material 3 navigation bar. Scaffold reserves its space
                    // so page content does not scroll or render underneath this bar.
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = panelColor,
                        tonalElevation = if (AppSettings.navPanelBackground) 3.dp else 0.dp,
                    ) {
                        Dest.entries.forEach { d ->
                            val selected = route == d.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(d.route) },
                                icon = {
                                    Icon(
                                        imageVector = d.icon,
                                        contentDescription = d.label,
                                    )
                                },
                                label = if (AppSettings.navLabels) {
                                    { Text(text = d.label, maxLines = 1) }
                                } else null,
                                alwaysShowLabel = AppSettings.navLabels,
                            )
                        }
                    }
                } else {
                    // Restore the original floating "pill" navigation, with full-height touch targets.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .background(Color.Transparent)
                            .padding(horizontal = FloatingBarSideInset, vertical = FloatingBarVerticalInset),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            color = panelColor,
                            tonalElevation = if (AppSettings.navPanelBackground) 3.dp else 0.dp,
                            shadowElevation = if (AppSettings.navPanelBackground) 8.dp else 0.dp,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Dest.entries.forEach { d ->
                                    val selected = route == d.route
                                    if (AppSettings.navLabels) {
                                        // С подписями: развёрнутая пилюля с названием только у выбранного пункта
                                        FloatingNavItem(
                                            icon = d.icon,
                                            label = d.label,
                                            selected = selected,
                                            onClick = { navigateTo(d.route) },
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                // Extra inset creates more space between the selected pill and panel.
                                                .padding(5.dp)
                                                .background(
                                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent,
                                                    shape = RoundedCornerShape(50),
                                                )
                                                .clickable { navigateTo(d.route) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = d.icon,
                                                contentDescription = d.label,
                                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        // Высота панели из Scaffold: в режиме floating она плавает поверх контента, экраны должны её учесть
        val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomBarInset = when {
            drawerMode -> navBarInset
            AppSettings.navStyle == "md3" -> 0.dp
            else -> innerPadding.calculateBottomPadding()
        }
        CompositionLocalProvider(LocalBottomBarInset provides bottomBarInset) {
            NavHost(
                navController = nav,
                startDestination = Dest.Home.route,
                // MD3 is docked and reserves space; the floating pill intentionally overlays content.
                modifier = if (AppSettings.navStyle == "md3" || drawerMode) Modifier.padding(innerPadding) else Modifier,
            ) {
                composable(Dest.Home.route) { HomeScreen() }
                composable(Dest.Radio.route) { RadioScreen(onOpenTracks = { nav.navigate("radio/tracks") }) }
                composable("radio/tracks") { RadioTracksScreen(onBack = back) }
                composable(Dest.Forum.route) {
                    ForumScreen(
                        onOpenTopic = { id -> nav.navigate("forum/topic/$id") },
                        onOpenNotification = openNotification,
                    )
                }
                // post — необязательный id поста, к которому прокрутить (переход из уведомления)
                composable(
                    "forum/topic/{id}?post={post}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("post") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { TopicScreen(back, onOpenNotification = openNotification) }
                composable(Dest.News.route) { NewsScreen(onOpen = openArticle) }
                composable(Dest.More.route) { MoreScreen(onNavigate = { nav.navigate(it) }) }
                composable("settings") { SettingsScreen(onBack = back) }
                composable("drochka") { DrochkaScreen(back) }
                composable("mods") { ModsScreen(back) }
                composable("history") { HistoryScreen(back) }
                composable("education") { EducationScreen(back, openArticle) }
                composable("pizdjuk") { PizdjukScreen(back) }
                composable("article/{id}") { stack ->
                    val ref = stack.arguments?.getString("id")?.let { Content.articles[it] }
                    if (ref != null) ArticleScreen(ref, back)
                }
            }
        }
    }
    }
}
