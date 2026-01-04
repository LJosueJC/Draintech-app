package com.draintech.draintech.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.draintech.draintech.R // Asegúrate de que esta sea tu ruta al R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    /**
     * Se llama cuando se recibe un mensaje.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Revisar si el mensaje tiene una notificación (esto lo definiste en la API)
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification Message Body: ${notification.body}")
            // Muestra la notificación
            sendNotification(notification.title, notification.body)
        }
    }

    /**
     * Se llama si el token de registro de la app cambia.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Aquí podrías enviar el token a tu servidor si usaras notificaciones directas,
        // pero como usamos tópicos, no es estrictamente necesario.
    }

    /**
     * Construye y muestra una notificación en el dispositivo.
     */
    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = "draintech_alertas" // ID de tu canal de notificación
        val notificationId = 1 // Un ID único para esta notificación

        // 1. Crear el Canal de Notificación (Obligatorio para Android 8.0+)
        createNotificationChannel(channelId)

        // 2. Construir la notificación
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // REEMPLAZA con tu ícono de app
            .setContentTitle(title ?: "Alerta de DrainTech")
            .setContentText(messageBody ?: "Nueva alerta recibida.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Cierra la notificación al tocarla

        // 3. Mostrar la notificación
        with(NotificationManagerCompat.from(this)) {
            // Revisa si tienes permiso (necesario en Android 13+)
            if (ActivityCompat.checkSelfPermission(this@MyFirebaseMessagingService, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permiso de notificaciones no concedido.")
                // En una app real, deberías pedir este permiso en tu Activity principal
                return
            }
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel(channelId: String) {
        // Solo para Android 8.0 (Oreo) y superiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Canastilla"
            val descriptionText = "Notificaciones sobre el estado de las canastillas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Registrar el canal con el sistema
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}