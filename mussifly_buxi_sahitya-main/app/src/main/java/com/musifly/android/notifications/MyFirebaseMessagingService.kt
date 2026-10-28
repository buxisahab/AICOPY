package com.musifly.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL
import com.musifly.android.MainActivity
import com.musifly.android.R
import com.musifly.android.repository.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationEntryPoint {
        fun profileRepository(): ProfileRepository
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag("FCM").d("Refreshed token: $token")
        
        // Update token in Supabase
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, NotificationEntryPoint::class.java)
        val repository = entryPoint.profileRepository()
        
        scope.launch {
            repository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag("FCM").d("From: ${remoteMessage.from}")
        Timber.tag("FCM").d("Message data payload: ${remoteMessage.data}")
        Timber.tag("FCM").d("Message notification payload: ${remoteMessage.notification?.body}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = data["title"] ?: notification?.title ?: "MusiFly"
        val message = data["message"] ?: data["body"] ?: notification?.body ?: "You have a new notification"
        val actorId = data["actor_id"]
        val avatarUrl = data["actor_avatar_url"]
        val type = data["type"]

        scope.launch {
            val largeIcon = if (!avatarUrl.isNullOrBlank()) {
                downloadBitmap(avatarUrl)
            } else null

            sendNotification(
                title = title,
                messageBody = message,
                actorId = actorId,
                type = type,
                largeIcon = largeIcon
            )
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Timber.e(e, "Error downloading notification image")
            null
        }
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        actorId: String? = null,
        type: String? = null,
        largeIcon: Bitmap? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (type == "follow" && actorId != null) {
                putExtra("screen", "profile")
                putExtra("userId", actorId)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "mussifly_social_v1"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Social Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for followers and social interactions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.music_note) 
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
