package com.app.foodranker.data.model

data class WeeklyChallenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val emoji: String = "🏆",
    val category: String = "",        // PlateCategory.name o "" para cualquier categoría
    val xpReward: Int = 100,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val participantCount: Int = 0,
    val participantIds: List<String> = emptyList()
) {
    val isActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startDate..endDate
        }

    val daysLeft: Int
        get() {
            val diff = endDate - System.currentTimeMillis()
            return if (diff > 0) (diff / (24 * 3600 * 1000)).toInt() else 0
        }
}
