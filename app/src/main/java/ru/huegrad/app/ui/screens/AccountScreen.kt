@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.graphics.shapes.Morph
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.huegrad.app.forum.ForumIdentity
import ru.huegrad.app.forum.ForumSession
import ru.huegrad.app.forum.MentionService
import ru.huegrad.app.ui.ForumAvatar

/** Страница входа в аккаунт форума: на весь экран, поверх форума. */
@Composable
internal fun AccountDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            AccountPage(onDismiss)
        }
    }
}

@Composable
private fun AccountPage(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val user = auth.currentUser
    val signedIn = user != null && !user.isAnonymous
    val pinnedNick by ForumSession.nickname.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var registering by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            busy = true
            auth.signInWithCredential(credential).addOnCompleteListener { login ->
                busy = false
                if (login.isSuccessful) {
                    Toast.makeText(context, "Вход через Google выполнен", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else Toast.makeText(context, login.exception?.localizedMessage ?: "Ошибка Google-входа", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            val statusCode = (e as? ApiException)?.statusCode
            val message = if (statusCode == 10) {
                "Ошибка Google 10 (DEVELOPER_ERROR): проверь, что выбран Web Client ID именно из этого Firebase-проекта, включён Google в Firebase Authentication и добавлен SHA-1 сертификата приложения в настройки Firebase. Затем скачай новый google-services.json и пересобери приложение."
            } else {
                e.localizedMessage ?: "Вход через Google отменён"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun startGoogle() {
        // Строку default_web_client_id создаёт плагин google-services, но только если в google-services.json есть
        // oauth_client. Ищем её по имени, чтобы проект собирался и без неё, а ошибка была понятной.
        val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (clientIdRes == 0) {
            Toast.makeText(
                context,
                "Нет Web Client ID: в google-services.json пусто поле oauth_client. Включи вход через Google в Firebase " +
                    "(Authentication → Sign-in method), добавь SHA-1 приложения в настройках проекта, скачай " +
                    "google-services.json заново, положи в app/ и пересобери проект.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        try {
            val clientId = context.getString(clientIdRes)
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(clientId).build()
            googleLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось запустить Google-вход: ${e.localizedMessage ?: "проверь OAuth Web Client ID"}", Toast.LENGTH_LONG).show()
        }
    }

    fun submitEmail() {
        busy = true
        val finish: (String?) -> Unit = { error ->
            busy = false
            if (error == null) {
                Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                onDismiss()
            } else Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
        if (registering) {
            // Ник проверяется и закрепляется за аккаунтом (usernames), как на сайте
            ForumSession.registerWithEmail(email, password, nickname, finish)
        } else {
            auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { result ->
                finish(if (result.isSuccessful) null else ForumIdentity.translateAuthError(result.exception ?: Exception("Ошибка входа")))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (signedIn && user != null) {
                // Закреплённый ник из usernames, пока он не загрузился, берём имя из профиля
                val name = pinnedNick ?: user.displayName.orEmpty()
                ForumAvatar(
                    nickname = name.ifBlank { user.email.orEmpty() },
                    photoUrl = user.photoUrl?.toString(),
                    size = 112.dp,
                )
                Text(
                    name.ifBlank { "Вы вошли в аккаунт" },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                if (user.email != null) {
                    Text(
                        user.email!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                var background by remember { mutableStateOf(MentionService.isEnabled(context)) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Проверка уведомлений", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Упоминания и ответы приходят в шторку и при свёрнутом приложении (в шторке будет постоянное тихое уведомление). Выключено: никаких фоновых проверок и уведомлений в шторке.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = background,
                        onCheckedChange = {
                            background = it
                            MentionService.setEnabled(context, it)
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        ForumSession.signOut()
                        Toast.makeText(context, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Выйти") }
            } else {
                // Печенье плавно перетекает из одной формы Material в другую и на секунду замирает
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    MorphingCookie(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer)
                    Icon(
                        Icons.Rounded.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    if (registering) "Создать аккаунт" else "Вход в аккаунт",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Войди, чтобы писать на форуме со своим именем и аватаркой. Анонимно писать можно и без входа.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = ::startGoogle,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Продолжить с Google")
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("или по почте", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Электронная почта") },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (registering) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(30) },
                        label = { Text("Ник") },
                        supportingText = { Text("2–30 символов: буквы, цифры, пробел, _ . -") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    supportingText = if (registering) ({ Text("Не менее 6 символов") }) else null,
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                FilledTonalButton(
                    onClick = ::submitEmail,
                    enabled = !busy && email.isNotBlank() && password.length >= 6,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    else Text(if (registering) "Зарегистрироваться" else "Войти")
                }
                TextButton(onClick = { registering = !registering }) {
                    Text(if (registering) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Формы, между которыми перетекает печенье. */
private val CookieShapes by lazy {
    listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Sunny,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.SoftBurst,
        MaterialShapes.Pentagon,
        MaterialShapes.Cookie12Sided,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Circle,
    )
}

/** Пружинистое перетекание: на конце форма слегка «перелетает» и возвращается. */
private val MorphEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

@Composable
private fun MorphingCookie(modifier: Modifier = Modifier, color: Color) {
    val shapes = CookieShapes
    val count = shapes.size
    // Одни линейные часы на весь круг: целая часть выбирает пару форм, дробная даёт ход перетекания
    val clock by rememberInfiniteTransition(label = "cookieMorph").animateFloat(
        initialValue = 0f,
        targetValue = count.toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = count * 2200, easing = LinearEasing)),
        label = "cookieMorphClock",
    )
    val index = clock.toInt().coerceIn(0, count - 1)
    val morph = remember(index) { Morph(shapes[index], shapes[(index + 1) % count]) }
    // Первые ~45% шага форма перетекает, остальное время стоит
    val progress = MorphEasing.transform(((clock - index) / 0.45f).coerceIn(0f, 1f))
    Box(modifier.clip(MorphShape(morph, progress)).background(color))
}

/**
 * Форма из Morph на заданном ходе перетекания. Путь строится из кубических кривых самого Morph
 * и растягивается на весь размер, поэтому не зависит от того, в каких координатах заданы формы.
 */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cubics = morph.asCubics(progress)
        val path = Path()
        if (cubics.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (c in cubics) {
                // Учитываем и контрольные точки: кривая выступает за свои опорные точки, иначе концы обрезаются
                minX = minOf(minX, c.anchor0X, c.control0X, c.control1X, c.anchor1X)
                maxX = maxOf(maxX, c.anchor0X, c.control0X, c.control1X, c.anchor1X)
                minY = minOf(minY, c.anchor0Y, c.control0Y, c.control1Y, c.anchor1Y)
                maxY = maxOf(maxY, c.anchor0Y, c.control0Y, c.control1Y, c.anchor1Y)
            }
            val w = (maxX - minX).coerceAtLeast(1e-6f)
            val h = (maxY - minY).coerceAtLeast(1e-6f)
            fun x(v: Float) = (v - minX) / w * size.width
            fun y(v: Float) = (v - minY) / h * size.height
            path.moveTo(x(cubics.first().anchor0X), y(cubics.first().anchor0Y))
            for (c in cubics) {
                path.cubicTo(
                    x(c.control0X), y(c.control0Y),
                    x(c.control1X), y(c.control1Y),
                    x(c.anchor1X), y(c.anchor1Y),
                )
            }
            path.close()
        }
        return Outline.Generic(path)
    }
}
