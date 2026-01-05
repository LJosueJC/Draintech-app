package com.draintech.draintech.ui.main

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.draintech.draintech.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// Se define la data class aquí para que sea la única referencia válida en este archivo.
data class Device(
    val key: String = "", // La llave única de Firebase para poder borrarlo.
    val nombre: String = "",
    val mac: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    var devices by remember { mutableStateOf(listOf<Device>()) }

    // Estados para el diálogo de confirmación de borrado
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<Device?>(null) }

    val database = FirebaseDatabase.getInstance().reference

    // Cargar los dispositivos del usuario en tiempo real
    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            val ref = database.child("usuarios").child(uid).child("dispositivos")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deviceList = mutableListOf<Device>()
                    for (child in snapshot.children) {
                        val device = child.getValue(Device::class.java)?.copy(key = child.key ?: "")
                        if (device != null) {
                            deviceList.add(device)
                        }
                    }
                    devices = deviceList
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error al cargar dispositivos", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // Función para borrar el dispositivo de Firebase
    fun deleteDevice(device: Device) {
        user?.uid?.let { uid ->
            database.child("usuarios").child(uid).child("dispositivos").child(device.key)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, "${device.nombre} eliminado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Dispositivos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    context.startActivity(Intent(context, AddDeviceActivity::class.java))
                },
                containerColor = Color(0xFFB0D0FF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar dispositivo", tint = Color(0xFF0D1B2A))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0D1B2A)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_draintech),
                contentDescription = "Logo de Draintech",
                modifier = Modifier
                    .height(160.dp)
                    .padding(vertical = 48.dp)
            )

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes dispositivos registrados",
                        color = Color.White
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        val intent =
                                            Intent(context, DeviceDetailActivity::class.java)
                                        intent.putExtra("mac", device.mac)
                                        intent.putExtra("nombre", device.nombre)
                                        context.startActivity(intent)
                                    },
                                    onLongClick = {
                                        deviceToDelete = device
                                        showDeleteDialog = true
                                    }
                                ),
                            border = BorderStroke(2.dp, Color(0xFFB0D0FF)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    device.nombre,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("MAC: ${device.mac}", color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deviceToDelete = null
            },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar el dispositivo '${deviceToDelete?.nombre}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        deviceToDelete?.let { deleteDevice(it) }
                        showDeleteDialog = false
                        deviceToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteDialog = false
                    deviceToDelete = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

