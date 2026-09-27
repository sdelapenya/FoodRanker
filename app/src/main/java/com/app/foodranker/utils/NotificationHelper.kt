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

object NotificationHelper {

    const val CHANNEL_SOCIAL      = "foodranker_social"
    const val CHANNEL_MODERATION  = "foodranker_moderation"
    const val CHANNEL_DAILY       = "foodranker_daily"

    // Legacy alias kept for DailyReminderWorker compatibility
    const val CHANNEL_ID = CHANNEL_DAILY

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
        // Id derivado del contenido, no un contador. El contador arrancaba de cero en cada
        // arranque del proceso, así que tras reiniciar la app el primer aviso nuevo se
        // colocaba con el id 1 y borraba de la bandeja uno anterior que siguiera ahí.
        // Con esto dos avisos distintos nunca se pisan, y el mismo repetido se reemplaza.
        val notifId = "$channelId|$title|$body|${plateId.orEmpty()}".hashCode()
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
            // Agrupar por canal: sin esto, cinco likes son cinco tarjetas sueltas
            // compitiendo con el resto de la bandeja.
            .setGroup(channelId)
            .build()
        manager(context).notify(notifId, notification)
        actualizarResumen(context, channelId)
    }

    /**
     * Publica (o retira) la tarjeta de resumen del grupo.
     *
     * Solo se muestra a partir de dos avisos: con uno solo, Android ya lo enseña entero y
     * un resumen encima aparecería como una tarjeta de más, medio vacía.
     */
    private fun actualizarResumen(context: Context, channelId: String) {
        val nm = manager(context)
        val resumenId = channelId.hashCode()
        val enElGrupo = try {
            nm.activeNotifications.count { it.notification.group == channelId && it.id != resumenId }
        } catch (e: Exception) {
            return  // algunos fabricantes restringen activeNotifications: mejor sin resumen que romper
        }
        if (enElGrupo < 2) {
            nm.cancel(resumenId)
            return
        }
        val resumen = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_color))
            .setContentTitle(tituloDeCanal(channelId))
            .setContentText("$enElGrupo novedades")
            .setGroup(channelId)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        nm.notify(resumenId, resumen)
    }

    private fun tituloDeCanal(channelId: String) = when (channelId) {
        CHANNEL_MODERATION -> "Moderación de platos"
        CHANNEL_DAILY      -> "Recordatorio diario"
        else               -> "FoodRanker"
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
