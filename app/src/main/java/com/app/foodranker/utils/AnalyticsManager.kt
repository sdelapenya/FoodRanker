package com.app.foodranker.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsManager {

    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        analytics = Firebase.analytics
    }

    // ── Platos ────────────────────────────────────────────────────────────
    fun logPlatePublished(category: String) = log("plate_published") {
        putString("category", category)
    }

    fun logPlateLiked(plateId: String) = log("plate_liked") {
        putString("plate_id", plateId)
    }

    fun logPlateUnliked(plateId: String) = log("plate_unliked") {
        putString("plate_id", plateId)
    }

    fun logPlateRated(score: Float, plateId: String) = log("plate_rated") {
        putFloat("score", score)
        putString("plate_id", plateId)
    }

    fun logPlateShared(method: String) = log("plate_shared") {
        putString("method", method) // "text", "image", "thefork"
    }

    fun logPlateSaved(plateId: String) = log("plate_saved") {
        putString("plate_id", plateId)
    }

    fun logPlateReported(reason: String) = log("plate_reported") {
        putString("reason", reason)
    }

    fun logPlateCommented() = log("plate_commented")

    // ── Feed & Descubrimiento ─────────────────────────────────────────────
    fun logFeedModeChanged(mode: String) = log("feed_mode_changed") {
        putString("mode", mode) // "for_you" | "following"
    }

    fun logSearchPerformed(query: String, mode: String, resultsCount: Int) = log("search_performed") {
        putString("query_length", query.length.toString())
        putString("mode", mode)
        putInt("results_count", resultsCount)
    }

    fun logImageZoomUsed() = log("image_zoom_used")

    // ── Social ────────────────────────────────────────────────────────────
    fun logUserFollowed() = log("user_followed")
    fun logUserUnfollowed() = log("user_unfollowed")

    // ── Perfil ────────────────────────────────────────────────────────────
    fun logProfileEdited() = log("profile_edited")
    fun logLevelUp(newLevel: Int) = log("level_up") {
        putInt("new_level", newLevel)
    }
    fun logBadgeEarned(badgeId: String) = log("badge_earned") {
        putString("badge_id", badgeId)
    }

    // ── Monetización ──────────────────────────────────────────────────────
    fun logPremiumAttempt(source: String) = log("premium_attempt") {
        putString("source", source) // "screen", "feature_gate"
    }
    fun logPremiumPurchased() = log("premium_purchased")
    fun logRewardedAdWatched() = log("rewarded_ad_watched")

    // ── Onboarding ────────────────────────────────────────────────────────
    fun logOnboardingCompleted() = log("onboarding_completed")
    fun logOnboardingSkipped(atPage: Int) = log("onboarding_skipped") {
        putInt("at_page", atPage)
    }

    // ── Errores (no técnicos, sino de UX) ────────────────────────────────
    fun logFormAbandoned(step: Int) = log("form_abandoned") {
        putInt("step", step)
    }
    fun logImageRejected(reason: String) = log("image_rejected") {
        putString("reason", reason) // "not_food" | "too_small" | "api_error"
    }

    // ── Utilidad privada ──────────────────────────────────────────────────
    private fun log(event: String, params: (Bundle.() -> Unit)? = null) {
        val bundle = params?.let { Bundle().apply(it) }
        analytics?.logEvent(event, bundle)
    }
}
