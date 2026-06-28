package com.app.foodranker.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyMissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val PREFS = "daily_mission"
        const val KEY_DATE = "mission_date"
        const val KEY_VOTES = "mission_votes"
        const val KEY_STREAK = "vote_streak"
        const val KEY_STREAK_DATE = "streak_date"
    }

    private fun todayKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    private fun yesterdayKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return sdf.format(cal.time)
    }

    fun getProgress(): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DATE, "") != todayKey()) return 0
        return prefs.getInt(KEY_VOTES, 0)
    }

    fun getStreak(): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val streakDate = prefs.getString(KEY_STREAK_DATE, "") ?: ""
        if (streakDate != todayKey() && streakDate != yesterdayKey()) return 0
        return prefs.getInt(KEY_STREAK, 0)
    }

    fun incrementVote(): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayKey()
        val isNewDay = prefs.getString(KEY_DATE, "") != today
        val current = if (!isNewDay) prefs.getInt(KEY_VOTES, 0) else 0
        val updated = current + 1
        val editor = prefs.edit().putString(KEY_DATE, today).putInt(KEY_VOTES, updated)
        if (isNewDay) {
            val streakDate = prefs.getString(KEY_STREAK_DATE, "") ?: ""
            val currentStreak = prefs.getInt(KEY_STREAK, 0)
            val newStreak = if (streakDate == yesterdayKey()) currentStreak + 1 else 1
            editor.putInt(KEY_STREAK, newStreak).putString(KEY_STREAK_DATE, today)
        }
        editor.apply()
        return updated
    }
}
