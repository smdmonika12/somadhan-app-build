package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextPrimary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.NoInternetOverlay
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.SomadhanActionBanner
import com.example.ui.navigation.Screen
import com.example.util.DistanceUtil
import com.example.util.LocationHelper
import com.example.util.NetworkConnectivityObserver
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AllOpenProblemsScreen
import com.example.ui.screens.BidManagementScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DisputeResultScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.FavoriteSolversScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InstantJobHistoryScreen
import com.example.ui.screens.InstantJobsScreen
import com.example.ui.screens.JobTrackingScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NotificationDetailScreen
import com.example.ui.screens.PostProblemScreen
import com.example.ui.screens.ProblemDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PublicProfileReviewsScreen
import com.example.ui.screens.PublicProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReputationDetailScreen
import com.example.ui.screens.SolverBalanceWithdrawScreen
import com.example.ui.screens.SolverCategoryPostsScreen
import com.example.ui.screens.SolverCompletedJobsScreen
import com.example.ui.screens.SolverKycScreen
import com.example.ui.screens.SolverMyBidsScreen
import com.example.ui.screens.SolverProblemsScreen
import com.example.ui.screens.SolverReviewsScreen
import com.example.ui.screens.SolverSkillsScreen
import com.example.ui.screens.SomadhanStandardPlaceholderScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SupportCenterScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.screens.UserInfoScreen
import com.example.ui.screens.UserProblemsScreen
import com.example.ui.screens.UserReviewsScreen
import com.example.ui.screens.UserWalletScreen
import com.example.ui.screens.UserWithdrawScreen
import com.example.ui.screens.WithdrawalHistoryScreen
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.viewmodel.SomadhanViewModel

fun findUnseenResult(problems: List<ProblemEntity>, user: UserEntity?, dismissedIds: Set<String> = emptySet()): ProblemEntity? {
    if (user == null) return null
    val isSolver = user.role.equals("SOLVER", ignoreCase = true)
    
    // Priority 1: Unseen resolved dispute (supports both normal posts and instant jobs)
    val unseenDispute = problems.firstOrNull { prob ->
        !dismissedIds.contains(prob.id) &&
        prob.disputeResolvedAt != null &&
        ((isSolver && prob.acceptedSolverId == user.id && !prob.disputeResultSeenBySolver) ||
         (!isSolver && prob.userId == user.id && !prob.disputeResultSeenByUser))
    }
    if (unseenDispute != null) return unseenDispute

    // Priority 2: Unseen normal completion (non-dispute)
    return problems.firstOrNull { prob ->
        !dismissedIds.contains(prob.id) &&
        prob.isInstantJob &&
        (prob.status == "COMPLETED" || prob.jobStatus == "JOB_COMPLETED" || prob.jobStatus == "COMPLETED") &&
        !prob.isDisputed && prob.disputeResolvedAt == null &&
        ((isSolver && prob.acceptedSolverId == user.id && !prob.completionResultSeenBySolver) ||
         (!isSolver && prob.userId == user.id && !prob.completionResultSeenByUser))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SomadhanBg
                ) {
                    SomadhanAppNavigation()
                }
            }
        }
    }
}

