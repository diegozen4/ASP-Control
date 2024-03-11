package com.asp.loginhome.recursos.segundoplano

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class BackgroundService : Service() {

    private lateinit var handler: Handler
    private lateinit var requestQueue: RequestQueue
    private val notificationId = 1
    private val channelId = "my_channel"

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        requestQueue = Volley.newRequestQueue(this)

        // Crea el canal de notificación (solo necesario en Android Oreo y superior)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Realiza las tareas de fondo aquí, como consultas a la API

        // Ejemplo de consulta a la API (reemplaza con tu lógica)
        makeApiRequest()

        // El servicio seguirá ejecutándose hasta que se detenga explícitamente
        return START_STICKY
    }

    private fun makeApiRequest() {
        // Reemplaza la URL con tu endpoint de la API
        val url = "https://tu-api.com/endpoint"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                // Procesa la respuesta de la API
                val success = response.getBoolean("success")

                if (success) {
                    // Si la respuesta es afirmativa, muestra una notificación
                    showNotification("Nuevo mensaje", "Has recibido un nuevo mensaje.")
                }
            },
            { error ->
                Log.e("API Error", "Error en la solicitud a la API: $error")
            }
        )

        // Agrega la solicitud a la cola de solicitudes
        requestQueue.add(jsonObjectRequest)
    }

    private fun showNotification(title: String, message: String) {
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)     
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Muestra la notificación
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        // Crea un canal de notificación (solo necesario en Android Oreo y superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
