package com.app.foodranker.utils

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.app.foodranker.R

class DailyReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        // Sin sesión no se recuerda nada: quien instala la app y no llega a registrarse
        // recibía igualmente el aviso de las 14:00, invitándole a "sumar XP" en una
        // cuenta que no existe. El worker queda programado; simplemente no molesta.
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val messages = listOf(
            R.string.rem_1_t to R.string.rem_1_b,
            R.string.rem_2_t to R.string.rem_2_b,
            R.string.rem_3_t to R.string.rem_3_b,
            R.string.rem_4_t to R.string.rem_4_b,
            R.string.rem_5_t to R.string.rem_5_b
        )
        val (tituloRes, cuerpoRes) = messages.random()
        val title = applicationContext.getString(tituloRes)
        val body = applicationContext.getString(cuerpoRes)
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