@Composable
fun SomadhanAppNavigation(
    viewModel: SomadhanViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val toastMessage by viewModel.uiToast.collectAsStateWithLifecycle()

    // Automatic runtime location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.refreshLiveLocation(showToast = false)
            viewModel.startContinuousLocationTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (!LocationHelper.hasLocationPermission(context)) {
            try {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } catch (_: Throwable) {}
        } else {
            viewModel.refreshLiveLocation(showToast = false)
            viewModel.startContinuousLocationTracking()
        }
    }

        // Real-time network connectivity monitoring
        val connectivityObserver = remember(context) { NetworkConnectivityObserver(context) }
        val isNetworkConnected by connectivityObserver.isConnectedFlow.collectAsStateWithLifecycle(
            initialValue = remember { connectivityObserver.isCurrentlyConnected() }
        )

    var manualOverrideConnected by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isNetworkConnected) {
        // Reset manual override whenever real-time callback emits a change
        manualOverrideConnected = null
    }

    val isOnline = manualOverrideConnected ?: isNetworkConnected

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val dismissedDisputeIds by viewModel.dismissedDisputeIds.collectAsStateWithLifecycle()
    val isMaintenanceMode by viewModel.isMaintenanceMode.collectAsStateWithLifecycle()
    val maintenanceMessage by viewModel.maintenanceMessage.collectAsStateWithLifecycle()
    // [Offline Action Gating ধাপ ৩] Strict Offline Block টগল (cached, ডিফল্ট true) —
    // নিচে global NoInternetOverlay/OfflineStatusBanner branch করার জন্য।
    val isStrictOfflineBlockEnabled by viewModel.isStrictOfflineBlockEnabled.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentUser != null && isMaintenanceMode && !currentUser?.role.equals("ADMIN", ignoreCase = true)) {
            MaintenanceScreen(
                maintenanceMessage = maintenanceMessage,
                currentUser = currentUser,
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                enterTransition = { fadeIn(animationSpec = tween(90)) },
                exitTransition = { fadeOut(animationSpec = tween(90)) },
                popEnterTransition = { fadeIn(animationSpec = tween(90)) },
                popExitTransition = { fadeOut(animationSpec = tween(90)) }
            ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                isSessionRestored = viewModel.isSessionRestored.collectAsState().value,
                onNavigateNext = {
                    val destination = if (viewModel.currentUser.value != null) {
                        Screen.Home.route
                    } else {
                        Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onAdminLoginSuccess = {
                    navController.navigate(Screen.AdminPanel.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Register Screen
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Route patterns that are transient, argument-based "detail" screens -- never meant to
        // be tab destinations in their own right. Built directly from Screen.kt's own {argument}
        // route patterns so this stays in sync if new detail screens are added later.
        val transientDetailRoutes = setOf(
            Screen.PostProblem.route,
            Screen.InstantJobsBid.route,
            Screen.JobTracking.route,
            Screen.DisputeResult.route,
            Screen.ProblemDetail.route,
            Screen.Chat.route,
            Screen.SolverCategoryPosts.route,
            Screen.PublicProfile.route,
            Screen.PublicProfileReviews.route,
            Screen.ReputationDetail.route,
            Screen.NotificationDetail.route
        )

        val navigateBottomTab: (String) -> Unit = { route ->
            // If currently sitting on one of the transient, argument-based detail screens above
            // (post details, chat, a public profile, the instant-job bid hub, etc.), pop it off
            // WITHOUT saving its state before switching tabs. Jetpack Navigation's
            // saveState = true (below) saves popped back-stack state keyed by ROUTE PATTERN, not
            // by the specific argument value -- so leaving a transient screen in place here would
            // cache THIS particular instance (e.g. problem_detail/{problemId} for one specific
            // post) under the shared route-pattern key, and a later restoreState = true
            // navigation matching that same pattern could resurrect that STALE entry instead of
            // behaving as expected. This was the root cause behind "হোম ট্যাবে ক্লিক করলে কিছু হয়
            // না, back চাপলে পুরনো পোস্ট ডিটেইলস পেজে চলে যায়" -- handleNavigation()'s
            // "instant_jobs_bid/" and "solver_category_posts/" branches already avoid
            // saveState/restoreState entirely for this same reason on the PUSH side; this loop
            // closes the matching gap on the bottom-nav-tab-switch side. Loops (not a single pop)
            // in case more than one transient screen is stacked (e.g. chat opened from a post
            // detail opened from a category list).
            while (navController.currentBackStackEntry?.destination?.route
                    ?.let { transientDetailRoutes.contains(it) } == true) {
                if (!navController.popBackStack()) break
            }
            if (route == Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) {
                        inclusive = false
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        val bottomNavRoutes = setOf("home", "dashboard", "wallet", "user_wallet", "messages", "profile", "solver_all_posts", "instant_jobs")
        val handleNavigation: (String) -> Unit = { route ->
            if (route == "instant_hub" || route == "instant_jobs" || route == Screen.InstantJobs.route) {
                val user = viewModel.currentUser.value
                val isSolver = user?.role.equals("SOLVER", ignoreCase = true)
                val unseenProblem = findUnseenResult(viewModel.allProblems.value, user)
                if (unseenProblem != null) {
                    val currentBackStack = navController.currentBackStackEntry
                    val currentRoute = currentBackStack?.destination?.route
                    val isDispute = unseenProblem.disputeResolvedAt != null
                    val targetRoute = if (isDispute) {
                        Screen.DisputeResult.createRoute(unseenProblem.id)
                    } else {
                        Screen.JobTracking.createRoute(unseenProblem.id)
                    }
                    val isAlreadyOnTargetScreen = if (isDispute) {
                        currentRoute?.startsWith("dispute_result") == true &&
                                (currentBackStack.arguments?.getString("problemId") == unseenProblem.id || currentRoute == targetRoute)
                    } else {
                        currentRoute?.startsWith("job_tracking") == true &&
                                (currentBackStack.arguments?.getString("problemId") == unseenProblem.id || currentRoute == targetRoute)
                    }
                    if (!isAlreadyOnTargetScreen) {
                        navController.navigate(targetRoute) {
                            launchSingleTop = true
                        }
                    }
                } else if (isSolver) {
                    navigateBottomTab(Screen.InstantJobs.route)
                } else {
                    val activeJob = viewModel.activeInstantJobForUser.value
                        ?: viewModel.allProblems.value.firstOrNull { prob ->
                            prob.isInstantJob &&
                            prob.userId == user?.id &&
                            prob.jobStatus != null &&
                            prob.jobStatus !in listOf("JOB_COMPLETED", "COMPLETED", "CANCELLED") &&
                            prob.status !in listOf("COMPLETED", "CANCELLED")
                        }
                    if (activeJob != null) {
                        navController.navigate(Screen.JobTracking.createRoute(activeJob.id))
                    } else {
                        navController.navigate(Screen.PostProblem.createRoute(instant = true))
                    }
                }
            } else if (route.startsWith("instant_jobs_bid/")) {
                // Bug fix (2 parts):
                // Part 1 (kept): this route (opened from a "বিড দিন" button on a normal Post
                // Details page) was previously reached via a plain navController.navigate(route)
                // call, which just pushed it on top of whatever was already on the back stack
                // instead of collapsing back to the Home tab first. That left users unable to
                // reach other menu items without repeatedly pressing Back. popUpTo(Home) here
                // still collapses the stack down to Home before pushing this screen, fixing that.
                //
                // Part 2 (new): this used to also use saveState = true / restoreState = true,
                // copying the bottom-nav-tab pattern from navigateBottomTab() below. That pattern
                // is only safe for argument-less, permanent destinations (Home, Dashboard,
                // Profile, ...) -- Navigation saves/restores state keyed by the destination's
                // route *pattern*, not by its arguments. ProblemDetail ("problem_detail/{problemId}")
                // and this InstantJobsBid route ("instant_jobs_bid/{problemId}") are both
                // parameterized, so saveState = true here was saving the ProblemDetail entry the
                // user just left, and a later restoreState = true (e.g. tapping "সব পোস্ট" or
                // "হোম" from the Instant Job Hub) could restore that stale saved ProblemDetail
                // entry instead of actually navigating to the tapped destination -- showing the
                // old post's details page instead of the posts list / home screen. Dropping
                // saveState/restoreState here removes that stale-entry risk while keeping the
                // same back-stack-collapsing behavior from Part 1.
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            } else if (route.startsWith("solver_category_posts/")) {
                // Bug fix: Home's category section reached this route ("বিড দিন" from Home ->
                // category -> post details -> Instant Job Hub redirect) via a plain
                // navController.navigate(route) call with no popUpTo at all -- unlike "সব পোস্ট"
                // (solver_all_posts), which is in bottomNavRoutes and always gets the clean
                // tab-style popUpTo(Home){saveState=true}/restoreState=true treatment below. That
                // asymmetry is exactly why re-tapping "সব পোস্ট" after the redirect worked fine
                // but re-tapping "হোম" after the SAME redirect from a category post kept showing
                // the old post's details page: with no popUpTo here, nothing ever collapsed this
                // route (or whatever was pushed on top of it, like ProblemDetail) back down to
                // Home, so it could still be sitting in the back stack for restoreState=true (see
                // the "instant_jobs_bid/" branch's comment above) to resurface later.
                //
                // Same fix, same reasoning as instant_jobs_bid: popUpTo(Home) collapses the stack
                // down first. No saveState/restoreState here either, for the same reason spelled
                // out above -- this is a parameterized route ("solver_category_posts/{categoryId}"),
                // and saveState/restoreState is only safe for the fixed, argument-less bottom-nav
                // destinations handled by navigateBottomTab() below.
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            } else if (bottomNavRoutes.contains(route)) {
                navigateBottomTab(route)
            } else {
                navController.navigate(route)
            }
        }

        val navigateToProblemOrTracking: (String) -> Unit = { problemId ->
            val problem = viewModel.getProblemByIdSync(problemId)
            val currentUser = viewModel.currentUser.value
            val currentUserId = currentUser?.id
            val currentUserRole = currentUser?.role
            val isSolver = currentUserRole.equals("SOLVER", ignoreCase = true)
            val isOwner = currentUserId != null && problem?.userId == currentUserId
            val isAssignedSolver = currentUserId != null && (
                problem?.acceptedSolverId == currentUserId
            )
            val isAdmin = currentUserRole.equals("ADMIN", ignoreCase = true)
            val isPartyToJob = isOwner || isAssignedSolver || isAdmin

            val hasActiveJobTracking = problem != null &&
                problem.isInstantJob &&
                problem.status != "COMPLETED" &&
                problem.status != "CANCELLED" &&
                (
                    problem.status == "IN_PROGRESS" ||
                    problem.jobStatus in listOf("ACCEPTED", "EN_ROUTE", "ARRIVED", "WORKING", "SUBMITTED", "PENDING_CONFIRMATION", "IN_PROGRESS") ||
                    !problem.acceptedSolverId.isNullOrBlank()
                )

            // The post owner's own "জরুরি" (instant) post always opens the Live Tracking page —
            // whether it's still broadcasting for bids, already accepted, or in-progress —
            // JobTrackingScreen renders the correct state for each stage internally.
            // NOTE: Once the post is finished (COMPLETED or CANCELLED), there is nothing left to
            // "track" live, so it must fall through to the normal Post Details page instead.
            val ownerAlwaysTracksOwnInstantPost = isOwner && problem != null && problem.isInstantJob &&
                problem.status != "COMPLETED" && problem.status != "CANCELLED"

            when {
                // Strict privacy guard: Only problem owner, assigned solver, or admin can access JobTracking screen.
                ownerAlwaysTracksOwnInstantPost || (isPartyToJob && hasActiveJobTracking) -> {
                    // Bug fix: navigateToProblemOrTracking() is ALWAYS reached directly from a
                    // list screen's onProblemClick (Home, All Open Problems, category posts,
                    // solver's own posts, notifications, etc. -- ~17 call sites in this file), a
                    // plain user tap on a post, never as a secondary hop inside the
                    // "instant_jobs_bid/" or "solver_category_posts/" redirect chains in
                    // handleNavigation() above (those branches call navController.navigate(route)
                    // directly, they never call this function). So popUpTo(Home) here was
                    // collapsing the ENTIRE back stack down to Home on every single post open,
                    // which is why pressing system Back from post details always landed on Home
                    // instead of the list screen the user actually came from. A plain stack push
                    // (no popUpTo) is correct here -- Back now returns to whatever list screen
                    // launched this. The stale-duplicate-entry bug that popUpTo(Home) was
                    // originally added to fix lives entirely in the "instant_jobs_bid/" and
                    // "solver_category_posts/" branches of handleNavigation() above, which keep
                    // their own popUpTo(Home) unchanged.
                    navController.navigate(Screen.JobTracking.createRoute(problemId)) {
                        launchSingleTop = true
                    }
                }
                else -> {
                    // Any other user — including a solver whose category/radar-radius match a
                    // still-broadcasting জরুরি post — lands on the normal post detail page.
                    // An eligible solver sees an enabled "বিড দিন" button there; tapping it is
                    // what takes them to "জরুরি পোস্ট হাব" (InstantJobsScreen) with this post's
                    // bid popup opened automatically.
                    // Same fix/reasoning as the JobTracking branch above: this is always a direct
                    // user tap from a list screen, never part of a redirect chain, so no
                    // popUpTo(Home) here either -- plain push so Back returns to the list.
                    navController.navigate(Screen.ProblemDetail.createRoute(problemId)) {
                        launchSingleTop = true
                    }
                }
            }
        }

        // 4. Home Screen (Bottom Nav: "home")
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                onPostProblemClick = {
                    navController.navigate(Screen.PostProblem.createRoute(instant = false))
                },
                onAdminClick = {
                    navController.navigate(Screen.AdminPanel.route)
                }
            )
        }

        // 5. Post Problem Screen (supports normal mode and emergency instant mode)
        composable(
            route = Screen.PostProblem.route,
            arguments = listOf(
                navArgument("instant") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isInstant = backStackEntry.arguments?.getBoolean("instant") ?: false
            PostProblemScreen(
                viewModel = viewModel,
                isInstantMode = isInstant,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onHistoryClick = {
                    navController.navigate(Screen.InstantJobHistory.route)
                },
                onProblemCreated = { createdProblemId ->
                    if (isInstant && !createdProblemId.isNullOrBlank()) {
                        navController.navigate(Screen.JobTracking.createRoute(createdProblemId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        // 5.1 Instant Jobs Screen (Solver side - On/Off Toggle & Live Nearby Broadcast Feed)
        composable(Screen.InstantJobs.route) {
            InstantJobsScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onNavigateBack = { navController.popBackStack() },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 5.1.b Instant Jobs Screen opened directly on a specific post — auto-opens that post's
        // bid popup for a matching solver (category + radar radius matched, eligible to bid).
        composable(
            route = Screen.InstantJobsBid.route,
            arguments = listOf(navArgument("problemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetProblemId = backStackEntry.arguments?.getString("problemId")
            InstantJobsScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onNavigateBack = { navController.popBackStack() },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                initialBidProblemId = targetProblemId
            )
        }

        // 5.1.1 Instant Job History Screen (Completed / Cancelled / Disputed tabs)
        composable(Screen.InstantJobHistory.route) {
            InstantJobHistoryScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5.2 Job Tracking Screen (Live Tracking)
        composable(
            route = Screen.JobTracking.route,
            arguments = listOf(
                navArgument("problemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            JobTrackingScreen(
                problemId = problemId,
                viewModel = viewModel,
                onNavigateBack = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        val isSolver = viewModel.currentUser.value?.role.equals("SOLVER", ignoreCase = true)
                        val fallbackRoute = if (isSolver) Screen.InstantJobs.route else Screen.Home.route
                        navController.navigate(fallbackRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // 5.3 Dispute Result Screen (Mandatory Full-Screen Result)
        composable(
            route = Screen.DisputeResult.route,
            arguments = listOf(
                navArgument("problemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            DisputeResultScreen(
                problemId = problemId,
                viewModel = viewModel,
                onNavigateBack = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        val isSolver = viewModel.currentUser.value?.role.equals("SOLVER", ignoreCase = true)
                        val fallbackRoute = if (isSolver) Screen.InstantJobs.route else Screen.Home.route
                        navController.navigate(fallbackRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 6. Problem Detail Screen
        composable(
            route = Screen.ProblemDetail.route,
            arguments = listOf(
                navArgument("problemId") { type = NavType.StringType },
                navArgument("readOnly") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            val readOnly = backStackEntry.arguments?.getBoolean("readOnly") ?: false
            ProblemDetailScreen(
                problemId = problemId,
                readOnly = readOnly,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onNavigateToKyc = {
                    navController.navigate(Screen.SolverKyc.route)
                },
                // Bug fix: was a raw navController.navigate(route) call, which bypassed
                // handleNavigation's popUpTo/launchSingleTop handling entirely (see the new
                // "instant_jobs_bid/" branch above). Routing through handleNavigation here fixes
                // the "বিড দিন" button specifically while behaving identically to before for
                // every other route ProblemDetailScreen navigates to (PublicProfile, JobTracking,
                // DisputeResult, etc.), since those all fall through handleNavigation's own
                // else-branch to the same plain navController.navigate(route) call.
                onNavigate = handleNavigation
            )
        }

        // 7. Messages Screen (Bottom Nav: "messages")
        composable(Screen.Messages.route) {
            MessagesScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onOpenChat = { problemId ->
                    navController.navigate(Screen.Chat.createRoute(problemId))
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                onAdminClick = {
                    navController.navigate(Screen.AdminPanel.route)
                }
            )
        }

        // 8. Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("problemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            ChatScreen(
                problemId = problemId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 9. Dashboard Screen (Bottom Nav: "dashboard")
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onNavigateToCompletedJobs = {
                    navController.navigate(Screen.SolverCompletedJobs.route)
                },
                onNavigateToWithdraw = {
                    navController.navigate(Screen.SolverBalanceWithdraw.route)
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                onAdminClick = {
                    navController.navigate(Screen.AdminPanel.route)
                }
            )
        }

        // 10. Solver Completed Jobs Screen
        composable(Screen.SolverCompletedJobs.route) {
            SolverCompletedJobsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 10b. Solver All Posts Screen
        composable(Screen.SolverAllPosts.route) {
            com.example.ui.screens.SolverAllPostsScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 10b2. Solver Category Posts Screen
        composable(
            route = Screen.SolverCategoryPosts.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            SolverCategoryPostsScreen(
                categoryId = categoryId,
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 10c. Solver Active Jobs Screen
        composable(Screen.SolverActiveJobs.route) {
            SolverProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                initialTab = 0
            )
        }

        // 10d. Solver Own Completed Jobs Screen
        composable(Screen.SolverOwnCompletedJobs.route) {
            SolverProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                initialTab = 1
            )
        }

        // 10e. Solver My Bids Screen
        composable(Screen.SolverMyBids.route) {
            SolverMyBidsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 11. Profile Screen (Bottom Nav: "profile")
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigate = handleNavigation,
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAdminClick = {
                    navController.navigate(Screen.AdminPanel.route)
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 12. User Info Screen
        composable(Screen.UserInfo.route) {
            UserInfoScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 13. User Problems Screen
        composable(Screen.UserProblems.route) {
            UserProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 13b. User Active Problems Screen
        composable(Screen.UserActiveProblems.route) {
            UserProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                initialTab = 0
            )
        }

        // 13c. User Cancelled Problems Screen
        composable(Screen.UserCancelledProblems.route) {
            UserProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                initialTab = 1
            )
        }

        // 13d. User Completed Problems Screen
        composable(Screen.UserCompletedProblems.route) {
            UserProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                },
                initialTab = 2
            )
        }

        // 14. User Reviews Screen
        composable(Screen.UserReviews.route) {
            UserReviewsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 15. Solver Skills Screen
        composable(Screen.SolverSkills.route) {
            SolverSkillsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 16. Solver KYC Screen
        composable(Screen.SolverKyc.route) {
            SolverKycScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 17. Solver Balance & Withdraw Screen
        composable(Screen.SolverBalanceWithdraw.route) {
            SolverBalanceWithdrawScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigate = handleNavigation,
                onNavigateToSupport = { navController.navigate(Screen.SupportCenter.route) }
            )
        }

        // 18. Solver Reviews Screen
        composable(Screen.SolverReviews.route) {
            SolverReviewsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 19. Admin Panel Screen
        composable(Screen.AdminPanel.route) {
            AdminPanelScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigate = { route ->
                    navController.navigate(route)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 20. Public Profile Screen
        composable(
            route = Screen.PublicProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("role") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val role = backStackEntry.arguments?.getString("role")
            PublicProfileScreen(
                userId = userId,
                profileRole = role,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSeeAllReviewsClick = {
                    navController.navigate(Screen.PublicProfileReviews.createRoute(userId, role))
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // 21. Public Profile Reviews Screen
        composable(
            route = Screen.PublicProfileReviews.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("role") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val role = backStackEntry.arguments?.getString("role")
            PublicProfileReviewsScreen(
                userId = userId,
                profileRole = role,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 22. Reputation Detail Screen
        composable(
            route = Screen.ReputationDetail.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("role") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val role = backStackEntry.arguments?.getString("role")
            val isOwnProfile = userId == viewModel.currentUser.value?.id
            ReputationDetailScreen(
                viewModel = viewModel,
                userId = userId,
                targetRole = role,
                isOwnProfile = isOwnProfile,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 23. Notification Detail Screen
        composable(
            route = Screen.NotificationDetail.route,
            arguments = listOf(navArgument("notificationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
            NotificationDetailScreen(
                viewModel = viewModel,
                notificationId = notificationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 24. All Open Problems Screen
        composable(Screen.AllOpenProblems.route) {
            AllOpenProblemsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 25. Bid Management Screen
        composable(Screen.BidManagement.route) {
            BidManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { problemId ->
                    navController.navigate(Screen.Chat.createRoute(problemId))
                },
                onNavigateToProblemDetail = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 26. Favorite Solvers Screen
        composable(Screen.FavoriteSolvers.route) {
            FavoriteSolversScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.PublicProfile.createRoute(userId, "SOLVER"))
                },
                onOpenChat = { problemId ->
                    navController.navigate(Screen.Chat.createRoute(problemId))
                }
            )
        }

        // 27. FAQ Screen
        composable(Screen.FAQ.route) {
            FaqScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSupportClick = { navController.navigate(Screen.SupportCenter.route) }
            )
        }

        // 28. Support Center Screen
        composable(Screen.SupportCenter.route) {
            SupportCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 29. Wallet Screen (Bottom Nav: "wallet" & "user_wallet")
        composable(Screen.Wallet.route) {
            UserWalletScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigate = handleNavigation,
                onNavigateToSupport = { navController.navigate(Screen.SupportCenter.route) },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }
        composable(Screen.UserWallet.route) {
            UserWalletScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigate = handleNavigation,
                onNavigateToSupport = { navController.navigate(Screen.SupportCenter.route) },
                onProblemClick = { problemId ->
                    navigateToProblemOrTracking(problemId)
                }
            )
        }

        // 29b. User Withdraw Screen (Full-page withdraw & history for user)
        composable(Screen.UserWithdraw.route) {
            UserWithdrawScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigate = handleNavigation,
                onNavigateToSupport = { navController.navigate(Screen.SupportCenter.route) }
            )
        }

        // 30. Transaction History Screen (Paginated: 10 items/page)
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 31. Withdrawal History Screen (Paginated: 10 items/page)
        composable(Screen.WithdrawalHistory.route) {
            WithdrawalHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSupport = { navController.navigate(Screen.SupportCenter.route) }
            )
        }
    }
        }

        SomadhanActionBanner(
            message = toastMessage,
            onDismiss = { viewModel.clearToast() }
        )

        // Real-time No Internet feedback across entire application.
        // [Offline Action Gating ধাপ ৩] isStrictOfflineBlockEnabled (cached টগল, ডিফল্ট true)
        // অনুযায়ী শাখা করা হয়েছে — splash-screen exception ও manualOverrideConnected লজিক
        // (isOnline-এর মধ্যেই আগে থেকে যুক্ত) দুই মোডেই অপরিবর্তিত থাকে।
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isSplashScreen = currentRoute == null || currentRoute == Screen.Splash.route

        val isSolver = currentUser?.role == "SOLVER"
        if (isStrictOfflineBlockEnabled) {
            // Strict মোড (টগল ON, ডিফল্ট): পুরনো আচরণ অপরিবর্তিত — পুরো অ্যাপ full-screen
            // ব্লক ও নিচের UI-তে ক্লিক-ইন্টারসেপ্ট (NoInternetScreen.kt দ্রষ্টব্য)।
            NoInternetOverlay(
                isVisible = !isOnline && !isSplashScreen,
                isSolver = isSolver,
                onRetry = {
                    val connected = connectivityObserver.isCurrentlyConnected()
                    manualOverrideConnected = if (connected) true else null
                    connected
                }
            )
        } else {
            // Non-strict মোড (টগল OFF): পুরো-অ্যাপ ব্লকের বদলে ছোট non-blocking status
            // banner — cached ডেটা দেখা ও নেভিগেশন স্বাভাবিকভাবে চলবে; শুধু network-writing
            // action-গুলো আলাদাভাবে (ধাপ ৪-এর guard দিয়ে) block হবে।
            OfflineStatusBanner(
                isVisible = !isOnline && !isSplashScreen
            )
        }
    }
}
