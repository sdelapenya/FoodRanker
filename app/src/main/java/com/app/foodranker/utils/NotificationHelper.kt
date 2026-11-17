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

    /**
     * Interacciones: likes, valoraciones, comentarios y seguidores nuevos.
     *
     * ⚠️ El id sigue siendo `foodranker_social` **a propósito**, aunque de cara al usuario
     * ahora se llame "Interacciones". Hasta el 2026-11-17 este canal llevaba SIETE tipos de
     * aviso (estos cuatro, más platos de gente seguida, logros y liga) y se mostraba como
     * "Likes y valoraciones": quien silenciaba lo que creía que eran likes se quedaba
     * también sin saber que alguien a quien sigue había publicado, sin enterarse de que lo
     * había hecho.
     *
     * Conservar el id es lo que hace que la migración no pise a nadie: **un canal ya creado
     * no se puede modificar desde el código** —el nombre y la descripción sí, la importancia
     * no—, así que quien lo tuviera silenciado lo conserva silenciado, que es justo lo que
     * quería para los likes. Y lo que se saca de aquí ([CHANNEL_FOLLOWING] y
     * [CHANNEL_ACHIEVEMENTS]) nace como canal nuevo con la importancia por defecto, así que
     * vuelve a llegarle. Cambiar el id habría reseteado también los likes.
     */
    const val CHANNEL_SOCIAL      = "foodranker_social"

    /** Alguien a quien sigues ha publicado. Separado del resto desde la v22. */
    const val CHANNEL_FOLLOWING   = "foodranker_following"

    /** Subidas de nivel, logros y resultados de la liga semanal. */
    const val CHANNEL_ACHIEVEMENTS = "foodranker_achievements"

    const val CHANNEL_MODERATION  = "foodranker_moderation"
    const val CHANNEL_DAILY       = "foodranker_daily"

    /**
     * De qué canal es cada tipo de aviso.
     *
     * Vive aquí y no en quien lo llama para que el reparto sea uno solo: lo necesitan tanto
     * `FoodRankerMessagingService` (push en primer plano) como el listener de Firestore, y
     * el servidor manda el mismo reparto en `android.notification.channelId` para cuando la
     * app está cerrada (ver `onNotificationCreated` en functions/src/index.ts). Si los dos
     * lados dejan de coincidir, el mismo aviso cae en un canal distinto según si la app
     * estaba abierta, que es exactamente el tipo de incoherencia que nadie reproduce.
     */
    fun canalDe(tipo: String?): String = when (tipo) {
        "moderation_approved", "moderation_rejected" -> CHANNEL_MODERATION
        "new_plate"                                  -> CHANNEL_FOLLOWING
        "level_up", "badge", "league_result"         -> CHANNEL_ACHIEVEMENTS
        else                                         -> CHANNEL_SOCIAL
    }

    // Legacy alias kept for DailyReminderWorker compatibility
    const val CHANNEL_ID = CHANNEL_DAILY

    fun createChannels(context: Context) {
        val nm = manager(context)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SOCIAL,
                context.getString(R.string.chan_social_t),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.chan_social_d) }
        )
        // Los dos que salieron de "social" el 2026-11-17. Nacen con IMPORTANCE_DEFAULT, la
        // misma que tenía el canal del que vienen: la idea es poder silenciarlos por
        // separado, no silenciarlos de oficio.
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOLLOWING,
                context.getString(R.string.chan_follow_t),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.chan_follow_d) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                context.getString(R.string.chan_achiev_t),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.chan_achiev_d) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MODERATION,
                context.getString(R.string.chan_moder_t),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.chan_moder_d) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                context.getString(R.string.chan_daily_t),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.chan_daily_d) }
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
            // `takeIf` además del `?.`: un plateId vacío no es "no hay plato" para
            // getStringExtra —devuelve "" y no null—, y en MainActivity eso acababa
            // navegando a la ruta "plate/", que no existe, tumbando la app. Mejor no
            // llegar a adjuntarlo. Ver el comentario de MainActivity.
            plateId?.takeIf { it.isNotBlank() }?.let { putExtra("plateId", it) }
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
            // Un aviso que se reemplaza NO vuelve a sonar. Sin esto, el id derivado del
            // texto evitaba la tarjeta duplicada pero no el ruido: `notify()` con un id que
            // ya existe reemplaza la tarjeta y **vuelve a sonar y vibrar**, asi que el mismo
            // hecho llegando por los dos caminos (push de FCM y listener de Firestore) se
            // oia dos veces aunque solo se viera una.
            .setOnlyAlertOnce(true)
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
            .setContentTitle(tituloDeCanal(context, channelId))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notif_group_summary, enElGrupo, enElGrupo
                )
            )
            .setGroup(channelId)
            .setGroupSummary(true)
            .setAutoCancel(true)
            // Este resumen se republica con el mismo id cada vez que entra un aviso nuevo
            // del grupo, asi que era un multiplicador del ruido: cinco likes sonaban cinco
            // veces por las tarjetas y otras cuatro por el resumen.
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(resumenId, resumen)
    }

    private fun tituloDeCanal(context: Context, channelId: String) = when (channelId) {
        CHANNEL_MODERATION   -> context.getString(R.string.chan_moder_t)
        CHANNEL_DAILY        -> context.getString(R.string.chan_daily_t)
        CHANNEL_FOLLOWING    -> context.getString(R.string.chan_follow_t)
        CHANNEL_ACHIEVEMENTS -> context.getString(R.string.chan_achiev_t)
        else                 -> context.getString(R.string.app_name)
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
