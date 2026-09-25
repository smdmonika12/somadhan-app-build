package com.example.data.repository

// [SUPABASE-MIGRATED - ধাপ ৩৩.৩, রিনেম করা হয়েছে ধাপ ৩৩.৫-এ] এই data class আগে
// FirebaseSyncManager.kt-এর ভেতরে ছিল এবং তখন নাম ছিল `FirestoreAdminMetrics` -- ধাপ ৩৩.৩-এ
// FirebaseSyncManager.kt ডিলিট হয়ে যাওয়ার সময় এটাকে এই আলাদা, স্বাধীন ফাইলে সরিয়ে রাখা হয়
// (একই প্যাকেজে, তাই কোনো import পরিবর্তন লাগেনি), কারণ এটা এখনো `SomadhanRepository.kt`/
// `SomadhanViewModel.kt`/একাধিক Admin screen থেকে রেফারেন্স করা হয় (live admin dashboard
// metrics মডেল হিসেবে)। এই class নিজে কোনো Firebase API ব্যবহার করে না (pure Kotlin data
// class) -- ধাপ ৩৩.৫-এ চূড়ান্ত "firebase/firestore" গ্রেপ শূন্য করার অংশ হিসেবে ক্লাস ও ফাইলের
// নাম `AdminDashboardMetrics`-এ রিনেম করা হলো (সব caller-এ mechanical rename, লজিক অপরিবর্তিত)।

/**
 * Real-time Admin Metrics Model
 */
data class AdminDashboardMetrics(
    val totalUsers: Int = 0,
    val totalSolvers: Int = 0,
    val totalClients: Int = 0,
    val totalProblems: Int = 0,
    val openProblems: Int = 0,
    val completedProblems: Int = 0,
    val inProgressProblems: Int = 0,
    val totalBids: Int = 0,
    val pendingBids: Int = 0,
    val acceptedBids: Int = 0,
    val totalTransactionVolume: Double = 0.0,
    val platformRevenue: Double = 0.0,
    val pendingWithdrawals: Int = 0,
    val completedWithdrawals: Double = 0.0,
    val isConnected: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncStatusMessage: String = "Supabase ক্লাউড রিয়েল-টাইম লাইভ",
    val categoryProblemCounts: Map<String, Int> = emptyMap(),
    val categoryBidCounts: Map<String, Int> = emptyMap()
)
