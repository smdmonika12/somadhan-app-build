package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৩২.৫
 *
 * `public.admin_get_dashboard_metrics()` RPC-এর jsonb রেসপন্স-এর DTO শেপ। ফিল্ডগুলো
 * `com.example.data.repository.AdminDashboardMetrics` (আগে FirebaseSyncManager.kt-এর ভেতরে
 * ছিল, নাম ছিল `FirestoreAdminMetrics` -- ধাপ ৩৩.৫-এ রিনেম হয়েছে)-এর সাথে সমান্তরাল রাখা হয়েছে,
 * যাতে `SomadhanViewModel.kt`-এ সহজেই ম্যাপ করা যায়।
 */
@Serializable
data class AdminDashboardMetricsDto(
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("total_solvers") val totalSolvers: Int = 0,
    @SerialName("total_problems") val totalProblems: Int = 0,
    @SerialName("open_problems") val openProblems: Int = 0,
    @SerialName("completed_problems") val completedProblems: Int = 0,
    @SerialName("in_progress_problems") val inProgressProblems: Int = 0,
    @SerialName("total_bids") val totalBids: Int = 0,
    @SerialName("pending_bids") val pendingBids: Int = 0,
    @SerialName("accepted_bids") val acceptedBids: Int = 0,
    @SerialName("total_transaction_volume") val totalTransactionVolume: Double = 0.0,
    @SerialName("platform_revenue") val platformRevenue: Double = 0.0,
    @SerialName("pending_withdrawals") val pendingWithdrawals: Int = 0,
    @SerialName("completed_withdrawals") val completedWithdrawals: Double = 0.0,
    @SerialName("category_problem_counts") val categoryProblemCounts: Map<String, Int> = emptyMap(),
    @SerialName("category_bid_counts") val categoryBidCounts: Map<String, Int> = emptyMap()
)
