package com.app.foodranker.utils

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val messages = listOf(
            "¿Qué has comido hoy? 🍽️" to "Comparte tu mejor plato y suma XP",
            "¡Hora del almuerzo! 🌟" to "Descubre los mejores platos del mundo en FoodRanker",
            "¿Has probado algo nuevo? 🌍" to "Valora y comparte tu experiencia gastronómica",
            "El plato del día te espera 🏆" to "¿Cuál es el mejor plato que has comido esta semana?",
            "¡Sube de nivel! ⭐" to "Publica un plato hoy y gana XP en FoodRanker"
        )
        val (title, body) = messages.random()
        NotificationHelper.show(applicationContext, title, body)
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
