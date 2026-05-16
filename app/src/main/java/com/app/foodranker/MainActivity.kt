package com.app.foodranker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.foodranker.ui.Screen
import com.app.foodranker.ui.screens.addplate.AddPlateScreen
import com.app.foodranker.ui.screens.auth.AuthScreen
import com.app.foodranker.ui.screens.home.HomeScreen
import com.app.foodranker.ui.theme.FoodRankerTheme
import com.app.foodranker.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.app.foodranker.ui.screens.profile.ProfileScreen
import com.app.foodranker.ui.screens.explore.ExploreScreen
import com.app.foodranker.ui.screens.platedetail.PlateDetailScreen
import com.app.foodranker.ui.screens.premium.PremiumScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.foodranker.ui.screens.discover.DiscoverScreen
import com.app.foodranker.ui.screens.notifications.NotificationsScreen
import com.app.foodranker.ui.screens.onboarding.OnboardingScreen
import com.app.foodranker.utils.parseFoodRankerProfileUserId
import com.app.foodranker.ui.screens.profile.FollowListScreen
import com.app.foodranker.utils.OnboardingManager
import com.app.foodranker.ui.screens.privacy.PrivacyPolicyScreen
import com.app.foodranker.ui.screens.privacy.TermsOfServiceScreen
import com.app.foodranker.ui.screens.splash.SplashScreen
import com.app.foodranker.ui.screens.trending.TrendingScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.navArgument


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodRankerTheme {
                FoodRankerNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun FoodRankerNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingProfileUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val startDestination = if (!OnboardingManager.isDone(context))
        Screen.Onboarding.route else Screen.Splash.route

    // Deep link perfil desde intent (cold start / onNewIntent) + notificación plateId
    DisposableEffect(lifecycleOwner, navController) {
        val activity = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            activity.intent?.data?.parseFoodRankerProfileUserId()?.let {
                pendingProfileUserId = it
            }

            val plateId = activity.intent.getStringExtra("plateId") ?: return@LifecycleEventObserver
            if (!authViewModel.isLoggedIn) return@LifecycleEventObserver
            val route = navController.currentDestination?.route ?: return@LifecycleEventObserver
            if (route != Screen.Discover.route) return@LifecycleEventObserver
            navController.navigate(Screen.PlateDetail.createRoute(plateId)) {
                launchSingleTop = true
            }
            activity.intent.removeExtra("plateId")
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        }
    ) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    com.app.foodranker.utils.AnalyticsManager.logOnboardingCompleted()
                    OnboardingManager.markDone(context)
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Splash.route) {
            SplashScreen(
                isLoggedIn = authViewModel.isLoggedIn,
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Discover.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Discover.route) {
            val pending = pendingProfileUserId
            LaunchedEffect(pending, authViewModel.isLoggedIn) {
                val uid = pending ?: return@LaunchedEffect
                if (!authViewModel.isLoggedIn) return@LaunchedEffect
                pendingProfileUserId = null
                navController.navigate(Screen.Profile.createRoute(uid)) {
                    launchSingleTop = true
                }
            }
            DiscoverScreen(
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToAddPlate = { navController.navigate(Screen.AddPlate.route) },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToPlateDetail = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        composable(Screen.Trending.route) {
            TrendingScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlateClick = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                }
            )
        }

        composable(Screen.Home.route) {
            val pending = pendingProfileUserId
            LaunchedEffect(pending, authViewModel.isLoggedIn) {
                val uid = pending ?: return@LaunchedEffect
                if (!authViewModel.isLoggedIn) return@LaunchedEffect
                pendingProfileUserId = null
                navController.navigate(Screen.Profile.createRoute(uid)) {
                    launchSingleTop = true
                }
            }
            HomeScreen(
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToAddPlate = { navController.navigate(Screen.AddPlate.route) },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToPlateDetail = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                },
                onNavigateToTrending = { navController.navigate(Screen.Trending.route) }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(
                androidx.navigation.navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlate = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToFollowList = { uid, followers ->
                    navController.navigate(Screen.FollowList.createRoute(uid, followers))
                },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) }
            )
        }

        composable(
            route = Screen.FollowList.route,
            arguments = listOf(
                androidx.navigation.navArgument("userId") { type = NavType.StringType },
                androidx.navigation.navArgument("listType") { type = NavType.StringType }
            )
        ) {
            FollowListScreen(
                onNavigateBack = { navController.popBackStack() },
                onUserClick = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Explore.route) {
            ExploreScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlateClick = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                }
            )
        }

        composable(Screen.PlateDetail.route) { backStackEntry ->
            val plateId = backStackEntry.arguments?.getString("plateId") ?: ""
            PlateDetailScreen(
                plateId = plateId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Discover.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlate = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Terms.route) {
            TermsOfServiceScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AddPlate.route) {
            AddPlateScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Discover.route) {
                        popUpTo(Screen.Discover.route) { inclusive = false }
                    }
                }
            )
        }
    }
}