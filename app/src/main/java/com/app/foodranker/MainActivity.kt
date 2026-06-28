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
import com.app.foodranker.utils.parseFoodRankerPlateId
import com.app.foodranker.utils.parseFoodRankerInviteCode
import com.app.foodranker.viewmodel.DiscoverViewModel
import com.app.foodranker.ui.screens.profile.FollowListScreen
import com.app.foodranker.utils.OnboardingManager
import com.app.foodranker.ui.screens.privacy.PrivacyPolicyScreen
import com.app.foodranker.ui.screens.privacy.TermsOfServiceScreen
import com.app.foodranker.ui.screens.splash.SplashScreen
import com.app.foodranker.ui.screens.trending.TrendingScreen
import com.app.foodranker.ui.screens.league.LeagueScreen
import com.app.foodranker.ui.screens.referral.ReferralScreen
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseOutCubic
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
import com.app.foodranker.utils.FoodRankerMessagingService


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
    var pendingPlateId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingInviteCode by rememberSaveable { mutableStateOf<String?>(null) }
    val startDestination = if (!OnboardingManager.isDone(context))
        Screen.Onboarding.route else Screen.Splash.route

    // Request POST_NOTIFICATIONS permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* result is informational — the system handles the denial UI */ }
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Deep links (cold start / onNewIntent) + notificación plateId
    DisposableEffect(lifecycleOwner, navController) {
        val activity = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val uri = activity.intent?.data
            if (uri != null) {
                uri.parseFoodRankerProfileUserId()?.let { pendingProfileUserId = it }
                uri.parseFoodRankerPlateId()?.let { pendingPlateId = it }
                uri.parseFoodRankerInviteCode()?.let { pendingInviteCode = it }
                activity.intent.setData(null)
            }

            val notifPlateId = activity.intent?.getStringExtra("plateId") ?: return@LifecycleEventObserver
            pendingPlateId = notifPlateId
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
                animationSpec = tween(200, easing = EaseOutCubic)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(200, easing = EaseOutCubic)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(200, easing = EaseOutCubic)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(200, easing = EaseOutCubic)
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
                awaitAuthReady = { authViewModel.awaitAuthReady() },
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
            val discoverViewModel: DiscoverViewModel = hiltViewModel()
            val pending = pendingProfileUserId
            LaunchedEffect(pending, authViewModel.isLoggedIn) {
                val uid = pending ?: return@LaunchedEffect
                if (!authViewModel.isLoggedIn) return@LaunchedEffect
                pendingProfileUserId = null
                navController.navigate(Screen.Profile.createRoute(uid)) {
                    launchSingleTop = true
                }
            }
            val pendingPlate = pendingPlateId
            LaunchedEffect(pendingPlate, authViewModel.isLoggedIn) {
                val pid = pendingPlate ?: return@LaunchedEffect
                if (!authViewModel.isLoggedIn) return@LaunchedEffect
                pendingPlateId = null
                navController.navigate(Screen.PlateDetail.createRoute(pid)) {
                    launchSingleTop = true
                }
            }
            val pendingInvite = pendingInviteCode
            LaunchedEffect(pendingInvite, authViewModel.isLoggedIn) {
                val code = pendingInvite ?: return@LaunchedEffect
                if (!authViewModel.isLoggedIn) return@LaunchedEffect
                pendingInviteCode = null
                discoverViewModel.applyInviteCode(code)
            }
            DiscoverScreen(
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToExploreUsers = { navController.navigate(Screen.ExploreUsers.route) },
                onNavigateToAddPlate = { navController.navigate(Screen.AddPlate.route) },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToPlateDetail = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToLeague = {
                    navController.navigate(Screen.League.route)
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
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                onNavigateToReferral = { navController.navigate(Screen.Referral.route) }
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
                },
                onUserClick = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.ExploreUsers.route) {
            ExploreScreen(
                startInUsersMode = true,
                onNavigateBack = { navController.popBackStack() },
                onPlateClick = { plateId ->
                    navController.navigate(Screen.PlateDetail.createRoute(plateId))
                },
                onUserClick = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid)) {
                        launchSingleTop = true
                    }
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
                    FoodRankerMessagingService.saveCurrentToken()
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

        composable(Screen.League.route) {
            LeagueScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiscover = { navController.popBackStack() },
                onNavigateToProfile = {
                    val uid = authViewModel.currentUser?.uid ?: return@LeagueScreen
                    navController.navigate(Screen.Profile.createRoute(uid)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Referral.route) {
            ReferralScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}