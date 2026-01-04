package com.draintech.draintech.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.draintech.draintech.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    // --- INICIO: CÓDIGO DE PERMISO DE NOTIFICACIÓN ---
    // Este es el lugar correcto para poner esta lógica

    // 1. Preparamos el lanzador que pedirá el permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISO", "Permiso de notificación CONCEDIDO")
        } else {
            Log.w("PERMISO", "Permiso de notificación DENEGADO")
        }
    }

    // 2. Lo ejecutamos una vez que la pantalla se compone
    LaunchedEffect(Unit) {
        // Solo aplica para Android 13 (API 33) y superiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // 3. Revisamos si ya tenemos el permiso
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("PERMISO", "El permiso de notificación ya estaba concedido.")
                }
                // 4. (Opcional) Aquí podrías mostrar un diálogo explicando por qué
                // !shouldShowRequestPermissionRationale(...) -> { ... }

                // 5. Si no lo tenemos, lo pedimos
                else -> {
                    Log.d("PERMISO", "Pidiendo permiso de notificación...")
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    // --- FIN: CÓDIGO DE PERMISO DE NOTIFICACIÓN ---


    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExeG9rZ200NHg2eHlmZmV6MGl6OHBla3Fkczk5OGw3amZpbGU4NmJlciZlcD12MV9naWZzX3NlYXJjaCZjdD1n/26DMWExfbZSiV0Btm/giphy.gif",
            contentDescription = "Fondo animado de login",
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CAMBIO AQUÍ ---
            // Se reemplaza el Text por el logo desde los recursos drawable.
            Image(
                painter = painterResource(id = R.drawable.logo_draintech),
                contentDescription = "Logo de Draintech",
                modifier = Modifier.height(80.dp) // Ajusta la altura del logo como prefieras
            )
            // --- FIN DEL CAMBIO ---

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.LightGray,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.LightGray,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onNavigateToRegister,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}
