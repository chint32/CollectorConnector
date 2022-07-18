package com.example.collectorconnector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.collectorconnector.auth.LoginActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM"
    private var notificationId = 123

    init {

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast

            Log.d(TAG, token)
        })
    }

    private fun createNotification(
        context: Context,
        smallIcon: Int,
        title: String = "",
        message: String = "",
        channelId: String ="",
        channelNameForAndroid0: String = "notification channel",
        channelDescriptionForAndroid0: String = "",
        pendingIntent: PendingIntent? = null,
        category: String = ""
    ): Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(channelId,channelNameForAndroid0,
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = channelDescriptionForAndroid0
            }

            NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(smallIcon).setContentTitle(title).setContentText(message)
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        if(category != "") builder.setCategory(category)
        builder.setAutoCancel(true)
        pendingIntent.let {
            builder.setContentIntent(it)
        }
        return builder.build()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if(message.data.isNotEmpty()){
            val map = message.data
            val title = map["title"] as String
            val message = map["message"] as String
            val pushType = map["pushType"] as String

            Intent(this, LoginActivity::class.java)
                .also { targetIntent ->
                    PendingIntent.getActivity(this, 111, targetIntent,
                        PendingIntent.FLAG_IMMUTABLE).also {
                            targetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        NotificationManagerCompat.from(this).notify(
                            notificationId,
                            createNotification(this, R.drawable.simple_border,
                            title, message, "my_channel_id", )
                        )
                        notificationId++

                    }
                }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendNewTokenToService(token)
    }

    private fun sendNewTokenToService(token: String){
        Log.d(TAG, "need to send new token to service $token")
    }




}