package com.app.foodranker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.app.foodranker.MainActivity
import com.app.foodranker.R
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {

    const val CHANNEL_SOCIAL      = "foodranker_social"
    const val CHANNEL_MODERATION  = "foodranker_moderation"
    const val CHANNEL_DAILY       = "foodranker_daily"

    // Legacy alias kept for DailyReminderWorker compatibility
    const val CHANNEL_ID = CHANNEL_DAILY

    private val notifIdCounter = AtomicInteger(0)

    fun createChannels(context: Context) {
        val nm = manager(context)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SOCIAL, "Likes y valoraciones", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Alguien ha valorado o dado like a tus platos" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MODERATION, "Moderación de platos", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Estado de tus platos enviados" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Recordatorio diario", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Recordatorio para votar platos cada día" }
        )
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        plateId: String? = null,
        channelId: String = CHANNEL_SOCIAL
    ) {
        createChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            plateId?.let { putExtra("plateId", it) }
        }
        val notifId = notifIdCounter.incrementAndGet()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setColor(ContextCompat.getColor(context, R.color.notification_color))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager(context).notify(notifId, notification)
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
