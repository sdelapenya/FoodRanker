package com.app.foodranker.utils

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        // Sin sesión no se recuerda nada: quien instala la app y no llega a registrarse
        // recibía igualmente el aviso de las 14:00, invitándole a "sumar XP" en una
        // cuenta que no existe. El worker queda programado; simplemente no molesta.
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val messages = listOf(
            "¿Qué has comido hoy? 🍽️" to "Comparte tu mejor plato y suma XP",
            "¡Hora del almuerzo! 🌟" to "Descubre los mejores platos del mundo en FoodRanker",
            "¿Has probado algo nuevo? 🌍" to "Valora y comparte tu experiencia gastronómica",
            "El plato del día te espera 🏆" to "¿Cuál es el mejor plato que has comido esta semana?",
            "¡Sube de nivel! ⭐" to "Publica un plato hoy y gana XP en FoodRanker"
        )
        val (title, body) = messages.random()
        // Canal propio, no el social. Compartiendo canal con los likes, silenciar el
        // recordatorio diario desde los ajustes de Android silenciaba también los avisos
        // de likes y valoraciones, y al revés — no había forma de separarlos.
        NotificationHelper.show(
            applicationContext, title, body,
            channelId = NotificationHelper.CHANNEL_DAILY
        )
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "daily_reminder"

        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 14)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                // Si ya pasaron las 14:00 hoy, programar para mañana
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}
