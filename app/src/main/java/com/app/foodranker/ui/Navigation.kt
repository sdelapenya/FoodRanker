package com.app.foodranker.ui

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Splash    : Screen("splash")
    object Auth      : Screen("auth")
    object Discover : Screen("discover")
    object Premium : Screen("premium")
    object Explore : Screen("explore")
    object ExploreUsers : Screen("explore_users")
    object AddPlate : Screen("add_plate")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object FollowList : Screen("follow_list/{userId}/{listType}") {
        fun createRoute(userId: String, followers: Boolean) =
            "follow_list/$userId/${if (followers) LIST_FOLLOWERS else LIST_FOLLOWING}"

        const val LIST_FOLLOWERS = "followers"
        const val LIST_FOLLOWING = "following"
    }
    object PlateDetail : Screen("plate/{plateId}") {
        fun createRoute(plateId: String) = "plate/$plateId"
    }
    object Trending : Screen("trending")
    object Privacy : Screen("privacy")
    object Terms         : Screen("terms")
    object Notifications : Screen("notifications")
    object League : Screen("league")
    object Referral : Screen("referral")
}