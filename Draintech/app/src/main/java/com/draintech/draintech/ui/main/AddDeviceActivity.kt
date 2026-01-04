package com.draintech.draintech.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.draintech.draintech.ui.theme.DraintechTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddDeviceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DraintechTheme {
                AddDeviceScreen { nombre, mac ->
                    addDevice(nombre, mac)
                }
            }
        }
    }

    private fun addDevice(nombre: String, mac: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val device = mapOf(
            "nombre" to nombre,
            "mac" to mac
        )

        val ref = FirebaseDatabase.getInstance()
            .reference
            .child("usuarios")
            .child(user.uid)
            .child("dispositivos")
            .child(mac)

        ref.setValue(device)
            .addOnSuccessListener {
                Toast.makeText(this, "Dispositivo agregado correctamente", Toast.LENGTH_SHORT).show()
                finish() // Cierra la actividad y vuelve a Main
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar dispositivo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
