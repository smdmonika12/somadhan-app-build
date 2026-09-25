package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object PostProblem : Screen("post_problem?instant={instant}") {
        fun createRoute(instant: Boolean = false) = "post_problem?instant=$instant"
    }
    object InstantJobs : Screen("instant_jobs")
    object InstantJobsBid : Screen("instant_jobs_bid/{problemId}") {
        fun createRoute(problemId: String) = "instant_jobs_bid/$problemId"
    }
    object InstantJobHistory : Screen("instant_job_history")
    object JobTracking : Screen("job_tracking/{problemId}") {
        fun createRoute(problemId: String) = "job_tracking/$problemId"
    }
    object DisputeResult : Screen("dispute_result/{problemId}") {
        fun createRoute(problemId: String) = "dispute_result/$problemId"
    }
    object ProblemDetail : Screen("problem_detail/{problemId}?readOnly={readOnly}") {
        fun createRoute(problemId: String, readOnly: Boolean = false) = "problem_detail/$problemId?readOnly=$readOnly"
    }
    object Messages : Screen("messages")
    object Chat : Screen("chat/{problemId}") {
        fun createRoute(problemId: String) = "chat/$problemId"
    }
    object Dashboard : Screen("dashboard")
    object SolverCompletedJobs : Screen("solver_completed_jobs")
    object SolverCategoryPosts : Screen("solver_category_posts/{categoryId}") {
        fun createRoute(categoryId: String) = "solver_category_posts/$categoryId"
    }
    object SolverAllPosts : Screen("solver_all_posts")
    object SolverActiveJobs : Screen("solver_active_jobs")
    object SolverOwnCompletedJobs : Screen("solver_own_completed_jobs")
    object SolverMyBids : Screen("solver_my_bids")
    object Profile : Screen("profile")
    object UserInfo : Screen("user_info")
    object UserProblems : Screen("user_problems")
    object UserActiveProblems : Screen("user_active_problems")
    object UserCancelledProblems : Screen("user_cancelled_problems")
    object UserCompletedProblems : Screen("user_completed_problems")
    object UserReviews : Screen("user_reviews")
    object SolverSkills : Screen("solver_skills")
    object SolverKyc : Screen("solver_kyc")
    object SolverBalanceWithdraw : Screen("solver_balance_withdraw")
    object SolverReviews : Screen("solver_reviews")
    object AdminPanel : Screen("admin_panel")
    object PublicProfile : Screen("public_profile/{userId}?role={role}") {
        fun createRoute(userId: String, role: String? = null) =
            if (!role.isNullOrBlank()) "public_profile/$userId?role=$role" else "public_profile/$userId"
    }
    object PublicProfileReviews : Screen("public_profile_reviews/{userId}?role={role}") {
        fun createRoute(userId: String, role: String? = null) =
            if (!role.isNullOrBlank()) "public_profile_reviews/$userId?role=$role" else "public_profile_reviews/$userId"
    }
    object ReputationDetail : Screen("reputation_detail/{userId}?role={role}") {
        fun createRoute(userId: String, role: String? = null) =
            if (!role.isNullOrBlank()) "reputation_detail/$userId?role=$role" else "reputation_detail/$userId"
    }
    object NotificationDetail : Screen("notification_detail/{notificationId}") {
        fun createRoute(notificationId: String) = "notification_detail/$notificationId"
    }
    object AllOpenProblems : Screen("all_open_problems")
    object BidManagement : Screen("bid_management")
    object FavoriteSolvers : Screen("favorite_solvers")
    object FAQ : Screen("faq")
    object SupportCenter : Screen("support_center")
    object Wallet : Screen("wallet")
    object UserWallet : Screen("user_wallet")
    object UserWithdraw : Screen("user_withdraw")
    object TransactionHistory : Screen("transaction_history")
    object WithdrawalHistory : Screen("withdrawal_history")
}
