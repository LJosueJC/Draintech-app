package com.draintech.draintech.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onAddDevice: (nombre: String, mac: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }

    // Colores para los campos de texto, consistentes con el resto de la app
    val textFieldColors = TextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        cursorColor = Color(0xFFB0D0FF),
        focusedIndicatorColor = Color(0xFFB0D0FF), // Borde cuando está enfocado
        unfocusedIndicatorColor = Color.LightGray, // Borde cuando no está enfocado
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.LightGray
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Dispositivo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0D1B2A)) // Fondo azul oscuro
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del dispositivo") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("Dirección MAC") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAddDevice(nombre, mac) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB0D0FF),
                        contentColor = Color(0xFF0D1B2A)
                    )
                ) {
                    Text("Agregar dispositivo")
                }
            }
        }
    }
}
