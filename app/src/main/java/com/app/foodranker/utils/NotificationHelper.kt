package com.app.foodranker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.app.foodranker.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "foodranker_notifications"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FoodRanker",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Likes y valoraciones de tus platos" }
        manager(context).createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, body: String, plateId: String? = null) {
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            plateId?.let { putExtra("plateId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
