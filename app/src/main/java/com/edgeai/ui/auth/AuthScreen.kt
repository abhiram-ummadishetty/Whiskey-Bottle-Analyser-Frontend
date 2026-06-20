package com.edgeai.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.edgeai.data.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(container: AppContainer, onAuthed: () -> Unit) {
    var mode by remember { mutableStateOf(AuthMode.SignUp) }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Edge AI", style = MaterialTheme.typography.headlineMedium)
        Text(if (mode == AuthMode.SignUp) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(first, { first = it }, label = { Text("First name") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(last, { last = it }, label = { Text("Last name") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            enabled = !busy,
            onClick = {
                busy = true; error = null
                scope.launch {
                    val result = if (mode == AuthMode.SignUp)
                        container.users.signUp(first, last, password)
                            .mapCatching { id -> container.session.setUser(id, first.trim()); id }
                    else
                        container.users.login(first, last, password)
                            .mapCatching { u -> container.session.setUser(u.id, u.firstName); u.id }
                    busy = false
                    result.onFailure { error = it.message ?: "Unknown error" }
                        .onSuccess { onAuthed() }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (mode == AuthMode.SignUp) "Sign up" else "Log in") }

        TextButton(onClick = {
            mode = if (mode == AuthMode.SignUp) AuthMode.Login else AuthMode.SignUp
            error = null
        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (mode == AuthMode.SignUp) "Have an account? Log in" else "New here? Sign up")
        }
    }
}

private enum class AuthMode { SignUp, Login }