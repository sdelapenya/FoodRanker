package com.app.foodranker.utils

import android.content.Context

object OnboardingManager {
    private const val PREFS = "foodranker_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DONE, false)

    fun markDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
}
