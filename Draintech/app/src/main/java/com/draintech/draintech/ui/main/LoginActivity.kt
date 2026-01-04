package com.draintech.draintech.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.draintech.draintech.ui.theme.DraintechTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.material3.Surface

class LoginActivity: ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // --- LÓGICA PARA VERIFICAR SESIÓN ACTIVA ---
        // Se comprueba si ya hay un usuario con la sesión iniciada.
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
            return
        }

        setContent {
            DraintechTheme {
                Surface {
                    LoginScreen(
                        onLogin = { email, password -> loginUser(email, password) },
                        onNavigateToRegister = {
                            startActivity(Intent(this, RegisterActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    // Obtener username
                    database.child("usuarios").child(uid).child("username")
                        .get().addOnSuccessListener { snapshot ->
                            val username = snapshot.value as? String ?: "Usuario"
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("username", username)
                            startActivity(intent)
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
