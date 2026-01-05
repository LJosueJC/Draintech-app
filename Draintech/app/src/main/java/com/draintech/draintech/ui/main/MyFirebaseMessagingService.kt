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
import com.draintech.draintech.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Revisar si el mensaje tiene una notificación
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification Message Body: ${notification.body}")
            // Muestra la notificación
            sendNotification(notification.title, notification.body)
        }
    }


    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }


    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = "draintech_alertas"
        val notificationId = 1 

        createNotificationChannel(channelId)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(title ?: "Alerta de DrainTech")
            .setContentText(messageBody ?: "Nueva alerta recibida.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) 

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@MyFirebaseMessagingService, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permiso de notificaciones no concedido.")
                return
            }
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Canastilla"
            val descriptionText = "Notificaciones sobre el estado de las canastillas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
