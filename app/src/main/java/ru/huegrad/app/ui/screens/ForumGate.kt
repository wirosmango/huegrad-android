package ru.huegrad.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

private fun isRealUser(user: FirebaseUser?) = user != null && !user.isAnonymous

/** Вошёл ли пользователь в аккаунт (анонимный вход Firebase не считается). Обновляется при входе и выходе. */
@Composable
internal fun rememberSignedIn(): Boolean {
    var signedIn by remember { mutableStateOf(isRealUser(runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull())) }
    DisposableEffect(Unit) {
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
        val listener = FirebaseAuth.AuthStateListener { signedIn = isRealUser(it.currentUser) }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }
    return signedIn
}

/**
 * При каждом переходе на вкладку «Форум» без входа открывается окно входа.
 * Его можно закрыть и спокойно читать темы, писать и голосовать без аккаунта нельзя.
 */
@Composable
fun ForumLoginPrompt() {
    val signedIn = rememberSignedIn()
    var show by remember { mutableStateOf(!signedIn) }
    if (show && !signedIn) AccountDialog(onDismiss = { show = false })
}
