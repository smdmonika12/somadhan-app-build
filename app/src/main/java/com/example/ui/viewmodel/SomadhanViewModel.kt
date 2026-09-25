package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.local.SolverReminderPrefs
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.GatewayPaymentEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.PlatformSettingEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.remote.SupabaseRealtimeManager
import com.example.data.remote.SupabaseSyncManager
import com.example.data.repository.AdminDashboardMetrics
import com.example.data.remote.SupabaseAuthManager
import com.example.data.repository.SomadhanRepository
import com.example.data.repository.EscrowReleaseRejectedException
import com.example.data.repository.MissingRefundRepairReport
import com.example.data.repository.BalanceReconciliationReport
import com.example.data.repository.MissingRefundRepairItem
import com.example.data.repository.WithdrawalUpdateResult
import com.example.data.repository.PlatformSettingUpdateResult
import com.example.data.security.PasswordHasher
import com.example.data.security.AdminAccountInfo
import com.example.data.security.AdminActivityLogEntry
import com.example.data.security.AdminActivityLogFilter
import com.example.data.security.AdminPermissionCatalog
import com.example.data.security.AdminProfileRules
import com.example.data.security.AdminRoleInfo
import com.example.data.security.AdminSession
import com.example.data.security.AdminSessionRecord
import com.example.util.AdminProfilePhotoUploader
import com.example.util.AiMatcherUtil
import com.example.util.DistanceUtil
import com.example.util.ImageStorageUtil
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.LocationResult
import com.example.util.OtpSendResult
import com.example.util.OtpService
import com.example.util.OtpVerifyResult
import com.example.worker.ScheduledNotificationWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BidBlockReason {
    NOT_SOLVER,
    POST_OWNER,
    BANNED,
    RESTRICTED,
    POST_NOT_OPEN,
    CATEGORY_MISMATCH,
    LOCATION_UNAVAILABLE,
    TOO_FAR,
    KYC_REQUIRED,
    ALREADY_BID,
    CANCELLED_PREVIOUSLY,
    INVALID_BUDGET
}

data class BidEligibility(
    val canBid: Boolean,
    val reason: BidBlockReason? = null,
    val calculatedDistanceKm: Double? = null
)

data class ActivePostWithActivity(
    val problem: ProblemEntity,
    val hasUnseenActivity: Boolean
)
typealias ActiveProblemItem = ActivePostWithActivity

data class NearbyInstantJobItem(
    val problem: ProblemEntity,
    val distanceKm: Double,
    val formattedDistance: String
)

class SomadhanViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // How recently a full pullAllCloudDataToLocal() must have succeeded before another one on
        // app startup is considered redundant (see the comment in init{} for why this is safe --
        // the real-time listeners already deliver the same scoped data independently).
        private const val FULL_SYNC_MIN_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        /** [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগের প্রতি পেজে সারি (সার্ভার সর্বোচ্চ ১০০ পর্যন্ত মানে)। */
        private const val ADMIN_ACTIVITY_LOG_PAGE_SIZE = 30
    }

    private val repository = SomadhanRepository(AppDatabase.getDatabase(application))
    private val sharedPrefs = application.getSharedPreferences("somadhan_session", Context.MODE_PRIVATE)

    // Motion/loading-skeleton session cache: tracks which screens' core data has
    // already loaded at least once in this app session (this ViewModel instance
    // survives across navigation, only reset by process death/app restart -
    // unlike a screen's own `remember` state, which resets on every fresh visit).
    // Screens check hasLoadedOnce(key) before showing a first-load skeleton, and
    // call markLoadedOnce(key) once their data arrives - so re-visiting an
    // already-loaded screen shows real content instantly, with no skeleton flash,
    // matching how Instagram/Uber/etc. only skeleton a genuinely cold load.
    private val sessionLoadedScreens = mutableSetOf<String>()
    fun hasLoadedOnce(screenKey: String): Boolean = sessionLoadedScreens.contains(screenKey)
    fun markLoadedOnce(screenKey: String) { sessionLoadedScreens.add(screenKey) }

    // [Offline Action Gating ধাপ ১৩ - ফলো-আপ] এই ইউজারের জন্য অন্তত একবার সম্পূর্ণ (কোনো টেবিল
    // ব্যর্থ না হওয়া) bulk-pull সফল হয়েছে কিনা -- SharedPreferences-এ persisted, তাই app restart-এর
    // পরেও (অফলাইনেও) জানা যায় যে লোকাল Room-এ দেখানোর মতো আসল ডেটা আছে। MotionToolkit-এর
    // SyncAware* কম্পোনেন্ট এটা দেখে: ERROR অবস্থায় (যেমন "wifi আছে কিন্তু ইন্টারনেট নেই"-তে
    // bulk-pull ২০s timeout খেলে) non-strict মোডে SyncErrorState-এর বদলে cached ডেটা দেখায়।
    // `last_full_sync_at` এই কাজে ব্যবহার করা যায় না -- ওটা ব্যর্থ pull-এর পরেও লেখা হয়।
    fun hasCleanSyncHistory(): Boolean {
        val userId = sharedPrefs.getString("saved_user_id", null) ?: return false
        return sharedPrefs.getBoolean("clean_bulk_sync_$userId", false)
    }

    private fun recordCleanSyncIfApplicable() {
        val userId = sharedPrefs.getString("saved_user_id", null) ?: return
        if (!sharedPrefs.getBoolean("clean_bulk_sync_$userId", false)) {
            sharedPrefs.edit().putBoolean("clean_bulk_sync_$userId", true).apply()
        }
    }

    private var userDataJobs: MutableList<Job> = mutableListOf()

    // Current User Session
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Session restore state
    private val _isSessionRestored = MutableStateFlow(false)
    val isSessionRestored: StateFlow<Boolean> = _isSessionRestored.asStateFlow()

    // Real-time user GPS location
    private val _liveLocation = MutableStateFlow<LocationResult>(
        LocationHelper.BD_DEFAULT_LOCATIONS.first()
    )
    val liveLocation: StateFlow<LocationResult> = _liveLocation.asStateFlow()

    // Real-time location updating indicator for 10-second ticker & manual refresh
    private val _isLocationUpdating = MutableStateFlow(false)
    val isLocationUpdating: StateFlow<Boolean> = _isLocationUpdating.asStateFlow()

    // Refreshing state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১c] `loginAsAdmin()`-এ real Supabase Auth সাইন-ইন ব্যর্থ
    // হলে আগে শুধু একবার ২-৩ সেকেন্ডের toast দেখানো হতো, তারপর admin ভুলে যেতে পারতেন যে তিনি
    // এখনো degraded local-only সেশনে আছেন (যেখানে RLS-গেটেড সব cloud write নীরবে ব্যর্থ হয়)।
    // এখন এই state persistent থাকে যতক্ষণ না admin নিজে dismiss করেন বা আবার সফলভাবে লগইন করেন
    // (dismissDegradedAdminSessionWarning()/logout()-এ রিসেট হয়) — AdminPanelScreen-এর টপ-বারে
    // RealtimeLocationBar-এর ঠিক নিচে persistent bar হিসেবে দেখানো হবে (সব admin ট্যাবেই দৃশ্যমান,
    // কারণ সবকটা ট্যাব একই AdminPanelScreen Scaffold-এর ভেতরে)।
    private val _isDegradedAdminSession = MutableStateFlow(false)
    val isDegradedAdminSession: StateFlow<Boolean> = _isDegradedAdminSession.asStateFlow()
    private val _degradedAdminSessionReason = MutableStateFlow<String?>(null)
    val degradedAdminSessionReason: StateFlow<String?> = _degradedAdminSessionReason.asStateFlow()

    // [ADMIN_ROLE_PROFILE সেশন ২] heartbeat-এ সার্ভার জানালে (অ্যাকাউন্ট নিষ্ক্রিয়/এডমিন-রো মুছে গেছে)
    // AdminPanelScreen এটা দেখে টোস্ট + লগআউট চালায় — non-null = কারণ-বার্তা, consume করলে null।
    private val _adminForcedLogoutReason = MutableStateFlow<String?>(null)
    val adminForcedLogoutReason: StateFlow<String?> = _adminForcedLogoutReason.asStateFlow()
    fun consumeAdminForcedLogout() { _adminForcedLogoutReason.value = null }
    private var adminHeartbeatJob: Job? = null

    // ============================================================
    // [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] রোল ম্যানেজমেন্ট (সুপার-অনলি) — state + অ্যাকশন
    // ============================================================
    // `admin_roles_list` RPC bulk-pull/realtime-এর অংশ না (একটা সুপার-অনলি one-shot RPC), তাই
    // initialSyncPhase-এর উপর ভর না করে নিজস্ব একটা phase রাখা হলো (categoriesSyncPhase/faqsSyncPhase-এর
    // মতোই কনভেনশন) — `SyncAwareContent(sessionKey = "admin_role_mgmt_sync")` এটাই পড়ে।
    //   LOADING = এখনো একবারও সফলভাবে আসেনি (cold-load skeleton), LOADED = অন্তত একবার এসেছে,
    //   ERROR   = একবারও আসেনি এবং সর্বশেষ চেষ্টা ব্যর্থ ("আবার চেষ্টা করুন" দেখায়)।
    // একবার LOADED হলে পরের কোনো রিফ্রেশ ব্যর্থ হলেও পুরনো তালিকা থাকে (ERROR-এ ফেরে না) — ব্যবহারকারী
    // ভুল করে "কোনো রোল নেই" দেখে না।
    private val _adminRoles = MutableStateFlow<List<AdminRoleInfo>>(emptyList())
    val adminRoles: StateFlow<List<AdminRoleInfo>> = _adminRoles.asStateFlow()
    private val _adminRolesSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADING)
    val adminRolesSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _adminRolesSyncPhase.asStateFlow()
    private var adminRolesLoadJob: Job? = null

    private fun isCurrentAdminSuper(): Boolean = AdminSession.current?.account?.isSuper == true

    /** একবার সার্ভার থেকে রোল-তালিকা এনে state বসায়। সফল হলে true। ([loadAdminRoles]/pull-to-refresh/সেভ-ডিলিটের পরে ব্যবহৃত) */
    private suspend fun fetchAdminRolesOnce(): Boolean {
        val res = SupabaseSyncManager.adminRolesList()
        return if (res.isSuccess) {
            _adminRoles.value = res.getOrNull().orEmpty().mapNotNull { AdminRoleInfo.fromJson(it) }
            _adminRolesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADED
            true
        } else {
            Log.e("SomadhanViewModel", "fetchAdminRolesOnce failed: ${res.exceptionOrNull()?.message}")
            if (_adminRolesSyncPhase.value != SupabaseRealtimeManager.SyncPhase.LOADED) {
                _adminRolesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.ERROR
            }
            false
        }
    }

    /** রোল-ম্যানেজমেন্ট ট্যাব খোলা/retry-তে কল হয়। সুপার না হলে কিছু করে না (সার্ভারও আটকায়)। ইতিমধ্যে চললে দ্বিতীয়বার শুরু করে না। */
    fun loadAdminRoles() {
        if (!isCurrentAdminSuper()) return
        if (adminRolesLoadJob?.isActive == true) return
        if (_adminRolesSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR) {
            _adminRolesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
        }
        adminRolesLoadJob = viewModelScope.launch { fetchAdminRolesOnce() }
    }

    /**
     * রোল তৈরি ([id] == null) বা আপডেট। [onDone] সফলতা জানায় — ভিউ সফল হলেই এডিটর বন্ধ করে, ব্যর্থ হলে
     * (যেমন ROLE_NAME_TAKEN) এডিটর খোলা থাকে যাতে টিক-করা পারমিশনগুলো হারিয়ে না যায়।
     * সার্ভার-এরর `roleErrorMessage`-এর বাংলা বার্তায় টোস্ট হয়।
     */
    fun adminSaveRole(
        id: String?,
        name: String,
        permissions: Set<String>,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!isCurrentAdminSuper()) {
                showToast(com.example.ui.screens.roleErrorMessage("SUPER_ADMIN_REQUIRED"))
                onDone(false)
                return@launch
            }
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রোল সংরক্ষণ করা যাবে না")) {
                onDone(false)
                return@launch
            }
            // defense-in-depth: ক্যাটালগে নেই এমন কী (বিশেষত admin_mgmt:* — সুপার-অনলি, কোনো রোলে
            // বরাদ্দযোগ্য না) সার্ভারে পাঠানোই হয় না।
            val allowed = AdminPermissionCatalog.nonSuperKeys().toSet()
            val cleaned = permissions.filter { it in allowed }.toSet()
            val res = SupabaseSyncManager.adminRoleUpsert(id, name.trim(), cleaned)
            if (res.isSuccess) {
                showToast(if (id == null) "নতুন রোল তৈরি হয়েছে।" else "রোল আপডেট হয়েছে।")
                fetchAdminRolesOnce()
                onDone(true)
            } else {
                Log.e("SomadhanViewModel", "adminSaveRole failed: ${res.exceptionOrNull()?.message}")
                showToast(com.example.ui.screens.roleErrorMessage(res.exceptionOrNull()?.message.orEmpty()))
                onDone(false)
            }
        }
    }

    /** রোল ডিলিট। অ্যাসাইনড অ্যাকাউন্ট থাকলে ক্লায়েন্টেই আগে আটকায় (সার্ভারের ROLE_IN_USE গার্ড এর পরেও আছে)। */
    fun adminDeleteRole(role: AdminRoleInfo) {
        viewModelScope.launch {
            if (!isCurrentAdminSuper()) {
                showToast(com.example.ui.screens.roleErrorMessage("SUPER_ADMIN_REQUIRED"))
                return@launch
            }
            if (role.isSuper) {
                showToast(com.example.ui.screens.roleErrorMessage("SUPER_ROLE_IMMUTABLE"))
                return@launch
            }
            if (role.accountCount > 0) {
                showToast(com.example.ui.screens.roleErrorMessage("ROLE_IN_USE"))
                return@launch
            }
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রোল মুছে ফেলা যাবে না")) return@launch
            val res = SupabaseSyncManager.adminRoleDelete(role.id)
            if (res.isSuccess) {
                showToast("রোল মুছে ফেলা হয়েছে।")
            } else {
                Log.e("SomadhanViewModel", "adminDeleteRole failed: ${res.exceptionOrNull()?.message}")
                showToast(com.example.ui.screens.roleErrorMessage(res.exceptionOrNull()?.message.orEmpty()))
            }
            // সফল/ব্যর্থ দুই ক্ষেত্রেই রিফ্রেশ — ব্যর্থতা "অন্য কোথাও বদলে গেছে" (ROLE_NOT_FOUND/ROLE_IN_USE) হলে তালিকা ঠিক হয়ে যায়।
            fetchAdminRolesOnce()
        }
    }

    // ============================================================
    // [ADMIN_ROLE_PROFILE সেশন ৪] এডমিন অ্যাকাউন্ট ম্যানেজমেন্ট (সুপার-অনলি) — state + অ্যাকশন
    // ============================================================
    // রোল-তালিকার ([adminRoles]) মতোই কনভেনশন: `admin_accounts_list` bulk-pull/realtime-এর অংশ না (একটা
    // সুপার-অনলি one-shot RPC), তাই নিজস্ব sync-phase। LOADING = এখনো একবারও আসেনি (cold-load skeleton),
    // LOADED = অন্তত একবার এসেছে, ERROR = একবারও আসেনি এবং সর্বশেষ চেষ্টা ব্যর্থ। একবার LOADED হলে পরের
    // রিফ্রেশ ব্যর্থ হলেও পুরনো তালিকা থাকে।
    //
    // 🟢 অনলাইন-ডট (প্রশ্ন ৩ কনফার্মড: "রিয়েল-টাইম heartbeat এবং last login দুটোই"): সার্ভার প্রতিটা অ্যাকাউন্টের
    // `is_online` হিসাব করে (৯০ সেকেন্ডের মধ্যে heartbeat এসেছে কিনা; প্রতিটা এডমিনের অ্যাপ ~৩০সে পরপর
    // heartbeat পাঠায়)। `admin_accounts` realtime publication-এ নেই (সেশন ১-এর মুলতুবি সিদ্ধান্ত) — তাই ট্যাব
    // খোলা থাকা অবস্থায় UI নিজেই ~২০সে পরপর নিঃশব্দে ([loadAdminAccounts] `silent = true`) তালিকা রিফ্রেশ করে।
    // সিদ্ধান্ত: realtime publication/presence-channel বাড়তি জটিলতা — ৯০সে-এর সার্ভার-হিসাবের সাথে ২০সে পোলিং
    // যথেষ্ট "লাইভ" (সর্বোচ্চ ~২০সে দেরি), আর কোনো নতুন মাইগ্রেশন/RLS লাগে না।
    private val _adminAccounts = MutableStateFlow<List<AdminAccountInfo>>(emptyList())
    val adminAccounts: StateFlow<List<AdminAccountInfo>> = _adminAccounts.asStateFlow()
    private val _adminAccountsSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADING)
    val adminAccountsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _adminAccountsSyncPhase.asStateFlow()
    private var adminAccountsLoadJob: Job? = null

    /** একবার সার্ভার থেকে অ্যাকাউন্ট-তালিকা এনে state বসায়। সফল হলে true। */
    private suspend fun fetchAdminAccountsOnce(): Boolean {
        val res = SupabaseSyncManager.adminAccountsList()
        return if (res.isSuccess) {
            _adminAccounts.value = res.getOrNull().orEmpty().mapNotNull { AdminAccountInfo.fromJson(it) }
            _adminAccountsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADED
            true
        } else {
            Log.e("SomadhanViewModel", "fetchAdminAccountsOnce failed: ${res.exceptionOrNull()?.message}")
            if (_adminAccountsSyncPhase.value != SupabaseRealtimeManager.SyncPhase.LOADED) {
                _adminAccountsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.ERROR
            }
            false
        }
    }

    /**
     * অ্যাকাউন্ট-ট্যাব খোলা/retry/পোলিং-এ কল হয়। সুপার না হলে কিছু করে না (সার্ভারও আটকায়)। ইতিমধ্যে চললে
     * দ্বিতীয়বার শুরু করে না। [silent] = true (পোলিং): ERROR অবস্থায় LOADING-এ ফেরায় না — নইলে প্রতি ২০সেকেন্ডে
     * skeleton ঝলকাতো (একবারও লোড না হওয়া অবস্থায় নেটওয়ার্ক না থাকলে)।
     */
    fun loadAdminAccounts(silent: Boolean = false) {
        if (!isCurrentAdminSuper()) return
        if (adminAccountsLoadJob?.isActive == true) return
        if (!silent && _adminAccountsSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR) {
            _adminAccountsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
        }
        adminAccountsLoadJob = viewModelScope.launch { fetchAdminAccountsOnce() }
    }

    /**
     * অ্যাকাউন্ট-ম্যানেজমেন্টের সব মিউটেটিং অ্যাকশনের অভিন্ন খোলস: সুপার-চেক → অনলাইন-চেক → RPC → টোস্ট → তালিকা
     * (আর রোল-কার্ডের account_count) রিফ্রেশ। [onDone] সফলতা জানায় — ভিউ সফল হলেই ডায়ালগ/ফর্ম বন্ধ করে, ব্যর্থ হলে
     * (কারণ টোস্টে) খোলা রাখে যাতে ইনপুট হারিয়ে না যায়। ব্যর্থতায়ও তালিকা রিফ্রেশ হয় — "অন্য কোথাও বদলে গেছে"
     * (ACCOUNT_NOT_FOUND ইত্যাদি) হলে স্ক্রিন ঠিক হয়ে যায়।
     */
    private fun runAdminAccountAction(
        offlineMessage: String,
        successMessage: String,
        logTag: String,
        onDone: (Boolean) -> Unit,
        call: suspend () -> Result<*>
    ) {
        viewModelScope.launch {
            if (!isCurrentAdminSuper()) {
                showToast(com.example.ui.screens.accountErrorMessage("SUPER_ADMIN_REQUIRED"))
                onDone(false)
                return@launch
            }
            if (!requireOnlineOrWarn(offlineMessage)) {
                onDone(false)
                return@launch
            }
            val res = call()
            if (res.isSuccess) {
                showToast(successMessage)
                fetchAdminAccountsOnce()
                fetchAdminRolesOnce()
                onDone(true)
            } else {
                Log.e("SomadhanViewModel", "$logTag failed: ${res.exceptionOrNull()?.message}")
                showToast(com.example.ui.screens.accountErrorMessage(res.exceptionOrNull()?.message.orEmpty()))
                fetchAdminAccountsOnce()
                onDone(false)
            }
        }
    }

    /** নতুন এডমিন অ্যাকাউন্ট (Supabase Auth user সহ, সার্ভারে অ্যাটমিক)। ইনপুট ক্লায়েন্টেও যাচাই — সার্ভার আবার যাচাই করে। */
    fun adminCreateAccount(
        name: String,
        phone: String,
        password: String,
        roleId: String,
        designation: String,
        email: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanPhone = com.example.ui.screens.normalizeAdminPhoneInput(phone)
        val cleanName = name.trim()
        val cleanEmail = email.trim().ifEmpty { null }
        val earlyError = when {
            cleanName.isEmpty() -> "INVALID_NAME"
            !com.example.ui.screens.isValidAdminPhone(cleanPhone) -> "INVALID_PHONE"
            password.length < 8 -> "INVALID_PASSWORD"
            roleId.isBlank() -> "ROLE_NOT_FOUND"
            else -> null
        }
        if (earlyError != null) {
            showToast(com.example.ui.screens.accountErrorMessage(earlyError))
            onDone(false)
            return
        }
        runAdminAccountAction(
            offlineMessage = "ইন্টারনেট সংযোগ ছাড়া এডমিন অ্যাকাউন্ট বানানো যাবে না",
            successMessage = "নতুন এডমিন অ্যাকাউন্ট তৈরি হয়েছে।",
            logTag = "adminCreateAccount",
            onDone = onDone
        ) {
            SupabaseSyncManager.adminAccountCreate(
                name = cleanName,
                phone = cleanPhone,
                password = password,
                roleId = roleId,
                designation = designation.trim(),
                email = cleanEmail
            )
        }
    }

    /** এডমিনের রোল বদল (সুপার অ্যাকাউন্টে নিষিদ্ধ)। */
    fun adminChangeAccountRole(account: AdminAccountInfo, roleId: String, onDone: (Boolean) -> Unit = {}) {
        if (account.isSuper) {
            showToast(com.example.ui.screens.accountErrorMessage("SUPER_ADMIN_IMMUTABLE"))
            onDone(false)
            return
        }
        runAdminAccountAction(
            offlineMessage = "ইন্টারনেট সংযোগ ছাড়া রোল বদলানো যাবে না",
            successMessage = "রোল বদলানো হয়েছে।",
            logTag = "adminChangeAccountRole",
            onDone = onDone
        ) { SupabaseSyncManager.adminAccountSetRole(account.id, roleId) }
    }

    /** সক্রিয়/নিষ্ক্রিয়। সুপার অ্যাকাউন্ট ও নিজেকে বদলানো যায় না (সার্ভারেও ব্লক)। নিষ্ক্রিয় হলে ওই এডমিন ~৩০সে-এ জোর-লগআউট হয়। */
    fun adminSetAccountActive(account: AdminAccountInfo, active: Boolean, reason: String, onDone: (Boolean) -> Unit = {}) {
        if (account.isSuper) {
            showToast(com.example.ui.screens.accountErrorMessage("SUPER_ADMIN_IMMUTABLE"))
            onDone(false)
            return
        }
        if (account.id == AdminSession.current?.account?.id) {
            showToast(com.example.ui.screens.accountErrorMessage("CANNOT_CHANGE_SELF"))
            onDone(false)
            return
        }
        runAdminAccountAction(
            offlineMessage = "ইন্টারনেট সংযোগ ছাড়া অ্যাকাউন্টের অবস্থা বদলানো যাবে না",
            successMessage = if (active) "অ্যাকাউন্ট সক্রিয় করা হয়েছে।" else "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে।",
            logTag = "adminSetAccountActive",
            onDone = onDone
        ) { SupabaseSyncManager.adminAccountSetActive(account.id, active, reason.trim()) }
    }

    /** ফ্ল্যাগ/আনফ্ল্যাগ। ফ্ল্যাগড এডমিন সব মেনু দেখে কিন্তু কোনো অ্যাকশন পারে না (গেটিং সেশন ৭-এ ওয়্যার হবে; DB-এর `admin_can_act` এখনই জানে)। */
    fun adminSetAccountFlagged(account: AdminAccountInfo, flagged: Boolean, reason: String, onDone: (Boolean) -> Unit = {}) {
        if (account.isSuper) {
            showToast(com.example.ui.screens.accountErrorMessage("SUPER_ADMIN_IMMUTABLE"))
            onDone(false)
            return
        }
        if (account.id == AdminSession.current?.account?.id) {
            showToast(com.example.ui.screens.accountErrorMessage("CANNOT_CHANGE_SELF"))
            onDone(false)
            return
        }
        runAdminAccountAction(
            offlineMessage = "ইন্টারনেট সংযোগ ছাড়া ফ্ল্যাগ বদলানো যাবে না",
            successMessage = if (flagged) "অ্যাকাউন্ট ফ্ল্যাগ করা হয়েছে।" else "ফ্ল্যাগ সরানো হয়েছে।",
            logTag = "adminSetAccountFlagged",
            onDone = onDone
        ) { SupabaseSyncManager.adminAccountSetFlagged(account.id, flagged, reason.trim()) }
    }

    /** অন্য এডমিনের পাসওয়ার্ড রিসেট (সুপার অ্যাকাউন্ট বাদে; নিজের পাসওয়ার্ড সেটিংস থেকে)। */
    fun adminResetAccountPassword(account: AdminAccountInfo, newPassword: String, onDone: (Boolean) -> Unit = {}) {
        if (account.isSuper) {
            showToast(com.example.ui.screens.accountErrorMessage("SUPER_ADMIN_IMMUTABLE"))
            onDone(false)
            return
        }
        if (newPassword.length < 8) {
            showToast(com.example.ui.screens.accountErrorMessage("INVALID_PASSWORD"))
            onDone(false)
            return
        }
        runAdminAccountAction(
            offlineMessage = "ইন্টারনেট সংযোগ ছাড়া পাসওয়ার্ড রিসেট করা যাবে না",
            successMessage = "পাসওয়ার্ড রিসেট হয়েছে। ওই এডমিনকে নতুন পাসওয়ার্ড জানিয়ে দিন।",
            logTag = "adminResetAccountPassword",
            onDone = onDone
        ) { SupabaseSyncManager.adminAccountResetPassword(account.id, newPassword) }
    }

    // ============================================================
    // [ADMIN_ROLE_PROFILE সেশন ৬] এডমিন প্রোফাইল — নিজের (সব এডমিন) / যেকারো (সুপার)
    // ============================================================
    // প্রোফাইলের ডেটা নতুন কোনো state-এ না: নিজেরটা [AdminSession] (heartbeat-এ তাজা), অন্যেরটা [adminAccounts]
    // (সুপার-অনলি তালিকা)। সংরক্ষণের পর নিজের হলে [AdminSession.update], সুপার হলে তালিকা রিফ্রেশ।
    // পাসওয়ার্ড বদল নতুন কিছু না — সেশন ২-এর [adminUpdateCredentials] (বর্তমান পাসওয়ার্ড re-verify, per-account)।

    /**
     * প্রোফাইল সংরক্ষণ। [target] নিজের হলে যেকোনো এডমিন পারে; অন্যের হলে শুধু সুপার (সার্ভারেও আটকায়)।
     * ছবির তিন অবস্থা (অগ্রাধিকার ক্রমে): [removePhoto] → ছবি সরানো; [uploadedPhotoUrl] → আগেই আপলোড হওয়া URL
     * (আগের চেষ্টায় আপলোড সফল কিন্তু RPC ব্যর্থ হলে পুনরায় আপলোড এড়াতে); [newPhotoUri] → এখন আপলোড। আপলোড সফল
     * হলে [onPhotoUploaded] URL জানায় (ভিউ ধরে রাখে)। কোনোটাই না → ছবি অপরিবর্তিত। [onDone] সফল হলে true —
     * ভিউ তখনই ড্রাফট/ছবির অবস্থা রিসেট করে, ব্যর্থ হলে ইনপুট অক্ষত (কারণ টোস্টে)।
     */
    fun adminSaveProfile(
        context: Context,
        target: AdminAccountInfo,
        name: String,
        designation: String,
        email: String,
        bio: String,
        newPhotoUri: Uri?,
        uploadedPhotoUrl: String?,
        removePhoto: Boolean,
        onPhotoUploaded: (String) -> Unit,
        onDone: (Boolean) -> Unit
    ) {
        val me = AdminSession.current?.account
        if (me == null) {
            showToast(com.example.ui.screens.profileErrorMessage("AUTH_REQUIRED"))
            onDone(false)
            return
        }
        val isSelf = target.id == me.id
        if (!isSelf && !me.isSuper) {
            showToast(com.example.ui.screens.profileErrorMessage("SUPER_ADMIN_REQUIRED"))
            onDone(false)
            return
        }
        val cleanName = name.trim()
        val cleanDesignation = designation.trim()
        val cleanEmail = email.trim()
        val cleanBio = bio.trim()
        val validationError = AdminProfileRules.validate(cleanName, cleanDesignation, cleanEmail, cleanBio)
        if (validationError != null) {
            showToast(validationError)
            onDone(false)
            return
        }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া প্রোফাইল সংরক্ষণ করা যাবে না")) {
                onDone(false)
                return@launch
            }
            var photoParam: String? = null
            when {
                removePhoto -> photoParam = ""
                uploadedPhotoUrl != null -> photoParam = uploadedPhotoUrl
                newPhotoUri != null -> {
                    val up = AdminProfilePhotoUploader.upload(context, newPhotoUri)
                    val url = up.getOrNull()
                    if (url == null) {
                        Log.e("SomadhanViewModel", "adminSaveProfile photo upload failed: ${up.exceptionOrNull()?.message}")
                        showToast(com.example.ui.screens.photoUploadErrorMessage(up.exceptionOrNull()?.message.orEmpty()))
                        onDone(false)
                        return@launch
                    }
                    onPhotoUploaded(url)
                    photoParam = url
                }
            }
            val res = SupabaseSyncManager.adminProfileUpdate(
                targetId = if (isSelf) null else target.id,
                name = cleanName,
                designation = cleanDesignation,
                email = cleanEmail,
                bio = cleanBio,
                photoUrl = photoParam
            )
            val updated = res.getOrNull()?.let { AdminAccountInfo.fromJson(it) }
            if (res.isSuccess && updated != null) {
                if (isSelf) {
                    AdminSession.update(updated)
                    // টপ-বার/অন্যান্য জায়গায় দেখানো নাম-ইমেইল (লগইনের সময় বসানো `_currentUser`) সাথে সাথে মিলিয়ে দেওয়া।
                    _currentUser.value = _currentUser.value?.copy(
                        name = updated.name.ifBlank { "Support Manager" },
                        email = updated.email ?: "admin@somadhan.com"
                    )
                }
                if (isCurrentAdminSuper()) fetchAdminAccountsOnce()
                showToast("প্রোফাইল সংরক্ষণ করা হয়েছে।")
                onDone(true)
            } else {
                Log.e("SomadhanViewModel", "adminSaveProfile failed: ${res.exceptionOrNull()?.message}")
                showToast(com.example.ui.screens.profileErrorMessage(res.exceptionOrNull()?.message.orEmpty()))
                if (!isSelf && isCurrentAdminSuper()) fetchAdminAccountsOnce()
                onDone(false)
            }
        }
    }

    /**
     * [adminId]-র সাম্প্রতিক লগইন-সেশন (সর্বোচ্চ [limit], নতুন আগে)। ব্যর্থ হলে null (ভিউ "আনা যায়নি" দেখায়,
     * ফাঁকা তালিকা থেকে আলাদা)। নিজের সেশন সবাই, অন্যেরটা শুধু সুপার দেখে — RLS আসল গেট।
     */
    fun fetchAdminSessions(adminId: String, limit: Int = 5, onResult: (List<AdminSessionRecord>?) -> Unit) {
        viewModelScope.launch {
            val res = SupabaseSyncManager.adminSessionsList(adminId, limit)
            if (res.isFailure) {
                Log.e("SomadhanViewModel", "fetchAdminSessions failed: ${res.exceptionOrNull()?.message}")
            }
            onResult(res.getOrNull())
        }
    }

    // ============================================================
    // [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগ (সুপার-অনলি) — state + অ্যাকশন
    // ============================================================
    // ডেটা ক্লাউড থেকে সরাসরি (`admin_activity_logs_list`), Room-এ না — বিদ্যমান লগ-ট্যাব (১২) শুধু ওই ডিভাইসের
    // নিজের Room-লগ দেখায়, মাল্টি-এডমিনে সুপারের দরকার সবার লগ। রোল/অ্যাকাউন্ট-তালিকার মতোই নিজস্ব sync-phase
    // (LOADING = এখনো একবারও আসেনি, LOADED = অন্তত একবার, ERROR = একবারও আসেনি ও সর্বশেষ চেষ্টা ব্যর্থ)।
    //
    // ফিল্টার = explicit "ফিল্টার করুন" বাটনে প্রয়োগ (লাইভ না)। [_adminActivityLogFilter] শুধু সফল fetch-এর পরেই
    // বদলায় — ব্যর্থ হলে আগের ফিল্টার + আগের তালিকা অক্ষত থাকে (তালিকা আর ফিল্টার কখনো গরমিল হয় না)।
    // [adminActivityLogsGeneration]: ফিল্টার বদলানো/রিফ্রেশে পুরনো in-flight রেসপন্স ফেলে দিতে (দেরিতে আসা পুরনো
    // ফলাফল নতুন তালিকা ওভাররাইট না করে)।
    private val _adminActivityLogs = MutableStateFlow<List<AdminActivityLogEntry>>(emptyList())
    val adminActivityLogs: StateFlow<List<AdminActivityLogEntry>> = _adminActivityLogs.asStateFlow()
    private val _adminActivityLogsHasMore = MutableStateFlow(false)
    val adminActivityLogsHasMore: StateFlow<Boolean> = _adminActivityLogsHasMore.asStateFlow()
    private val _adminActivityLogsSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADING)
    val adminActivityLogsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _adminActivityLogsSyncPhase.asStateFlow()
    private val _adminActivityLogFilter = MutableStateFlow(AdminActivityLogFilter())
    val adminActivityLogFilter: StateFlow<AdminActivityLogFilter> = _adminActivityLogFilter.asStateFlow()
    /** ফিল্টার প্রয়োগ চলছে — UI ছোট প্রগ্রেস দেখায় (পুরো-পেজ skeleton/ঝলক না, Ground Rule ১৮)। */
    private val _adminActivityLogsFiltering = MutableStateFlow(false)
    val adminActivityLogsFiltering: StateFlow<Boolean> = _adminActivityLogsFiltering.asStateFlow()
    private val _adminActivityLogsLoadingMore = MutableStateFlow(false)
    val adminActivityLogsLoadingMore: StateFlow<Boolean> = _adminActivityLogsLoadingMore.asStateFlow()
    private var adminActivityLogsLoadJob: Job? = null
    private var adminActivityLogsGeneration = 0
    private var adminActivityLogFilterRequest = 0

    /**
     * প্রথম পেজ এনে তালিকা *প্রতিস্থাপন* করে। সফল হলে true। যদি এর মধ্যে নতুন fetch শুরু হয়ে যায় (ফিল্টার
     * বদল/রিফ্রেশ) তাহলে এই রেসপন্স নিঃশব্দে বাদ যায় এবং true ফেরত আসে (নতুন fetch-ই সিদ্ধান্ত নেবে; কলার যেন
     * ভুল "ব্যর্থ" টোস্ট না দেখায়)।
     */
    private suspend fun fetchAdminActivityLogsFirstPage(filter: AdminActivityLogFilter): Boolean {
        val gen = ++adminActivityLogsGeneration
        val res = SupabaseSyncManager.adminActivityLogsList(
            adminId = filter.adminId,
            nameQuery = filter.nameQuery.trim(),
            unattributedOnly = filter.unattributedOnly,
            beforeTimestamp = null,
            beforeId = null,
            limit = ADMIN_ACTIVITY_LOG_PAGE_SIZE
        )
        if (gen != adminActivityLogsGeneration) return true
        val page = res.getOrNull()
        return if (page != null) {
            _adminActivityLogs.value = page.rows
            _adminActivityLogsHasMore.value = page.hasMore
            _adminActivityLogFilter.value = filter
            _adminActivityLogsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADED
            true
        } else {
            Log.e("SomadhanViewModel", "fetchAdminActivityLogsFirstPage failed: ${res.exceptionOrNull()?.message}")
            if (_adminActivityLogsSyncPhase.value != SupabaseRealtimeManager.SyncPhase.LOADED) {
                _adminActivityLogsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.ERROR
            }
            false
        }
    }

    /**
     * ট্যাব খোলা/retry-তে কল হয় — বর্তমান (প্রয়োগকৃত) ফিল্টারে প্রথম পেজ আনে। সুপার না হলে কিছু করে না
     * (সার্ভারও আটকায়)। ইতিমধ্যে চললে দ্বিতীয়বার শুরু করে না। ERROR অবস্থায় retry-তে LOADING-এ ফেরায়
     * (skeleton দেখানোর জন্য — একবারও ডেটা না থাকা অবস্থায় এটাই সঠিক)।
     */
    fun loadAdminActivityLogs() {
        if (!isCurrentAdminSuper()) return
        if (adminActivityLogsLoadJob?.isActive == true) return
        if (_adminActivityLogsSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR) {
            _adminActivityLogsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
        }
        adminActivityLogsLoadJob = viewModelScope.launch {
            fetchAdminActivityLogsFirstPage(_adminActivityLogFilter.value)
        }
    }

    /** "ফিল্টার করুন" বাটন। ব্যর্থ হলে টোস্ট, আগের ফিল্টার/তালিকা অক্ষত। [onDone] সফলতা জানায়। */
    fun applyAdminActivityLogFilter(filter: AdminActivityLogFilter, onDone: (Boolean) -> Unit = {}) {
        if (!isCurrentAdminSuper()) {
            showToast(com.example.ui.screens.activityLogErrorMessage("SUPER_ADMIN_REQUIRED"))
            onDone(false)
            return
        }
        if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ফিল্টার করা যাবে না")) {
            onDone(false)
            return
        }
        adminActivityLogsLoadJob?.cancel()
        _adminActivityLogsFiltering.value = true
        // শুধু সর্বশেষ "ফিল্টার করুন" চাপার ফলাফলই প্রগ্রেস বন্ধ/টোস্ট/onDone করবে (পুরনো, বাতিল-হওয়া চাপ নয়)।
        val myRequest = ++adminActivityLogFilterRequest
        adminActivityLogsLoadJob = viewModelScope.launch {
            val ok = fetchAdminActivityLogsFirstPage(filter)
            if (adminActivityLogFilterRequest == myRequest) {
                _adminActivityLogsFiltering.value = false
                if (!ok) showToast(com.example.ui.screens.activityLogErrorMessage(""))
                onDone(ok)
            }
        }
    }

    /** পরের পেজ (আগের পেজের শেষ সারির (timestamp, id) কার্সরে)। ফিল্টার/রিফ্রেশ চলাকালীন কিছু করে না। */
    fun loadMoreAdminActivityLogs() {
        if (!isCurrentAdminSuper()) return
        if (!_adminActivityLogsHasMore.value || _adminActivityLogsLoadingMore.value) return
        if (_adminActivityLogsFiltering.value || adminActivityLogsLoadJob?.isActive == true) return
        val last = _adminActivityLogs.value.lastOrNull() ?: return
        if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া আরও লগ আনা যাবে না")) return
        val filter = _adminActivityLogFilter.value
        val gen = adminActivityLogsGeneration
        _adminActivityLogsLoadingMore.value = true
        viewModelScope.launch {
            val res = SupabaseSyncManager.adminActivityLogsList(
                adminId = filter.adminId,
                nameQuery = filter.nameQuery.trim(),
                unattributedOnly = filter.unattributedOnly,
                beforeTimestamp = last.timestamp,
                beforeId = last.id,
                limit = ADMIN_ACTIVITY_LOG_PAGE_SIZE
            )
            // ইতিমধ্যে ফিল্টার/রিফ্রেশে তালিকা বদলে গেলে এই পেজ আর প্রযোজ্য না
            if (gen == adminActivityLogsGeneration) {
                val page = res.getOrNull()
                if (page != null) {
                    val known = _adminActivityLogs.value.mapTo(HashSet()) { it.id }
                    _adminActivityLogs.value = _adminActivityLogs.value + page.rows.filter { it.id !in known }
                    _adminActivityLogsHasMore.value = page.hasMore
                } else {
                    Log.e("SomadhanViewModel", "loadMoreAdminActivityLogs failed: ${res.exceptionOrNull()?.message}")
                    showToast(com.example.ui.screens.activityLogErrorMessage(res.exceptionOrNull()?.message.orEmpty()))
                }
            }
            _adminActivityLogsLoadingMore.value = false
        }
    }

    // ধাপ ৮ (RPC_SYNC_FIX ট্র্যাক, UI ইন্ডিকেটর) — outbox-এ কতগুলো এন্ট্রি এখনো
    // PENDING/RETRYING আছে তার লাইভ কাউন্ট (repository.observeOutboxPendingCount(), Step 6-এর
    // DAO Flow সরাসরি এক্সপোজ)। বিদ্যমান allCategories/allPlatformSettings-এর মতোই কনভেনশন।
    // শুধু read, outbox-এর ডেটা এখানে কোথাও বদলানো হয় না।
    val outboxPendingCount: StateFlow<Int> = repository.observeOutboxPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ধাপ ৮ — "এখনই আবার চেষ্টা করুন" বাটনের জন্য: outbox worker-কে ম্যানুয়ালি এক্ষুনি ট্রিগার
    // করে (OutboxSyncWorker.triggerImmediate(), Step 6-এ রেডি করা, Step 7 থেকে periodic
    // schedule চালু আছে)। এই ফাংশন নিজে outbox-এর কোনো ডেটা বদলায় না, শুধু WorkManager-কে
    // এক্ষুনি একটা রান তোলার জন্য অনুরোধ করে -- বাকিটা (retry/status update) worker নিজেই করে।
    fun retryOutboxSyncNow() {
        com.example.worker.OutboxSyncWorker.triggerImmediate(getApplication())
    }

    // Categories (Unfiltered for Admin and system lookup)
    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Platform Settings
    val allPlatformSettings: StateFlow<List<PlatformSettingEntity>> = repository.getAllSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Master Switch: Physical categories enabled (default true)
    val isPhysicalWorkEnabled: StateFlow<Boolean> = allPlatformSettings.map { list ->
        list.find { it.key == "physical_categories_enabled" }?.value != "false"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Global Master Switch: Virtual categories enabled (default true)
    val isVirtualWorkEnabled: StateFlow<Boolean> = allPlatformSettings.map { list ->
        list.find { it.key == "virtual_categories_enabled" }?.value != "false"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Visible Categories for regular users/solvers (filtered by isActive and global master switches)
    val visibleCategories: StateFlow<List<CategoryEntity>> = combine(
        allCategories,
        isPhysicalWorkEnabled,
        isVirtualWorkEnabled
    ) { categories, physicalEnabled, virtualEnabled ->
        categories.filter { cat ->
            cat.isActive && (
                (cat.isPhysical && physicalEnabled) ||
                (!cat.isPhysical && virtualEnabled)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Categories (Derived from visibleCategories to ensure all user screens automatically respect master switches)
    val activeCategories: StateFlow<List<CategoryEntity>> = visibleCategories

    // Set of Active Category IDs for fast filtering
    val activeCategoryIds: StateFlow<Set<String>> = activeCategories.map { list ->
        list.map { it.id }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // [Offline Action Gating ধাপ ২] Strict Offline Block টগল -- না থাকলে ডিফল্ট true
    // (ব্যাকওয়ার্ড-কম্প্যাটিবল, বর্তমান strict full-block আচরণ অপরিবর্তিত)। allPlatformSettings
    // Room-এর Flow থেকে আসায় এটা admin-এর নিজের ডিভাইসে লোকাল সেভ, আর
    // syncStrictOfflineBlockSettingFromCloud() (startup/login-এ কল হয়) থেকে cloud-pull -- দুটো
    // ক্ষেত্রেই এমনিতেই react করবে, আলাদা কিছু লাগবে না। MainActivity ধাপ ৩-এ এই ফ্ল্যাগ read
    // করে global blocking overlay branch করবে (এই ধাপে শুধু infra, এখনো ব্যবহার হচ্ছে না)।
    val isStrictOfflineBlockEnabled: StateFlow<Boolean> = allPlatformSettings.map { list ->
        list.find { it.key == "strict_offline_block" }?.value?.toBooleanStrictOrNull() ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Maintenance Mode StateFlow
    val isMaintenanceMode: StateFlow<Boolean> = allPlatformSettings.map { list ->
        list.find { it.key == "maintenance_mode" }?.value == "true"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Maintenance Notice Message StateFlow
    val maintenanceMessage: StateFlow<String> = allPlatformSettings.map { list ->
        list.find { it.key == "maintenance_message" }?.value?.takeIf { it.isNotBlank() }
            ?: "অ্যাপটির রক্ষণাবেক্ষণ ও সার্ভার আপগ্রেডের কাজ চলছে। শীঘ্রই স্বাভাবিক সেবা চালু হবে। আপনার সহযোগিতার জন্য ধন্যবাদ।"
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "অ্যাপটির রক্ষণাবেক্ষণ ও সার্ভার আপগ্রেডের কাজ চলছে। শীঘ্রই স্বাভাবিক সেবা চালু হবে। আপনার সহযোগিতার জন্য ধন্যবাদ।"
    )

    // Physical Category Radius Range in KM (Admin controlled)
    val physicalCategoryRadiusKm: StateFlow<Double> = allPlatformSettings.map { list ->
        list.find { it.key == "physical_category_radius_km" }?.value?.toDoubleOrNull() ?: 10.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10.0)

    // Platform Commission Percent (Admin controlled)
    val platformCommissionPercent: StateFlow<Double> = allPlatformSettings.map { list ->
        list.find { it.key == "commission_percent" }?.value?.toDoubleOrNull() ?: 10.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10.0)

    // Minimum Withdrawal Amount in Taka (Admin controlled)
    val minWithdrawalAmount: StateFlow<Double> = allPlatformSettings.map { list ->
        list.find { it.key == "min_withdrawal" }?.value?.toDoubleOrNull() ?: 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100.0)

    // Platform Urgency Levels (Admin controlled)
    val platformUrgencyLevels: StateFlow<List<String>> = allPlatformSettings.map { list ->
        list.find { it.key == "urgency_levels" }?.value?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf("সাধারণ", "জরুরি", "খুব জরুরি")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("সাধারণ", "জরুরি", "খুব জরুরি"))

    // Instant Job Settings (Admin controlled)
    val isInstantJobFeatureEnabled: StateFlow<Boolean> = allPlatformSettings.map { list ->
        list.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _dismissedDisputeIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedDisputeIds: StateFlow<Set<String>> = _dismissedDisputeIds.asStateFlow()

    val instantJobDefaultRadiusKm: StateFlow<Double> = allPlatformSettings.map { list ->
        list.find { it.key == "instant_job_default_radius_km" }?.value?.toDoubleOrNull() ?: 5.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5.0)

    val instantJobBroadcastTimeoutSeconds: StateFlow<Int> = allPlatformSettings.map { list ->
        list.find { it.key == "instant_job_broadcast_timeout_seconds" }?.value?.toIntOrNull() ?: 120
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    val instantJobMaxActivePerSolver: StateFlow<Int> = allPlatformSettings.map { list ->
        list.find { it.key == "instant_job_max_active_per_solver" }?.value?.toIntOrNull() ?: 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // All Problems (Unfiltered for Admin and owner history)
    val allProblems: StateFlow<List<ProblemEntity>> = repository.getAllProblems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active FAQs for Users/Solvers (isActive = 1, sorted by displayOrder ASC)
    val activeFaqs: StateFlow<List<FaqEntity>> = repository.getActiveFaqs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All FAQs for Admin management
    val allFaqsForAdmin: StateFlow<List<FaqEntity>> = repository.getAllFaqsForAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Open Problems (Filtered by activeCategoryIds and strictly public non-direct contracts for public & solver feeds)
    val openProblems: StateFlow<List<ProblemEntity>> = combine(
        repository.getOpenProblems(),
        activeCategoryIds
    ) { problems, activeIds ->
        problems.filter {
            activeIds.contains(it.categoryId) &&
            !it.isDirectContract &&
            it.isPublic &&
            !it.isUserDeleted &&
            (!it.isInstantJob || it.solverCancelledNotice.isNullOrBlank())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed Problems (Filtered by activeCategoryIds and strictly public non-direct contracts for public & solver feeds)
    val completedProblems: StateFlow<List<ProblemEntity>> = combine(
        repository.getCompletedProblems(),
        activeCategoryIds
    ) { problems, activeIds ->
        problems.filter { activeIds.contains(it.categoryId) && !it.isDirectContract && it.isPublic && !it.isUserDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =========================================================================
    // PAGINATION: PROBLEMS & POSTS (BATCH 1)
    // =========================================================================

    // --- 1. All Open Problems (AllOpenProblemsScreen) ---
    private val openProblemsPageSize = 10
    private var openProblemsOffset = 0
    val openProblemsPaged = mutableStateListOf<ProblemEntity>()
    var openProblemsLoadingMore by mutableStateOf(false)
        private set
    var openProblemsHasMore by mutableStateOf(true)
        private set

    fun loadNextOpenProblemsPage() {
        if (openProblemsLoadingMore || !openProblemsHasMore) return
        viewModelScope.launch {
            openProblemsLoadingMore = true
            try {
                val page = repository.getOpenProblemsPage(openProblemsPageSize, openProblemsOffset)
                openProblemsPaged.addAll(page)
                openProblemsOffset += page.size
                if (page.size < openProblemsPageSize) {
                    openProblemsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading open problems page", e)
            } finally {
                openProblemsLoadingMore = false
            }
        }
    }

    fun resetOpenProblemsPagination() {
        openProblemsPaged.clear()
        openProblemsOffset = 0
        openProblemsHasMore = true
        loadNextOpenProblemsPage()
    }

    // --- 2. Solver All Posts (SolverAllPostsScreen) ---
    private val solverAllPostsPageSize = 10
    private var solverAllPostsOffset = 0
    val solverAllPostsPaged = mutableStateListOf<ProblemEntity>()
    var solverAllPostsLoadingMore by mutableStateOf(false)
        private set
    var solverAllPostsHasMore by mutableStateOf(true)
        private set

    fun loadNextSolverAllPostsPage() {
        if (solverAllPostsLoadingMore || !solverAllPostsHasMore) return
        viewModelScope.launch {
            solverAllPostsLoadingMore = true
            try {
                val page = repository.getOpenProblemsPage(solverAllPostsPageSize, solverAllPostsOffset)
                solverAllPostsPaged.addAll(page)
                solverAllPostsOffset += page.size
                if (page.size < solverAllPostsPageSize) {
                    solverAllPostsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading solver all posts page", e)
            } finally {
                solverAllPostsLoadingMore = false
            }
        }
    }

    fun resetSolverAllPostsPagination() {
        solverAllPostsPaged.clear()
        solverAllPostsOffset = 0
        solverAllPostsHasMore = true
        loadNextSolverAllPostsPage()
    }

    // --- 3. Solver Category Posts (SolverCategoryPostsScreen) ---
    private val solverCategoryPostsPageSize = 10
    private var solverCategoryPostsOffset = 0
    private var currentCategoryPostsCategoryId: String? = null
    val solverCategoryPostsPaged = mutableStateListOf<ProblemEntity>()
    var solverCategoryPostsLoadingMore by mutableStateOf(false)
        private set
    var solverCategoryPostsHasMore by mutableStateOf(true)
        private set

    fun loadNextSolverCategoryPostsPage(
        categoryId: String? = null,
        solverLat: Double = 0.0,
        solverLon: Double = 0.0,
        radiusKm: Double = physicalCategoryRadiusKm.value,
        isSolver: Boolean = true
    ) {
        val targetCatId = categoryId ?: currentCategoryPostsCategoryId
        if (targetCatId.isNullOrBlank() || solverCategoryPostsLoadingMore || !solverCategoryPostsHasMore) return
        viewModelScope.launch {
            solverCategoryPostsLoadingMore = true
            try {
                // Distance filtering (isPhysical jobs) can shrink a raw DB page down
                // to very few or zero visible items, especially for a sparse category.
                // Keep pulling subsequent DB pages until we've gathered a full page of
                // *visible* items (or run out of data), so scrolling doesn't require
                // several empty taps before another visible card shows up.
                var addedCount = 0
                var attempts = 0
                while (addedCount < solverCategoryPostsPageSize && solverCategoryPostsHasMore && attempts < 5) {
                    attempts++
                    val page = repository.getOpenProblemsByCategoryPage(
                        targetCatId,
                        solverCategoryPostsPageSize,
                        solverCategoryPostsOffset
                    )
                    solverCategoryPostsOffset += page.size
                    if (page.size < solverCategoryPostsPageSize) {
                        solverCategoryPostsHasMore = false
                    }
                    val visiblePage = if (isSolver) {
                        page.filter { isProblemVisibleToSolver(it, solverLat, solverLon, radiusKm) }
                    } else {
                        page
                    }
                    solverCategoryPostsPaged.addAll(visiblePage)
                    addedCount += visiblePage.size
                    if (page.isEmpty()) break
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading solver category posts page", e)
            } finally {
                solverCategoryPostsLoadingMore = false
            }
        }
    }

    fun resetSolverCategoryPostsPagination(
        categoryId: String? = null,
        solverLat: Double = 0.0,
        solverLon: Double = 0.0,
        radiusKm: Double = physicalCategoryRadiusKm.value,
        isSolver: Boolean = true
    ) {
        currentCategoryPostsCategoryId = categoryId
        solverCategoryPostsPaged.clear()
        solverCategoryPostsOffset = 0
        solverCategoryPostsHasMore = true
        loadNextSolverCategoryPostsPage(categoryId, solverLat, solverLon, radiusKm, isSolver)
    }

    // --- 4. User Problems (UserProblemsScreen) ---
    private val userProblemsPageSize = 10
    private var userProblemsOffset = 0
    private var currentFilteredUserId: String? = null
    val userProblemsPaged = mutableStateListOf<ProblemEntity>()
    var userProblemsLoadingMore by mutableStateOf(false)
        private set
    var userProblemsHasMore by mutableStateOf(true)
        private set

    fun loadNextUserProblemsPage(userId: String? = null) {
        val targetUserId = userId ?: currentFilteredUserId ?: _currentUser.value?.id ?: return
        currentFilteredUserId = targetUserId
        if (userProblemsLoadingMore || !userProblemsHasMore) return
        viewModelScope.launch {
            userProblemsLoadingMore = true
            try {
                val page = repository.getProblemsByUserIdPage(targetUserId, userProblemsPageSize, userProblemsOffset)
                userProblemsPaged.addAll(page)
                userProblemsOffset += page.size
                if (page.size < userProblemsPageSize) {
                    userProblemsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading user problems page", e)
            } finally {
                userProblemsLoadingMore = false
            }
        }
    }

    fun resetUserProblemsPagination(userId: String? = null) {
        currentFilteredUserId = userId ?: _currentUser.value?.id
        userProblemsPaged.clear()
        userProblemsOffset = 0
        userProblemsHasMore = true
        loadNextUserProblemsPage(currentFilteredUserId)
    }

    // --- 4b. User Completed Problems (UserProblemsScreen) ---
    private val userCompletedProblemsPageSize = 10
    private var userCompletedProblemsOffset = 0
    private var currentCompletedUserId: String? = null
    val userCompletedProblemsPaged = mutableStateListOf<ProblemEntity>()
    var userCompletedProblemsLoadingMore by mutableStateOf(false)
        private set
    var userCompletedProblemsHasMore by mutableStateOf(true)
        private set

    fun loadNextUserCompletedProblemsPage(userId: String? = null) {
        val targetUserId = userId ?: currentCompletedUserId ?: _currentUser.value?.id ?: return
        currentCompletedUserId = targetUserId
        if (userCompletedProblemsLoadingMore || !userCompletedProblemsHasMore) return
        viewModelScope.launch {
            userCompletedProblemsLoadingMore = true
            try {
                val page = repository.getCompletedProblemsByUserIdPage(targetUserId, userCompletedProblemsPageSize, userCompletedProblemsOffset)
                userCompletedProblemsPaged.addAll(page)
                userCompletedProblemsOffset += page.size
                if (page.size < userCompletedProblemsPageSize) {
                    userCompletedProblemsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading user completed problems page", e)
            } finally {
                userCompletedProblemsLoadingMore = false
            }
        }
    }

    fun resetUserCompletedProblemsPagination(userId: String? = null) {
        currentCompletedUserId = userId ?: _currentUser.value?.id
        userCompletedProblemsPaged.clear()
        userCompletedProblemsOffset = 0
        userCompletedProblemsHasMore = true
        loadNextUserCompletedProblemsPage(currentCompletedUserId)
    }

    // --- 5. Solver Problems (SolverProblemsScreen) ---
    private val solverProblemsPageSize = 10
    private var solverProblemsOffset = 0
    private var currentFilteredSolverId: String? = null
    val solverProblemsPaged = mutableStateListOf<ProblemEntity>()
    var solverProblemsLoadingMore by mutableStateOf(false)
        private set
    var solverProblemsHasMore by mutableStateOf(true)
        private set

    fun loadNextSolverProblemsPage(solverId: String? = null) {
        val targetSolverId = solverId ?: currentFilteredSolverId ?: _currentUser.value?.id ?: return
        currentFilteredSolverId = targetSolverId
        if (solverProblemsLoadingMore || !solverProblemsHasMore) return
        viewModelScope.launch {
            solverProblemsLoadingMore = true
            try {
                val page = repository.getProblemsBySolverPage(targetSolverId, solverProblemsPageSize, solverProblemsOffset)
                solverProblemsPaged.addAll(page)
                solverProblemsOffset += page.size
                if (page.size < solverProblemsPageSize) {
                    solverProblemsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading solver problems page", e)
            } finally {
                solverProblemsLoadingMore = false
            }
        }
    }

    fun resetSolverProblemsPagination(solverId: String? = null) {
        currentFilteredSolverId = solverId ?: _currentUser.value?.id
        solverProblemsPaged.clear()
        solverProblemsOffset = 0
        solverProblemsHasMore = true
        loadNextSolverProblemsPage(currentFilteredSolverId)
    }

    // --- 6. Solver Completed Jobs (SolverCompletedJobsScreen) ---
    private val solverCompletedJobsPageSize = 10
    private var solverCompletedJobsOffset = 0
    private var currentCompletedJobsSolverId: String? = null
    val solverCompletedJobsPaged = mutableStateListOf<ProblemEntity>()
    var solverCompletedJobsLoadingMore by mutableStateOf(false)
        private set
    var solverCompletedJobsHasMore by mutableStateOf(true)
        private set

    fun loadNextSolverCompletedJobsPage(solverId: String? = null) {
        val targetSolverId = solverId ?: currentCompletedJobsSolverId ?: _currentUser.value?.id ?: return
        currentCompletedJobsSolverId = targetSolverId
        if (solverCompletedJobsLoadingMore || !solverCompletedJobsHasMore) return
        viewModelScope.launch {
            solverCompletedJobsLoadingMore = true
            try {
                val page = repository.getCompletedProblemsBySolverPage(targetSolverId, solverCompletedJobsPageSize, solverCompletedJobsOffset)
                solverCompletedJobsPaged.addAll(page)
                solverCompletedJobsOffset += page.size
                if (page.size < solverCompletedJobsPageSize) {
                    solverCompletedJobsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading solver completed jobs page", e)
            } finally {
                solverCompletedJobsLoadingMore = false
            }
        }
    }

    fun resetSolverCompletedJobsPagination(solverId: String? = null) {
        currentCompletedJobsSolverId = solverId ?: _currentUser.value?.id
        solverCompletedJobsPaged.clear()
        solverCompletedJobsOffset = 0
        solverCompletedJobsHasMore = true
        loadNextSolverCompletedJobsPage(currentCompletedJobsSolverId)
    }

    // =========================================================================
    // PAGINATION: BIDS, TRANSACTIONS, WITHDRAWALS, REVIEWS, NOTIFICATIONS,
    //             FAVORITE SOLVERS, ADMIN SOLVER QUOTA (BATCH 2)
    // =========================================================================

    // --- 1. Problem Bids (BidManagementScreen) ---
    private val problemBidsPageSize = 10
    private var problemBidsOffset = 0
    private var currentProblemBidsProblemId: String? = null
    val problemBidsPaged = mutableStateListOf<BidEntity>()
    var problemBidsLoadingMore by mutableStateOf(false)
        private set
    var problemBidsHasMore by mutableStateOf(true)
        private set

    fun loadNextProblemBidsPage(problemId: String? = null) {
        val targetProblemId = problemId ?: currentProblemBidsProblemId ?: return
        currentProblemBidsProblemId = targetProblemId
        if (problemBidsLoadingMore || !problemBidsHasMore) return
        viewModelScope.launch {
            problemBidsLoadingMore = true
            try {
                val page = repository.getBidsForProblemPage(targetProblemId, problemBidsPageSize, problemBidsOffset)
                problemBidsPaged.addAll(page)
                problemBidsOffset += page.size
                if (page.size < problemBidsPageSize) {
                    problemBidsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading problem bids page", e)
            } finally {
                problemBidsLoadingMore = false
            }
        }
    }

    fun resetProblemBidsPagination(problemId: String? = null) {
        currentProblemBidsProblemId = problemId
        problemBidsPaged.clear()
        problemBidsOffset = 0
        problemBidsHasMore = true
        if (problemId != null) {
            loadNextProblemBidsPage(problemId)
        }
    }

    // --- 2. Solver My Bids (SolverMyBidsScreen) ---
    private val solverMyBidsPageSize = 10
    private var solverMyBidsOffset = 0
    private var currentSolverBidsSolverId: String? = null
    val solverMyBidsPaged = mutableStateListOf<BidEntity>()
    var solverMyBidsLoadingMore by mutableStateOf(false)
        private set
    var solverMyBidsHasMore by mutableStateOf(true)
        private set

    fun loadNextSolverMyBidsPage(solverId: String? = null) {
        val targetSolverId = solverId ?: currentSolverBidsSolverId ?: _currentUser.value?.id ?: return
        currentSolverBidsSolverId = targetSolverId
        if (solverMyBidsLoadingMore || !solverMyBidsHasMore) return
        viewModelScope.launch {
            solverMyBidsLoadingMore = true
            try {
                val page = repository.getBidsBySolverPage(targetSolverId, solverMyBidsPageSize, solverMyBidsOffset)
                solverMyBidsPaged.addAll(page)
                solverMyBidsOffset += page.size
                if (page.size < solverMyBidsPageSize) {
                    solverMyBidsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading solver my bids page", e)
            } finally {
                solverMyBidsLoadingMore = false
            }
        }
    }

    fun resetSolverMyBidsPagination(solverId: String? = null) {
        currentSolverBidsSolverId = solverId ?: _currentUser.value?.id
        solverMyBidsPaged.clear()
        solverMyBidsOffset = 0
        solverMyBidsHasMore = true
        loadNextSolverMyBidsPage(currentSolverBidsSolverId)
    }

    // --- 3. Transactions (TransactionHistoryScreen) ---
    private val transactionsPageSize = 10
    private var transactionsOffset = 0
    private var currentTransactionsUserId: String? = null
    val transactionsPaged = mutableStateListOf<TransactionEntity>()
    var transactionsLoadingMore by mutableStateOf(false)
        private set
    var transactionsHasMore by mutableStateOf(true)
        private set

    fun loadNextTransactionsPage(userId: String? = null) {
        val user = _currentUser.value
        val isAdmin = user?.role == "ADMIN"
        val targetUserId = userId ?: currentTransactionsUserId ?: user?.id
        currentTransactionsUserId = targetUserId
        if (transactionsLoadingMore || !transactionsHasMore) return
        viewModelScope.launch {
            transactionsLoadingMore = true
            try {
                val page = if (targetUserId == null || (isAdmin && userId == null)) {
                    repository.getAllTransactionsPage(transactionsPageSize, transactionsOffset)
                } else {
                    repository.getTransactionsForUserPage(targetUserId, transactionsPageSize, transactionsOffset)
                }
                transactionsPaged.addAll(page)
                transactionsOffset += page.size
                if (page.size < transactionsPageSize) {
                    transactionsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading transactions page", e)
            } finally {
                transactionsLoadingMore = false
            }
        }
    }

    fun resetTransactionsPagination(userId: String? = null) {
        currentTransactionsUserId = userId ?: _currentUser.value?.id
        transactionsPaged.clear()
        transactionsOffset = 0
        transactionsHasMore = true
        loadNextTransactionsPage(currentTransactionsUserId)
    }

    // --- 4. Withdrawals (WithdrawalHistoryScreen) ---
    private val withdrawalsPageSize = 10
    private var withdrawalsOffset = 0
    private var currentWithdrawalsSolverId: String? = null
    val withdrawalsPaged = mutableStateListOf<WithdrawalEntity>()
    var withdrawalsLoadingMore by mutableStateOf(false)
        private set
    var withdrawalsHasMore by mutableStateOf(true)
        private set

    fun loadNextWithdrawalsPage(solverId: String? = null) {
        val user = _currentUser.value
        val isAdmin = user?.role == "ADMIN"
        val targetSolverId = solverId ?: currentWithdrawalsSolverId ?: user?.id
        currentWithdrawalsSolverId = targetSolverId
        if (withdrawalsLoadingMore || !withdrawalsHasMore) return
        viewModelScope.launch {
            withdrawalsLoadingMore = true
            try {
                val page = if (targetSolverId == null || (isAdmin && solverId == null)) {
                    repository.getAllWithdrawalsPage(withdrawalsPageSize, withdrawalsOffset)
                } else {
                    repository.getWithdrawalsForSolverPage(targetSolverId, withdrawalsPageSize, withdrawalsOffset)
                }
                withdrawalsPaged.addAll(page)
                withdrawalsOffset += page.size
                if (page.size < withdrawalsPageSize) {
                    withdrawalsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading withdrawals page", e)
            } finally {
                withdrawalsLoadingMore = false
            }
        }
    }

    fun resetWithdrawalsPagination(solverId: String? = null) {
        currentWithdrawalsSolverId = solverId ?: _currentUser.value?.id
        withdrawalsPaged.clear()
        withdrawalsOffset = 0
        withdrawalsHasMore = true
        loadNextWithdrawalsPage(currentWithdrawalsSolverId)
    }

    // --- 5. Reviews / Ratings (UserReviewsScreen / SolverReviewsScreen / PublicProfileReviewsScreen) ---
    // 5a. Solver Reviews Received
    private val solverReviewsPageSize = 10
    private var solverReviewsOffset = 0
    private var currentSolverReviewsSolverId: String? = null
    val solverReviewsPaged = mutableStateListOf<RatingEntity>()
    var solverReviewsLoadingMore by mutableStateOf(false)
        private set
    var solverReviewsHasMore by mutableStateOf(true)
        private set

    fun loadNextSolverReviewsPage(solverId: String? = null) {
        val targetSolverId = solverId ?: currentSolverReviewsSolverId ?: _currentUser.value?.id ?: return
        currentSolverReviewsSolverId = targetSolverId
        if (solverReviewsLoadingMore || !solverReviewsHasMore) return
        viewModelScope.launch {
            solverReviewsLoadingMore = true
            try {
                val page = repository.getRatingsReceivedBySolverPage(targetSolverId, solverReviewsPageSize, solverReviewsOffset)
                solverReviewsPaged.addAll(page)
                solverReviewsOffset += page.size
                if (page.size < solverReviewsPageSize) {
                    solverReviewsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading solver reviews page", e)
            } finally {
                solverReviewsLoadingMore = false
            }
        }
    }

    fun resetSolverReviewsPagination(solverId: String? = null) {
        currentSolverReviewsSolverId = solverId ?: _currentUser.value?.id
        solverReviewsPaged.clear()
        solverReviewsOffset = 0
        solverReviewsHasMore = true
        loadNextSolverReviewsPage(currentSolverReviewsSolverId)
    }

    // 5b. User Reviews Received
    private val userReviewsPageSize = 10
    private var userReviewsOffset = 0
    private var currentUserReviewsUserId: String? = null
    val userReviewsPaged = mutableStateListOf<RatingEntity>()
    var userReviewsLoadingMore by mutableStateOf(false)
        private set
    var userReviewsHasMore by mutableStateOf(true)
        private set

    fun loadNextUserReviewsPage(userId: String? = null) {
        val targetUserId = userId ?: currentUserReviewsUserId ?: _currentUser.value?.id ?: return
        currentUserReviewsUserId = targetUserId
        if (userReviewsLoadingMore || !userReviewsHasMore) return
        viewModelScope.launch {
            userReviewsLoadingMore = true
            try {
                val page = repository.getRatingsReceivedByUserPage(targetUserId, userReviewsPageSize, userReviewsOffset)
                userReviewsPaged.addAll(page)
                userReviewsOffset += page.size
                if (page.size < userReviewsPageSize) {
                    userReviewsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading user reviews page", e)
            } finally {
                userReviewsLoadingMore = false
            }
        }
    }

    fun resetUserReviewsPagination(userId: String? = null) {
        currentUserReviewsUserId = userId ?: _currentUser.value?.id
        userReviewsPaged.clear()
        userReviewsOffset = 0
        userReviewsHasMore = true
        loadNextUserReviewsPage(currentUserReviewsUserId)
    }

    // 5c. Public Profile Reviews (all reviews received by person as user or solver)
    private val publicProfileReviewsPageSize = 10
    private var publicProfileReviewsOffset = 0
    private var currentPublicProfilePersonId: String? = null
    val publicProfileReviewsPaged = mutableStateListOf<RatingEntity>()
    var publicProfileReviewsLoadingMore by mutableStateOf(false)
        private set
    var publicProfileReviewsHasMore by mutableStateOf(true)
        private set

    fun loadNextPublicProfileReviewsPage(personId: String? = null) {
        val targetPersonId = personId ?: currentPublicProfilePersonId ?: _currentUser.value?.id ?: return
        currentPublicProfilePersonId = targetPersonId
        if (publicProfileReviewsLoadingMore || !publicProfileReviewsHasMore) return
        viewModelScope.launch {
            publicProfileReviewsLoadingMore = true
            try {
                val page = repository.getAllRatingsReceivedByPersonPage(targetPersonId, publicProfileReviewsPageSize, publicProfileReviewsOffset)
                publicProfileReviewsPaged.addAll(page)
                publicProfileReviewsOffset += page.size
                if (page.size < publicProfileReviewsPageSize) {
                    publicProfileReviewsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading public profile reviews page", e)
            } finally {
                publicProfileReviewsLoadingMore = false
            }
        }
    }

    fun resetPublicProfileReviewsPagination(personId: String? = null) {
        currentPublicProfilePersonId = personId ?: _currentUser.value?.id
        publicProfileReviewsPaged.clear()
        publicProfileReviewsOffset = 0
        publicProfileReviewsHasMore = true
        loadNextPublicProfileReviewsPage(currentPublicProfilePersonId)
    }

    // --- 6. Favorite Solvers (FavoriteSolversScreen) ---
    private val favoriteSolversPageSize = 10
    private var favoriteSolversOffset = 0
    val favoriteSolversPaged = mutableStateListOf<UserEntity>()
    var favoriteSolversLoadingMore by mutableStateOf(false)
        private set
    var favoriteSolversHasMore by mutableStateOf(true)
        private set

    fun loadNextFavoriteSolversPage() {
        val favIdList = _currentUser.value?.favoriteSolverIds
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() } ?: emptyList()

        if (favoriteSolversLoadingMore || !favoriteSolversHasMore) return
        if (favoriteSolversOffset >= favIdList.size) {
            favoriteSolversHasMore = false
            return
        }

        viewModelScope.launch {
            favoriteSolversLoadingMore = true
            try {
                val subList = favIdList.drop(favoriteSolversOffset).take(favoriteSolversPageSize)
                val loadedUsers = subList.mapNotNull { solverId ->
                    repository.getUserById(solverId)
                }
                favoriteSolversPaged.addAll(loadedUsers)
                favoriteSolversOffset += subList.size
                if (favoriteSolversOffset >= favIdList.size || subList.size < favoriteSolversPageSize) {
                    favoriteSolversHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading favorite solvers page", e)
            } finally {
                favoriteSolversLoadingMore = false
            }
        }
    }

    fun resetFavoriteSolversPagination() {
        favoriteSolversPaged.clear()
        favoriteSolversOffset = 0
        favoriteSolversHasMore = true
        loadNextFavoriteSolversPage()
    }

    // --- 6b. Admin: All Users (AdminUsersView) ---
    // Backs the existing next/prev page UI in AdminUsersView with real DB paging instead of a
    // fully-preloaded in-memory list. Only used for the unfiltered/no-search baseline view --
    // when the admin types a search query or picks a role filter, the screen falls back to the
    // already-loaded full `users` list (same trade-off as openProblemsPaged/solverAllPostsPaged
    // in ENGINEERING_NOTES.md §8: DB pagination can't search text it hasn't loaded yet).
    private val adminUsersPageSize = 10
    private var adminUsersOffset = 0
    private var currentAdminUsersRoleFilter: String? = null
    val adminUsersPaged = mutableStateListOf<UserEntity>()
    var adminUsersLoadingMore by mutableStateOf(false)
        private set
    var adminUsersHasMore by mutableStateOf(true)
        private set

    fun loadNextAdminUsersPage() {
        if (adminUsersLoadingMore || !adminUsersHasMore) return
        viewModelScope.launch {
            adminUsersLoadingMore = true
            try {
                val roleFilter = currentAdminUsersRoleFilter
                val page = if (roleFilter.isNullOrBlank() || roleFilter == "ALL") {
                    repository.getAllUsersPage(adminUsersPageSize, adminUsersOffset)
                } else {
                    repository.getUsersByRolePage(roleFilter, adminUsersPageSize, adminUsersOffset)
                }
                adminUsersPaged.addAll(page)
                adminUsersOffset += page.size
                if (page.size < adminUsersPageSize) {
                    adminUsersHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading admin users page", e)
            } finally {
                adminUsersLoadingMore = false
            }
        }
    }

    fun resetAdminUsersPagination(roleFilter: String? = null) {
        currentAdminUsersRoleFilter = roleFilter
        adminUsersPaged.clear()
        adminUsersOffset = 0
        adminUsersHasMore = true
        loadNextAdminUsersPage()
    }

    // --- 6c. Admin: All Withdrawals (AdminWithdrawalsView) ---
    // A dedicated admin function rather than reusing loadNextWithdrawalsPage/
    // resetWithdrawalsPagination (which are for WithdrawalHistoryScreen, a specific solver's own
    // withdrawals). Those default `solverId ?: currentUser.id` when called with null, which for
    // an admin resolves to the ADMIN'S OWN id -- silently showing only the admin's personal
    // withdrawals instead of everyone's. Kept fully separate to avoid touching that
    // solver-facing behaviour.
    private val adminWithdrawalsPageSize = 10
    private var adminWithdrawalsOffset = 0
    val adminWithdrawalsPaged = mutableStateListOf<WithdrawalEntity>()
    var adminWithdrawalsLoadingMore by mutableStateOf(false)
        private set
    var adminWithdrawalsHasMore by mutableStateOf(true)
        private set

    fun loadNextAdminWithdrawalsPage() {
        if (adminWithdrawalsLoadingMore || !adminWithdrawalsHasMore) return
        viewModelScope.launch {
            adminWithdrawalsLoadingMore = true
            try {
                val page = repository.getAllWithdrawalsPage(adminWithdrawalsPageSize, adminWithdrawalsOffset)
                adminWithdrawalsPaged.addAll(page)
                adminWithdrawalsOffset += page.size
                if (page.size < adminWithdrawalsPageSize) {
                    adminWithdrawalsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading admin withdrawals page", e)
            } finally {
                adminWithdrawalsLoadingMore = false
            }
        }
    }

    fun resetAdminWithdrawalsPagination() {
        adminWithdrawalsPaged.clear()
        adminWithdrawalsOffset = 0
        adminWithdrawalsHasMore = true
        loadNextAdminWithdrawalsPage()
    }

    // --- 7. Notifications (HomeScreen, DashboardScreen, Notifications) ---
    private val notificationsPageSize = 10
    private var notificationsOffset = 0
    private var currentNotificationsUserId: String? = null
    val notificationsPaged = mutableStateListOf<NotificationEntity>()
    var notificationsLoadingMore by mutableStateOf(false)
        private set
    var notificationsHasMore by mutableStateOf(true)
        private set

    fun loadNextNotificationsPage(userId: String? = null) {
        val targetUserId = userId ?: currentNotificationsUserId ?: _currentUser.value?.id ?: return
        currentNotificationsUserId = targetUserId
        if (notificationsLoadingMore || !notificationsHasMore) return
        viewModelScope.launch {
            notificationsLoadingMore = true
            try {
                // [ROLE_SEPARATION ধাপ ৬] targetUserId সবসময় বর্তমান লগ-ইনকৃত ইউজার না-ও হতে
                // পারে (userId প্যারামিটার ঐচ্ছিক) -- তাই _currentUser.value.id মিললে তবেই
                // তার active role পাস করা হচ্ছে, নাহলে role-neutral ("") রেখে পুরনো (সব দেখানো)
                // আচরণ বজায় থাকছে।
                val pageRole = if (targetUserId == _currentUser.value?.id) _currentUser.value?.role ?: "" else ""
                val page = repository.getNotificationsForUserPage(targetUserId, notificationsPageSize, notificationsOffset, pageRole)
                notificationsPaged.addAll(page)
                notificationsOffset += page.size
                if (page.size < notificationsPageSize) {
                    notificationsHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading notifications page", e)
            } finally {
                notificationsLoadingMore = false
            }
        }
    }

    fun resetNotificationsPagination(userId: String? = null) {
        currentNotificationsUserId = userId ?: _currentUser.value?.id
        notificationsPaged.clear()
        notificationsOffset = 0
        notificationsHasMore = true
        loadNextNotificationsPage(currentNotificationsUserId)
    }

    // --- 8. Admin Solver Quota (AdminSolverQuotaView) ---
    private val adminSolverQuotaPageSize = 10
    private var adminSolverQuotaOffset = 0
    val adminSolverQuotaPaged = mutableStateListOf<UserEntity>()
    var adminSolverQuotaLoadingMore by mutableStateOf(false)
        private set
    var adminSolverQuotaHasMore by mutableStateOf(true)
        private set

    fun loadNextAdminSolverQuotaPage() {
        if (adminSolverQuotaLoadingMore || !adminSolverQuotaHasMore) return
        viewModelScope.launch {
            adminSolverQuotaLoadingMore = true
            try {
                val page = repository.getUsersByRolePage("SOLVER", adminSolverQuotaPageSize, adminSolverQuotaOffset)
                adminSolverQuotaPaged.addAll(page)
                adminSolverQuotaOffset += page.size
                if (page.size < adminSolverQuotaPageSize) {
                    adminSolverQuotaHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading admin solver quota page", e)
            } finally {
                adminSolverQuotaLoadingMore = false
            }
        }
    }

    fun resetAdminSolverQuotaPagination() {
        adminSolverQuotaPaged.clear()
        adminSolverQuotaOffset = 0
        adminSolverQuotaHasMore = true
        loadNextAdminSolverQuotaPage()
    }

    // =========================================================================
    // PAGINATION: CHAT / REVERSE MESSAGES (BATCH 3 / STEP 5)
    // =========================================================================
    // chatMessagesPaged stores messages in newest-to-oldest order (index 0 is newest)
    // or loaded for reverseLayout = true LazyColumn.
    private val chatMessagesPageSize = 20
    private var chatMessagesOffset = 0
    private var currentChatProblemId: String? = null
    val chatMessagesPaged = mutableStateListOf<MessageEntity>()
    var chatMessagesLoadingMore by mutableStateOf(false)
        private set
    var chatMessagesHasMore by mutableStateOf(true)
        private set

    fun resetChatMessagesPagination(problemId: String) {
        currentChatProblemId = problemId
        chatMessagesPaged.clear()
        chatMessagesOffset = 0
        chatMessagesHasMore = true
        loadOlderMessages(problemId)
    }

    fun loadOlderMessages(problemId: String? = null) {
        val targetProblemId = problemId ?: currentChatProblemId ?: return
        if (chatMessagesLoadingMore || !chatMessagesHasMore) return
        viewModelScope.launch {
            chatMessagesLoadingMore = true
            try {
                // getMessagesForProblemPage returns messages ORDER BY timestamp DESC
                val olderPage = repository.getMessagesForProblemPage(targetProblemId, chatMessagesPageSize, chatMessagesOffset)
                if (olderPage.isNotEmpty()) {
                    // Filter out any messages already present by id to prevent duplicates
                    val existingIds = chatMessagesPaged.map { it.id }.toSet()
                    val distinctOlder = olderPage.filter { it.id !in existingIds }
                    chatMessagesPaged.addAll(distinctOlder)
                    chatMessagesOffset += olderPage.size
                }
                if (olderPage.size < chatMessagesPageSize) {
                    chatMessagesHasMore = false
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error loading older chat messages", e)
            } finally {
                chatMessagesLoadingMore = false
            }
        }
    }

    // Solver Active Winning Posts (Accepted solver = Current User, Active status, with unseen activity flag)
    val solverActiveWinningPosts: StateFlow<List<ActivePostWithActivity>> = combine(
        allProblems,
        _currentUser
    ) { problems, user ->
        if (user == null) return@combine emptyList()
        val uid = user.id
        problems
            .filter { it.acceptedSolverId == uid && it.status != "COMPLETED" && it.status != "CANCELLED" && !it.isUserDeleted }
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
            .map { problem ->
                val hasUnseen = problem.lastActivityAt != null && (problem.solverLastSeenAt == null || problem.lastActivityAt > problem.solverLastSeenAt)
                ActivePostWithActivity(problem = problem, hasUnseenActivity = hasUnseen)
            }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Solver Active Jobs indicators
    val solverHasActiveJobs: StateFlow<Boolean> = solverActiveWinningPosts.map { list ->
        list.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val solverHasAnyUnseenActivity: StateFlow<Boolean> = solverActiveWinningPosts.map { list ->
        list.any { it.hasUnseenActivity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // User Active Accepted Posts (Owner = Current User, Has accepted solver, Active status, with unseen activity flag)
    val userActiveAcceptedPosts: StateFlow<List<ActivePostWithActivity>> = combine(
        allProblems,
        _currentUser
    ) { problems, user ->
        if (user == null) return@combine emptyList()
        val uid = user.id
        problems
            .filter { it.userId == uid && !it.acceptedSolverId.isNullOrBlank() && it.status != "COMPLETED" && it.status != "CANCELLED" && !it.isUserDeleted }
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
            .map { problem ->
                val hasUnseen = problem.lastActivityAt != null && (problem.userLastSeenAt == null || problem.lastActivityAt > problem.userLastSeenAt)
                ActivePostWithActivity(problem = problem, hasUnseenActivity = hasUnseen)
            }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Active Jobs indicators
    val userHasActiveJobs: StateFlow<Boolean> = userActiveAcceptedPosts.map { list ->
        list.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userHasAnyUnseenActivity: StateFlow<Boolean> = userActiveAcceptedPosts.map { list ->
        list.any { it.hasUnseenActivity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // User Home Category Filter (Single-select category filter for User Home)
    private val _userFilterCategory = MutableStateFlow<String?>(null)
    val userFilterCategory: StateFlow<String?> = _userFilterCategory.asStateFlow()

    // Solver Search Query
    private val _solverSearchQuery = MutableStateFlow("")
    val solverSearchQuery: StateFlow<String> = _solverSearchQuery.asStateFlow()

    // Solver All-Posts Feed Filter Category
    private val _solverFeedFilterCategory = MutableStateFlow<String?>(null)
    val solverFeedFilterCategory: StateFlow<String?> = _solverFeedFilterCategory.asStateFlow()

    // Solver Matched Categories set
    val solverSelectedCategoryIds: StateFlow<List<String>> = _currentUser.combine(allCategories) { user, _ ->
        user?.solverCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Solver Active Instant Job (if any job is accepted and in progress)
    val activeInstantJobForSolver: StateFlow<ProblemEntity?> = combine(
        allProblems,
        _currentUser
    ) { problems, user ->
        if (user == null || user.role != "SOLVER") return@combine null
        val uid = user.id
        problems.firstOrNull { prob ->
            prob.isInstantJob &&
            prob.jobStatus != null &&
            prob.jobStatus != "BROADCASTING" &&
            prob.jobStatus != "JOB_COMPLETED" &&
            prob.jobStatus != "CANCELLED" &&
            prob.status != "COMPLETED" &&
            prob.status != "CANCELLED" &&
            prob.acceptedSolverId == uid
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User Active Instant Job (if user has an instant job in progress or broadcasting)
    val activeInstantJobForUser: StateFlow<ProblemEntity?> = combine(
        allProblems,
        _currentUser
    ) { problems, user ->
        if (user == null) return@combine null
        val uid = user.id
        problems.firstOrNull { prob ->
            prob.isInstantJob &&
            prob.userId == uid &&
            prob.jobStatus != null &&
            prob.jobStatus != "JOB_COMPLETED" &&
            prob.jobStatus != "COMPLETED" &&
            prob.jobStatus != "CANCELLED" &&
            prob.status != "COMPLETED" &&
            prob.status != "CANCELLED"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Broadcasting Instant Jobs matched for Solver
    val broadcastingInstantJobsForSolver: StateFlow<List<NearbyInstantJobItem>> = combine(
        allProblems,
        _currentUser,
        _liveLocation,
        activeCategoryIds,
        repository.getAllBids()
    ) { problems, user, location, activeCatIds, bids ->
        if (user == null || user.role != "SOLVER") return@combine emptyList()
        // If toggle is disabled, return empty list
        if (!user.instantJobNotificationsEnabled) return@combine emptyList()

        val solverCategorySet = (user.solverCategories ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val solverLat = location.latitude
        val solverLon = location.longitude

        val broadcastingJobs = problems.filter { prob ->
            prob.isInstantJob &&
            prob.jobStatus == "BROADCASTING" &&
            prob.status == "OPEN" &&
            prob.solverCancelledNotice.isNullOrBlank() &&
            activeCatIds.contains(prob.categoryId) &&
            (solverCategorySet.isEmpty() || solverCategorySet.contains(prob.categoryId)) &&
            !bids.any { b -> b.problemId == prob.id && b.solverId == user.id && b.status == "CANCELLED" }
        }

        broadcastingJobs.mapNotNull { prob ->
            val dist = DistanceUtil.calculateDistanceKm(
                solverLat,
                solverLon,
                prob.latitude,
                prob.longitude
            )
            val maxRadius = prob.broadcastRadiusKm ?: 5.0
            if (dist <= maxRadius) {
                NearbyInstantJobItem(
                    problem = prob,
                    distanceKm = dist,
                    formattedDistance = DistanceUtil.formatDistance(dist)
                )
            } else {
                null
            }
        }.sortedBy { it.distanceKm }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Shared helper function to verify if an active problem is visible to a solver.
     * Criteria:
     * - Status must be OPEN or IN_PROGRESS (eligible active work)
     * - If physical: distance from solver's location <= radiusKm (Admin dynamic setting)
     * - If virtual: no distance restriction (visible nationwide across Bangladesh)
     */
    fun isProblemVisibleToSolver(
        problem: ProblemEntity,
        solverLat: Double,
        solverLon: Double,
        radiusKm: Double = physicalCategoryRadiusKm.value
    ): Boolean {
        if (problem.isDirectContract || !problem.isPublic || problem.isUserDeleted) return false
        if (problem.status != "OPEN" && problem.status != "IN_PROGRESS") return false
        if (problem.isInstantJob && !problem.solverCancelledNotice.isNullOrBlank()) return false
        if (!problem.isPhysical) return true
        if (solverLat == 0.0 && solverLon == 0.0) return false
        val distance = DistanceUtil.calculateDistanceKm(
            lat1 = solverLat,
            lon1 = solverLon,
            lat2 = problem.latitude,
            lon2 = problem.longitude
        )
        return distance <= radiusKm
    }

    // Real-time Active Post Counts per Category (Admin radius physical / Nationwide virtual for Solvers, Nationwide for regular Users, active categories only)
    val categoryActivePostCounts: StateFlow<Map<String, Int>> = combine(
        _currentUser,
        _liveLocation,
        openProblems,
        physicalCategoryRadiusKm
    ) { user, liveLoc, problems, radiusKm ->
        val isSolver = user?.role == "SOLVER"
        val solverLat = if (liveLoc.latitude != 0.0) liveLoc.latitude else (user?.latitude ?: 0.0)
        val solverLon = if (liveLoc.longitude != 0.0) liveLoc.longitude else (user?.longitude ?: 0.0)

        problems
            .filter { problem ->
                if (isSolver) {
                    isProblemVisibleToSolver(problem, solverLat, solverLon, radiusKm)
                } else {
                    // Regular users see all active problems nationwide without distance restriction
                    true
                }
            }
            .groupingBy { it.categoryId }
            .eachCount()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Solver Matched Active Problems (Admin dynamic radius real-time distance matching for physical jobs)
    val solverMatchedProblems: StateFlow<List<ProblemEntity>> = combine(
        combine(_currentUser, _liveLocation, physicalCategoryRadiusKm) { user, liveLoc, radiusKm ->
            Triple(user, liveLoc, radiusKm)
        },
        openProblems,
        _solverSearchQuery,
        allCategories,
        repository.getAllBids()
    ) { (user, liveLoc, radiusKm), problems, searchQuery, categories, bids ->
        if (user == null || user.role != "SOLVER") return@combine emptyList()

        val selectedCatIds = user.solverCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (selectedCatIds.isEmpty()) return@combine emptyList()

        val categoryMatched = problems.filter { prob ->
            selectedCatIds.contains(prob.categoryId) &&
            !bids.any { b -> b.problemId == prob.id && b.solverId == user.id && (b.status == "CANCELLED" || b.status == "REJECTED") }
        }

        // Use latest real-time GPS coordinates; fallback to user's saved DB coordinates
        val solverLat = if (liveLoc.latitude != 0.0) liveLoc.latitude else user.latitude
        val solverLon = if (liveLoc.longitude != 0.0) liveLoc.longitude else user.longitude

        val locationFiltered = categoryMatched.filter { post ->
            if (post.isPhysical) {
                val distance = DistanceUtil.calculateDistanceKm(
                    lat1 = solverLat,
                    lon1 = solverLon,
                    lat2 = post.latitude,
                    lon2 = post.longitude
                )
                distance <= radiusKm
            } else {
                true
            }
        }

        if (searchQuery.isNotBlank()) {
            AiMatcherUtil.searchSimilarProblems(searchQuery, locationFiltered, categories)
        } else {
            locationFiltered
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Alias for solver matched problems feed
    val solverFilteredProblems: StateFlow<List<ProblemEntity>> = solverMatchedProblems

    // Solver Matched Completed Problems (Rule 6.1 & 6.2)
    val solverMatchedCompletedProblems: StateFlow<List<ProblemEntity>> = combine(
        _currentUser,
        completedProblems
    ) { user, compList ->
        if (user == null || user.role != "SOLVER") return@combine emptyList()
        val selectedCatIds = user.solverCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        compList.filter { selectedCatIds.contains(it.categoryId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Solver All Posts Feed: Admin dynamic KM logic for physical jobs, nationwide for virtual, ALL categories, sorted by recent first
    val solverAllPostsFeed: StateFlow<List<ProblemEntity>> = combine(
        combine(openProblems, _solverFeedFilterCategory, _solverSearchQuery) { probs, filterCat, searchQuery ->
            Triple(probs, filterCat, searchQuery)
        },
        combine(allCategories, _currentUser, _liveLocation, physicalCategoryRadiusKm) { cats, user, liveLoc, radiusKm ->
            object {
                val categories = cats
                val user = user
                val liveLoc = liveLoc
                val radiusKm = radiusKm
            }
        }
    ) { feedQuery, feedEnv ->
        val problems = feedQuery.first
        val filterCat = feedQuery.second
        val searchQuery = feedQuery.third
        val categories = feedEnv.categories
        val user = feedEnv.user
        val liveLoc = feedEnv.liveLoc
        val radiusKm = feedEnv.radiusKm

        val solverLat = if (liveLoc.latitude != 0.0) liveLoc.latitude else (user?.latitude ?: 0.0)
        val solverLon = if (liveLoc.longitude != 0.0) liveLoc.longitude else (user?.longitude ?: 0.0)

        // Dynamic KM for physical jobs within radius, nationwide for virtual jobs
        var list = problems.filter { problem ->
            if (!problem.isPhysical) {
                true
            } else if (solverLat != 0.0 && solverLon != 0.0 && problem.latitude != 0.0 && problem.longitude != 0.0) {
                DistanceUtil.calculateDistanceKm(solverLat, solverLon, problem.latitude, problem.longitude) <= radiusKm
            } else {
                true
            }
        }

        // Category filter (if solver selects a specific category from dropdown/filter)
        if (!filterCat.isNullOrBlank()) {
            list = list.filter { it.categoryId == filterCat }
        }

        // Search query filter (AI-based semantic & text matching)
        if (searchQuery.isNotBlank()) {
            AiMatcherUtil.searchSimilarProblems(searchQuery, list, categories)
        } else {
            list.sortedByDescending { it.createdAt }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Filtered Open Problems
    val userFilteredProblems: StateFlow<List<ProblemEntity>> = combine(
        openProblems,
        _userFilterCategory
    ) { problems, filterCat ->
        if (filterCat.isNullOrBlank()) {
            problems
        } else {
            problems.filter { it.categoryId == filterCat }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    val allAdminNotifications: StateFlow<List<NotificationEntity>> = repository.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private val _unreadProblemNotificationCount = MutableStateFlow(0)
    val unreadProblemNotificationCount: StateFlow<Int> = _unreadProblemNotificationCount.asStateFlow()

    // Active problem & detail
    private val _selectedProblem = MutableStateFlow<ProblemEntity?>(null)
    val selectedProblem: StateFlow<ProblemEntity?> = _selectedProblem.asStateFlow()

    private val _problemBids = MutableStateFlow<List<BidEntity>>(emptyList())
    val problemBids: StateFlow<List<BidEntity>> = _problemBids.asStateFlow()

    private val _problemMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val problemMessages: StateFlow<List<MessageEntity>> = _problemMessages.asStateFlow()

    // All messages for conversation list
    private val _userConversations = MutableStateFlow<List<MessageEntity>>(emptyList())
    val userConversations: StateFlow<List<MessageEntity>> = _userConversations.asStateFlow()

    // Reviews given by User
    private val _userGivenReviews = MutableStateFlow<List<RatingEntity>>(emptyList())
    val userGivenReviews: StateFlow<List<RatingEntity>> = _userGivenReviews.asStateFlow()

    private val _userReceivedReviews = MutableStateFlow<List<RatingEntity>>(emptyList())
    val userReceivedReviews: StateFlow<List<RatingEntity>> = _userReceivedReviews.asStateFlow()

    private val _dismissedReminderIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedReminderIds: StateFlow<Set<String>> = _dismissedReminderIds.asStateFlow()

    // Reviews received by Solver
    private val _solverReceivedReviews = MutableStateFlow<List<RatingEntity>>(emptyList())
    val solverReceivedReviews: StateFlow<List<RatingEntity>> = _solverReceivedReviews.asStateFlow()

    // Reviews given by Solver
    private val _solverGivenReviews = MutableStateFlow<List<RatingEntity>>(emptyList())
    val solverGivenReviews: StateFlow<List<RatingEntity>> = _solverGivenReviews.asStateFlow()

    // Completed jobs by Solver
    private val _solverCompletedJobs = MutableStateFlow<List<ProblemEntity>>(emptyList())
    val solverCompletedJobs: StateFlow<List<ProblemEntity>> = _solverCompletedJobs.asStateFlow()

    // Platform Recent Completed Jobs (matching solver skills/categories, top 3)
    val platformRecentCompletedJobs: StateFlow<List<ProblemEntity>> = combine(
        completedProblems,
        _currentUser
    ) { problems: List<ProblemEntity>, user: UserEntity? ->
        val myCats = user?.solverCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        problems.filter { problem ->
            problem.status == "COMPLETED" && (myCats.isEmpty() || problem.categoryId in myCats)
        }
            .sortedByDescending { it.completedAt ?: it.createdAt }
            .take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Platform Matched Completed Jobs (all completed jobs in solver's categories)
    val platformMatchedCompletedJobs: StateFlow<List<ProblemEntity>> = combine(
        completedProblems,
        _currentUser
    ) { problems: List<ProblemEntity>, user: UserEntity? ->
        val myCats = user?.solverCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        problems.filter { problem ->
            problem.status == "COMPLETED" && (myCats.isEmpty() || problem.categoryId in myCats)
        }
            .sortedByDescending { it.completedAt ?: it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Solver Bids
    private val _solverBids = MutableStateFlow<List<BidEntity>>(emptyList())
    val solverBids: StateFlow<List<BidEntity>> = _solverBids.asStateFlow()

    fun getBidsForSolver(solverId: String): Flow<List<BidEntity>> = repository.getBidsForSolver(solverId)

    // Solver Withdrawals
    private val _solverWithdrawals = MutableStateFlow<List<WithdrawalEntity>>(emptyList())
    val solverWithdrawals: StateFlow<List<WithdrawalEntity>> = _solverWithdrawals.asStateFlow()

    // User Escrows
    private val _userEscrows = MutableStateFlow<List<EscrowEntity>>(emptyList())
    val userEscrows: StateFlow<List<EscrowEntity>> = _userEscrows.asStateFlow()

    // User Transactions
    private val _userTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val userTransactions: StateFlow<List<TransactionEntity>> = _userTransactions.asStateFlow()

    // User Gateway Payments (bKash/Nagad/Rocket/Bank Deposits & Direct Gateway Escrow payments)
    private val _userGatewayPayments = MutableStateFlow<List<GatewayPaymentEntity>>(emptyList())
    val userGatewayPayments: StateFlow<List<GatewayPaymentEntity>> = _userGatewayPayments.asStateFlow()

    // Admin All Gateway Payments
    val allGatewayPayments: StateFlow<List<GatewayPaymentEntity>> = repository.getAllGatewayPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Withdrawals
    val allWithdrawals: StateFlow<List<WithdrawalEntity>> = repository.getAllWithdrawals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Users List
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ground Rule ২১ (ADMIN_PANEL_LOADING_MASTER_PROMPT.md), সেশন ২.১৯.৩ — realtime-এ সম্পূর্ণ
    // নতুন insert হওয়া user id-গুলোর সংক্ষিপ্ত-সময়ের সেট, AdminUsersView-এর per-item pulse-এ
    // ব্যবহারের জন্য (দেখো SupabaseRealtimeManager.recentlyInsertedUserIds-এর কমেন্ট)। object
    // singleton-এর StateFlow সরাসরি এক্সপোজ (initialSyncPhase-এর প্যাটার্নের মতোই), আলাদা কোনো
    // ট্রান্সফর্মেশন লাগেনি।
    val recentlyInsertedUserIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyInsertedUserIds

    // Ground Rule ২১, সেশন ২.১৯.৪ — KYC ট্যাব। যে user-দের kyc_status সবেমাত্র pending/verified/
    // rejected-এ ট্রানজিশন করেছে তাদের id-র সংক্ষিপ্ত-সময়ের সেট, AdminKycView-এর per-item pulse-এ
    // ব্যবহারের জন্য (দেখো SupabaseRealtimeManager.recentlyKycChangedUserIds-এর কমেন্ট)।
    val recentlyKycChangedUserIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyKycChangedUserIds

    // Ground Rule ২১, সেশন ২.১৯.৫ — Withdrawal ট্যাব। নতুন PENDING রিকোয়েস্ট (Insert) অথবা
    // COMPLETED/REJECTED-এ status transition (Update) — দুটোরই id এখানে সংক্ষিপ্ত সময়ের জন্য
    // থাকে, AdminWithdrawalsView-এর per-item pulse-এ ব্যবহারের জন্য (দেখো
    // SupabaseRealtimeManager.recentlyChangedWithdrawalIds-এর কমেন্ট)।
    val recentlyChangedWithdrawalIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyChangedWithdrawalIds

    // Ground Rule ২১, সেশন ২.১৯.৬ — Problems ট্যাব। নতুন problem post (Insert) অথবা status
    // transition (Update) — দুটোরই id এখানে সংক্ষিপ্ত সময়ের জন্য থাকে, AdminProblemsView-এর
    // per-item pulse-এ ব্যবহারের জন্য (দেখো SupabaseRealtimeManager.recentlyChangedProblemIds-এর
    // কমেন্ট)।
    val recentlyChangedProblemIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyChangedProblemIds

    // Ground Rule ২১, সেশন ২.১৯.৬ — Escrow ট্যাব। নতুন escrow তৈরি (Insert) অথবা HELD থেকে
    // RELEASED/REFUNDED-এ status transition (Update) — দুটোরই id এখানে সংক্ষিপ্ত সময়ের জন্য থাকে,
    // AdminEscrowView-এর তিনটা তালিকাতেই (Held/Released/Refunded) per-item pulse-এ ব্যবহারের জন্য
    // (দেখো SupabaseRealtimeManager.recentlyChangedEscrowIds-এর কমেন্ট)।
    val recentlyChangedEscrowIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyChangedEscrowIds

    // Ground Rule ২১, সেশন ২.২০ — Transactions ট্যাব। নতুন WorkTransaction (Insert-only) আর নতুন
    // WalletRecharge/gateway payment (Insert-only) — দুটো আলাদা টেবিল বলে দুটো আলাদা id-সেট,
    // AdminTransactionsView-এর per-item pulse-এ ব্যবহারের জন্য (দেখো
    // SupabaseRealtimeManager.recentlyChangedTransactionIds/recentlyChangedGatewayPaymentIds-এর
    // কমেন্ট)।
    val recentlyChangedTransactionIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyChangedTransactionIds
    val recentlyChangedGatewayPaymentIds: StateFlow<Set<String>> =
        SupabaseRealtimeManager.recentlyChangedGatewayPaymentIds

    // All Messages Flow for Admin Chat Monitoring
    val allAdminMessages: StateFlow<List<MessageEntity>> = repository.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unread messages count for current user
    private val _unreadMessagesCount = MutableStateFlow(0)
    val unreadMessagesCount: StateFlow<Int> = _unreadMessagesCount.asStateFlow()

    // Real-time typing status map
    // [SUPABASE-MIGRATED - ধাপ ৩২.৯ক] repository.typingStatusMap এখন Firebase+Supabase combine()
    // করা একটা cold Flow (আগে সরাসরি Firebase-এর StateFlow ছিল) -- তাই এখানে stateIn() দিয়ে
    // StateFlow বানানো হচ্ছে, বাকি সব repository-flow-এর প্রতিষ্ঠিত প্যাটার্নেই (যেমন
    // allTransactions, নিচে)।
    val typingStatusMap: StateFlow<Map<String, Long>> = repository.typingStatusMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Loading/Sync Fix Roadmap v2, ধাপ ১ — SupabaseRealtimeManager.attachDatabase()-এর মূল
    // bulk-pull (users/problems/bids/messages/transactions/escrows/gateway_payments/
    // additional_charges/notifications/withdrawals) কোন অবস্থায় আছে (LOADING/LOADED/ERROR) তার
    // সিগন্যাল। ইতিমধ্যেই একটা হট StateFlow, তাই সরাসরি forward করা হলো (typingStatusMap-এর মতো
    // stateIn() লাগবে না, কারণ ওটা repository-তে cold combine() হয়ে গিয়েছিল)। ধাপ ৩/৪-এ UI-তে
    // ব্যবহার হবে — এই ধাপে শুধু expose করা হচ্ছে।
    val initialSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> =
        SupabaseRealtimeManager.initialSyncPhase

    // Loading/Sync Fix Roadmap v2, ধাপ ২ — bulk-pull-এর বাইরের ডাটার SyncPhase (দেখুন
    // SomadhanRepository-তে categoriesSyncPhase-এর উপরের কমেন্ট বিস্তারিত ব্যাখ্যার জন্য)।
    // categoriesSyncPhase/faqsSyncPhase repository-তে হট StateFlow, সরাসরি forward করা হলো।
    // cancelled_bids-এর জন্য আলাদা কোনো phase নেই — `bids` টেবিল ইতিমধ্যেই initialSyncPhase-এর
    // (উপরে) আওতাভুক্ত bulk-pull-এর অংশ, তাই AllOpenProblemsScreen-জাতীয় কোনো cancelled-bids UI
    // ধাপ ৩/৪-এ initialSyncPhase-ই ব্যবহার করবে।
    val categoriesSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = repository.categoriesSyncPhase
    val faqsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = repository.faqsSyncPhase
    val platformSettingsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = repository.platformSettingsSyncPhase
    val auditLogsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = repository.auditLogsSyncPhase
    val ratingsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = repository.ratingsSyncPhase

    // Loading/Sync Fix Roadmap v2, ধাপ ৩ — SyncAwareContent-এর `onRetry` কলব্যাকের জন্য একটা
    // plain `() -> Unit` entry point (repository.ensureFaqsSeeded() suspend, তাই UI থেকে সরাসরি
    // কল করা যায় না)। FaqScreen-এ পরীক্ষামূলক ওয়্যারিং এটাই ব্যবহার করে। ধাপ ৪-এ বাকি ৩২টা
    // স্ক্রিনের জন্যও একই প্যাটার্নে (attachDatabase()/pullBulkDataFromSupabase() বা অন্যান্য
    // per-domain ফাংশনের জন্য) আলাদা ছোট retry-ফাংশন যোগ হবে।
    fun retryFaqsSync() {
        viewModelScope.launch {
            runCatching { repository.ensureFaqsSeeded() }
                .onFailure { Log.e("SomadhanViewModel", "retryFaqsSync failed", it) }
        }
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ৪ — initialSyncPhase-নির্ভর স্ক্রিনগুলোর (users/problems/
    // bids/messages/transactions/escrows/gateway_payments/additional_charges/notifications/
    // withdrawals -- সব কটাই bulk-pull-এর অংশ) `SyncAwareContent`-এর `onRetry`-এর জন্য entry
    // point। SupabaseRealtimeManager নিজে একটা plain object (ViewModel না), তাই suspend
    // repository-কল লাগে না -- সরাসরি ফরওয়ার্ড করাই যথেষ্ট (retryFaqsSync-এর মতো
    // viewModelScope.launch লাগে না, কারণ retryInitialSync() নিজেই managerScope-এ launch করে)।
    fun retryInitialSync() {
        SupabaseRealtimeManager.retryInitialSync()
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B সম্প্রসারণ) — categoriesSyncPhase-নির্ভর
    // স্ক্রিনগুলোর (PostProblemScreen/SolverCategoryPostsScreen/SolverSkillsScreen) জন্য
    // `SyncAwareContent`/`SyncBlockedRetryState`-এর `onRetry`-এর entry point। retryFaqsSync()-এর
    // মতোই প্যাটার্ন — repository.ensureCategoriesSeeded() suspend, তাই viewModelScope.launch
    // লাগে।
    fun retryCategoriesSync() {
        viewModelScope.launch {
            runCatching { repository.ensureCategoriesSeeded() }
                .onFailure { Log.e("SomadhanViewModel", "retryCategoriesSync failed", it) }
        }
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ৭ (ঐচ্ছিক/বোনাস) — নেট সংযোগ ফিরে এলে, ইউজার pull বা পেজ
    // পরিবর্তন না করেও, ERROR-এ আটকে থাকা SyncPhase-গুলো নিজে থেকেই retry হয়। শুধু তিনটা
    // SyncPhase-ই কখনো ERROR-এ যায় ([initialSyncPhase]/[categoriesSyncPhase]/[faqsSyncPhase]) —
    // [platformSettingsSyncPhase]/[auditLogsSyncPhase]/[ratingsSyncPhase] ডিজাইন অনুযায়ীই সবসময়
    // LOADED থাকে (কোনো async অপারেশন নেই, ধাপ ২-এর নোট দেখুন), তাদেরও কোনো retry-ফাংশন নেই —
    // তাই এখানে শুধু এই তিনটাই observe করা হচ্ছে।
    //
    // `ConnectivityManager` সরাসরি এখানে (ViewModel-এ, আলাদা helper ক্লাসে না) ব্যবহার করা হয়েছে,
    // কারণ `AndroidViewModel`-এর `getApplication()` থেকেই Context পাওয়া যায় (উপরের
    // `LocationHelper`/`ImageStorageUtil` কলগুলোর মতোই প্যাটার্ন), আর এই একটাই ব্যবহার-স্থান বলে
    // আলাদা ক্লাস বানানো অপ্রয়োজনীয় জটিলতা।
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // ধাপ ৫-এর SyncResumeRetryCooldown (MotionToolkit.kt)-এর মতোই একই ধরনের ৫-সেকেন্ড cooldown,
    // কিন্তু সেটা UI-স্তরে per-sessionKey (প্রতিটা স্ক্রিনের জন্য আলাদা) — এটা এখানে
    // per-sync-domain (initial/categories/faqs, স্ক্রিন-নির্বিশেষে একবারই), কারণ connectivity
    // ফিরে আসা একটা app-wide ইভেন্ট, কোনো নির্দিষ্ট স্ক্রিনের সাথে বাঁধা না। এটা শুধু flapping
    // network (বারবার দ্রুত অন/অফ হওয়া) থেকে একই sync ফাংশন পরপর কয়েকবার ট্রিগার হওয়া ঠেকায় —
    // আসল duplicate/racing-fetch protection ইতিমধ্যেই ধাপ ৫-এর in-flight guard-গুলো
    // ([SupabaseRealtimeManager.initialSyncInFlight]/`categoriesSyncInFlight`/`faqsSyncInFlight`)
    // দিয়ে হয়ে যায়, এই cooldown সেটার প্রতিস্থাপন না, বাড়তি একটা নিরাপত্তা-স্তর মাত্র।
    private val connectivityRetryLastAttemptMs = mutableMapOf<String, Long>()
    private val CONNECTIVITY_RETRY_COOLDOWN_MS = 5_000L

    private fun tryConsumeConnectivityRetryCooldown(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = connectivityRetryLastAttemptMs[key]
        if (last != null && now - last < CONNECTIVITY_RETRY_COOLDOWN_MS) return false
        connectivityRetryLastAttemptMs[key] = now
        return true
    }

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * অফলাইন→অনলাইন transition-এ, বর্তমানে `ERROR`-এ থাকা প্রতিটা sync phase-এর জন্য সংশ্লিষ্ট
     * retry ফাংশন কল করে। নীরবে ব্যাকগ্রাউন্ডে হয় (কোনো toast/snackbar না) — SyncPhase আর Room
     * দুটোই reactive বলে ডাটা এলে UI নিজে থেকেই আপডেট হয়ে যাবে।
     */
    // [Offline Action Gating ধাপ ৯] অফলাইনে পাঠানো ব্যর্থ (sendStatus="FAILED") মেসেজগুলো
    // reconnect হওয়ার সাথে সাথেই নিজে থেকে আবার পাঠানোর চেষ্টা -- retryAllErroredSyncPhasesOnReconnect()-এর
    // ঠিক পাশেই, একই false->true transition-এ কল হবে। নীরবে ব্যাকগ্রাউন্ডে হয় (কোনো toast না,
    // ChatScreen নিজে থেকেই Room Flow দিয়ে sendStatus আপডেট দেখতে পাবে)। একই connectivity-retry
    // cooldown ম্যাপ পুনরায়-ব্যবহার করা হলো (key="messages"), flapping network-এ বারবার ট্রিগার
    // ঠেকাতে।
    private fun retryAllFailedMessagesOnReconnect() {
        val userId = _currentUser.value?.id ?: return
        if (!tryConsumeConnectivityRetryCooldown("messages")) return
        viewModelScope.launch {
            runCatching { repository.getFailedMessagesForSender(userId) }
                .onSuccess { failed ->
                    failed.forEach { msg -> repository.retrySendMessage(msg.id) }
                }
                .onFailure { e ->
                    Log.w("SomadhanViewModel", "retryAllFailedMessagesOnReconnect failed: ${e.message}")
                }
        }
    }

    private fun retryAllErroredSyncPhasesOnReconnect() {
        // [Offline Action Gating ধাপ ১৩] অফলাইনে app খুললে bulk-pull-এর টেবিলগুলো নীরবে ব্যর্থ হয়ে
        // phase তবু LOADED থাকে (নিচের ERROR-চেক তাই চলে না)। সেই ক্ষেত্রে reconnect-এ নীরব catch-up --
        // phase ছোঁয়া হয় না, তাই কোনো skeleton ঝলক নেই। আলাদা cooldown key ("initial_catchup")।
        if (initialSyncPhase.value != SupabaseRealtimeManager.SyncPhase.ERROR &&
            SupabaseRealtimeManager.hasIncompleteInitialPull() &&
            tryConsumeConnectivityRetryCooldown("initial_catchup")
        ) {
            viewModelScope.launch {
                runCatching { SupabaseRealtimeManager.catchUpIncompleteInitialPull() }
                    .onFailure { Log.w("SomadhanViewModel", "initial catch-up on reconnect failed: ${it.message}") }
            }
        }
        if (initialSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR &&
            tryConsumeConnectivityRetryCooldown("initial")
        ) {
            retryInitialSync()
        }
        if (categoriesSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR &&
            tryConsumeConnectivityRetryCooldown("categories")
        ) {
            retryCategoriesSync()
        }
        if (faqsSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR &&
            tryConsumeConnectivityRetryCooldown("faqs")
        ) {
            retryFaqsSync()
        }
    }

    private fun startConnectivityObserver() {
        // [Somadhan Bug-Fix Step 6, ৩.১b] `NET_CAPABILITY_INTERNET` মানে নেটওয়ার্ক শুধু *দাবি করে*
        // তার ইন্টারনেট আছে (declared) -- captive-portal নেটওয়ার্কেও (wifi লগইন পেজ, কোনো real
        // internet না) এটা `true` থাকে। `NET_CAPABILITY_VALIDATED` মানে Android *সত্যিই যাচাই
        // করেছে* (actual reachability/captive-portal-detection পাস)। নিচের `isReallyOnline()`
        // দুটোই লাগে (`&&`, `||` না) -- নাহলে captive-portal নেটওয়ার্কে `_isOnline=true`
        // false-positive থেকে যাবে, ঠিক যে বাগটা এখানে ফিক্স করা হচ্ছে।
        // (`NetworkConnectivityObserver.kt::isCurrentlyConnected()`-এর OR-প্যাটার্ন ইচ্ছাকৃতভাবে
        // অনুসরণ করা হয়নি -- ওটা ভিন্ন বাগ (false-negative/hardcoded-true fallback) ঠিক করে,
        // এখানকার বাগ false-positive, উল্টো দিকের সমস্যা -- তাই উল্টো (AND) সমাধান লাগে।)
        fun isReallyOnline(caps: NetworkCapabilities?): Boolean =
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        runCatching {
            val app = getApplication<Application>()
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm == null) {
                Log.e("SomadhanViewModel", "startConnectivityObserver: ConnectivityManager unavailable")
                return
            }
            connectivityManager = cm
            // শুরুতে বর্তমান অবস্থা দিয়েই _isOnline সেট করা হচ্ছে (network callback শুধু
            // *পরিবর্তনের* সময় ফায়ার করে, শুরুতে না)।
            val activeCaps = cm.getNetworkCapabilities(cm.activeNetwork)
            _isOnline.value = isReallyOnline(activeCaps)

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // [Somadhan Bug-Fix Step 6, ৩.১b] আগে এখানে unconditionally `true` সেট হতো,
                    // network-এর capability আসলে validated কিনা না দেখেই -- captive-portal
                    // নেটওয়ার্কেও onAvailable() ফায়ার করে (validation তখনো শেষ হয়নি)।
                    val wasOnline = _isOnline.value
                    val nowOnline = isReallyOnline(cm.getNetworkCapabilities(network))
                    _isOnline.value = nowOnline
                    // false -> true transition-এই শুধু retry ট্রিগার হবে, বারবার onAvailable
                    // (একাধিক network interface) থেকে না।
                    if (!wasOnline && nowOnline) {
                        retryAllErroredSyncPhasesOnReconnect()
                        retryAllFailedMessagesOnReconnect()
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    // [Somadhan Bug-Fix Step 6, ৩.১b] captive-portal নেটওয়ার্ক সাধারণত আগে শুধু
                    // NET_CAPABILITY_INTERNET নিয়ে কানেক্ট হয় (onAvailable ফায়ার করে), তারপর
                    // Android-এর ভ্যালিডেশন শেষ হলে/ব্যর্থ হলে এই callback-এ capability বদলায় --
                    // এখানে re-check না করলে প্রাথমিক অবস্থাই (validated না হলেও) স্থায়ী থেকে যেত,
                    // অথবা পরবর্তীতে validation হারালেও (portal আবার আটকে দিলে) `_isOnline` জানতে
                    // পারত না।
                    val wasOnline = _isOnline.value
                    val nowOnline = isReallyOnline(networkCapabilities)
                    _isOnline.value = nowOnline
                    if (!wasOnline && nowOnline) {
                        retryAllErroredSyncPhasesOnReconnect()
                        retryAllFailedMessagesOnReconnect()
                    }
                }

                override fun onLost(network: Network) {
                    val stillHasOther = isReallyOnline(cm.getNetworkCapabilities(cm.activeNetwork))
                    if (!stillHasOther) {
                        _isOnline.value = false
                    }
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
        }.onFailure { Log.e("SomadhanViewModel", "startConnectivityObserver failed", it) }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        }.onFailure { Log.e("SomadhanViewModel", "onCleared: unregisterNetworkCallback failed", it) }
    }

    // Admin Transactions List
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin All Bids
    val allBids: StateFlow<List<BidEntity>> = repository.getAllBids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Cancelled Bids
    val allCancelledBids: StateFlow<List<BidEntity>> = repository.getAllCancelledBids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHeldEscrows: StateFlow<List<EscrowEntity>> = repository.getAllHeldEscrows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReleasedEscrows: StateFlow<List<EscrowEntity>> = repository.getAllReleasedEscrows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRefundedEscrows: StateFlow<List<EscrowEntity>> = repository.getAllRefundedEscrows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAdditionalCharges: StateFlow<List<AdditionalChargeEntity>> = repository.getAllAdditionalCharges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin All Ratings
    val allRatings: StateFlow<List<RatingEntity>> = repository.getAllRatings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Audit Logs
    val recentAuditLogs: StateFlow<List<AdminAuditLogEntity>> = repository.getRecentAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ধাপ ৩২.৫: `admin_get_dashboard_metrics` RPC থেকে আসা সংখ্যা এখানে রাখা হয় (Firebase-এর
    // `liveMetrics`-এর সমান্তরাল, একই `AdminDashboardMetrics` শেপ পুনর্ব্যবহার করে)। এটা push-ভিত্তিক
    // realtime না — `refreshAdminMetrics()`/`triggerCloudSync()` কল হলেই আপডেট হয়।
    private val _supabaseAdminMetrics = MutableStateFlow(AdminDashboardMetrics(isConnected = false))

    // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে Firebase-এর FirebaseSyncManager.liveMetrics-ও
    // combine হতো (৩-তালিকার priority chain: supa > live(Firestore) > local(Room))। এখন Firebase
    // সরিয়ে ফেলায় মাঝের "live" স্তরটা বাদ, ঠিক AdminCredentials.kt-তে করা তিন-স্তর থেকে দুই-স্তরে
    // নামানোর মতোই প্যাটার্নে: এখন শুধু supa (ধাপ ৩২.৫-এর admin_get_dashboard_metrics RPC) >
    // local Room fallback — এই দুই-স্তরের চেইন।
    val adminDashboardMetrics: StateFlow<AdminDashboardMetrics> = combine(
        _supabaseAdminMetrics,
        combine(allUsers, allProblems, allBids) { users, probs, bids ->
            Triple(users, probs, bids)
        },
        allWithdrawals,
        allTransactions,
        platformCommissionPercent
    ) { supa, localData, localWithdrawals, localTransactions, commissionPct ->
        val localUsers = localData.first
        val localProblems = localData.second
        val localBids = localData.third

        // Somadhan Bug-Fix Step 2 (BUG_INVENTORY.md গ্রুপ ২.১, CI_TEST_SUITE Step 19.6 Option খ):
        // আগে এখানে supa (Supabase RPC snapshot) > local Room priority chain ছিল --
        // supa.totalUsers > 0 একবার true হলে চিরকাল সেই stale RPC snapshot আটকে থাকতো, local
        // Room-এর পরবর্তী কোনো পরিবর্তন (নতুন user/withdrawal/problem ইত্যাদি) আর প্রতিফলিত হতো
        // না যতক্ষণ না admin আবার manually refreshAdminMetrics()/triggerCloudSync() চালাতেন।
        // এখন headline metric-এ সবসময় local reactive aggregate ব্যবহার করা হয় -- Room-এর যেকোনো
        // পরিবর্তনে এই combine() নিজে থেকেই re-emit করবে, কোনো manual trigger ছাড়াই। supa এখনো
        // fetch হয় (isConnected/lastSyncTimestamp/categoryProblemCounts/categoryBidCounts-এর
        // মতো মেটাডেটার জন্য, নিচে অপরিবর্তিত), শুধু headline metric-value হিসেবে আর override করে না।
        val totalUsers = localUsers.size
        val totalSolvers = localUsers.count { it.role == "SOLVER" }
        val totalProblems = localProblems.size
        val openProblems = localProblems.count { it.status == "OPEN" }
        val completedProblems = localProblems.count { it.status == "COMPLETED" }
        val inProgressProblems = localProblems.count { it.status == "IN_PROGRESS" }
        val totalBids = localBids.size
        val pendingBids = localBids.count { it.status == "PENDING" }
        val acceptedBids = localBids.count { it.status == "ACCEPTED" }
        val totalVolume = if (localTransactions.isNotEmpty()) {
            localTransactions.sumOf { it.grossAmount }
        } else {
            localProblems.filter { it.status == "COMPLETED" }.sumOf { it.acceptedAmount ?: 0.0 }
        }
        val platformRev = if (localTransactions.isNotEmpty()) {
            localTransactions.sumOf { it.commissionAmount }
        } else {
            localProblems.filter { it.status == "COMPLETED" }.sumOf { p ->
                val amt = p.acceptedAmount ?: 0.0
                val rate = p.appliedCommissionRate ?: commissionPct
                amt * (rate / 100.0)
            }
        }
        val pendingWith = localWithdrawals.count { it.status == "PENDING" }
        val compWith = localWithdrawals.filter { it.status == "COMPLETED" }.sumOf { it.amount }

        val catProbCounts = if (supa.categoryProblemCounts.isNotEmpty()) supa.categoryProblemCounts
        else {
            val map = mutableMapOf<String, Int>()
            localProblems.forEach { p ->
                val name = p.categoryName.ifBlank { "অন্যান্য" }
                map[name] = (map[name] ?: 0) + 1
            }
            map
        }

        val catBidCounts = if (supa.categoryBidCounts.isNotEmpty()) supa.categoryBidCounts
        else {
            val map = mutableMapOf<String, Int>()
            localBids.forEach { b ->
                val prob = localProblems.find { it.id == b.problemId }
                val name = prob?.categoryName?.ifBlank { "সাধারণ" } ?: "সাধারণ"
                map[name] = (map[name] ?: 0) + 1
            }
            map
        }

        AdminDashboardMetrics(
            totalUsers = totalUsers,
            totalSolvers = totalSolvers,
            totalClients = (totalUsers - totalSolvers).coerceAtLeast(0),
            totalProblems = totalProblems,
            openProblems = openProblems,
            completedProblems = completedProblems,
            inProgressProblems = inProgressProblems,
            totalBids = totalBids,
            pendingBids = pendingBids,
            acceptedBids = acceptedBids,
            totalTransactionVolume = totalVolume,
            platformRevenue = platformRev,
            pendingWithdrawals = pendingWith,
            completedWithdrawals = compWith,
            isConnected = true,
            lastSyncTimestamp = System.currentTimeMillis(),
            syncStatusMessage = "Supabase রিয়েল-টাইম লাইভ",
            categoryProblemCounts = catProbCounts,
            categoryBidCounts = catBidCounts
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardMetrics(isConnected = true, syncStatusMessage = "Supabase রিয়েল-টাইম লাইভ"))

    // UI Message / Toast state
    private val _uiToast = MutableStateFlow<String?>(null)
    val uiToast: StateFlow<String?> = _uiToast.asStateFlow()

    private var locationTrackingJob: Job? = null

    init {
        // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.attachDatabase(...) কলও
        // ছিল (Firebase real-time auto sync attach + isFirebaseConfigured() startup diagnostic
        // log)। দুটোই সরানো হলো -- SupabaseRealtimeManager.attachDatabase() নিচে আগে থেকেই আছে
        // এবং একাই এই কাজ করে।
        runCatching { SupabaseRealtimeManager.attachDatabase(AppDatabase.getDatabase(application)) }
            .onFailure { Log.e("SomadhanViewModel", "SupabaseRealtimeManager.attachDatabase failed", it) }

        // Loading/Sync Fix Roadmap v2, ধাপ ৭ — connectivity observer চালু করা।
        startConnectivityObserver()

        // [Offline Action Gating ধাপ ১৩ - ফলো-আপ] bulk-pull সম্পূর্ণ সফল (LOADED + কোনো টেবিল
        // ব্যর্থ না) হলে সেটা এই ইউজারের জন্য persist করা -- উপরের hasCleanSyncHistory() দেখুন।
        // শুধু observe করে, কোনো sync-আচরণ বদলায় না।
        viewModelScope.launch {
            initialSyncPhase.collect { phase ->
                if (phase == SupabaseRealtimeManager.SyncPhase.LOADED &&
                    !SupabaseRealtimeManager.hasIncompleteInitialPull()
                ) {
                    recordCleanSyncIfApplicable()
                }
            }
        }

        viewModelScope.launch {
            // Stor 2: each startup step isolated in its own runCatching. Before this change these
            // four ran back-to-back with no try/catch at all -- one throwing (e.g. a bad Room
            // migration, a corrupted seed row, or any other unrelated bug) killed this whole
            // coroutine, which meant every step after the failure point silently never ran, AND
            // (since this is viewModelScope, not a scope with its own exception handler) an
            // uncaught exception here crashes the whole app a few moments after launch -- exactly
            // the "opens, runs briefly, then gets kicked out" symptom. Isolating each step means a
            // single bad step is logged and skipped, the rest still run, and the app never crashes
            // from this block again regardless of the root cause.
            runCatching { repository.ensureCategoriesSeeded() }
                .onFailure { Log.e("SomadhanViewModel", "Startup: ensureCategoriesSeeded failed", it) }
            runCatching { repository.ensureFaqsSeeded() }
                .onFailure { Log.e("SomadhanViewModel", "Startup: ensureFaqsSeeded failed", it) }
            runCatching { repository.checkAndProcess48HourAutoReleases() }
                .onFailure { Log.e("SomadhanViewModel", "Startup: checkAndProcess48HourAutoReleases failed", it) }
            runCatching { repository.checkAndExpireInstantJobs() }
                .onFailure { Log.e("SomadhanViewModel", "Startup: checkAndExpireInstantJobs failed", it) }
            // [Offline Action Gating ধাপ ২] app-open-এর সময় strict_offline_block-এর সাম্প্রতিক
            // cloud মান টেনে local Room cache আপডেট করা -- ছোট, single-row কল (উপরের full
            // bulk-pull থ্রেশহোল্ডের সাথে বাঁধা না, তাই প্রতিবার app খোলার সময়ই চলে)।
            runCatching { repository.syncStrictOfflineBlockSettingFromCloud() }
                .onFailure { Log.e("SomadhanViewModel", "Startup: syncStrictOfflineBlockSettingFromCloud failed", it) }
            // Scoped sync (see ENGINEERING_NOTES.md §2) needs to know the logged-in user's id,
            // and whether they're an admin (who needs full, unscoped visibility), before the
            // very first pullAllCloudDataToLocal() call below. Done here -- sequentially, in the
            // same coroutine, right before that call -- rather than in a separately-launched
            // coroutine, so there's no race between "who's logged in" and "what gets synced".
            // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.setCurrentUserId(...)
            // কল করে Firestore client-side admin/non-admin scoping সেট করা হতো। Supabase-এ এই
            // ধরনের manual scoping দরকার নেই -- RLS policy সার্ভার-সাইডে auth.uid()/is_admin()
            // অনুযায়ী নিজে থেকেই row filter করে, আর real Supabase Auth session
            // (SupabaseAuthManager phone sign-in-এর মাধ্যমে) supabase-kt SDK নিজেই persist/restore
            // করে -- তাই এখানে আলাদা করে কিছু সেট করার দরকার নেই।
            // Auto pull all Firestore data in background on startup -- but only a FULL pull if
            // one hasn't happened recently. The real-time listeners set up just above (via
            // setCurrentUserId -> startRealtimeListeners) already deliver the same scoped data
            // through their own initial snapshot moments after attaching -- calling
            // pullAllCloudDataToLocal() unconditionally on every single app open was fetching the
            // exact same data a second time via a separate .get() round trip. Skipping it when a
            // full sync already happened recently is a safe, low-risk form of incremental sync:
            // it doesn't change any Firestore query shape (no new composite indexes needed,
            // unlike a field-based "updatedAt > lastSync" delta approach would require), it just
            // avoids repeating work that was already done. checkListenerHealthAndFallbackSync()
            // (a completely separate 10-minute staleness check) remains the safety net if a
            // listener silently stops delivering updates. See ENGINEERING_NOTES.md §2.
            try {
                val lastFullSyncAt = sharedPrefs.getLong("last_full_sync_at", 0L)
                val sinceLastFullSync = System.currentTimeMillis() - lastFullSyncAt
                if (lastFullSyncAt == 0L || sinceLastFullSync > FULL_SYNC_MIN_INTERVAL_MS) {
                    // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.pullAllCloudDataToLocal()
                    // কলও ছিল, Supabase bulk-pull-এর পাশাপাশি। এখন শুধু Supabase-এর পথটাই থাকল --
                    // attachDatabase() ইতিমধ্যে একবার bulk-pull চালিয়েছে (উপরে init{}-এ), কিন্তু এটা
                    // পরবর্তী app-open startup-এও (একই "কতদিন পর পুরো পুল দরকার" থ্রেশহোল্ড মেনে)
                    // চালানো হলো, যাতে Supabase side-ও Firebase side-এর মতো একই "সাম্প্রতিক পূর্ণ
                    // পুল" ছন্দ অনুসরণ করে। ব্যর্থ হলে Firebase path অপ্রভাবিত (আলাদা runCatching)।
                    runCatching { SupabaseRealtimeManager.pullBulkDataFromSupabase() }
                        .onFailure { Log.e("SomadhanViewModel", "Startup: Supabase bulk pull failed", it) }
                    sharedPrefs.edit().putLong("last_full_sync_at", System.currentTimeMillis()).apply()
                } else {
                    Log.d("SomadhanViewModel", "Skipped redundant full pull -- last one was ${sinceLastFullSync / 1000}s ago, listeners already cover this")
                }
                repository.cleanupCorruptedCommissionRates()
                // Opportunistic, admin-only, self-throttled to ~2-3x/day (see
                // maybeAutoReconcileBalances()'s own doc comment) -- safe to call here even if
                // currentUser isn't loaded yet this particular time (falls through to the null
                // check below and simply skips), since the pull-to-refresh call sites below will
                // pick it up once currentUser is available.
                if (_currentUser.value?.role.equals("ADMIN", ignoreCase = true) == true) {
                    repository.maybeAutoReconcileBalances()
                }
            } catch (e: Exception) {
                Log.w("SomadhanViewModel", "Auto Supabase sync on startup: ${e.message}")
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    repository.checkAndExpireInstantJobs()
                } catch (_: Exception) {}
                delay(30_000L)
            }
        }
        restoreSession()
        startContinuousLocationTracking()
    }

    /**
     * Starts continuous, lifecycle-aware, 10-second real-time GPS tracking.
     * Continuously collects GPS updates every 10 seconds,
     * updates liveLocation StateFlow, and automatically syncs new coordinates into
     * the logged-in UserEntity in Room database on Dispatchers.IO.
     */
    fun startContinuousLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = viewModelScope.launch {
            try {
                // Immediate fetch on startup without popup toast
                _isLocationUpdating.value = true
                val initialRes = LocationHelper.getCurrentLocation(getApplication())
                if (initialRes is LocationFetchResult.Success) {
                    val initialLoc = initialRes.location
                    _liveLocation.value = initialLoc
                    syncUserLocationToDb(initialLoc)
                }
            } catch (_: Throwable) {
            } finally {
                delay(1000)
                _isLocationUpdating.value = false
            }

            try {
                LocationHelper.observeRealtimeLocation(getApplication())
                    .collect { loc ->
                        // 10-second automatic ticker: visually shows "আপডেট হচ্ছে..." and updates data everywhere without popup toast
                        _isLocationUpdating.value = true
                        _liveLocation.value = loc
                        syncUserLocationToDb(loc)
                        delay(1200)
                        _isLocationUpdating.value = false
                    }
            } catch (_: Throwable) {}
        }
    }

    private suspend fun syncUserLocationToDb(loc: LocationResult) {
        val user = _currentUser.value ?: return
        val distKm = if (user.latitude != 0.0 && user.longitude != 0.0) {
            DistanceUtil.calculateDistanceKm(user.latitude, user.longitude, loc.latitude, loc.longitude)
        } else {
            1.0 // Force initial update if not set yet
        }

        // Update user in Room DB if location coordinates or address changed
        if (distKm >= 0.01 || user.latitude == 0.0 || user.longitude == 0.0 || user.address != loc.address) {
            withContext(Dispatchers.IO) {
                val updatedUser = user.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    address = loc.address,
                    updatedAt = System.currentTimeMillis()
                )
                // [SUPABASE-MIGRATED - ধাপ ১৩] আগে এখানে repository.updateUser() এর পরও আলাদাভাবে
                // FirebaseSyncManager.syncUser(updatedUser) কল হতো -- কিন্তু repository.updateUser()
                // নিজেই ইতিমধ্যে FirebaseSyncManager.syncUser() কল করে (এবং এটা যদি বর্তমান
                // Supabase-logged-in ইউজারের নিজের row হয়, SupabaseSyncManager.updateOwnProfile()ও
                // কল করে) -- তাই সেই দ্বিতীয় সরাসরি কলটা বিশুদ্ধ ডুপ্লিকেট ছিল, সরিয়ে ফেলা হলো।
                repository.updateUser(updatedUser)
                withContext(Dispatchers.Main) {
                    _currentUser.value = updatedUser
                }
            }
        }
    }

    fun stopContinuousLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }

    fun refreshLiveLocation(showToast: Boolean = true) {
        viewModelScope.launch {
            refreshLiveLocationAwait(showToast)
        }
    }

    // Bug fix: PostProblemScreen-এর pull-to-refresh স্পিনার আগে সরাসরি [_isLocationUpdating]-এর
    // সাথে বাঁধা ছিল — কিন্তু এই একই ফ্ল্যাগ [startContinuousLocationTracking]-এর ব্যাকগ্রাউন্ড
    // ১০-সেকেন্ড GPS টিকার-ও (পুরো অ্যাপজুড়ে, স্ক্রিন-নির্বিশেষে) true/false করে — ফলে ব্যবহারকারী
    // কিছু না করা সত্ত্বেও প্রতি ~১০ সেকেন্ডে পোস্ট-জব পেজে pull-to-refresh স্পিনার নিজে থেকেই
    // ভেসে উঠত। এই suspend ভ্যারিয়েন্টটা caller-কে আসল কাজ শেষ হওয়া পর্যন্ত await করতে দেয়, যাতে
    // caller (PostProblemScreen) নিজের আলাদা, স্ক্রিন-লোকাল "isManualRefreshing" state ব্যবহার
    // করে শুধু ব্যবহারকারীর pull gesture-এই স্পিনার দেখাতে পারে — ব্যাকগ্রাউন্ড টিকারের সাথে আর
    // জড়ানো থাকে না। পুরনো non-suspend [refreshLiveLocation] (উপরে) এই ফাংশনটাই ভেতরে কল করে,
    // তাই বিদ্যমান বাকি সব caller-এর আচরণ অপরিবর্তিত থাকে।
    suspend fun refreshLiveLocationAwait(showToast: Boolean = true) {
        _isLocationUpdating.value = true
        try {
            val res = LocationHelper.getCurrentLocation(getApplication())
            if (res is LocationFetchResult.Success) {
                val loc = res.location
                _liveLocation.value = loc
                syncUserLocationToDb(loc)
                if (showToast) {
                    _uiToast.value = "📍 বর্তমান অবস্থান: ${loc.address}"
                }
            } else if (res is LocationFetchResult.PermissionDenied && showToast) {
                _uiToast.value = "অনুগ্রহ করে ডিভাইসের লোকেশন পারমিশন চালু করুন"
            }
        } catch (_: Throwable) {
            if (showToast) {
                _uiToast.value = "লোকেশন রিফ্রেশ করা যায়নি"
            }
        } finally {
            delay(1200)
            _isLocationUpdating.value = false
        }
    }

    fun setManualLocation(loc: LocationResult) {
        viewModelScope.launch {
            _liveLocation.value = loc
            syncUserLocationToDb(loc)
            _uiToast.value = "📍 লোকেশন সেট করা হয়েছে: ${loc.address}"
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            try {
                val savedUserId = sharedPrefs.getString("saved_user_id", null)
                if (!savedUserId.isNullOrBlank()) {
                    val user = repository.getUserById(savedUserId)
                    if (user != null) {
                        _currentUser.value = user
                        observeUserData(user.id)
                    }
                    // Fetch latest profile & wallet balance from Firestore in background
                    val freshCloudUser = repository.refreshUserDataFromCloud(savedUserId)
                    if (freshCloudUser != null) {
                        _currentUser.value = freshCloudUser
                    }
                }
            } catch (_: Throwable) {
            } finally {
                _isSessionRestored.value = true
            }
        }
    }

    fun refreshData() {
        // [Offline Action Gating ধাপ ১৩] অফলাইনে pull-to-refresh-এ কিছুই আসলে refresh হয় না, অথচ আগে
        // "ডেটা রিফ্রেশ সম্পন্ন হয়েছে।" success toast দেখাত। এখন স্পিনার শুরুর আগেই সৎ বার্তা দিয়ে ফেরত
        // (Strict মোডে অফলাইনে ইউজার এই স্ক্রিনেই পৌঁছায় না, তাই শুধু non-strict মোডে কার্যকর)।
        if (!isOnline.value) {
            showToast("ইন্টারনেট নেই — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে")
            return
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Loading/Sync Fix Roadmap v2, ধাপ ৬ — pull-to-refresh-কে আসল retry বানানো।
                // আগে এই ফাংশন শুধু listener health check করত, কখনোই আসল ব্যর্থ হওয়া bulk-pull
                // (attachDatabase()/pullBulkDataFromSupabase()) আবার কল করত না -- তাই যদি
                // initialSyncPhase ইতিমধ্যে ERROR-এ থাকে (শুরুর bulk-pull সম্পূর্ণ ব্যর্থ হয়ে),
                // pull-to-refresh সেটা ঠিক করতে পারত না। এখন সেই ফাঁক বন্ধ করা হলো: ERROR হলে
                // retryInitialSync() কল করে আসল ব্যর্থ ডাটা-পুলটাই retry হয়। retryInitialSync()-এর
                // ভেতরের [initialSyncInFlight] guard (ধাপ ৫) নিশ্চিত করে যে phase ইতিমধ্যে LOADING
                // থাকলে (অন্য কোনো ট্রিগার থেকে চলমান) এটা দ্বিতীয়বার একই fetch শুরু করবে না --
                // pull-to-refresh স্পিনার তখন existing fetch শেষ হওয়া পর্যন্ত অপেক্ষা করলেই যথেষ্ট।
                if (initialSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR) {
                    retryInitialSync()
                }

                // 1. Real-time listeners already keep local Room continuously synced (see
                // SupabaseRealtimeManager.startRealtimeListeners()). A routine pull-to-refresh no
                // longer needs to re-fetch all collections unconditionally -- that was the
                // slow, unnecessary work happening on every single screen's pull-to-refresh
                // gesture across the app. Instead, only the specific tables whose listener
                // looks stale get a targeted fallback pull.
                // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.checkListenerHealthAndFallbackSync()
                // কলও ছিল, নিচের Supabase কলটার পাশে -- এখন শুধু Supabase পথ।
                runCatching { SupabaseRealtimeManager.checkListenerHealthAndFallbackSync() }
                    .onFailure { Log.e("SomadhanViewModel", "refreshData: Supabase health check failed", it) }

                // Check and process 48-hour auto releases & instant job auto expiry
                repository.checkAndProcess48HourAutoReleases()
                repository.checkAndExpireInstantJobs()
                repository.reconcileEscrowStates()

                // 2. Reload current user from local database if logged in
                val userId = _currentUser.value?.id
                if (userId != null) {
                    val freshUser = repository.getUserById(userId)
                    if (freshUser != null) {
                        _currentUser.value = freshUser
                    }
                }

                // 3. Reload selected problem if currently viewing
                _selectedProblem.value?.id?.let { probId ->
                    selectProblem(probId)
                }

                // Minimal delay for smooth visual feedback
                delay(300)
                showToast("ডেটা রিফ্রেশ সম্পন্ন হয়েছে।")
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Refresh data failed: ${e.message}", e)
                showToast("রিফ্রেশ করতে সমস্যা হয়েছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Pull-to-refresh for the Wallet screen and the Solver Dashboard specifically. A refund or a
     * dispute-release credited by someone ELSE's device has to travel through Firestore before
     * this device's listener picks it up -- unlike a gateway deposit, which is entirely local and
     * instant. This does a small, targeted sync of just this user's own balance and recent
     * transactions FIRST (fast, since it's one document + two small queries, not the whole app's
     * data), so a cross-device balance change shows up here as quickly as the network allows,
     * before falling back to the same general listener-health-check as refreshData().
     */
    fun refreshWalletData() {
        // [Offline Action Gating ধাপ ১৩] অফলাইনে pull-to-refresh-এ কিছুই আসলে refresh হয় না, অথচ আগে
        // "ডেটা রিফ্রেশ সম্পন্ন হয়েছে।" success toast দেখাত। এখন স্পিনার শুরুর আগেই সৎ বার্তা দিয়ে ফেরত
        // (Strict মোডে অফলাইনে ইউজার এই স্ক্রিনেই পৌঁছায় না, তাই শুধু non-strict মোডে কার্যকর)।
        if (!isOnline.value) {
            showToast("ইন্টারনেট নেই — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে")
            return
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Loading/Sync Fix Roadmap v2, ধাপ ৬ — refreshData()-এর মতোই retry-if-ERROR।
                // Wallet/Withdrawal-এর ডাটা (users balance, transactions, withdrawals) সবই
                // bulk-pull-এর অংশ, তাই এখানেও initialSyncPhase-ই প্রাসঙ্গিক signal।
                if (initialSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR) {
                    retryInitialSync()
                }

                val userId = _currentUser.value?.id

                // 1. Priority: this user's own balance + recent transactions only.
                // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.syncCurrentUserBalanceAndTransactions(userId)
                // কল করে Firestore থেকে এই ইউজারের balance+transactions targeted-pull করা হতো।
                // Supabase-এর কোনো এক্স্যাক্ট সমতুল্য ফাংশন নেই -- কিন্তু দরকারও নেই, কারণ users ও
                // transactions দুটো টেবিলই ইতিমধ্যে SupabaseRealtimeManager.startRealtimeListeners()-এর
                // গ্লোবাল realtime channel-এ আছে (attachDatabase()-এর সময় থেকেই চালু), আর নিচের
                // checkListenerHealthAndFallbackSync() সেই চ্যানেল স্টেল হয়ে গেলে fallback pull করে --
                // তাই এই টার্গেটেড কলটা বাদ দিলেও কভারেজে গ্যাপ থাকছে না।
                if (userId != null) {
                    val freshUser = repository.getUserById(userId)
                    if (freshUser != null) {
                        _currentUser.value = freshUser
                    }
                }

                // 2. Then the same general health-check + local reconciliation as refreshData().
                runCatching { SupabaseRealtimeManager.checkListenerHealthAndFallbackSync() }
                    .onFailure { Log.e("SomadhanViewModel", "refreshWalletData: Supabase health check failed", it) }
                repository.checkAndProcess48HourAutoReleases()
                repository.checkAndExpireInstantJobs()
                repository.reconcileEscrowStates()

                if (userId != null) {
                    val freshUser = repository.getUserById(userId)
                    if (freshUser != null) {
                        _currentUser.value = freshUser
                    }
                }

                _selectedProblem.value?.id?.let { probId ->
                    selectProblem(probId)
                }

                delay(300)
                showToast("ডেটা রিফ্রেশ সম্পন্ন হয়েছে।")
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Refresh wallet data failed: ${e.message}", e)
                showToast("রিফ্রেশ করতে সমস্যা হয়েছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshCurrentUser() {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val freshUser = repository.getUserById(userId)
            if (freshUser != null) {
                _currentUser.value = freshUser
            }
        }
    }

    fun clearToast() {
        _uiToast.value = null
    }

    fun showToast(message: String) {
        _uiToast.value = message
    }

    // [Offline Action Gating ধাপ ৪] Reusable online-guard — গ্রুপ B-এর প্রতিটি network-writing
    // action-এর (RPC/insert/update) ঠিক আগে caller এটা কল করবে। অফলাইন হলে toast দেখিয়ে
    // `false` রিটার্ন করে (caller তখন সাথে সাথে return করবে, action এক্সিকিউট হবে না) --
    // স্ক্রিন/নেভিগেশন অক্ষত থাকে, silent fail না। উপরের বিদ্যমান [isOnline] StateFlow-ই
    // (`startConnectivityObserver()`, init{}-এ শুরু হয়) reuse করা হয়েছে -- এটা
    // MainActivity.kt-এর NetworkConnectivityObserver-এর থেকে আলাদা, স্বাধীন
    // ConnectivityManager.NetworkCallback ইনস্ট্যান্স (আগে থেকেই ছিল, Loading/Sync Fix Roadmap
    // v2 ধাপ ৭-এর error-retry-এর জন্য) -- তাই নতুন কোনো observer বানাতে হয়নি, শুধু বিদ্যমানটাই
    // ব্যবহার করা হলো।
    //
    // টগল (`isStrictOfflineBlockEnabled`) এখানে চেক করা হয় না -- Strict (ON) মোডে
    // MainActivity-এর NoInternetOverlay (ধাপ ৩) পুরো স্ক্রিনই ব্লক করে রাখে, তাই ইউজার এই
    // ফাংশন-কল পর্যন্ত পৌঁছাতেই পারবে না; শুধু non-strict (OFF) মোডেই এটা বাস্তবে কার্যকর হয়।
    fun requireOnlineOrWarn(
        message: String = "ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না"
    ): Boolean {
        if (!isOnline.value) {
            showToast(message)
            return false
        }
        return true
    }

    // [Offline Action Gating ধাপ ৯] ChatScreen-এ ব্যর্থ (sendStatus="FAILED") মেসেজের নিচের
    // ইনলাইন "আবার পাঠান" ট্যাপ এই ফাংশন কল করে। এখানে requireOnlineOrWarn() ব্যবহার করা হলো --
    // manual retry-তেও যদি এখনো নেটওয়ার্ক না থাকে তাহলে সাথে সাথে টোস্ট দেখাবে (নীরবে আবার fail
    // হয়ে অপেক্ষা করানোর বদলে), ঠিক ইউজারের বর্ণনা অনুযায়ী ("network fire pelei message send hobe")।
    fun retryFailedMessage(messageId: String) {
        if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া মেসেজ পাঠানো যাবে না")) return
        viewModelScope.launch {
            val ok = repository.retrySendMessage(messageId)
            if (!ok) {
                showToast("মেসেজ পাঠানো যায়নি, আবার চেষ্টা করুন")
            }
        }
    }

    // Refreshes admin dashboard numbers via server-side count()/sum() aggregation only -- no
    // full data pull. Cheap enough to call every time the admin opens the Stats tab.
    // See ENGINEERING_NOTES.md §2.
    fun refreshAdminMetrics() {
        viewModelScope.launch {
            // additional_charges is no longer live-listened (ENGINEERING_NOTES.md §9) -- the
            // overview page's "additional charges" counters need this explicit refresh instead.
            // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.refreshAdminMetricsViaAggregation()
            // ও FirebaseSyncManager.refreshAdditionalCharges() কলও ছিল -- নিচের
            // SupabaseSyncManager.getAdminDashboardMetrics() (dto) ও
            // repository.refreshAdditionalChargesFromSupabase() (নিচে) এখন এই দুটোর একমাত্র পথ।
            SupabaseSyncManager.getAdminDashboardMetrics().onSuccess { dto ->
                _supabaseAdminMetrics.value = AdminDashboardMetrics(
                    totalUsers = dto.totalUsers,
                    totalSolvers = dto.totalSolvers,
                    totalClients = (dto.totalUsers - dto.totalSolvers).coerceAtLeast(0),
                    totalProblems = dto.totalProblems,
                    openProblems = dto.openProblems,
                    completedProblems = dto.completedProblems,
                    inProgressProblems = dto.inProgressProblems,
                    totalBids = dto.totalBids,
                    pendingBids = dto.pendingBids,
                    acceptedBids = dto.acceptedBids,
                    totalTransactionVolume = dto.totalTransactionVolume,
                    platformRevenue = dto.platformRevenue,
                    pendingWithdrawals = dto.pendingWithdrawals,
                    completedWithdrawals = dto.completedWithdrawals,
                    isConnected = true,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    categoryProblemCounts = dto.categoryProblemCounts,
                    categoryBidCounts = dto.categoryBidCounts
                )
            }.onFailure { e ->
                Log.w("SomadhanViewModel", "refreshAdminMetrics: Supabase RPC failed", e)
            }
            repository.refreshAdditionalChargesFromSupabase()
        }
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৭ — বাগ C ফিক্স] Admin panel-এর প্রতিটা ট্যাবের
    // pull-to-refresh আগে সরাসরি triggerCloudSync() কল করত — যেটা টপ-বারের "Force Sync"
    // বাটনের মতোই একটা সম্পূর্ণ push (syncAllLocalToSupabase) + pull
    // (pullBulkDataFromSupabase, ৪৫ সেকেন্ড পর্যন্ত টাইমআউট) গ্লোবাল অপারেশন, তাই স্লো/আটকে
    // যাওয়া মনে হতো। এই ফাংশনটা user/solver-সাইড refreshData()-এর মতোই হালকা: প্রতিটা টেবিল
    // ইতিমধ্যেই SupabaseRealtimeManager-এর realtime listener দিয়ে continuously সিঙ্ক থাকে
    // (allUsers/allProblems/allWithdrawals/allHeldEscrows ইত্যাদি সব StateFlow), তাই পুরো
    // push+pull-এর দরকার নেই — শুধু listener stale হয়ে গেলে targeted fallback pull
    // (checkListenerHealthAndFallbackSync) আর active ট্যাবের non-realtime (RPC-aggregated)
    // ডেটা থাকলে সেটাই refresh করা হয়। বর্তমানে শুধু Overview ট্যাব (index 0, adminMetrics)
    // RPC-aggregated — বাকি সব ট্যাবের ডেটা realtime StateFlow দিয়েই কাভার্ড।
    // টপ-বারের "Force Sync" বাটন (triggerCloudSync()) অপরিবর্তিত — সেটা ইচ্ছাকৃত ফুল-সিঙ্ক
    // অপশন হিসেবেই থেকে যাচ্ছে।
    fun refreshAdminTab(tabIndex: Int) {
        // [Offline Action Gating ধাপ ১৩] অফলাইনে pull-to-refresh-এ কিছুই আসলে refresh হয় না, অথচ আগে
        // "ডেটা রিফ্রেশ সম্পন্ন হয়েছে।" success toast দেখাত। এখন স্পিনার শুরুর আগেই সৎ বার্তা দিয়ে ফেরত
        // (Strict মোডে অফলাইনে ইউজার এই স্ক্রিনেই পৌঁছায় না, তাই শুধু non-strict মোডে কার্যকর)।
        if (!isOnline.value) {
            showToast("ইন্টারনেট নেই — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে")
            return
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (initialSyncPhase.value == SupabaseRealtimeManager.SyncPhase.ERROR) {
                    retryInitialSync()
                }

                runCatching { SupabaseRealtimeManager.checkListenerHealthAndFallbackSync() }
                    .onFailure { Log.e("SomadhanViewModel", "refreshAdminTab: Supabase health check failed", it) }

                // [Somadhan Bug-Fix Step 5 — গ্রুপ ২.৪] pull-to-refresh টানলেও Withdrawals/Users
                // ট্যাবের ডিফল্ট (unfiltered) paged snapshot (`adminWithdrawalsPaged`/
                // `adminUsersPaged`) আগে রিফ্রেশ হতো না -- শুধু filter-toggle/re-entry-তেই হতো।
                // এই দুটো লাইটওয়েট (page size 10) রিসেট এখন unconditionally কল করা হচ্ছে, tabIndex
                // যাই হোক না কেন -- AdminUsersView.kt-ও (rule #৮) একই non-reactive-snapshot
                // architecture শেয়ার করে বলে (verify করা হয়েছে, নিচে দেখুন) দুটোই এখানে কভার করা হলো।
                // resetAdminUsersPagination()-কে বর্তমান `currentAdminUsersRoleFilter` দিয়েই কল করা
                // হচ্ছে (default null না) -- নইলে admin কোনো role-filter সিলেক্ট করা অবস্থায়
                // pull-to-refresh টানলে filter silently "ALL"-এ রিসেট হয়ে যেত, কারণ
                // `AdminUsersView.kt`-এর `isBrowsingUnfiltered` শুধু search/sort দেখে, role-filter
                // দেখে না, তাই সেই LaunchedEffect আবার ফায়ার হয়ে ঠিক করে দিত না।
                resetAdminWithdrawalsPagination()
                resetAdminUsersPagination(currentAdminUsersRoleFilter)

                repository.checkAndProcess48HourAutoReleases()
                repository.checkAndExpireInstantJobs()
                repository.reconcileEscrowStates()

                if (tabIndex == 0) {
                    refreshAdminMetrics()
                }

                // [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] রোল-ম্যানেজমেন্ট ট্যাবের pull-to-refresh।
                if (tabIndex == AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX && isCurrentAdminSuper()) {
                    // ব্যর্থ হলে নিচের catch-এ পৌঁছাই ("রিফ্রেশ করতে সমস্যা" টোস্ট) — মিথ্যা "সম্পন্ন" টোস্ট না।
                    if (!fetchAdminRolesOnce()) throw IllegalStateException("admin roles refresh failed")
                }

                // [ADMIN_ROLE_PROFILE সেশন ৪] এডমিন-অ্যাকাউন্ট ট্যাবের pull-to-refresh — অ্যাকাউন্ট + রোল দুটোই
                // (রোল-ড্রপডাউন/account_count তাজা থাকে)। যেকোনোটা ব্যর্থ হলে "রিফ্রেশ করতে সমস্যা" টোস্ট।
                if (tabIndex == AdminPermissionCatalog.ADMIN_ACCOUNTS_TAB_INDEX && isCurrentAdminSuper()) {
                    val accountsOk = fetchAdminAccountsOnce()
                    val rolesOk = fetchAdminRolesOnce()
                    if (!accountsOk || !rolesOk) throw IllegalStateException("admin accounts refresh failed")
                }

                // [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগ ট্যাবের pull-to-refresh — বর্তমান ফিল্টারে প্রথম পেজ
                // আবার আনে (স্ক্রল-করা বাড়তি পেজগুলো বাদ যায়, নতুন লগ ওপরে আসে)।
                if (tabIndex == AdminPermissionCatalog.ACTIVITY_LOG_TAB_INDEX && isCurrentAdminSuper()) {
                    if (!fetchAdminActivityLogsFirstPage(_adminActivityLogFilter.value)) {
                        throw IllegalStateException("admin activity logs refresh failed")
                    }
                }

                delay(300)
                showToast("ডেটা রিফ্রেশ সম্পন্ন হয়েছে।")
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "refreshAdminTab failed: ${e.message}", e)
                showToast("রিফ্রেশ করতে সমস্যা হয়েছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // [পুল-টু-রিফ্রেশ/সিঙ্ক-স্টাক ফিক্স] আগে এই ফাংশনের কোনো try/finally ছিল না —
            // নিচের `repository.getAllUsers()/getAllProblems()/getAllBids()` বা
            // `repository.syncAllLocalToSupabase(...)` কোনো exception ছুঁড়লে (নেটওয়ার্ক সমস্যা,
            // Supabase timeout ইত্যাদি) পুরো coroutine exceptionally শেষ হয়ে যেত, আর নিচের
            // `_isRefreshing.value = false` লাইনটা কখনো চলতোই না। `_isRefreshing` একটা
            // অ্যাপ-জোড়া শেয়ার্ড StateFlow (এই সিঙ্ক বাটন আর প্রতিটা স্ক্রিনের pull-to-refresh
            // স্পিনার দুটোই এটার উপর নির্ভর করে) — একবার এভাবে আটকে গেলে পুরো অ্যাপ জুড়ে সব
            // pull-to-refresh চিরকালের জন্য "চলছে" দেখাত, যতক্ষণ না অ্যাপ force-quit করে আবার
            // খোলা হয় (যা in-memory StateFlow-কে রিসেট করে দেয়) — ঠিক এই উপসর্গটাই রিপোর্ট করা
            // হয়েছিল। এখন try/finally দিয়ে নিশ্চিত করা হলো, যাই ঘটুক না কেন, isRefreshing সবসময়
            // false-এ ফিরবেই।
            try {
                // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "পুল-টু-রিফ্রেশ/সিঙ্ক আটকে যাচ্ছে"] দ্বিতীয়
                // স্তরের সুরক্ষা — SupabaseClientProvider-এ HttpTimeout যোগ করাই মূল ফিক্স
                // (নেটওয়ার্ক কল নিজেই এখন টাইমআউট ছুঁড়বে), কিন্তু এই ব্লকে Room DB কলও আছে
                // (repository.getAllUsers() ইত্যাদি) যেটা তাত্ত্বিকভাবে অন্য কোনো কারণে হ্যাং
                // করতে পারে। withTimeoutOrNull দিয়ে পুরো ব্লকটাই একটা হার্ড ডেডলাইনের
                // ভেতরে রাখা হলো — এর বেশি সময় লাগলে ব্লকটা cancel হয়ে null রিটার্ন করবে,
                // exception ছোঁড়ে না (তাই নিচের catch না গিয়ে সরাসরি finally-তে পৌঁছাবে,
                // isRefreshing false হবেই)।
                val completed = withTimeoutOrNull(45_000L) {
                    sharedPrefs.edit().putLong("last_full_sync_at", System.currentTimeMillis()).apply()
                    // 1b. Admin dashboard aggregation refresh now happens via refreshAdminMetrics() below
                    //     (which itself calls the Supabase RPC) -- no separate Firebase call needed here.

                    // 2. Sync local changes to cloud.
                    // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে
                    // FirebaseSyncManager.syncAllLocalToFirestore(users, problems, bids, withdrawals) কল
                    // ছিল। সিদ্ধান্ত (A): repository.syncAllLocalToSupabase(users, problems, bids) — নতুন
                    // recovery ফাংশন, দেখো SomadhanRepository.kt। withdrawals **ইচ্ছাকৃতভাবে বাদ** —
                    // request_withdrawal RPC non-idempotent (money-affecting), তাই bulk re-push-এ ডুপ্লিকেট
                    // টাকা কাটার ঝুঁকি আছে (বিস্তারিত কারণ ঐ ফাংশনের KDoc-এ)। withdrawal creation-এর সময়
                    // requestWithdrawal() ইতিমধ্যেই dual-write করে বলে এই recovery-নেট ছাড়াও কভার্ড।
                    val users = repository.getAllUsers().firstOrNull() ?: emptyList()
                    val problems = repository.getAllProblems().firstOrNull() ?: emptyList()
                    val bids = repository.getAllBids().firstOrNull() ?: emptyList()

                    runCatching { repository.syncAllLocalToSupabase(users, problems, bids) }
                        .onFailure { Log.e("SomadhanViewModel", "triggerCloudSync: syncAllLocalToSupabase failed", it) }
                    // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.pullAllCloudDataToLocal()
                    // ও FirebaseSyncManager.startRealtimeListeners() কলও ছিল, নিচের Supabase কলগুলোর
                    // পাশাপাশি -- এখন Supabase-ই এই "Force Sync" বাটনের পূর্ণ পুল পথ, আর toast-এর জন্য
                    // ফলাফলও এখান থেকেই আসে।
                    // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "Force Sync অনেকক্ষণ লোড হয় / শেষ হয় না"] আগে এখানে
                    // pullBulkDataFromSupabase()-এর পর SupabaseRealtimeManager.startRealtimeListeners()-ও
                    // কল হতো, যেটা প্রথমে stopRealtimeListeners() দিয়ে ১০টা টেবিলের (users, problems,
                    // bids, messages, transactions, escrows, gateway_payments, additional_charges,
                    // notifications, withdrawals) সব active channel বন্ধ করে, তারপর একটার পর একটা
                    // সিকোয়েন্সিয়ালি (parallel না) নতুন করে subscribe() করত -- প্রতিটাই server-side ACK-এর
                    // জন্য অপেক্ষা করে, আর websocket subscribe-এর কোনো নির্দিষ্ট timeout নেই (৩০s
                    // HttpTimeout শুধু REST কল কভার করে)। নেটওয়ার্ক একটু দুর্বল হলে এই ১০টা মিলিয়ে Force
                    // Sync অনেক সময় নিচ্ছিল/আটকে থাকার মতো মনে হচ্ছিল। এই restart আদৌ দরকারই ছিল না --
                    // listener ইতিমধ্যেই সবসময় চালু থাকে: session শুরুতে attachDatabase() →
                    // performInitialSync()-এর মাধ্যমে একবার, আর admin panel ওপেন হওয়ার সময়
                    // AdminPanelScreen.kt-এর নিজস্ব LaunchedEffect(Unit)-এ আরেকবার। তাই এখানে third
                    // restart-টা সরানো হলো -- শুধু push (উপরে) + pull থাকবে, ডেটা-কভারেজ অপরিবর্তিত।
                    val result = runCatching {
                        SupabaseRealtimeManager.pullBulkDataFromSupabase()
                    }.onFailure { Log.e("SomadhanViewModel", "triggerCloudSync: Supabase side failed", it) }

                    // ধাপ ৩২.৫ — Force Sync চাপলে admin dashboard aggregation + additional_charges
                    // dual-run pull-ও একইসাথে রিফ্রেশ হবে।
                    runCatching { refreshAdminMetrics() }
                        .onFailure { Log.e("SomadhanViewModel", "triggerCloudSync: refreshAdminMetrics failed", it) }
                    val pullResult = result.getOrNull()
                    if (pullResult?.isSuccess == true) {
                        showToast("সিঙ্ক সম্পন্ন: ${pullResult.usersCount} ইউজার, ${pullResult.problemsCount} সমস্যা লোড হয়েছে।")
                    } else {
                        showToast("সিঙ্ক ফলাফল: ${pullResult?.error ?: "সিঙ্ক করতে সমস্যা হয়েছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।"}")
                    }
                }
                if (completed == null) {
                    Log.e("SomadhanViewModel", "triggerCloudSync: timed out after 45s")
                    showToast("সিঙ্ক করতে অনেক সময় লাগছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।")
                }
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "triggerCloudSync: unexpected failure: ${e.message}", e)
                showToast("সিঙ্ক করতে সমস্যা হয়েছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৪] আগে এটা Firebase project id/API key রিকনফিগার করে
     * `FirebaseSyncManager.startRealtimeListeners()`/`pullAllCloudDataToLocal()` কল করতো। এখন
     * ফিচারটা ডিলিট না করে Supabase URL/anon key রিকনফিগার করার কাজে repurpose করা হয়েছে —
     * সেভ সফল হলে নতুন client দিয়ে `SupabaseRealtimeManager` রিয়েল-টাইম লিসেনার শুরু করে ও
     * একটা bulk pull চালায়, তারপর কানেকশন টেস্ট করে ফলাফল দেখায়।
     */
    fun reconfigureSupabaseAndSync(
        context: android.content.Context,
        url: String,
        anonKey: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isRefreshing.value = true
            // [পুল-টু-রিফ্রেশ/সিঙ্ক-স্টাক ফিক্স] আগে এখানেও কোনো try/finally ছিল না —
            // `saveAndReconfigure`/`testConnection` কোনো exception ছুঁড়লে দুইটা
            // `_isRefreshing.value = false` লাইনের একটাও চলত না, একই class-এর "চিরকাল
            // refreshing" বাগ।
            try {
                val ok = com.example.util.SupabaseConfigHelper.saveAndReconfigure(
                    context = context,
                    url = url,
                    anonKey = anonKey
                )
                if (ok) {
                    val testResult = com.example.util.SupabaseConfigHelper.testConnection(context)
                    if (testResult.first) {
                        runCatching {
                            com.example.data.remote.SupabaseRealtimeManager.stopRealtimeListeners()
                            com.example.data.remote.SupabaseRealtimeManager.startRealtimeListeners()
                            com.example.data.remote.SupabaseRealtimeManager.pullBulkDataFromSupabase()
                        }.onFailure {
                            Log.w("SomadhanViewModel", "reconfigureSupabaseAndSync: post-reconfigure sync failed", it)
                        }
                    }
                    showToast(testResult.second)
                    onResult(testResult.first, testResult.second)
                } else {
                    val msg = "Supabase পুনঃসংযোগ ব্যর্থ হয়েছে। অনুগ্রহ করে Project URL ও anon key যাচাই করুন।"
                    showToast(msg)
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Supabase পুনঃসংযোগ ব্যর্থ হয়েছে।"
                showToast(msg)
                onResult(false, msg)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setUserFilterCategory(categoryId: String?) {
        _userFilterCategory.value = categoryId
    }

    fun setSolverFeedFilterCategory(categoryId: String?) {
        _solverFeedFilterCategory.value = categoryId
    }

    fun setSolverSearchQuery(query: String) {
        _solverSearchQuery.value = query
    }

    // ---------------- AUTH METHODS ----------------

    /**
     * Checks [enteredPass] against the user's stored password, which may be
     * either a bcrypt hash (new accounts) or plaintext (legacy accounts
     * created before hashing was introduced). On a successful legacy match,
     * transparently re-saves the password as a bcrypt hash — locally and in
     * Firestore — so the account is migrated the moment the owner proves
     * they know the password.
     */
    private suspend fun verifyPassword(user: UserEntity, enteredPass: String): Boolean {
        return if (PasswordHasher.isHashed(user.password)) {
            PasswordHasher.verify(enteredPass, user.password)
        } else {
            val matches = user.password == enteredPass
            if (matches) {
                repository.migrateLegacyPlaintextPassword(user, enteredPass)
            }
            matches
        }
    }

    /**
     * Step 1: Validate login credentials (phone/password).
     *
     * ধাপ ১৪: আগে এখানে local bcrypt verify + Firestore cross-device fetch হতো; এখন
     * `repository.loginWithPhonePassword()` কল করে, যেটা real Supabase Auth (phone+password)
     * দিয়ে credential check করে — এই কলেই আসলে Supabase session তৈরি হয়ে যায় (এটাই "step 1 এ
     * session তৈরি হয় না" — মূল প্রম্পটের ধারণার থেকে ভিন্ন; কারণ ব্যাখ্যা
     * `SupabaseAuthManager`-এর ক্লাস-ডকে আছে)। নিচের demo OTP ধাপ (sendOtp/verifyOtp,
     * LoginScreen-এ) অপরিবর্তিত থেকে যায় — technically session ইতিমধ্যে তৈরি, কিন্তু UI-এর
     * দৃষ্টিতে ব্যবহারকারীর জন্য flow same-ই দেখাবে (OTP screen এখনো আসবে, demo code দিতে হবে)।
     */
    fun validateLoginCredentials(
        phone: String,
        pass: String,
        onSuccess: (UserEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৫] Login-এর real network call (Supabase Auth sign-in +
            // cloud profile fetch, কোনো local fallback নেই -- SomadhanRepository.loginWithPhonePassword
            // দ্রষ্টব্য) -- অফলাইনে গেলে Supabase একটা ভুল "phone/password" এরর দিত (আসল কারণ
            // network, credential না), তাই এখানে গার্ড দিয়ে স্পষ্ট মেসেজ দেওয়া হলো। onError()-ও কল
            // করা হয়েছে (শুধু toast না) যাতে LoginScreen-এর isLoading স্পিনার আটকে না থাকে।
            if (!requireOnlineOrWarn()) {
                onError("ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না")
                return@launch
            }
            val result = repository.loginWithPhonePassword(phone.trim(), pass)
            result.onSuccess { user ->
                onSuccess(user)
            }.onFailure {
                onError(it.message ?: "লগইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।")
            }
        }
    }

    /**
     * Step 2: Complete login session creation after OTP is verified successfully.
     * Saves user ID to SharedPreferences for persistent session across app restarts.
     *
     * [Offline Action Gating ধাপ ৫ — ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি] এটাও একটা নেটওয়ার্ক কল
     * (`refreshUserDataFromCloud`) করে, কিন্তু ব্যর্থ হলে `?: user` fallback দিয়ে ইতিমধ্যেই
     * gracefully আগে-থেকে-verify-করা `user` (validateLoginCredentials থেকে) দিয়ে local
     * session সম্পূর্ণ হয়ে যায় -- অর্থাৎ এই ফাংশন ইতিমধ্যেই offline-safe। এখানে
     * `requireOnlineOrWarn()` গার্ড বসালে বরং rule ১ ভাঙত -- OTP verify করার পরও ইউজারকে
     * পুরো লগইন থেকে আটকে দিত, যেটা বর্তমান (ভালো) আচরণের চেয়ে খারাপ হতো।
     */
    fun completeLoginAfterOtp(
        user: UserEntity,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val freshUser = repository.refreshUserDataFromCloud(user.id) ?: user
            _currentUser.value = freshUser
            sharedPrefs.edit().putString("saved_user_id", freshUser.id).apply()
            // [ROLE-UID-SYNC-FIX ধাপ ৪] Login-এর ঠিক পরে, প্রতি logged-in ইউজারের জন্য একবার --
            // পুরনো (আগে role switch করা) ডিভাইসে যদি SOLVER_xxx/USER_xxx legacy linked row থাকে,
            // তার KYC/categories/balance-conflict non-destructively root row-এ merge করা হয়
            // (ধাপ ৩-এ লেখা migrateLegacyDualRowsIntoRoot(), idempotent -- archived guard থাকায়
            // দ্বিতীয়বার কল হলে কিছু করে না)। কোনো UI/flow বদলায় না, শুধু pure local Room merge।
            runCatching { repository.migrateLegacyDualRowsIntoRoot(freshUser.id) }
                .onFailure { Log.e("SomadhanViewModel", "completeLoginAfterOtp: legacy dual-row migration failed", it) }
            // [Offline Action Gating ধাপ ২] Login সম্পন্ন হওয়ার মুহূর্তেই strict_offline_block-এর
            // cloud মান টেনে local cache আপডেট -- এই ডিভাইসে fresh login হলে (আগে কখনো sync
            // হয়নি) এখানেই প্রথমবার সঠিক admin-সেট মান পাওয়া নিশ্চিত হয়।
            runCatching { repository.syncStrictOfflineBlockSettingFromCloud() }
                .onFailure { Log.e("SomadhanViewModel", "completeLoginAfterOtp: syncStrictOfflineBlockSettingFromCloud failed", it) }
            // [Somadhan Bug-Fix Step 1 — গ্রুপ ১, money-critical] এই account-এর জন্য realtime
            // broadcast subscription (balance/escrow/transactions/withdrawals ইত্যাদি ৭টা টেবিল)
            // re-scope করা -- আগে এই কলটা কোথাও ছিল না বলে app-process-এর প্রথম login-এই সাবস্ক্রাইবড
            // account-এ চিরকাল আটকে থাকত, পরবর্তী কোনো account-switch/re-login-এ না। idempotent
            // (নিজের doc-কমেন্ট দ্রষ্টব্য), তাই re-call নিরাপদ।
            // [Somadhan Bug-Fix — গ্রুপ ১, নতুন সাব-বাগ ১.১০] আগে এই কলটা এখানেই await হতো (suspend
            // call, blocking) -- Step 1-এর ফিক্স (১.৪) parallel করার পরও worst-case ~১০ সেকেন্ড
            // পর্যন্ত `onSuccess()` আটকে থাকত, real-device-এ OTP popup বন্ধ হওয়ার পর কোনো
            // loading UI না থাকায় ব্যবহারকারীর কাছে app আটকে গেছে মনে হতো (state ইতিমধ্যে কমিট
            // হয়ে গেলেও)। realtime resubscribe idempotent ও best-effort (live-update-এর জন্য,
            // সঠিকতার জন্য না), তাই এখন fire-and-forget করা হলো যাতে নিচের observeUserData/
            // onSuccess() আর এর জন্য অপেক্ষা না করে।
            viewModelScope.launch {
                runCatching { SupabaseRealtimeManager.startRealtimeListeners() }
                    .onFailure { Log.e("SomadhanViewModel", "completeLoginAfterOtp: startRealtimeListeners failed", it) }
            }
            // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.setCurrentUserId(...)
            // কল ছিল -- Supabase RLS auth.uid()-ভিত্তিক সার্ভার-সাইড scoping করে, তাই আলাদা করে
            // client-side scoping সেট করার দরকার নেই।
            observeUserData(freshUser.id)
            startContinuousLocationTracking()
            clearOtp(user.phone)
            onSuccess()
        }
    }

    // [ধাপ ১৪ ফলো-আপ — ✅ সরানো হয়েছে] quickLoginForDemo() এখানে ছিল (one-tap demo
    // user/solver login, কোনো real Supabase Auth session তৈরি করত না)। ব্যবহারকারীর
    // স্পষ্ট সিদ্ধান্তে (rule #4 exception) সম্পূর্ণ সরানো হয়েছে -- LoginScreen.kt-এর demo
    // বাটনগুলোও একইসাথে সরানো হয়েছে (এই ফাংশনের একমাত্র caller ছিল সেগুলো)।

    fun register(
        name: String,
        phone: String,
        email: String,
        pass: String,
        role: String,
        lat: Double,
        lon: Double,
        address: String,
        solverCategories: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || phone.isBlank() || email.isBlank() || pass.isBlank() || address.isBlank()) {
                onError("সকল তথ্য প্রদান করা আবশ্যক।")
                return@launch
            }
            if (role == "SOLVER" && (solverCategories.isEmpty() || solverCategories.size > 3)) {
                onError("সমাধানকারী হিসেবে সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করতে হবে।")
                return@launch
            }

            // [Offline Action Gating ধাপ ৫] Register-এর real network call (Supabase Auth sign-up +
            // complete_registration_profile RPC + cloud profile fetch, SomadhanRepository.registerUser
            // দ্রষ্টব্য -- এখানেই লক্ষ্য করা গেল যে `complete_registration_profile` (ধাপ ১-এর
            // ইনভেন্টরিতে আলাদা আইটেম হিসেবে তালিকাভুক্ত ছিল) আসলে registerUser()-এর ভেতরের একটা
            // sub-step, স্বতন্ত্র কোনো UI call-site না -- তাই এই একটা গার্ডই যথেষ্ট, আলাদা করে
            // guard বসানোর দরকার নেই)। কোনো local fallback নেই, তাই আগেই আটকানো হলো।
            if (!requireOnlineOrWarn()) {
                onError("ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না")
                return@launch
            }

            val result = repository.registerUser(
                name = name,
                phone = phone,
                email = email,
                password = pass,
                role = role,
                latitude = lat,
                longitude = lon,
                address = address,
                solverCategories = solverCategories
            )

            result.onSuccess { user ->
                _currentUser.value = user
                sharedPrefs.edit().putString("saved_user_id", user.id).apply()
                // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগের FirebaseSyncManager.setCurrentUserId(...)
                // কলটা সরানো হলো -- RLS-ভিত্তিক scoping-এ এটা দরকার নেই।
                observeUserData(user.id)
                startContinuousLocationTracking()
                onSuccess()
            }.onFailure {
                onError(it.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে।")
            }
        }
    }

    /**
     * Establishes an actual admin session. MUST be called right after AdminCredentials
     * phone+password verification succeeds (see LoginScreen's admin-phone branch) -- before
     * navigating to the Admin Panel.
     *
     * [ধাপ ১৪ ফলো-আপ] আগে একটা "demo admin" one-tap quick-login বাটনও এই ফাংশন সরাসরি কল করত
     * (কোনো password verify ছাড়াই)। ব্যবহারকারীর স্পষ্ট সিদ্ধান্তে সেই বাটন সম্পূর্ণ সরানো
     * হয়েছে -- এখন এই ফাংশনের একমাত্র caller হলো LoginScreen-এর real AdminCredentials
     * phone+password-verified পথ।
     *
     * Bug fix (v3_6): both admin login entry points in LoginScreen used to call
     * onAdminLoginSuccess() directly, which only *navigates* to Screen.AdminPanel. Nothing ever
     * set _currentUser to an ADMIN-role user or called FirebaseSyncManager.setCurrentUserId(...,
     * isAdmin = true). As a result:
     *   - FirebaseSyncManager.isCurrentUserAdmin stayed false, so pullUsers()/pullProblems()/
     *     pullBids()/pullTransactions() kept running their normal SCOPED branch (see
     *     ENGINEERING_NOTES.md §2/§7) instead of the unscoped "admin, full visibility" branch --
     *     this is why the users menu only showed 1-2 locally-cached users, and KYC / user-solver
     *     search / solver quota screens (which all read the same scoped local tables) had nothing.
     *   - Any code elsewhere gated on `currentUser?.role == "ADMIN"` also silently evaluated to
     *     false for the whole admin session.
     *
     * Bug fix (v3_7 follow-up): the first version of this function AWAITED a full
     * pullAllCloudDataToLocal() (every collection, unscoped) before calling onReady() -- on a
     * database with a non-trivial amount of data, or a slow connection, that full pull can take a
     * long time, so the admin login button looked "stuck"/unresponsive (never seemed to log in).
     * Now the session flags are set (fast, local-only) and onReady() fires immediately so the
     * Admin Panel opens right away; the full pull + the now-unscoped real-time listeners fill in
     * the actual data shortly after, in the background, same as how a normal login never blocks
     * navigation on a full sync either.
     */
    /**
     * ... (আগের সব doc comment অপরিবর্তিত)
     *
     * [SUPABASE-MIGRATED - ধাপ ৩৩.২ ফিক্স, ব্যবহারকারীর সরাসরি সিদ্ধান্তে] আগে এখানে flag করা
     * ছিল যে "ADMIN_SYSTEM" ব্যবহারকারীর কোনো real Supabase Auth session নেই (id একটা fixed
     * string, কোনো auth.users row না) — ফলে `is_admin(auth.uid())`-নির্ভর প্রতিটা RPC/RLS admin
     * সেশনে silently ব্যর্থ হতো (auth.uid() null থাকায়)।
     *
     * ফিক্স: এখন একটা real Supabase Auth account (phone+password, Supabase MCP দিয়ে DB-তে সরাসরি
     * বানানো ও `public.users.role='ADMIN'` সেট করে `is_admin()` দিয়ে verify করা হয়েছে) আছে।
     * নিচে সবার আগে [SupabaseAuthManager.signInWithPhonePassword] দিয়ে real session তৈরি করা
     * হয় (admin login form-এ যাচাই-করা ঠিক সেই phone+password দিয়েই — আলাদা কোনো password এখানে
     * hardcode করা হয়নি)। sign-in ব্যর্থ হলে (নেটওয়ার্ক ইত্যাদি) local admin UI session তখনও
     * block করা হয় না — non-fatal, শুধু log — কিন্তু সেক্ষেত্রে RLS-scoped raw table read/write
     * (SECURITY DEFINER RPC না এমন plain `.select()/.insert()/.update()`) আগের মতোই অসম্পূর্ণ
     * থাকবে।
     *
     * `adminPhone`/`rawPassword` caller (`LoginScreen.kt`) থেকে আসে — এগুলো ইতিমধ্যেই
     * `AdminCredentials.verifyAdminPassword()` দিয়ে verify হয়ে গেছে, এখানে শুধু Supabase Auth-এ
     * পাস করা হচ্ছে। phone local format-এ (যেমন "01XXXXXXXXX") থাকে — Supabase phone provider
     * E.164 চায় (`+880...`), তাই `OtpService.normalizeTarget()` (আগে থেকেই বিদ্যমান, demo OTP
     * flow-এর জন্য বানানো হেল্পার) দিয়ে কনভার্ট করা হয়।
     *
     * ⚠️ নোট (এই ফিক্সের সময় আবিষ্কৃত, স্কোপের বাইরে তাই এখানে হাত দেওয়া হয়নি): normal user/solver
     * registration/login flow-এ (`SomadhanRepository.kt`-এর `signUpWithPhonePassword`/
     * `signInWithPhonePassword` call-site, লাইন ~৮২১/৯৩০) `trimmedPhone` সরাসরি (কোনো E.164
     * normalize ছাড়া) Supabase-এ পাঠানো হয় — Supabase phone auth docs অনুযায়ী এটা likely reject
     * হবে (E.164 প্রয়োজন, উদাহরণ `+13334445555`)। অর্থাৎ **সব normal user-এর real phone+password
     * sign-up/sign-in সম্ভবত এখনো ভাঙা** (admin-এর এই নতুন ফিক্সের বাইরে) — এটা ধাপ ১৪-এর মূল
     * scope-এর একটা bug, পরের কোনো session-এ আলাদাভাবে ঠিক করা দরকার (সম্ভবত একই
     * `OtpService.normalizeTarget()` হেল্পার দিয়ে, caller-side এ wrap করে)।
     *
     * [Offline Action Gating ধাপ ৫ — নোট আপডেট] এই ফাইল আবার পড়ে (rule ১০) দেখা গেল উপরের
     * নোটটা এখন stale: `SomadhanRepository.loginWithPhonePassword`/`registerUser`-এ (লাইন
     * ~১০৫০/৯৩৭) `OtpService.normalizeTarget()` দিয়ে E.164 normalize **ইতিমধ্যেই যোগ করা
     * হয়েছে** ("[ফিক্স - ধাপ ৩৪ পরবর্তী]" কমেন্ট) — অর্থাৎ এই bug সম্ভবত অন্য কোনো (offline
     * gating-এর বাইরের) সেশনে আগেই ঠিক হয়ে গেছে। স্কোপের বাইরে বলে verify/টেস্ট করা হয়নি, শুধু
     * ডকুমেন্টেশন সিঙ্কে আনতে উল্লেখ করা হলো।
     *
     * [Offline Action Gating ধাপ ৫ — ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি] নিচের
     * `SupabaseAuthManager.signInWithPhonePassword` কলটা ইতিমধ্যেই try/catch দিয়ে gracefully
     * handle করা -- ব্যর্থ হলে (network না থাকা-সহ) শুধু একটা "সীমিত ভিউ" সতর্কবার্তা toast
     * দেখিয়ে local ADMIN_SYSTEM সেশন দিয়েই এগিয়ে যায় (ইচ্ছাকৃত graceful degradation, উপরের
     * ডক-কমেন্ট দ্রষ্টব্য)। এখানে `requireOnlineOrWarn()` বসালে rule ১ ভাঙত -- admin অফলাইনে
     * প্যানেলেই ঢুকতে পারত না, যেটা বর্তমান (ইচ্ছাকৃত, ইতিমধ্যে ডকুমেন্টেড) আচরণের চেয়ে খারাপ।
     */
    /**
     * [ADMIN_ROLE_PROFILE সেশন ২ — মাল্টি-এডমিন লগইন, উপরের সব ডক-কমেন্টের পর এটাই বর্তমান আচরণ]
     * সিঙ্গেল-ক্রেডেনশিয়াল গেট ([com.example.data.security.AdminCredentials]) আর লগইনে ব্যবহার হয় না।
     * এখন পাসওয়ার্ড যাচাই মানেই real Supabase Auth phone+password sign-in (প্রতিটা এডমিনের নিজস্ব
     * auth user, সেশন ১-এর `admin_accounts.auth_user_id`) — sign-in ব্যর্থ = লগইন ব্যর্থ (আগের "সীমিত
     * ভিউ"/degraded পথ আর নেই, কারণ সার্ভার-যাচাই ছাড়া কেউ এডমিন হতে পারে না)। তারপর `admin_session_start`
     * RPC (সার্ভারই জানে কে কল করছে; নিষ্ক্রিয় অ্যাকাউন্ট/এডমিন-নয় এমন ইউজার এখানেই আটকায়) → [AdminSession]
     * সেট → ~৩০সে heartbeat শুরু। ব্যর্থ হলে [onError]-এ বার্তা, আংশিক অবস্থা (sign-in হয়ে গেছে কিন্তু
     * সেশন-স্টার্ট ব্যর্থ) sign-out করে ক্লিন করা হয়।
     * ⚠️ অফলাইনে এডমিন লগইন আর সম্ভব না (পাসওয়ার্ড শুধু Supabase Auth-এ) — আগের লোকাল DataStore
     * fallback ইচ্ছাকৃতভাবে বাদ, নাহলে সেটাই মাল্টি-এডমিন গেট এড়ানোর পিছনের দরজা হতো।
     */
    fun loginAsAdmin(
        adminPhone: String,
        rawPassword: String,
        onReady: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            // পুরনো degraded-state রিসেট (এখন আর কখনো true হয় না, কিন্তু বার বিদ্যমান UI-তে আছে)
            _isDegradedAdminSession.value = false
            _degradedAdminSessionReason.value = null
            _adminForcedLogoutReason.value = null

            // ১) পাসওয়ার্ড যাচাই = real Supabase Auth sign-in
            val e164Phone = com.example.util.OtpService.normalizeTarget(adminPhone)
            val signIn = try {
                SupabaseAuthManager.signInWithPhonePassword(e164Phone, rawPassword)
            } catch (e: Exception) {
                Result.failure(e)
            }
            signIn.onFailure { e ->
                android.util.Log.w("SomadhanViewModel", "Admin Supabase Auth sign-in failed: ${e.message}")
                onError(adminSignInErrorMessage(e))
                return@launch
            }

            // ২) সার্ভার-সেশন খোলা — এডমিন-অ্যাকাউন্ট কিনা/সক্রিয় কিনা এখানেই যাচাই হয়
            val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
            val started = SupabaseSyncManager.adminSessionStart(device)
            val startedOk = started.getOrNull()
            val account = startedOk?.second?.let { AdminAccountInfo.fromJson(it) }
            if (startedOk == null || account == null) {
                val err = started.exceptionOrNull()
                android.util.Log.w("SomadhanViewModel", "admin_session_start failed: ${err?.message}")
                // আংশিক অবস্থা পরিষ্কার: sign-in হয়ে গেছে কিন্তু এডমিন-সেশন হয়নি → sign-out
                runCatching { SupabaseAuthManager.signOut() }
                onError(adminSessionStartErrorMessage(err))
                return@launch
            }
            AdminSession.set(startedOk.first, account)

            val adminUser = UserEntity(
                id = "ADMIN_SYSTEM",
                name = account.name.ifBlank { "Support Manager" },
                phone = account.phone.ifBlank { adminPhone },
                email = account.email ?: "admin@somadhan.com",
                password = "",
                role = "ADMIN",
                latitude = 23.8103,
                longitude = 90.4125,
                address = "ঢাকা, বাংলাদেশ"
            )
            _currentUser.value = adminUser
            // [Somadhan Bug-Fix Step 1 / গ্রুপ ১.১০ — অপরিবর্তিত] realtime রি-স্কোপ fire-and-forget,
            // onReady() সাথে সাথে — লগইন বাটন আটকে থাকে না।
            viewModelScope.launch {
                runCatching { SupabaseRealtimeManager.startRealtimeListeners() }
                    .onFailure { Log.e("SomadhanViewModel", "loginAsAdmin: startRealtimeListeners failed", it) }
            }
            startAdminHeartbeat()
            onReady()
            // [ধাপ ৩৩.২ — অপরিবর্তিত] real সেশন এইমাত্র হয়েছে → RLS-এর অধীনে দৃশ্যমান ডেটার ফ্রেশ bulk-pull।
            try {
                SupabaseRealtimeManager.pullBulkDataFromSupabase()
            } catch (e: Exception) {
                // Non-fatal -- background sync/real-time listeners catch up.
            }
        }
    }

    private fun adminSignInErrorMessage(e: Throwable): String {
        val m = e.message.orEmpty().lowercase()
        return when {
            "invalid" in m || "credentials" in m -> "ভুল অ্যাডমিন পাসওয়ার্ড"
            "unable to resolve" in m || "timeout" in m || "timed out" in m || "network" in m ||
                "connect" in m || "host" in m ->
                "ইন্টারনেট সংযোগ নেই — অ্যাডমিন লগইনের জন্য ইন্টারনেট প্রয়োজন।"
            else -> "অ্যাডমিন লগইন ব্যর্থ হয়েছে: ${e.message ?: "অজানা কারণ"}"
        }
    }

    private fun adminSessionStartErrorMessage(e: Throwable?): String {
        val m = e?.message.orEmpty()
        return when {
            "ACCOUNT_INACTIVE" in m -> "আপনার অ্যাডমিন অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে। সুপার অ্যাডমিনের সাথে যোগাযোগ করুন।"
            "NOT_AN_ADMIN" in m -> "এই ফোন নম্বরটি কোনো অ্যাডমিন অ্যাকাউন্টের সাথে যুক্ত নয়।"
            else -> "অ্যাডমিন সেশন শুরু করা যায়নি: ${if (m.isBlank()) "অজানা কারণ" else m}"
        }
    }

    /**
     * প্রতি ~৩০ সেকেন্ডে `admin_heartbeat` — (ক) অনলাইন-স্ট্যাটাসের last_seen আপডেট, (খ) সার্ভারের সর্বশেষ
     * রোল/পারমিশন/flag [AdminSession]-এ বসানো (রিলগইন ছাড়াই), (গ) নিষ্ক্রিয়/এডমিন-নয় ধরা পড়লে জোর-লগআউট
     * সিগন্যাল। নেটওয়ার্ক-ব্যর্থতা নীরবে উপেক্ষা (আগের স্ন্যাপশট থাকে; সার্ভার-সাইড is_admin/RLS আসল গেট)।
     */
    private fun startAdminHeartbeat() {
        adminHeartbeatJob?.cancel()
        adminHeartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                val sid = AdminSession.current?.sessionId
                val res = SupabaseSyncManager.adminHeartbeat(sid)
                res.onSuccess { obj ->
                    AdminAccountInfo.fromJson(obj)?.let { AdminSession.update(it) }
                }
                res.onFailure { e ->
                    val m = e.message.orEmpty()
                    if ("ACCOUNT_INACTIVE" in m) {
                        _adminForcedLogoutReason.value = "আপনার অ্যাডমিন অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে।"
                        return@launch
                    }
                    if ("NOT_AN_ADMIN" in m) {
                        _adminForcedLogoutReason.value = "আপনার অ্যাডমিন অ্যাক্সেস আর নেই।"
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * [ADMIN_ROLE_PROFILE সেশন ২] লগইন স্ক্রিনের জন্য: এই ফোনটা কি কোনো এডমিন অ্যাকাউন্টের? (anon RPC,
     * ব্যর্থ হলে false — অর্থাৎ সাধারণ ইউজার-পথ; এডমিন হলে সেখানে পাসওয়ার্ড মিলবে না, নিরাপদ ডিফল্ট।)
     */
    suspend fun isAdminPhone(phone: String): Boolean =
        SupabaseSyncManager.adminLoginCheck(phone.trim()).getOrDefault(false)

    // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১c] Admin নিজে persistent degraded-session bar-এর "✕"
    // বাটনে ট্যাপ করলে এটা কল হয় -- শুধু UI-তে bar-টা লুকানো হয় (dismiss), আসল সেশন এখনো degraded-ই
    // থাকে (পরের real login/logout না হওয়া পর্যন্ত), তাই এটা কোনো auth/RLS state বদলায় না।
    fun dismissDegradedAdminSessionWarning() {
        _isDegradedAdminSession.value = false
    }

    /**
     * ধাপ ১৪: `SupabaseAuthManager.signOut()` যোগ করা হয়েছে (viewModelScope.launch-এ, non-blocking
     * — sign-out ব্যর্থ হলেও local logout যেন আটকে না থাকে)। demo/admin session-এর জন্য কোনো real
     * Supabase session না থাকায় এই কল সেক্ষেত্রে no-op-এর মতো আচরণ করবে (নিরাপদ)।
     *
     * [Offline Action Gating ধাপ ৫ — ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি] Local session
     * clear (SharedPrefs/`_currentUser`/ইত্যাদি) synchronous ও unconditional; নিচের
     * `SupabaseAuthManager.signOut()` একটা আলাদা fire-and-forget coroutine, তার সফল/ব্যর্থ
     * হওয়া local logout-কে প্রভাবিত করে না। এই ফাংশনের শুরুতে `requireOnlineOrWarn()` গার্ড
     * বসালে rule ১ ভাঙত -- ইউজার অফলাইনে থাকলে **লগআউটই করতে পারত না**, যেটা একটা নতুন,
     * গুরুতর রিগ্রেশন হতো। তাই Logout ইচ্ছাকৃতভাবে গার্ডবিহীন রাখা হলো।
     */
    fun logout() {
        userDataJobs.forEach { it.cancel() }
        userDataJobs.clear()
        stopContinuousLocationTracking()
        sharedPrefs.edit().remove("saved_user_id").apply()
        // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseSyncManager.setCurrentUserId(null) কল
        // ছিল -- নিচের SupabaseAuthManager.signOut() real session শেষ করে দেয়, তারপর কোনো active
        // session না থাকায় RLS আপনাআপনি কিছুই visible করবে না, তাই আলাদা করে scoping ক্লিয়ার
        // করার দরকার নেই।
        // [Balance-reset race ফিক্স] এই fire-and-forget Job-টা এখন SupabaseAuthManager-এ
        // রেজিস্টার করা হচ্ছে, যাতে পরের কোনো login (অন্য account-এও) নতুন session তৈরির আগে এই
        // signOut() সত্যিই শেষ হয়েছে কিনা নিশ্চিত করে নিতে পারে -- নাহলে এই দেরি-হওয়া signOut()
        // পরে ফায়ার হয়ে নতুন account-এর session মুছে দিতে পারত (মূল কারণ: acceptBid()-এর মতো
        // money-critical dual-write গার্ড `currentUserId() == problem.userId` চেক করে, session
        // race করলে সেটা silently mismatch হয়ে পুরো cloud sync ব্লক skip হয়ে যেত)।
        // [ADMIN_ROLE_PROFILE সেশন ২] এডমিন হলে সার্ভার-সেশন বন্ধ (অনলাইন-স্ট্যাটাস অফলাইন হয়) — অবশ্যই
        // signOut()-এর আগে, কারণ RPC-র জন্য auth সেশন লাগে; অফলাইনে ৩সে টাইমআউটে লগআউট আটকে থাকে না।
        val adminSessionToEnd = AdminSession.current
        adminHeartbeatJob?.cancel()
        adminHeartbeatJob = null
        AdminSession.clear()
        val signOutJob = viewModelScope.launch {
            if (adminSessionToEnd != null) {
                runCatching {
                    withTimeoutOrNull(3000L) { SupabaseSyncManager.adminSessionEnd(adminSessionToEnd.sessionId) }
                }
            }
            SupabaseAuthManager.signOut()
            // [Somadhan Bug-Fix Step 1 — গ্রুপ ১, money-critical] আগে এখানে কোনো
            // stopRealtimeListeners() কল ছিল না, ফলে পুরনো account-এর user:<uuid> broadcast
            // subscription চালু থেকে যেত পরের login পর্যন্ত (আর সেই পরের login-ও রি-স্কোপ করত না)।
            runCatching { SupabaseRealtimeManager.stopRealtimeListeners() }
                .onFailure { Log.e("SomadhanViewModel", "logout: stopRealtimeListeners failed", it) }
        }
        SupabaseAuthManager.trackPendingSignOut(signOutJob)
        _currentUser.value = null
        _selectedProblem.value = null
        _problemBids.value = emptyList()
        _problemMessages.value = emptyList()
        _dismissedReminderIds.value = emptySet()
        _userEscrows.value = emptyList()
        _userTransactions.value = emptyList()
        _userGatewayPayments.value = emptyList()
        _solverWithdrawals.value = emptyList()
        // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১c] Logout-এ degraded-admin-session bar-ও ক্লিয়ার করা
        // হচ্ছে, নাহলে পরের (অ-admin) লগইনেও পুরনো state persist করত।
        _isDegradedAdminSession.value = false
        _degradedAdminSessionReason.value = null
        // [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] রোল-তালিকা ক্লিয়ার — পরের লগইনে (অন্য এডমিন হলেও) পুরনো
        // তালিকা ভেসে না ওঠে, cold-load skeleton আবার ঠিকভাবে চলে।
        adminRolesLoadJob?.cancel()
        adminRolesLoadJob = null
        _adminRoles.value = emptyList()
        _adminRolesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
        // [ADMIN_ROLE_PROFILE সেশন ৪] অ্যাকাউন্ট-তালিকাও ক্লিয়ার — অন্য এডমিন (বা অ-সুপার) লগইন করলে
        // আগের সেশনের অ্যাকাউন্ট/ফোন-নম্বরের তালিকা মেমরিতে ভেসে না থাকে।
        adminAccountsLoadJob?.cancel()
        adminAccountsLoadJob = null
        _adminAccounts.value = emptyList()
        _adminAccountsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
    }

    fun dismissSolverRatingReminder(context: Context, problemId: String) {
        val solverId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            SolverReminderPrefs.markDismissed(context, solverId, problemId)
            _dismissedReminderIds.value = _dismissedReminderIds.value + problemId
        }
    }

    private fun observeUserData(userId: String) {
        userDataJobs.forEach { it.cancel() }
        userDataJobs.clear()

        // [ROLE_SEPARATION ধাপ ৬] role-switch (switchRoleToSolver/User)-এর পরও observeUserData
        // আবার কল হয় (userId অপরিবর্তিত থাকলেও), আর সেই কলের ঠিক আগেই _currentUser.value
        // আপডেট হয় -- তাই এখানে পড়লে সবসময় বর্তমান active role পাওয়া যাবে।
        val currentRole = _currentUser.value?.role ?: ""

        viewModelScope.launch {
            _dismissedReminderIds.value = SolverReminderPrefs.getDismissedJobIds(getApplication(), userId)
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getUserByIdFlow(userId).collect { user ->
                if (user != null) {
                    _currentUser.value = user
                }
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getNotificationsForUser(userId, currentRole).collect {
                _notifications.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getUnreadNotificationCount(userId, currentRole).collect {
                _unreadNotificationCount.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getUnreadProblemNotificationCount(userId, currentRole).collect {
                _unreadProblemNotificationCount.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getAllUserMessages(userId).collect {
                _userConversations.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getUnreadMessagesCount(userId).collect {
                _unreadMessagesCount.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getRatingsGivenByUser(userId).collect {
                _userGivenReviews.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getRatingsReceivedByUser(userId).collect {
                _userReceivedReviews.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getRatingsReceivedBySolver(userId).collect {
                _solverReceivedReviews.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getRatingsGivenBySolver(userId).collect {
                _solverGivenReviews.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getCompletedProblemsBySolver(userId).collect {
                _solverCompletedJobs.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getBidsBySolver(userId).collect {
                _solverBids.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getWithdrawalsForSolver(userId).collect {
                _solverWithdrawals.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getEscrowsForUser(userId).collect {
                _userEscrows.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getTransactionsForUser(userId).collect {
                _userTransactions.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.getGatewayPaymentsForUser(userId).collect {
                _userGatewayPayments.value = it
            }
        }.also { userDataJobs.add(it) }

        viewModelScope.launch {
            repository.reconcileEscrowStates()
        }.also { userDataJobs.add(it) }
    }

    fun reconcileEscrows() {
        viewModelScope.launch {
            repository.reconcileEscrowStates()
        }
    }

    // ---------------- ROLE SWITCHING ----------------

    fun requestSwitchRole(
        onShowCategoryPicker: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        if (user.role == "SOLVER") {
            // Currently Solver -> Switch to User directly
            switchRoleToUser(onSuccess = onSuccess, onError = onError)
        } else {
            // Currently User -> Check if solver setup was already completed once
            viewModelScope.launch {
                val hasExisting = repository.hasExistingSolverProfile(user)
                if (hasExisting) {
                    // Already configured categories in the past -> switch directly without asking!
                    switchRoleToSolver(
                        newCategories = emptyList(),
                        onSuccess = onSuccess,
                        onError = {
                            showToast(it)
                            onError(it)
                        }
                    )
                } else {
                    // Very first time switching to Solver -> prompt category selection
                    onShowCategoryPicker()
                }
            }
        }
    }

    fun switchRoleToSolver(newCategories: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- switchRoleInPlace() local role আগে flip করে, cloud switch_role RPC best-effort।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রোল পরিবর্তন করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া রোল পরিবর্তন করা যাবে না")
                return@launch
            }
            val hasExistingSolver = repository.hasExistingSolverProfile(user)
            if (!hasExistingSolver && (newCategories.isEmpty() || newCategories.size > 3)) {
                onError("Solver হতে সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করতে হবে।")
                return@launch
            }
            if (newCategories.isNotEmpty() && newCategories.size > 3) {
                onError("সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করতে পারবেন।")
                return@launch
            }

            // [ROLE-UID-SYNC-FIX ধাপ ৪] পুরনো repository.switchRole() (যেটা নতুন SOLVER_xxx/USER_xxx
            // dual row বানাত, id বদলে ফেলত) এখন থেকে আর কল হয় না -- switchRoleInPlace() ব্যবহার হচ্ছে,
            // যেটা root row-কেই আপডেট করে (id অপরিবর্তিত থাকে)। পুরনো switchRole() ফাংশনটা
            // repository-তে dead code হিসেবে থেকে যাচ্ছে (ইচ্ছাকৃতভাবে ডিলিট করা হয়নি, rule #৩)।
            val updated = repository.switchRoleInPlace(user, "SOLVER", newCategories)
            _currentUser.value = updated
            sharedPrefs.edit().putString("saved_user_id", updated.id).apply()
            // [SUPABASE-MIGRATED - ধাপ ৩৩.২] FirebaseSyncManager.setCurrentUserId(...) সরানো হলো --
            // RLS scoping-এ দরকার নেই, একই auth.uid() রোল পরিবর্তনের পরেও অক্ষত থাকে।
            observeUserData(updated.id)
            // [Somadhan Bug-Fix Step 1 — গ্রুপ ১, money-critical] user id অপরিবর্তিত থাকলেও role
            // বদলে যাওয়া মানে balance/escrow দেখানোর প্রসঙ্গ বদলে যাওয়া -- re-scope করা হলো যাতে
            // পুরনো role-এ subscribe থাকা কোনো stale channel না থাকে।
            // [Somadhan Bug-Fix — গ্রুপ ১, নতুন সাব-বাগ ১.১০] completeLoginAfterOtp()/loginAsAdmin()-এর
            // একই কারণে fire-and-forget করা হলো -- আগে এই suspend কল await হতো বলে worst-case ~১০
            // সেকেন্ড `onSuccess()` (ফলে ProfileScreen-এর isSwitchingRole spinner) আটকে থাকত, যদিও
            // local role/DB এতক্ষণে already committed (real-device-এ "background e role switch হয়ে
            // গেছে, শুধু loading আটকে আছে" হিসেবে ধরা পড়েছে, ২০২৬-০৯-২৪)।
            viewModelScope.launch {
                runCatching { SupabaseRealtimeManager.startRealtimeListeners() }
                    .onFailure { Log.e("SomadhanViewModel", "switchRoleToSolver: startRealtimeListeners failed", it) }
            }
            showToast("আপনার রোল সফলভাবে 'সমাধানকারী' তে পরিবর্তিত হয়েছে।")
            onSuccess()
        }
    }

    fun switchRoleToUser(onError: (String) -> Unit = {}, onSuccess: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- switchRoleToSolver()-এর মতোই। নতুন onError প্যারামিটার onSuccess-এর আগে (trailing-lambda কলার LoginScreen ভাঙবে না)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রোল পরিবর্তন করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া রোল পরিবর্তন করা যাবে না")
                return@launch
            }
            // [ROLE-UID-SYNC-FIX ধাপ ৪] switchRoleToSolver()-এর মতোই কারণে switchRoleInPlace()
            // ব্যবহার করা হচ্ছে -- পুরনো switchRole() dead code হিসেবে repository-তে থেকে যাচ্ছে।
            val updated = repository.switchRoleInPlace(user, "USER")
            _currentUser.value = updated
            sharedPrefs.edit().putString("saved_user_id", updated.id).apply()
            // [SUPABASE-MIGRATED - ধাপ ৩৩.২] FirebaseSyncManager.setCurrentUserId(...) সরানো হলো --
            // RLS scoping-এ দরকার নেই, একই auth.uid() রোল পরিবর্তনের পরেও অক্ষত থাকে।
            observeUserData(updated.id)
            // [Somadhan Bug-Fix Step 1 — গ্রুপ ১, money-critical] switchRoleToSolver()-এর মতোই
            // কারণে re-scope করা হলো।
            // [Somadhan Bug-Fix — গ্রুপ ১, নতুন সাব-বাগ ১.১০] switchRoleToSolver()-এর মতোই কারণে
            // fire-and-forget করা হলো।
            viewModelScope.launch {
                runCatching { SupabaseRealtimeManager.startRealtimeListeners() }
                    .onFailure { Log.e("SomadhanViewModel", "switchRoleToUser: startRealtimeListeners failed", it) }
            }
            showToast("আপনার রোল সফলভাবে 'ইউজার' এ পরিবর্তিত হয়েছে।")
            onSuccess()
        }
    }

    fun updateSolverSkills(newCategories: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUser.value ?: return
        if (newCategories.isEmpty() || newCategories.size > 3) {
            onError("সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করতে হবে।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩) -- local-first,
            // টাকা জড়িত না, কিন্তু ইউজার চেয়েছেন প্রোফাইল-এডিটেও স্পষ্ট গার্ড থাকুক।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ক্যাটাগরি পরিবর্তন করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া ক্যাটাগরি পরিবর্তন করা যাবে না")
                return@launch
            }
            val updated = user.copy(
                solverCategories = newCategories.joinToString(","),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            showToast("ক্যাটাগরি সফলভাবে আপডেট করা হয়েছে।")
            onSuccess()
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩) -- callback নেই,
            // শুধু return@launch (requireOnlineOrWarn() নিজেই toast দেখায়)। কল-সাইট:
            // ReputationDetailScreen.kt (profile edit-এর মতোই ব্যবহার)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া প্রোফাইল আপডেট করা যাবে না")) return@launch
            repository.updateUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
        }
    }

    fun updatePhoneNumber(newPhone: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া মোবাইল নম্বর পরিবর্তন করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া মোবাইল নম্বর পরিবর্তন করা যাবে না")
                return@launch
            }
            val existing = repository.getUserByPhoneOrEmail(newPhone.trim(), "")
            if (existing != null && existing.id != user.id) {
                onError("এই নম্বরে ইতোমধ্যে অন্য একটি অ্যাকাউন্ট আছে।")
                return@launch
            }
            val updated = user.copy(phone = newPhone.trim(), updatedAt = System.currentTimeMillis())
            // [SUPABASE-MIGRATED - ধাপ ১৩] আগে এখানে repository.updateUser()-এর পরও আলাদাভাবে
            // FirebaseSyncManager.syncUser(updated) কল হতো -- repository.updateUser() নিজেই এটা
            // (এবং প্রযোজ্য হলে SupabaseSyncManager.updateOwnProfile()) কল করে, তাই ডুপ্লিকেট কলটা
            // সরিয়ে ফেলা হলো।
            repository.updateUser(updated)
            _currentUser.value = updated
            showToast("মোবাইল নম্বর সফলভাবে পরিবর্তন হয়েছে ✅")
            onSuccess()
        }
    }

    fun updateProfile(
        name: String,
        email: String,
        address: String,
        lat: Double? = null,
        lon: Double? = null,
        profileImage: String? = null,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩) -- এই
            // ফাংশনে onError নেই, শুধু onSuccess; তাই ব্যর্থ হলে শুধু toast + return@launch।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া প্রোফাইল তথ্য আপডেট করা যাবে না")) return@launch
            val updated = user.copy(
                name = name.trim(),
                email = email.trim(),
                address = address.trim(),
                latitude = lat ?: user.latitude,
                longitude = lon ?: user.longitude,
                profileImageUri = profileImage ?: user.profileImageUri,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            showToast("প্রোফাইল তথ্য আপডেট সম্পন্ন হয়েছে।")
            onSuccess()
        }
    }

    fun toggleFavoriteSolver(solverId: String, onResult: (Boolean) -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া পছন্দের তালিকা পরিবর্তন করা যাবে না")) {
                onResult(false)
                return@launch
            }
            val isNowFav = repository.toggleFavoriteSolver(user.id, solverId)
            val freshUser = repository.getUserById(user.id)
            if (freshUser != null) {
                _currentUser.value = freshUser
            }
            if (isNowFav) {
                showToast("পছন্দের তালিকায় যুক্ত হয়েছে ❤️")
            } else {
                showToast("পছন্দের তালিকা থেকে বাদ দেওয়া হয়েছে")
            }
            onResult(isNowFav)
        }
    }

    fun addFavoriteSolver(solverId: String, onResult: (Boolean) -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া পছন্দের তালিকায় যোগ করা যাবে না")) {
                onResult(false)
                return@launch
            }
            val added = repository.addFavoriteSolver(user.id, solverId)
            val freshUser = repository.getUserById(user.id)
            if (freshUser != null) {
                _currentUser.value = freshUser
            }
            if (added) {
                showToast("সমাধানকারীকে পছন্দের তালিকায় সফলভাবে যোগ করা হয়েছে ❤️")
            } else {
                showToast("সমাধানকারী ইতোমধ্যেই পছন্দের তালিকায় যুক্ত আছেন।")
            }
            onResult(added)
        }
    }

    suspend fun canFavoriteSolver(solverId: String): Boolean {
        val userId = _currentUser.value?.id ?: return false
        return repository.canFavoriteSolver(userId, solverId)
    }

    suspend fun getCompletedProblemsCountBetween(solverId: String): Int {
        val userId = _currentUser.value?.id ?: return 0
        return repository.getCompletedProblemsBetween(userId, solverId).size
    }

    suspend fun getFavoriteSolvers(): List<UserEntity> {
        val userId = _currentUser.value?.id ?: return emptyList()
        return repository.getFavoriteSolvers(userId)
    }

    // Profile photo upload loading state
    private val _isUploadingProfileImage = MutableStateFlow(false)
    val isUploadingProfileImage: StateFlow<Boolean> = _isUploadingProfileImage.asStateFlow()

    // [ধাপ ৩৩.৫] নাম rename করা হলো (আগে "ToFirebase" ছিল, স্টেল -- বডি ইতিমধ্যেই
    // ImageStorageUtil দিয়ে Supabase Storage ব্যবহার করে, কোনো Firebase কল নেই)। এই ফাংশনের
    // কোনো caller নেই (dead/unused wrapper), কিন্তু নিরাপদে রিনেম করা হলো যাতে ভবিষ্যতে কেউ
    // এটা ব্যবহার করলে বিভ্রান্ত না হয়।
    suspend fun uploadProfileImage(uri: Uri, userId: String): Result<String> {
        return ImageStorageUtil.uploadProfilePhoto(getApplication(), userId, uri)
    }

    fun uploadAndSaveProfileImage(
        uri: Uri,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            val err = "অনুগ্রহ করে প্রথমে লগইন করুন।"
            showToast(err)
            onError(err)
            return
        }

        _isUploadingProfileImage.value = true
        viewModelScope.launch {
            val result = ImageStorageUtil.uploadProfilePhoto(getApplication(), user.id, uri)
            _isUploadingProfileImage.value = false
            result.onSuccess { downloadUrl ->
                updateProfileImage(downloadUrl)
                onSuccess(downloadUrl)
            }.onFailure { error ->
                val msg = "ছবি আপলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun updateProfileImage(imageUri: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B (ইউজার-কনফার্মড, প্রশ্ন ৩) -- callback
            // নেই, শুধু return@launch। বেশিরভাগ কল-সাইট uploadAndSaveProfileImage()-এর সফল আপলোডের
            // পরে আসে (তখন নেটওয়ার্ক এমনিতেই ছিল), কিন্তু ProfileScreen.kt-এ ছবি মুছার (imageUri="")
            // সরাসরি কল-ও আছে -- তাই এখানেও গার্ড প্রয়োজন।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া প্রোফাইল ছবি আপডেট করা যাবে না")) return@launch
            val updated = user.copy(
                profileImageUri = imageUri,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            showToast("প্রোফাইল ছবি সফলভাবে আপডেট করা হয়েছে!")
        }
    }

    // ---------------- PROBLEM CREATION & DETAILS ----------------

    fun createProblem(
        title: String,
        description: String,
        category: CategoryEntity,
        latitude: Double,
        longitude: Double,
        address: String,
        minBudget: Double,
        maxBudget: Double,
        urgency: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে প্রথমে লগইন করুন।")
            return
        }
        if (user.isBannedUser) {
            onError("আপনার অ্যাকাউন্টটি ব্যান করা হয়েছে।")
            return
        }
        if (user.isRestrictedUser) {
            onError("আপনার অ্যাকাউন্টটি সাময়িকভাবে রেস্ট্রিক্ট (সীমিত) করা হয়েছে। আপনি নতুন কোনো সমস্যা পোস্ট করতে পারবেন না।")
            return
        }
        if (title.isBlank() || description.isBlank() || address.isBlank()) {
            onError("সকল ফিল্ড পূরণ করা বাধ্যতামূলক।")
            return
        }
        if (minBudget <= 0 || maxBudget < minBudget) {
            onError("সঠিক বাজেট রেঞ্জ প্রদান করুন। (সর্বোচ্চ বাজেট সর্বনিম্ন বাজেটের সমান বা বেশি হতে হবে)")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না, কিন্তু মাস্টার
            // প্রম্পটের Goal সেকশনে "post" স্পষ্টভাবে উদাহরণ হিসেবে উল্লেখ আছে। repository.createProblem()
            // local-first (problems insert সরাসরি Room-এ, Supabase dual-write fire-and-forget) --
            // guard ছাড়া অফলাইনেও লোকালি পোস্ট হয়ে যেত কিন্তু ইউজার কখনো জানতে পারতেন না এটা
            // cloud-এ যায়নি (Outbox-কভার্ড না)। onError আগে থেকেই প্যারামিটার হিসেবে ছিল কিন্তু
            // ব্যবহার হতো না (PostProblemScreen.kt-এ isSubmitting রিসেট করে) -- এখানে প্রথমবার কাজে লাগানো হলো।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সমস্যা পোস্ট করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া সমস্যা পোস্ট করা যাবে না")
                return@launch
            }
            repository.createProblem(
                user = user,
                title = title,
                description = description,
                category = category,
                latitude = latitude,
                longitude = longitude,
                address = address,
                minBudget = minBudget,
                maxBudget = maxBudget,
                urgency = urgency
            )
            showToast("সমস্যা সফলভাবে পোস্ট করা হয়েছে!")
            onSuccess()
        }
    }

    fun createInstantJob(
        title: String,
        description: String,
        category: CategoryEntity,
        latitude: Double,
        longitude: Double,
        address: String,
        minBudget: Double,
        maxBudget: Double,
        onSuccess: (ProblemEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে প্রথমে লগইন করুন।")
            return
        }
        if (user.isBannedUser) {
            onError("আপনার অ্যাকাউন্টটি ব্যান করা হয়েছে।")
            return
        }
        if (user.isRestrictedUser) {
            onError("আপনার অ্যাকাউন্টটি সাময়িকভাবে রেস্ট্রিক্ট (সীমিত) করা হয়েছে।")
            return
        }
        if (title.isBlank() || description.isBlank() || address.isBlank()) {
            onError("সকল ফিল্ড পূরণ করা বাধ্যতামূলক।")
            return
        }
        if (minBudget <= 0 || maxBudget < minBudget) {
            onError("সঠিক বাজেট রেঞ্জ প্রদান করুন।")
            return
        }
        if (!category.isPhysical) {
            onError("জরুরি জব শুধুমাত্র ফিজিক্যাল ক্যাটাগরির জন্য প্রযোজ্য।")
            return
        }
        if (!category.instantJobEnabled) {
            onError("এই ক্যাটাগরিতে জরুরি সার্ভিস বন্ধ আছে।")
            return
        }

        viewModelScope.launch {
            try {
                // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না জব তৈরির মুহূর্তে
                // (money শুধু bid accept-এ, যেটা ধাপ ৭-এ acceptInstantJobBid() দিয়ে ইতিমধ্যে গার্ড হয়েছে)।
                // repository.createInstantJob() local-first, আর তার ভেতরেই সফল Supabase insert-এর
                // পরে broadcast_instant_job RPC কল হয় (কোনো আলাদা entry point নেই) -- তাই এই একটা
                // গার্ডই দুটোই কভার করে।
                if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া জরুরি কাজ তৈরি করা যাবে না")) {
                    onError("ইন্টারনেট সংযোগ ছাড়া জরুরি কাজ তৈরি করা যাবে না")
                    return@launch
                }
                val isFeatureEnabled = repository.getPlatformSetting("instant_job_feature_enabled")?.toBooleanStrictOrNull() ?: true
                if (!isFeatureEnabled) {
                    onError("বর্তমানে প্ল্যাটফর্মে জরুরি সেবা সাময়িকভাবে বন্ধ রয়েছে।")
                    return@launch
                }
                val created = repository.createInstantJob(
                    user = user,
                    title = title,
                    description = description,
                    category = category,
                    lat = latitude,
                    lng = longitude,
                    address = address,
                    minBudget = minBudget,
                    maxBudget = maxBudget
                )
                val timeoutSeconds = repository.getPlatformSetting("instant_job_broadcast_timeout_seconds")?.toLongOrNull() ?: 300L
                if (com.example.BuildConfig.DEBUG) {
                    Log.d("InstantJobDebug", "createInstantJob created: id=${created.id}, timeoutSeconds=$timeoutSeconds, timestamp=${System.currentTimeMillis()}")
                }
                com.example.worker.InstantJobExpiryWorker.scheduleExactExpiryAlarm(getApplication(), created.id, timeoutSeconds)
                com.example.worker.InstantJobExpiryWorker.schedulePeriodic(getApplication())
                showToast("জরুরি কাজ সফলভাবে পাঠানো হয়েছে! ⚡")
                onSuccess(created)
            } catch (e: Exception) {
                if (com.example.BuildConfig.DEBUG) {
                    Log.e("InstantJobDebug", "createInstantJob failed: ${e.message}", e)
                }
                val msg = e.message ?: "জরুরি কাজ তৈরি করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ৮] ইচ্ছাকৃতভাবে গার্ড করা হয়নি -- এই ফাংশন কোনো UI বাটন থেকে
    // কল হয় না, বরং UserJobBroadcastingScreen-এর countdown timer (LaunchedEffect) ০-তে পৌঁছালে
    // স্বয়ংক্রিয়ভাবে ট্রিগার হয় (আর একই repository.checkAndExpireInstantJobs()
    // ScheduledNotificationWorker/InstantJobAlarmReceiver/InstantJobExpiryWorker থেকেও
    // ব্যাকগ্রাউন্ডে কল হয়) -- তাই এটা cron/background-triggered, "button/onClick যেটা সরাসরি
    // network hit করে" (মাস্টার প্রম্পটের Group B সংজ্ঞা) না। ইনভেন্টরির "cron-triggered, যাচাই
    // দরকার" নোট এখানে কনফার্ম হলো। repository.expireBroadcastingInstantJob() RPC-ও best-effort,
    // guard বসালে local expiry cleanup-ও আটকে যেত (rule ১ ভঙ্গ করত)।
    fun triggerInstantJobExpiryCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val expiredCount = repository.checkAndExpireInstantJobs()
                if (com.example.BuildConfig.DEBUG) {
                    Log.d("InstantJobDebug", "triggerInstantJobExpiryCheck manual triggered. Expired count: $expiredCount, timestamp=${System.currentTimeMillis()}")
                }
            } catch (e: Exception) {
                if (com.example.BuildConfig.DEBUG) {
                    Log.e("InstantJobDebug", "triggerInstantJobExpiryCheck error: ${e.message}", e)
                }
            }
        }
    }

    fun updateInstantJobToggle(enabled: Boolean) {
        val user = _currentUser.value ?: return
        val updated = user.copy(
            instantJobNotificationsEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        _currentUser.value = updated
        viewModelScope.launch {
            try {
                repository.updateInstantJobToggle(user.id, enabled)
                if (enabled) {
                    showToast("জরুরি জব নোটিফিকেশন চালু করা হয়েছে ⚡")
                } else {
                    showToast("জরুরি জব নোটিফিকেশন বন্ধ করা হয়েছে")
                }
            } catch (e: Exception) {
                Log.w("SomadhanVM", "Failed to update instant job toggle: ${e.message}")
            }
        }
    }

    fun getProblemByIdFlow(problemId: String): Flow<ProblemEntity?> {
        return repository.getProblemByIdFlow(problemId)
    }

    // [Offline Action Gating ধাপ ৭] 🔴 money-critical (rule ২) -- ইনভেন্টরিতে (ধাপ ১) এই
    // ফাংশনটা আলাদা আইটেম হিসেবে তালিকাভুক্ত ছিল না, কিন্তু কোড পড়ে দেখা গেল এটা ভেতরে
    // repository.acceptBid()-কেই কল করে (নিচে) -- অর্থাৎ ঠিক একই local wallet-deduction +
    // escrow-open money-path (acceptBid()-এর মতোই) যেটা rule ২-এ "bid accept" নামে স্পষ্টভাবে
    // উল্লেখ করা আছে, শুধু ভিন্ন UI entry point (instant-job flow) দিয়ে। তাই acceptBid()-এর
    // মতোই এখানেও গার্ড আবশ্যক -- inventory-এর এই ফাঁকটা এই ধাপেই বন্ধ করা হলো।
    fun acceptInstantJobBid(
        problem: ProblemEntity,
        bid: BidEntity,
        userLiveLat: Double? = null,
        userLiveLng: Double? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিড গ্রহণ করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিড গ্রহণ করা যাবে না")
                return@launch
            }
            try {
                com.example.worker.InstantJobExpiryWorker.cancelExpiryAlarm(getApplication(), problem.id)
                repository.acceptInstantJobBid(problem, bid, userLiveLat, userLiveLng)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("বিড সফলভাবে গৃহীত হয়েছে! 🚀")
                onSuccess()
            } catch (e: Exception) {
                // Bug fix (loading-lock): this used to only showToast() on failure and never call
                // back to the caller -- callers that set isAccepting = true before this call had no
                // way to reset that flag on failure, so a failed bid-accept left the confirm button
                // spinning forever until the screen was left and re-entered. onError() lets callers
                // reset their own state.
                val msg = "বিড গ্রহণে সমস্যা হয়েছে: ${e.message}"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] ⚠️ ইচ্ছাকৃতভাবে কোনো requireOnlineOrWarn() গার্ড বসানো হয়নি --
    // কোড পড়ে (repository.markSolverOnWay()) যাচাই করা হয়েছে এটা সম্পূর্ণ local-first (Room
    // status/notification instant) + best-effort Supabase dual-write (.onSuccess/.onFailure দিয়ে
    // wrap করা, কখনো throw করে না) -- কোনো টাকা/escrow না, শুধু UI/ট্র্যাকিং তথ্য। এখানে গার্ড
    // বসালে rule ১ ভঙ্গ হতো: অফলাইনে বর্তমানে এই লোকাল status-update নির্বিঘ্নে কাজ করে (শুধু
    // cloud sync পরে best-effort), গার্ড বসালে সেটাও ব্লক হয়ে যেত -- একটা নতুন regression, কোনো
    // real সুবিধা ছাড়াই (নিচের markSolverArrived/markJobStarted/updateSolverLiveLocation/
    // markCompletionResultSeen-এও একই যুক্তি প্রযোজ্য)।
    fun markSolverOnWay(problemId: String) {
        viewModelScope.launch {
            repository.markSolverOnWay(problemId)
            showToast("আপনি লোকেশনের উদ্দেশ্যে রওনা হয়েছেন 🚀")
        }
    }

    // [Offline Action Gating ধাপ ৭] ⚠️ ইচ্ছাকৃতভাবে গার্ড নেই -- markSolverOnWay()-এর উপরের
    // ব্যাখ্যা প্রযোজ্য (local-first + best-effort dual-write, কখনো throw করে না, কোনো টাকা না)।
    fun markSolverArrived(problemId: String, onComplete: () -> Unit = {}) {
        // Bug fix (loading-lock, JobTrackingScreen "পৌঁছে গেছি" button): added an optional
        // onComplete callback, defaulted to a no-op so any other existing caller is unaffected,
        // so the screen can reset its own isSubmitting flag once this actually finishes (success
        // or failure) instead of having no way to know when the fire-and-forget launch{} above
        // resolves. Wrapped in try/finally so onComplete still fires (and the button re-enables)
        // even if repository.markSolverArrived() throws.
        viewModelScope.launch {
            try {
                repository.markSolverArrived(problemId)
                showToast("আপনি লোকেশনে পৌঁছে গেছেন 📍")
            } finally {
                onComplete()
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] ⚠️ ইচ্ছাকৃতভাবে গার্ড নেই -- markSolverOnWay()-এর উপরের
    // ব্যাখ্যা প্রযোজ্য (local-first + best-effort dual-write, কখনো throw করে না, কোনো টাকা না)।
    fun markJobStarted(problemId: String, onComplete: () -> Unit = {}) {
        // Same loading-lock fix as markSolverArrived() above, for the "কাজ শুরু করুন" button.
        viewModelScope.launch {
            try {
                repository.markJobStarted(problemId)
                showToast("কাজ শুরু হয়েছে ⚡")
            } finally {
                onComplete()
            }
        }
    }

    fun cancelInstantJob(problemId: String, onCancelled: () -> Unit = {}) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] 🔴 money-critical (rule ২, "escrow release/refund")।
            // কোড পড়ে যাচাই করা হয়েছে: repository.cancelInstantJob() ইতিমধ্যে accepted solver থাকলে
            // (escrow HELD অবস্থায়) সরাসরি refundEscrowOnce() দিয়ে LOCAL optimistic wallet-credit
            // করে (`refund_escrow_once` RPC শুধু best-effort dual-write, cancel_instant_job RPC
            // ডুপ্লিকেট রিফান্ড এড়াতে ইচ্ছাকৃতভাবে escrow স্পর্শ করে না)। কোনো callback/onError
            // প্যারামিটার নেই বলে adminRejectBid()-এর (ধাপ ৭) প্যাটার্নে শুধু toast + return@launch।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া জরুরি কাজ বাতিল করা যাবে না")) {
                return@launch
            }
            com.example.worker.InstantJobExpiryWorker.cancelExpiryAlarm(getApplication(), problemId)
            repository.cancelInstantJob(problemId)
            showToast("কাজটি বাতিল করা হয়েছে")
            onCancelled()
        }
    }

    fun clearSolverCancelledNotice(problemId: String, onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- problem reset + bids CANCELLED local-first, clear_solver_cancelled_notice RPC best-effort। কোনো callback নেই।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া কাজটি আবার প্রচার করা যাবে না")) return@launch
            repository.clearSolverCancelledNotice(problemId)
            val timeoutSeconds = repository.getPlatformSetting("instant_job_broadcast_timeout_seconds")?.toLongOrNull() ?: 300L
            com.example.worker.InstantJobExpiryWorker.scheduleExactExpiryAlarm(getApplication(), problemId, timeoutSeconds)
            onCleared()
        }
    }

    fun completeInstantJob(
        problem: ProblemEntity,
        stars: Int = 5,
        reviewComment: String = "",
        onCompleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            com.example.worker.InstantJobExpiryWorker.cancelExpiryAlarm(getApplication(), problem.id)
            val updatedProb = problem.copy(jobStatus = "JOB_COMPLETED")
            try {
                repository.confirmReleaseAndComplete(
                    problem = updatedProb,
                    includeExtraAmount = true,
                    stars = stars,
                    reviewComment = reviewComment
                )
                showToast("কাজটি সফলভাবে সম্পন্ন হয়েছে! 🎉")
                onCompleted()
            } catch (e: EscrowReleaseRejectedException) {
                // [Step 12.10d] সার্ভার পেমেন্ট রিলিজ স্থায়ীভাবে প্রত্যাখ্যান করেছে (কোনো local write হয়নি)
                showToast(e.message ?: "পেমেন্ট রিলিজ করা যায়নি")
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] ⚠️ ইচ্ছাকৃতভাবে গার্ড নেই -- markSolverOnWay()-এর উপরের
    // ব্যাখ্যা প্রযোজ্য (local-first + best-effort dual-write, কখনো throw করে না, কোনো টাকা না;
    // GPS আপডেট প্রতি কয়েক সেকেন্ডে আসে, একটা ব্যর্থ কল উপেক্ষা করা নিরাপদ)।
    fun updateSolverLiveLocation(problemId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.updateSolverLiveLocation(problemId, lat, lng)
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.requestExtraAmount() কোনো
    // টাকা নড়ে না (শুধু problem-এ pending fields সেট করে + notification/chat-notice পাঠায়,
    // আসল ব্যালেন্স/এসক্রো পরিবর্তন হয় userConfirmExtraAmount()-এ, confirm করার সময়)।
    fun requestExtraAmount(
        problem: ProblemEntity,
        amount: Double,
        note: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (amount <= 0.0) {
            onError("সঠিক অতিরিক্ত টাকার পরিমাণ লিখুন।")
            return
        }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিলের অনুরোধ পাঠানো যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিলের অনুরোধ পাঠানো যাবে না")
                return@launch
            }
            try {
                repository.requestExtraAmount(problem, amount, note)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("অতিরিক্ত বিলের অনুরোধ পাঠানো হয়েছে! ➕")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "অনুরোধ পাঠাতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) --
    // repository.userConfirmExtraAmount() LOCAL wallet deduction (deductBalanceForUserRole) +
    // addToEscrow() -- উভয়ই optimistic Room write; `user_confirm_extra_amount` RPC শুধু
    // best-effort dual-write, কখনো throw করে না। ঠিক respondToAdditionalCharge()-এর accept
    // পাথের মতোই একই money-path (দুটোই "pending অতিরিক্ত বিল কনফার্ম করলে ওয়ালেট কাটা + escrow
    // বাড়া" প্যাটার্ন)।
    fun userConfirmExtraAmountPaid(
        problem: ProblemEntity,
        walletDeducted: Double = 0.0,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিল কনফার্ম করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিল কনফার্ম করা যাবে না")
                return@launch
            }
            try {
                repository.userConfirmExtraAmount(problem, walletDeducted)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("অতিরিক্ত বিল সফলভাবে কনফার্ম করা হয়েছে! ✅")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "কনফার্ম করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun userConfirmExtraAmount(
        problem: ProblemEntity,
        walletDeducted: Double = 0.0,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        userConfirmExtraAmountPaid(problem, walletDeducted, onSuccess, onError)
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.userRejectExtraAmount() কোনো
    // টাকা স্পর্শ করে না, শুধু pending fields ক্লিয়ার করে + সলভারকে notification/chat-notice
    // পাঠায়।
    fun userRejectExtraAmount(
        problem: ProblemEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিলের অনুরোধ প্রত্যাখ্যান করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিলের অনুরোধ প্রত্যাখ্যান করা যাবে না")
                return@launch
            }
            try {
                repository.userRejectExtraAmount(problem)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("অতিরিক্ত বিলের অনুরোধ বাতিল করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "বাতিল করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun getProblemByIdSync(problemId: String): ProblemEntity? {
        val cleanId = problemId.trim()
        if (cleanId.isBlank()) return null
        return allProblems.value.find { 
            it.id.trim() == cleanId || it.id.equals(cleanId, ignoreCase = true) 
        }
    }

    private val problemDetailJobs = mutableListOf<kotlinx.coroutines.Job>()

    fun selectProblem(problemId: String) {
        val cleanId = problemId.trim()
        problemDetailJobs.forEach { it.cancel() }
        problemDetailJobs.clear()

        if (cleanId.isBlank()) {
            _selectedProblem.value = null
            _problemBids.value = emptyList()
            _problemMessages.value = emptyList()
            return
        }

        // 1. Immediately populate from in-memory cached problems if available
        val cached = allProblems.value.find { 
            it.id.trim() == cleanId || it.id.equals(cleanId, ignoreCase = true) 
        }
        if (cached != null) {
            _selectedProblem.value = cached
        }

        // 2. Observe local database in real-time
        viewModelScope.launch {
            repository.getProblemByIdFlow(cleanId).collect { problem ->
                if (problem != null) {
                    _selectedProblem.value = problem
                } else if (_selectedProblem.value == null) {
                    val fallback = allProblems.value.find { 
                        it.id.trim() == cleanId || it.id.contains(cleanId) || cleanId.contains(it.id)
                    }
                    if (fallback != null) {
                        _selectedProblem.value = fallback
                    }
                }
            }
        }.also { problemDetailJobs.add(it) }

        viewModelScope.launch {
            repository.getBidsForProblem(cleanId).collect {
                _problemBids.value = it
            }
        }.also { problemDetailJobs.add(it) }

        viewModelScope.launch {
            repository.getMessagesForProblem(cleanId).collect { msgs ->
                _problemMessages.value = msgs
                // Real-time update: If new messages arrive that aren't yet in chatMessagesPaged,
                // add new ones at index 0 (newest side) without breaking older pagination offset.
                if (currentChatProblemId == cleanId && chatMessagesPaged.isNotEmpty()) {
                    val existingIds = chatMessagesPaged.map { it.id }.toSet()
                    val newIncoming = msgs.filter { it.id !in existingIds }
                    if (newIncoming.isNotEmpty()) {
                        // Sort incoming descending by timestamp so the newest is at the top/index 0
                        val sortedNew = newIncoming.sortedByDescending { it.timestamp }
                        chatMessagesPaged.addAll(0, sortedNew)
                    }
                }
            }
        }.also { problemDetailJobs.add(it) }

        // Automatically mark messages as read for this problem
        markMessagesAsReadForProblem(cleanId)
    }

    /**
     * Authoritative Bid Eligibility validator shared between UI and placeBid() business logic.
     * Evaluates Solver Role, Ownership, Banned/Restricted Status, Post OPEN Status,
     * Category Match, Physical Dynamic Radius KM Distance / Instant Job Radar Radius, KYC, and Duplicate Bid.
     */
    fun checkBidEligibility(
        problem: ProblemEntity,
        solver: UserEntity?,
        solverLat: Double?,
        solverLon: Double?,
        userBid: BidEntity? = null,
        radiusKm: Double = physicalCategoryRadiusKm.value
    ): BidEligibility {
        if (solver == null || solver.role != "SOLVER") {
            return BidEligibility(canBid = false, reason = BidBlockReason.NOT_SOLVER)
        }
        if (solver.id == problem.userId) {
            return BidEligibility(canBid = false, reason = BidBlockReason.POST_OWNER)
        }
        if (solver.isBannedSolver) {
            return BidEligibility(canBid = false, reason = BidBlockReason.BANNED)
        }
        if (solver.isRestrictedSolver) {
            return BidEligibility(canBid = false, reason = BidBlockReason.RESTRICTED)
        }
        if (problem.status != "OPEN" || problem.isDirectContract) {
            return BidEligibility(canBid = false, reason = BidBlockReason.POST_NOT_OPEN)
        }

        // Category match validation: Solver can only bid if the problem category is in their selected profile categories
        val solverCategories = solver.solverCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (!solverCategories.contains(problem.categoryId)) {
            return BidEligibility(canBid = false, reason = BidBlockReason.CATEGORY_MISMATCH)
        }

        // Determine effective radius: Instant emergency posts use category's instantJobRadiusKm (or admin default), normal physical posts use radiusKm (admin controlled)
        val effectiveRadiusKm = if (problem.isInstantJob) {
            val cat = allCategories.value.find { it.id == problem.categoryId }
            cat?.instantJobRadiusKm ?: instantJobDefaultRadiusKm.value
        } else {
            radiusKm
        }

        // Physical / Instant job distance validation (<= effectiveRadiusKm threshold)
        if (problem.isPhysical || problem.isInstantJob) {
            val effectiveLat = if (solverLat != null && solverLat != 0.0) solverLat else solver.latitude
            val effectiveLon = if (solverLon != null && solverLon != 0.0) solverLon else solver.longitude
            if (effectiveLat == 0.0 && effectiveLon == 0.0) {
                return BidEligibility(canBid = false, reason = BidBlockReason.LOCATION_UNAVAILABLE)
            }
            val distance = DistanceUtil.calculateDistanceKm(
                lat1 = effectiveLat,
                lon1 = effectiveLon,
                lat2 = problem.latitude,
                lon2 = problem.longitude
            )
            if (distance > effectiveRadiusKm) {
                return BidEligibility(canBid = false, reason = BidBlockReason.TOO_FAR, calculatedDistanceKm = distance)
            }
        }

        // KYC verification validation
        if (!solver.isKycVerified) {
            return BidEligibility(canBid = false, reason = BidBlockReason.KYC_REQUIRED)
        }

        // Cancelled job / previous contract cancellation validation (Solver who cancelled cannot re-bid)
        val hasCancelledBid = allBids.value.any {
            it.problemId == problem.id && it.solverId == solver.id && it.status == "CANCELLED"
        }
        if (hasCancelledBid) {
            return BidEligibility(canBid = false, reason = BidBlockReason.CANCELLED_PREVIOUSLY)
        }

        // Duplicate bid validation
        if (userBid != null) {
            return BidEligibility(canBid = false, reason = BidBlockReason.ALREADY_BID)
        }

        val finalDist = if (problem.isPhysical || problem.isInstantJob) {
            val effectiveLat = if (solverLat != null && solverLat != 0.0) solverLat else solver.latitude
            val effectiveLon = if (solverLon != null && solverLon != 0.0) solverLon else solver.longitude
            if (effectiveLat != 0.0 || effectiveLon != 0.0) {
                DistanceUtil.calculateDistanceKm(effectiveLat, effectiveLon, problem.latitude, problem.longitude)
            } else null
        } else null

        return BidEligibility(canBid = true, reason = null, calculatedDistanceKm = finalDist)
    }

    /**
     * Estimates commission rate for a solver (Phase M).
     */
    suspend fun estimateCommissionRate(solverId: String): Double {
        return repository.estimateCommissionRate(solverId)
    }

    /**
     * Previews commission breakdown for a solver with split base and extra calculations (Phase N).
     */
    suspend fun previewCommissionBreakdown(
        problem: ProblemEntity,
        solverId: String,
        baseAmount: Double,
        extraAmount: Double
    ) = repository.previewCommissionBreakdown(problem, solverId, baseAmount, extraAmount)

    fun placeBid(
        problem: ProblemEntity,
        amount: Double,
        message: String,
        estimatedTime: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val solver = _currentUser.value
        if (solver == null || solver.role != "SOLVER") {
            onError("শুধুমাত্র সমাধানকারীরা বিড করতে পারেন।")
            return
        }

        val liveLoc = _liveLocation.value
        val solverLat = if (liveLoc.latitude != 0.0) liveLoc.latitude else solver.latitude
        val solverLon = if (liveLoc.longitude != 0.0) liveLoc.longitude else solver.longitude

        val existingUserBid = allBids.value.find {
            it.problemId == problem.id &&
            it.solverId == solver.id &&
            it.status != "CANCELLED"
        } ?: if (_selectedProblem.value?.id == problem.id) {
            _problemBids.value.find {
                it.problemId == problem.id &&
                it.solverId == solver.id &&
                it.status != "CANCELLED"
            }
        } else null

        val eligibility = checkBidEligibility(
            problem = problem,
            solver = solver,
            solverLat = solverLat,
            solverLon = solverLon,
            userBid = existingUserBid,
            radiusKm = physicalCategoryRadiusKm.value
        )

        if (!eligibility.canBid) {
            val effectiveRadiusKm = if (problem.isInstantJob) {
                val cat = allCategories.value.find { it.id == problem.categoryId }
                cat?.instantJobRadiusKm ?: 5.0
            } else {
                physicalCategoryRadiusKm.value
            }
            val radiusBengali = DistanceUtil.toBengaliDigits(
                if (effectiveRadiusKm % 1.0 == 0.0) effectiveRadiusKm.toInt().toString() else effectiveRadiusKm.toString()
            )
            val errorMessage = when (eligibility.reason) {
                BidBlockReason.NOT_SOLVER -> "শুধুমাত্র সমাধানকারীরা বিড করতে পারেন।"
                BidBlockReason.POST_OWNER -> "নিজের পোস্টে বিড করা যাবে না।"
                BidBlockReason.BANNED -> "আপনার অ্যাকাউন্টটি ব্যান করা হয়েছে।"
                BidBlockReason.RESTRICTED -> "আপনার অ্যাকাউন্টটি সাময়িকভাবে রেস্ট্রিক্ট করা হয়েছে। আপনি কোনো বিড বা সমাধান প্রস্তাব পাঠাতে পারবেন না।"
                BidBlockReason.POST_NOT_OPEN -> "কাজটি বর্তমানে বিড করার জন্য উন্মুক্ত নয়।"
                BidBlockReason.CATEGORY_MISMATCH -> "এই পোস্টটি আপনার নির্বাচিত স্কিল ক্যাটাগরির সাথে মিলে না। আপনি আপনার প্রোফাইলে নির্বাচিত স্কিল ক্যাটাগরির কাজেই বিড করতে পারবেন।"
                BidBlockReason.LOCATION_UNAVAILABLE -> "আপনার অবস্থান নির্ধারণ করা যাচ্ছে না। ফিজিক্যাল/জরুরি কাজের জন্য বিড করতে লোকেশন প্রয়োজন।"
                BidBlockReason.TOO_FAR -> if (problem.isInstantJob) {
                    "আপনি এই জরুরি কাজের ${radiusBengali} কিমি রাডার সীমার বাইরে অবস্থান করছেন। তাই এতে বিড করতে পারবেন না।"
                } else {
                    "এই কাজটি আপনার বর্তমান অবস্থান থেকে ${radiusBengali} কিমি দূরত্বের বাইরে। ফিজিক্যাল কাজে সর্বোচ্চ ${radiusBengali} কিমি দূরত্বের মধ্যে বিড করা যাবে।"
                }
                BidBlockReason.KYC_REQUIRED -> "বিড করার পূর্বে KYC ভেরিফিকেশন সম্পন্ন করা আবশ্যক।"
                BidBlockReason.ALREADY_BID -> "আপনি ইতোমধ্যে এই কাজে বিড জমা দিয়েছেন।"
                BidBlockReason.CANCELLED_PREVIOUSLY -> "আপনি পূর্বে এই কাজ বাতিল করেছেন, তাই পুনরায় বিড করা সম্ভব নয়।"
                BidBlockReason.INVALID_BUDGET, null -> "বিড জমা দেওয়া সম্ভব হচ্ছে না।"
            }
            onError(errorMessage)
            return
        }

        if (amount <= 0 || message.isBlank() || estimatedTime.isBlank()) {
            onError("সঠিক মূল্য, আনুমানিক সময় ও বার্তা লিখুন।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- মাস্টার প্রম্পটের Goal সেকশনে "bid"
            // স্পষ্টভাবে উদাহরণ হিসেবে উল্লেখ আছে। repository.placeBid() local-first (bids insert
            // সরাসরি Room-এ, Supabase dual-write fire-and-forget)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিড জমা দেওয়া যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিড জমা দেওয়া যাবে না")
                return@launch
            }
            val result = repository.placeBid(
                problem = problem,
                solver = solver,
                amount = amount,
                message = message,
                estimatedTime = estimatedTime
            )
            result.onSuccess {
                showToast("বিড সফলভাবে জমা দেওয়া হয়েছে!")
                onSuccess()
            }.onFailure {
                onError(it.message ?: "বিড জমা দিতে সমস্যা হয়েছে।")
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] 🔴 money-critical (rule ২, "bid accept" স্পষ্টভাবে নামোল্লেখ
    // করা আছে) -- কোড পড়ে যাচাই করা হয়েছে: repository.acceptBid() সম্পূর্ণ local-first (wallet
    // deduction + openEscrow() উভয়ই optimistic Room write, Supabase `accept_bid` RPC শুধু
    // best-effort dual-write, কখনো throw করে না)। অর্থাৎ গার্ড ছাড়া অফলাইনেও (toggle OFF অবস্থায়)
    // real money escrow-এ লক হয়ে যেত, কোনো network confirm ছাড়াই -- ঠিক যা rule ২ নিষেধ করে। তাই
    // এখানে সবসময় (ফাংশনের একদম শুরুতে) গার্ড বসানো হলো, deposit-money-via-gateway (ধাপ ৬)-এর
    // মতোই -- অফলাইনে local write-টুকুও ঘটবে না।
    fun acceptBid(
        problem: ProblemEntity,
        bid: BidEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        // Bug fix (loading-lock groundwork): added the try/catch + onError so a screen-level
        // isProcessing flag driven off this callback can never get stuck locked forever if the
        // repository call throws -- previously an exception here had nowhere to go but crash/be
        // swallowed, and the caller's onSuccess (the only unlock signal) would just never fire.
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিড গ্রহণ করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিড গ্রহণ করা যাবে না")
                return@launch
            }
            try {
                repository.acceptBid(problem, bid)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("বিড সফলভাবে গৃহীত হয়েছে! 🎉")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "বিড গ্রহণ করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] কোড পড়ে যাচাই: কোনো টাকা/escrow জড়িত না (এটা accept হওয়ার
    // আগের PENDING বিড, repository.withdrawBid()-এর comment অনুযায়ী) -- তাই rule ২ প্রযোজ্য না,
    // কিন্তু এটা Group B সরাসরি network-write (cancel_bid RPC, best-effort dual-write, Outbox-এ
    // নেই)। গার্ড না বসালে অফলাইনে local বিড "বাতিল" দেখাবে অথচ সার্ভারে চিরস্থায়ীভাবে PENDING
    // থেকে যাবে (কোনো retry/outbox নেই এটা ধরার জন্য) -- তাই সাধারণ Group B প্যাটার্নে গার্ড।
    fun withdrawBid(bid: BidEntity, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিড প্রত্যাহার করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিড প্রত্যাহার করা যাবে না")
                return@launch
            }
            try {
                repository.withdrawBid(bid)
                showToast("বিড প্রত্যাহার করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "বিড প্রত্যাহার করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    // See SomadhanRepository.ownerResetOrphanedAcceptedBid() for the full explanation of when
    // this is safe to use: only when the accepted bid on this post has no real money locked
    // behind it (the "unknown solver accepted, no cancel option, only dispute" dead-end).
    // [Offline Action Gating ধাপ ৭] কোড পড়ে যাচাই: repository.ownerResetOrphanedAcceptedBid()
    // নিজেই লকড-অ্যামাউন্ট চেক করে real money থাকলে false রিটার্ন করে রিফিউজ করে (rule ২-এর
    // money-critical case কখনো এখানে পৌঁছায় না, ফাংশনের নিজস্ব ডিজাইনেই) -- তাই এটা rule ২ না,
    // সাধারণ Group B guard (network-write, best-effort dual-write, Outbox-এ নেই)।
    fun ownerResetOrphanedAcceptedBid(
        problem: ProblemEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এই বিড রিসেট করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া এই বিড রিসেট করা যাবে না")
                return@launch
            }
            try {
                val ownerId = _currentUser.value?.id ?: return@launch
                val didReset = repository.ownerResetOrphanedAcceptedBid(problem.id, ownerId)
                if (didReset) {
                    val updated = repository.getProblemById(problem.id)
                    _selectedProblem.value = updated
                    showToast("বিডটি বাতিল করে পোস্টটি পুনরায় ব্রডকাস্ট করা হয়েছে।")
                    onSuccess()
                } else {
                    val err = "এই পোস্টে ইতিমধ্যে টাকা লকড আছে বা এটি রিসেট করা সম্ভব নয় — অনুগ্রহ করে Dispute ব্যবহার করুন।"
                    showToast(err)
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.message ?: "রিসেট করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    // Bug fix (see repository.solverCancelJob()'s reopenAsOpen=true path / issue #4): when a
    // solver cancels an ACCEPTED instant job, the problem goes back to BROADCASTING immediately,
    // but previously no new exact AlarmManager alarm was scheduled for it right then -- only when
    // the user later dismissed the "solver cancelled" notice (see clearSolverCancelledNotice()
    // above). If the user is slow to notice/dismiss that banner, the job's only expiry safety net
    // was the 15-minute periodic worker, far later than the promised broadcast timeout. Called
    // right after every solverCancelJob()/solverCancelAcceptedJob() repository call below so the
    // new broadcast window gets its own exact alarm immediately, same as createInstantJob() does.
    private suspend fun rescheduleExpiryAlarmIfBroadcasting(updated: ProblemEntity?) {
        if (updated?.isInstantJob == true && updated.jobStatus == "BROADCASTING") {
            val timeoutSeconds = repository.getPlatformSetting("instant_job_broadcast_timeout_seconds")?.toLongOrNull() ?: 300L
            com.example.worker.InstantJobExpiryWorker.scheduleExactExpiryAlarm(getApplication(), updated.id, timeoutSeconds)
        }
    }

    // [Offline Action Gating ধাপ ৭] 🔴 money-critical (rule ২, "escrow release/refund")।
    // repository.solverCancelAcceptedJob() → solverCancelJob()-কেই কল করে, যেটা কোড পড়ে
    // নিশ্চিত হওয়া গিয়েছে সরাসরি refundEscrowOnce() দিয়ে LOCAL optimistic wallet-credit করে
    // (Supabase `solver_cancel_job` RPC শুধু best-effort dual-write)। তাই acceptBid()-এর মতোই
    // toggle-independent হার্ড গার্ড দরকার।
    fun solverCancelAcceptedJob(
        problem: ProblemEntity,
        bid: BidEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া কাজ বাতিল করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া কাজ বাতিল করা যাবে না")
                return@launch
            }
            try {
                repository.solverCancelAcceptedJob(problem, bid)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                rescheduleExpiryAlarmIfBroadcasting(updated)
                showToast("কাজটি বাতিল করা হয়েছে এবং গ্রাহককে রিফান্ড পাঠানো হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "কাজ বাতিল করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    fun solverCancelAcceptedJob(
        problem: ProblemEntity,
        reason: String = "বাতিল",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val sId = _currentUser.value?.id ?: problem.acceptedSolverId ?: ""
        solverCancelJob(problem.id, sId, reason, reopenAsOpen = true, onSuccess = onSuccess, onError = onError)
    }

    // [Offline Action Gating ধাপ ৭] 🔴 money-critical (rule ২, "escrow release/refund")। এই
    // ফাংশনই solverCancelAcceptedJob(problem, reason, ...) ওভারলোড-টাও ভেতরে ভেতরে কল করে
    // (উপরে দেখুন) -- তাই একটা মাত্র গার্ড দুটো UI entry point-ই কভার করে।
    fun solverCancelJob(
        problemId: String,
        solverId: String,
        reason: String = "বিরোধ চলাকালীন সমাধানকারী কর্তৃক বিড বাতিল",
        reopenAsOpen: Boolean = true,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া কাজ বাতিল করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া কাজ বাতিল করা যাবে না")
                return@launch
            }
            try {
                repository.solverCancelJob(problemId, solverId, reason, reopenAsOpen)
                val updated = repository.getProblemById(problemId)
                _selectedProblem.value = updated
                if (reopenAsOpen) rescheduleExpiryAlarmIfBroadcasting(updated)
                showToast("বিড বাতিল করা হয়েছে এবং ক্লায়েন্টকে এসক্রো রিফান্ড করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "বিড বাতিল করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] Job-release ফ্লো (rule ২-এর সরাসরি money-movement না হলেও
    // money-adjacent -- এই কলটাই owner-এর ৪৮-ঘণ্টা auto-release/payment প্রক্রিয়া শুরু করে,
    // pending AdditionalChargeEntity তৈরি করে)। কোড পড়ে যাচাই: local-first + best-effort
    // dual-write (কখনো throw করে না), কিন্তু সিঙ্ক ছাড়া অন্য পক্ষ এই request-ই দেখতে পাবে না --
    // তাই সাধারণ Group B গার্ড।
    fun requestJobRelease(
        problem: ProblemEntity,
        extraAmount: Double,
        note: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রিলিজের অনুরোধ পাঠানো যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া রিলিজের অনুরোধ পাঠানো যাবে না")
                return@launch
            }
            try {
                repository.requestJobRelease(problem, extraAmount, note)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("কাজ সম্পন্ন ও অর্থ রিলিজের অনুরোধ সফলভাবে পাঠানো হয়েছে! 🚀")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "অনুরোধ পাঠাতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] Job-release ফ্লো (requestJobRelease()-এর মতো একই কারণে গার্ড)।
    fun cancelJobReleaseRequest(
        problem: ProblemEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রিলিজ অনুরোধ প্রত্যাহার করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া রিলিজ অনুরোধ প্রত্যাহার করা যাবে না")
                return@launch
            }
            try {
                repository.cancelJobReleaseRequest(problem)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("রিলিজ অনুরোধ প্রত্যাহার করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "অনুরোধ প্রত্যাহার করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] Job-release ফ্লো (requestJobRelease()-এর মতো একই কারণে গার্ড)।
    fun rejectJobReleaseRequest(
        problem: ProblemEntity,
        reason: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রিলিজ অনুরোধ প্রত্যাখ্যান করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া রিলিজ অনুরোধ প্রত্যাখ্যান করা যাবে না")
                return@launch
            }
            try {
                repository.rejectJobReleaseRequest(problem, reason)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("রিলিজ অনুরোধ প্রত্যাখ্যান করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "অনুরোধ প্রত্যাখ্যান করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.raiseDispute() কোনো টাকা
    // স্পর্শ করে না, শুধু problem-এ dispute flags সেট করে + অপর পক্ষকে notification/chat-notice
    // পাঠায়।
    fun raiseDispute(
        problem: ProblemEntity,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        if (reason.isBlank()) {
            onError("বিরোধের কারণ উল্লেখ করুন।")
            return
        }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিরোধ (Dispute) রেজিস্টার করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিরোধ (Dispute) রেজিস্টার করা যাবে না")
                return@launch
            }
            try {
                repository.raiseDispute(problem, user.id, reason)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("বিরোধ (Dispute) সফলভাবে রেজিস্টার হয়েছে। চ্যাটে আলোচনা করুন।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "বিরোধ রেজিস্ট্রেশন করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.withdrawDispute() কোনো টাকা
    // স্পর্শ করে না, শুধু dispute flags রিসেট করে + অপর পক্ষকে notification/chat-notice পাঠায়।
    fun withdrawDispute(problem: ProblemEntity, onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val requesterId = _currentUser.value?.id ?: return@launch
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিরোধ প্রত্যাহার করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিরোধ প্রত্যাহার করা যাবে না")
                return@launch
            }
            try {
                // মূল ফিক্স: acceptedSolverId নয়, disputeInitiatorId মিলিয়ে দেখো
                if (problem.isDisputed && problem.disputeInitiatorId == requesterId) {
                    repository.withdrawDispute(problem, requesterId)
                    val updated = repository.getProblemById(problem.id)
                    _selectedProblem.value = updated
                    showToast("বিরোধ প্রত্যাহার করা হয়েছে।")
                }
                onDone()
            } catch (e: Exception) {
                // Bug fix (loading-lock groundwork): previously any exception here left onDone()
                // uncalled, so a caller using this to drive a button's isProcessing flag would
                // never see it unlock.
                val err = e.message ?: "বিরোধ প্রত্যাহার করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B (ইনভেন্টরি সংশোধন) -- ধাপ ১-এর ইনভেন্টরিতে
    // `settle_dispute` 🔴 হিসেবে ট্যাগ করা ছিল, কিন্তু কোড পড়ে (rule ১০) নিশ্চিত হওয়া গেছে
    // repository.settleDispute() কোনো টাকা স্পর্শ করে না (উভয় পক্ষের সমঝোতায় dispute flags
    // রিসেট করে + notification/chat-notice পাঠায় মাত্র) -- adminResolveDispute()-এর মতো
    // escrow/wallet payout নেই। rule ২ প্রযোজ্য না, তাই সাধারণ Group B guard।
    fun settleDispute(
        problem: ProblemEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সমঝোতা সম্পন্ন করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া সমঝোতা সম্পন্ন করা যাবে না")
                return@launch
            }
            try {
                repository.settleDispute(problem, user.id)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("সমঝোতা সম্পন্ন হয়েছে। কাজের প্রক্রিয়া পুনরায় স্বাভাবিকভাবে চলবে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "সমঝোতা প্রক্রিয়ায় সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.requestAdminAssistance()
    // কোনো টাকা স্পর্শ করে না, শুধু problem flag সেট করে + অ্যাডমিনদের notification পাঠায়।
    fun requestAdminAssistance(
        problem: ProblemEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        val role = user.role.ifBlank { if (problem.userId == user.id) "USER" else "SOLVER" }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া অ্যাডমিন সহায়তা অনুরোধ করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া অ্যাডমিন সহায়তা অনুরোধ করা যাবে না")
                return@launch
            }
            try {
                repository.requestAdminAssistance(problem, user.id, role)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                showToast("অ্যাডমিন সহায়তার অনুরোধ পাঠানো হয়েছে! 🛡️")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "অ্যাডমিন সহায়তা অনুরোধে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ৭] 🔴 money-critical (rule ২, "escrow release/refund") -- এটা
    // solverCancelJob(problemId, solverId, ...) ওভারলোডের-ই একটা আলাদা UI entry point
    // (dispute-flow থেকে কল হয়, নিজের viewModelScope.launch ব্লক আলাদা), একই
    // repository.solverCancelJob() escrow-refund পাথে যায় -- তাই এখানেও আলাদাভাবে গার্ড লাগবে।
    fun solverCancelJob(
        problem: ProblemEntity,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া কাজ বাতিল করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া কাজ বাতিল করা যাবে না")
                return@launch
            }
            try {
                repository.solverCancelJob(problem.id, user.id, reason, reopenAsOpen = true)
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                rescheduleExpiryAlarmIfBroadcasting(updated)
                showToast("কাজটি বাতিল ও রিফান্ড করা হয়েছে, সমস্যাটি পুনরায় সবার জন্য উন্মুক্ত হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "কাজ বাতিল করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun userDeleteProblem(
        problem: ProblemEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে লগইন করুন।")
            showToast("অনুগ্রহ করে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- repository.userDeleteProblem()
            // local-first (soft-delete + bid cancel সরাসরি Room-এ, একটা RPC দিয়ে dual-write
            // best-effort)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া পোস্ট ডিলিট করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া পোস্ট ডিলিট করা যাবে না")
                return@launch
            }
            try {
                repository.userDeleteProblem(problem.id, user.id)
                showToast("পোস্টটি সফলভাবে ডিলিট করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "পোস্ট ডিলিট করতে ব্যর্থ হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    fun adminSendMessageToProblemChat(
        problemId: String,
        content: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val adminUser = _currentUser.value?.takeIf { it.role == "ADMIN" } ?: UserEntity(
            id = "ADMIN_SYSTEM",
            name = "Support Manager",
            phone = "01700000000",
            email = "admin@somadhan.com",
            password = "",
            role = "ADMIN",
            latitude = 23.8103,
            longitude = 90.4125,
            address = "ঢাকা, বাংলাদেশ"
        )
        if (content.isBlank()) return

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৯] সাধারণ Group B -- এটা admin panel-এর সাপোর্ট-মেসেজ
            // (ধাপ ৯-এ শুধু কনজিউমার চ্যাটের জন্য optimistic-send/retry UX রাখা হয়েছে, admin flow
            // না) -- তাই এখানে ধাপ ৫-৮-এর স্ট্যান্ডার্ড হার্ড-গার্ড প্যাটার্ন।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া মেসেজ পাঠানো যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া মেসেজ পাঠানো যাবে না")
                return@launch
            }
            try {
                repository.adminSendMessageToProblemChat(problemId, adminUser, content)
                val updated = repository.getProblemById(problemId)
                _selectedProblem.value = updated
                showToast("সাপোর্ট ম্যানেজার বার্তা সফলভাবে পাঠানো হয়েছে 🛡️")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "বার্তা পাঠাতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) --
    // repository.adminResolveDispute() কোড পড়ে (rule ১০) নিশ্চিত হওয়া গেছে তিনটা resolution
    // branch-ই (RELEASE_TO_SOLVER, SPLIT_SETTLEMENT/CUSTOM_SPLIT/SETTLE, REFUND_TO_USER) LOCAL
    // optimistic wallet/escrow write করে (RELEASE_TO_SOLVER-এ confirmReleaseAndComplete()-এর
    // wallet-deduction path কল হয়; SPLIT-এ userDao.addBalanceForSolverRole() সরাসরি; REFUND-এ
    // refundEscrowOnce()) -- সংশ্লিষ্ট RPC-গুলো (`resolve_dispute_split` সহ) শুধু best-effort
    // dual-write, কখনো throw করে না। ইনভেন্টরি-নোট: `resolve_dispute` (non-split) RPC আসলে
    // dead code (কোথাও কল হয় না, SupabaseSyncManager.kt-এর নিজস্ব comment-এই confirm করা আছে) --
    // rule ২ এখানে প্রযোজ্য RPC dual-write-এর কারণে না, বরং উপরের LOCAL optimistic write-এর
    // কারণে, ঠিক acceptBid()-এর মতোই। এই একই গার্ড confirmReleaseAndComplete()-এর
    // RELEASE_TO_SOLVER-সাব-কল আর mark_additional_charge_settled (ধাপ ১০-এর Additional charge
    // আইটেম, confirmReleaseAndComplete()-এর ভেতর থেকে সাব-স্টেপ হিসেবে কল হয়) দুটোকেই
    // স্বয়ংক্রিয়ভাবে কভার করে -- যেমনটা ধাপ ৯-এর progress-এন্ট্রিতে আগেই ইঙ্গিত দেওয়া হয়েছিল।
    fun adminResolveDispute(
        problemId: String,
        resolution: String,
        decisionNote: String,
        splitSolverPercent: Double = 50.0,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ডিসপিউট মীমাংসা করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া ডিসপিউট মীমাংসা করা যাবে না")
                return@launch
            }
            try {
                repository.adminResolveDispute(problemId, resolution, decisionNote, splitSolverPercent)
                val updated = repository.getProblemById(problemId)
                _selectedProblem.value = updated
                showToast("ডিসপিউট সিদ্ধান্ত সফলভাবে কার্যকর করা হয়েছে! ⚖️")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "ডিসপিউট মীমাংসা করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] ⚠️ ইচ্ছাকৃতভাবে গার্ড নেই (rule ১ রক্ষা করতে) --
    // repository.markDisputeResultSeen() সম্পূর্ণ local-first "দেখা হয়েছে" flag (কোনো টাকা না),
    // markCompletionResultSeen()/markProblemSeen()-এর মতোই একই সিদ্ধান্ত।
    fun markDisputeResultSeen(problemId: String, isUser: Boolean) {
        _dismissedDisputeIds.value = _dismissedDisputeIds.value + problemId
        viewModelScope.launch {
            try {
                repository.markDisputeResultSeen(problemId, isUser)
            } catch (_: Exception) {}
        }
    }

    // [Offline Action Gating ধাপ ৭] ⚠️ ইচ্ছাকৃতভাবে গার্ড নেই -- এটা শুধু একটা "দেখা হয়েছে" UI
    // ফ্ল্যাগ (completionResultSeenByUser/BySolver), local-first + best-effort dual-write, কোনো
    // টাকা না। markSolverOnWay()-এর উপরের ব্যাখ্যা প্রযোজ্য।
    fun markCompletionResultSeen(problemId: String, isUser: Boolean) {
        _dismissedDisputeIds.value = _dismissedDisputeIds.value + problemId
        viewModelScope.launch {
            try {
                repository.markCompletionResultSeen(problemId, isUser)
            } catch (_: Exception) {}
        }
    }

    fun adminIssueWarningStrike(
        targetUserId: String,
        problemId: String,
        reason: String,
        penaltyReputation: Double = 5.0,
        customMessage: String = "",
        // [ROLE_SEPARATION ধাপ ৬] UI (AdminDisputeCenterView) থেকে targetParty পাস করার জন্য।
        targetRole: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.adminIssueWarningStrike(
                    targetUserId = targetUserId,
                    problemId = problemId,
                    reason = reason,
                    penaltyReputation = penaltyReputation,
                    customMessage = customMessage,
                    targetRole = targetRole
                )
                showToast("সতর্কবার্তা ও পেনাল্টি সফলভাবে পাঠানো হয়েছে! 🚨")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "সতর্কবার্তা পাঠাতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.adminManuallyFlagDispute()
    // কোনো টাকা স্পর্শ করে না, শুধু problem-এ dispute flags সেট করে + উভয় পক্ষকে
    // notification/chat-notice পাঠায়।
    fun adminManuallyFlagDispute(
        problemId: String,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিরোধ চিহ্নিত করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিরোধ চিহ্নিত করা যাবে না")
                return@launch
            }
            try {
                repository.adminManuallyFlagDispute(problemId, reason)
                showToast("সমস্যাটি বিরোধ হিসেবে চিহ্নিত করা হয়েছে।")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "বিরোধ চিহ্নিত করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun checkAndProcess48HourAutoReleases() {
        viewModelScope.launch {
            try {
                repository.checkAndProcess48HourAutoReleases()
            } catch (e: Exception) {
                Log.e("SomadhanVM", "Check auto releases error: ${e.message}")
            }
        }
    }

    // [Offline Action Gating ধাপ ১০] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) --
    // repository.confirmReleaseAndComplete() কোড পড়ে (rule ১০) নিশ্চিত হওয়া গেছে এটাই "every
    // settlement path"-এর choke point: LOCAL wallet deduction (walletDeduction > 0 হলে) +
    // escrow release (payoutEscrowToSolver() -> release_escrow RPC, Group A কিন্তু rule ২-এর
    // "escrow" নামোল্লেখ toggle-independent) -- উভয়ই optimistic Room write। এই একই ফাংশনের
    // ভেতর থেকেই `mark_additional_charge_settled` (ধাপ ১০-এর Additional charge আইটেম) সাব-স্টেপ
    // হিসেবে কল হয় -- তাই এখানে গার্ড বসালে সেটাও স্বয়ংক্রিয়ভাবে কভার্ড হয়ে যায় (ধাপ ৯-এর
    // progress-এন্ট্রিতে আগেই এই প্ল্যান লেখা ছিল)। এই একই ফাংশন
    // checkAndProcess48HourAutoReleases() (cron sweep) আর adminUpdateDirectContractStatus()
    // (ধাপ ১১-এর Direct contract ডোমেইন)-এর ভেতর থেকেও কল হয় -- দুটোই repository-লেভেলে সরাসরি
    // কল করে, এই ViewModel-গার্ড এড়িয়ে যায় (ইচ্ছাকৃতভাবে, cron-triggered flow guard করা ঠিক না,
    // ধাপ ৮-এর expire_broadcasting_instant_job-এর মতোই); adminUpdateDirectContractStatus()-এর
    // নিজস্ব ViewModel entry point ধাপ ১১-এ আলাদাভাবে গার্ড করতে হবে (নিচে "পরবর্তী সেশন" নোট
    // দ্রষ্টব্য)।
    fun confirmReleaseAndComplete(
        problem: ProblemEntity,
        includeExtraAmount: Boolean,
        walletDeduction: Double = 0.0,
        stars: Int = 0,
        reviewComment: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া কাজ সম্পন্ন করে টাকা রিলিজ করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া কাজ সম্পন্ন করে টাকা রিলিজ করা যাবে না")
                return@launch
            }
            try {
                repository.confirmReleaseAndComplete(
                    problem = problem,
                    includeExtraAmount = includeExtraAmount,
                    stars = stars,
                    reviewComment = reviewComment,
                    walletDeduction = walletDeduction
                )
                val updated = repository.getProblemById(problem.id)
                _selectedProblem.value = updated
                refreshCurrentUser()
                showToast("কাজটি সম্পন্ন হয়েছে এবং অর্থ সফলভাবে রিলিজ হয়েছে! 🎉")
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "রিলিজ সম্পন্ন করতে সমস্যা হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun markProblemCompleted(
        problem: ProblemEntity,
        onSuccess: () -> Unit
    ) {
        confirmReleaseAndComplete(
            problem = problem,
            includeExtraAmount = true,
            onSuccess = onSuccess
        )
    }

    fun markProblemCompleted(
        problem: ProblemEntity,
        stars: Int,
        reviewComment: String,
        onSuccess: () -> Unit
    ) {
        confirmReleaseAndComplete(
            problem = problem,
            includeExtraAmount = true,
            stars = stars,
            reviewComment = reviewComment,
            onSuccess = onSuccess
        )
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.submitSolverRatingForUser()
    // কোনো টাকা স্পর্শ করে না (শুধু rating insert + সলভারের reputation আপডেট), কিন্তু কোনো
    // callback/onError না থাকায় guard ব্যর্থ হলে শুধু toast দেখানো হচ্ছে (ধাপ ৯-এর
    // adminDeleteMessage()-এর প্যাটার্ন)।
    fun submitSolverRatingForUser(problem: ProblemEntity, stars: Int, comment: String) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রেটিং দেওয়া যাবে না")) return@launch
            repository.submitSolverRatingForUser(problem, stars, comment)
            showToast("ইউজারকে রেটিং দেওয়া হয়েছে ✅")
        }
    }

    // [Offline Action Gating ধাপ ৮] ইচ্ছাকৃতভাবে গার্ড করা হয়নি (rule ১ রক্ষা করতে) --
    // repository.markProblemSeen() সম্পূর্ণ local-first ("দেখা হয়েছে" flag সবসময় instant Room
    // update, Supabase dual-write .onFailure দিয়ে wrap করা, কখনো caller পর্যন্ত throw করে না)।
    // কোনো callback/onError নেই -- guard বসালে পুরো ফাংশন কল-ই স্কিপ হয়ে যেত, অর্থাৎ লোকাল
    // flag-আপডেটও আর হতো না, যা বর্তমান কার্যকর graceful-degradation ভেঙে দিত কোনো real সুবিধা
    // ছাড়াই (এখানে কোনো টাকা/state-integrity ঝুঁকি নেই)। ধাপ ৭-এর markSolverOnWay ইত্যাদি
    // পাঁচটা লাইভ-স্ট্যাটাস ফাংশনের মতোই একই সিদ্ধান্ত।
    fun markProblemSeen(problemId: String, role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markProblemSeen(problemId, role)
        }
    }

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- উপরের submitSolverRatingForUser()-এর
    // মতোই একই কারণ (কোনো টাকা না, কোনো callback নেই)।
    fun submitUserRatingForSolver(problem: ProblemEntity, stars: Int, comment: String) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রেটিং দেওয়া যাবে না")) return@launch
            repository.submitUserRatingForSolver(problem, stars, comment)
            showToast("সমাধানকারীকে রেটিং দেওয়া হয়েছে ✅")
        }
    }

    fun sendMessage(
        problemId: String,
        receiverId: String,
        content: String,
        fileUrl: String? = null,
        fileName: String? = null,
        fileType: String? = null,
        isDirectContractProposal: Boolean = false,
        directContractBudget: Double? = null,
        directContractDuration: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                problemId = problemId,
                senderId = user.id,
                receiverId = receiverId,
                senderName = user.name,
                content = content,
                fileUrl = fileUrl,
                fileName = fileName,
                fileType = fileType,
                isDirectContractProposal = isDirectContractProposal,
                directContractBudget = directContractBudget,
                directContractDuration = directContractDuration
            )
            repository.setTypingStatus(problemId, user.id, false)
            onSuccess()
        }
    }

    fun setTypingStatus(problemId: String, isTyping: Boolean) {
        val user = _currentUser.value ?: return
        repository.setTypingStatus(problemId, user.id, isTyping)
    }

    // Realtime Scoping ফিক্স, ধাপ ৩ (Chat, dual-run) — ChatScreen-এর LaunchedEffect (suspend
    // context)-এর পাশাপাশি DisposableEffect-এর onDispose (non-suspend lambda)-থেকেও কল করা লাগবে,
    // তাই [setTypingStatus]-এর মতোই non-suspend wrapper + viewModelScope.launch প্যাটার্ন রাখা হলো।
    fun joinProblemChatBroadcast(problemId: String) {
        viewModelScope.launch { repository.joinProblemChatBroadcast(problemId) }
    }

    fun leaveProblemChatBroadcast(problemId: String) {
        viewModelScope.launch { repository.leaveProblemChatBroadcast(problemId) }
    }

    // Realtime Scoping ফিক্স, ধাপ ৪ (Bids, dual-run) — একই কারণে (ProblemDetailScreen-এর
    // LaunchedEffect + DisposableEffect) non-suspend wrapper, joinProblemChatBroadcast-এর মতোই।
    fun joinProblemBidsBroadcast(problemId: String) {
        viewModelScope.launch { repository.joinProblemBidsBroadcast(problemId) }
    }

    fun leaveProblemBidsBroadcast(problemId: String) {
        viewModelScope.launch { repository.leaveProblemBidsBroadcast(problemId) }
    }

    fun createDirectContractProject(
        solver: UserEntity,
        title: String,
        description: String,
        category: CategoryEntity,
        budget: Double,
        durationDays: Int,
        address: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("অনুগ্রহ করে প্রথমে লগইন করুন।")
            return
        }
        if (user.isBannedUser) {
            onError("আপনার অ্যাকাউন্টটি ব্যান করা হয়েছে।")
            return
        }
        if (user.isRestrictedUser) {
            onError("আপনার অ্যাকাউন্টটি রেস্ট্রিক্ট করা হয়েছে।")
            return
        }

        // Constraint 1: Direct contract only allowed if solver is in user's favorite solvers list
        val favSolverIds = user.favoriteSolverIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (!favSolverIds.contains(solver.id)) {
            onError("সরাসরি কাজের প্রস্তাব পাঠাতে হলে সমাধানকারীকে প্রথমে আপনার পছন্দের তালিকায় যুক্ত থাকতে হবে।")
            return
        }

        // Constraint 2: Category match validation - Solver must have this category in their profile solverCategories
        val solverCats = solver.solverCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (!solverCats.contains(category.id)) {
            onError("নির্বাচিত ক্যাটাগরি (${category.nameBangla}) সমাধানকারীর তালিকাভুক্ত দক্ষতার সাথে মেলেনি। সরাসরি অ্যাসাইন করা যাবে না।")
            return
        }

        if (title.isBlank() || description.isBlank()) {
            onError("কাজের শিরোনাম ও বিবরণ পূরণ করা আবশ্যক।")
            return
        }
        if (budget <= 0.0) {
            onError("সঠিক বাজেট প্রদান করুন।")
            return
        }
        if (category.isPhysical) {
            onError("সরাসরি প্রজেক্ট অ্যাসাইনমেন্ট শুধুমাত্র ভার্চুয়াল/অনলাইন কাজের ক্যাটাগরির জন্য প্রযোজ্য।")
            return
        }

        // Spam prevention rate limit: Max 5 direct contract proposals per solver per day
        val recentDirectCount = allProblems.value.count {
            it.userId == user.id && it.acceptedSolverId == solver.id && it.isDirectContract &&
                (System.currentTimeMillis() - it.createdAt < 24L * 60 * 60 * 1000)
        }
        if (recentDirectCount >= 5) {
            onError("স্প্যাম প্রতিরোধে আপনি এই সমাধানকারীকে ২৪ ঘণ্টায় সর্বোচ্চ ৫টি সরাসরি প্রস্তাব পাঠাতে পারবেন।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- createProblem()-এর (ধাপ ৮) মতোই problem insert + cloud dual-write।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সরাসরি চুক্তির প্রস্তাব পাঠানো যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া সরাসরি চুক্তির প্রস্তাব পাঠানো যাবে না")
                return@launch
            }
            try {
                val effectiveLat = if (latitude != 0.0) latitude else user.latitude
                val effectiveLon = if (longitude != 0.0) longitude else user.longitude
                val effectiveAddr = address.ifBlank { user.address.ifBlank { "অনলাইন / ভার্চুয়াল" } }

                val created = repository.createDirectContract(
                    user = user,
                    solver = solver,
                    title = title,
                    description = description,
                    category = category,
                    budget = budget,
                    durationDays = durationDays,
                    address = effectiveAddr,
                    latitude = effectiveLat,
                    longitude = effectiveLon
                )
                showToast("সরাসরি চুক্তি প্রস্তাব পাঠানো হয়েছে! 🎉")
                onSuccess(created.id)
            } catch (e: Exception) {
                val msg = e.message ?: "চুক্তি তৈরি করতে ব্যর্থ হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun acceptDirectContractProposal(
        problemId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value ?: run {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) -- repository.acceptDirectContractProposal() local EscrowEntity বানায় (acceptBid()-এর মতোই money-adjacent)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সরাসরি চুক্তি গ্রহণ করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া সরাসরি চুক্তি গ্রহণ করা যাবে না")
                return@launch
            }
            try {
                val updated = repository.acceptDirectContractProposal(problemId, user.id)
                if (updated != null) {
                    _selectedProblem.value = updated
                    showToast("সরাসরি চুক্তি গ্রহণ করা হয়েছে! কাজ শুরু হলো। 🎉")
                    onSuccess()
                } else {
                    onError("চুক্তি গ্রহণ করতে সমস্যা হয়েছে।")
                }
            } catch (e: Exception) {
                val msg = e.message ?: "চুক্তি গ্রহণ ব্যর্থ হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun declineDirectContractProposal(
        problemId: String,
        reason: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value ?: run {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- কোনো টাকা স্পর্শ করে না, শুধু status + notification + chat message।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সরাসরি চুক্তি প্রত্যাখ্যান করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া সরাসরি চুক্তি প্রত্যাখ্যান করা যাবে না")
                return@launch
            }
            try {
                val updated = repository.declineDirectContractProposal(problemId, user.id, reason)
                if (updated != null) {
                    _selectedProblem.value = updated
                    showToast("সরাসরি চুক্তি প্রস্তাব প্রত্যাখ্যান করা হয়েছে।")
                    onSuccess()
                } else {
                    onError("চুক্তি প্রত্যাখ্যান করতে সমস্যা হয়েছে।")
                }
            } catch (e: Exception) {
                val msg = e.message ?: "চুক্তি প্রত্যাখ্যান ব্যর্থ হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun reportUser(
        reportedUserId: String,
        reason: String,
        problemId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.reportUser(user.id, reportedUserId, reason, problemId)
            showToast("আপনার রিপোর্ট সফলভাবে জমা হয়েছে। অ্যাডমিন টিম এটি পর্যালোচনা করবে।")
            onSuccess()
        }
    }

    fun markMessagesAsReadForProblem(problemId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markMessagesAsReadForProblem(problemId, user.id)
        }
    }

    // ---------------- KYC SUBMISSION (RULE 10.1 - 10.3) ----------------

    fun submitKyc(
        documentType: String,
        documentNumber: String,
        docFrontUri: String,
        docBackUri: String,
        selfieUri: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onError("অনুগ্রহ করে প্রথমে লগইন করুন।")
            return
        }
        if (documentNumber.isBlank()) {
            onError("ডকুমেন্ট নম্বর পূরণ করা বাধ্যতামূলক।")
            return
        }
        if (docFrontUri.isBlank() || docBackUri.isBlank() || selfieUri.isBlank()) {
            onError("ডকুমেন্টের সামনের ছবি, পেছনের ছবি এবং সেলফি - তিনটি ছবিই আপলোড করতে হবে।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- repository.submitKyc() local Room-এ "pending" বসায়, cloud dual-write ব্যর্থ হলে শুধু log (অফলাইনে admin কখনো আবেদন দেখত না)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া KYC আবেদন জমা দেওয়া যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া KYC আবেদন জমা দেওয়া যাবে না")
                return@launch
            }
            repository.submitKyc(
                user = user,
                firstName = user.name.split(" ").firstOrNull() ?: user.name,
                lastName = user.name.split(" ").drop(1).joinToString(" ").ifBlank { "N/A" },
                address = user.address,
                documentType = documentType,
                documentNumber = documentNumber,
                docFrontUri = docFrontUri,
                docBackUri = docBackUri,
                selfieUri = selfieUri
            )
            showToast("KYC আবেদন পর্যালোচনার জন্য সফলভাবে জমা হয়েছে!")
            onSuccess()
        }
    }

    fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentImage: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (documentNumber.isBlank()) {
            onError("ডকুমেন্ট নম্বর পূরণ করা বাধ্যতামূলক।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- repository.submitKyc() local Room-এ "pending" বসায়, cloud dual-write ব্যর্থ হলে শুধু log (অফলাইনে admin কখনো আবেদন দেখত না)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া KYC আবেদন জমা দেওয়া যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া KYC আবেদন জমা দেওয়া যাবে না")
                return@launch
            }
            repository.submitKyc(
                user = user,
                firstName = user.name.split(" ").firstOrNull() ?: user.name,
                lastName = user.name.split(" ").drop(1).joinToString(" ").ifBlank { "N/A" },
                address = user.address,
                documentType = documentType,
                documentNumber = documentNumber,
                docFrontUri = documentImage ?: "default_doc_front.png",
                docBackUri = "default_doc_back.png",
                selfieUri = user.profileImageUri ?: "default_selfie.png"
            )
            showToast("KYC আবেদন পর্যালোচনার জন্য সফলভাবে জমা হয়েছে!")
            onSuccess()
        }
    }

    fun submitKyc(
        firstName: String,
        lastName: String,
        address: String,
        documentType: String,
        documentNumber: String,
        docFrontUri: String,
        docBackUri: String,
        selfieUri: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (firstName.isBlank() || lastName.isBlank() || address.isBlank() || documentNumber.isBlank()) {
            onError("সকল ফিল্ড পূরণ করা বাধ্যতামূলক।")
            return
        }
        if (docFrontUri.isBlank() || docBackUri.isBlank() || selfieUri.isBlank()) {
            onError("ডকুমেন্টের সামনের ছবি, পেছনের ছবি এবং সেলফি - তিনটি ছবিই আপলোড করতে হবে।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- repository.submitKyc() local Room-এ "pending" বসায়, cloud dual-write ব্যর্থ হলে শুধু log (অফলাইনে admin কখনো আবেদন দেখত না)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া KYC আবেদন জমা দেওয়া যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া KYC আবেদন জমা দেওয়া যাবে না")
                return@launch
            }
            repository.submitKyc(
                user = user,
                firstName = firstName,
                lastName = lastName,
                address = address,
                documentType = documentType,
                documentNumber = documentNumber,
                docFrontUri = docFrontUri,
                docBackUri = docBackUri,
                selfieUri = selfieUri
            )
            showToast("KYC আবেদন পর্যালোচনার জন্য সফলভাবে জমা হয়েছে!")
            onSuccess()
        }
    }

    // ---------------- OTP & PASSWORD RESET ----------------

    private val _otpTarget = MutableStateFlow<String?>(null)
    val otpTarget: StateFlow<String?> = _otpTarget.asStateFlow()

    private val _isSendingOtp = MutableStateFlow(false)
    val isSendingOtp: StateFlow<Boolean> = _isSendingOtp.asStateFlow()

    /**
     * Send real SMS/Email OTP to target with 60s cooldown and 5-min expiry.
     * Never exposes OTP code in client UI state.
     */
    fun sendOtp(
        target: String,
        purpose: String = "verification",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (target.isBlank()) {
            onError("সঠিক মোবাইল নম্বর বা ইমেইল প্রদান করুন।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৫] OTP পাঠানো real network call (SMS/Email গেটওয়ে HTTP,
            // OtpService.requestOtp দ্রষ্টব্য) -- এটাই login/register/forgot-password তিনটা
            // ফ্লো-র একমাত্র নেটওয়ার্ক ট্রিগার পয়েন্ট (নিচের verifyOtp() সম্পূর্ণ local, কোনো
            // guard লাগে না -- OtpService.verifyOtp() নেটওয়ার্ক কল করে না)। sendForgotPasswordOtp()
            // নিজেও শেষে এই ফাংশনই কল করে, তাই আলাদা করে সেখানে guard বসানোর দরকার নেই।
            if (!requireOnlineOrWarn()) {
                onError("ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না")
                return@launch
            }
            _isSendingOtp.value = true
            when (val result = OtpService.requestOtp(target, purpose)) {
                is OtpSendResult.Success -> {
                    _otpTarget.value = target
                    _isSendingOtp.value = false
                    showToast(result.message)
                    onSuccess(result.message)
                }
                is OtpSendResult.RateLimited -> {
                    _isSendingOtp.value = false
                    onError(result.message)
                }
                is OtpSendResult.Error -> {
                    _isSendingOtp.value = false
                    onError(result.message)
                }
            }
        }
    }

    /**
     * Verify the user-entered OTP against the stored challenge without UI leak.
     */
    fun verifyOtp(
        target: String,
        enteredOtp: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (enteredOtp.isBlank() || enteredOtp.length < 6) {
            onError("৬ ডিজিটের সঠিক OTP প্রদান করুন।")
            return
        }

        when (val result = OtpService.verifyOtp(target, enteredOtp)) {
            is OtpVerifyResult.Success -> {
                onSuccess()
            }
            is OtpVerifyResult.InvalidCode -> {
                onError(result.message)
            }
            is OtpVerifyResult.Expired -> {
                onError(result.message)
            }
            is OtpVerifyResult.TooManyAttempts -> {
                onError(result.message)
            }
            is OtpVerifyResult.NotFound -> {
                onError(result.message)
            }
        }
    }

    /**
     * Helper to initiate password recovery OTP flow after checking account existence
     */
    fun sendForgotPasswordOtp(
        phoneOrEmail: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = repository.getUserByPhoneOrEmail(phoneOrEmail.trim(), phoneOrEmail.trim())
            if (user == null) {
                onError("এই ফোন নম্বর বা ইমেইলে কোনো অ্যাকাউন্ট পাওয়া যায়নি।")
                return@launch
            }
            sendOtp(
                target = phoneOrEmail,
                purpose = "password_reset",
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    fun clearOtp(target: String? = null) {
        if (target != null) {
            OtpService.clearOtp(target)
        }
        _otpTarget.value = null
    }

    fun resetPassword(
        phoneOrEmail: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = repository.getUserByPhoneOrEmail(phoneOrEmail.trim(), phoneOrEmail.trim())
            if (user == null) {
                onError("এই ফোন নম্বর বা ইমেইলে কোনো অ্যাকাউন্ট পাওয়া যায়নি।")
                return@launch
            }
            repository.updateUserPassword(user.id, newPass)
            clearOtp(phoneOrEmail)
            showToast("পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে! নতুন পাসওয়ার্ড দিয়ে লগইন করুন।")
            onSuccess()
        }
    }

    // ---------------- WITHDRAWALS ----------------

    fun requestWithdrawal(
        amount: Double,
        method: String,
        accountNumber: String,
        bankName: String? = null,
        branchName: String? = null,
        accountHolderName: String? = null,
        // [Step 12.10d] "SOLVER" (ডিফল্ট) বা "USER" — কোন role-এর ব্যালেন্স থেকে উইথড্র।
        role: String = "SOLVER",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (accountNumber.isBlank()) {
            onError("অ্যাকাউন্ট নম্বর আবশ্যক।")
            return
        }
        if (method == "ব্যাংক" && (bankName.isNullOrBlank() || branchName.isNullOrBlank() || accountHolderName.isNullOrBlank())) {
            onError("ব্যাংকের সকল তথ্য পূরণ করুন।")
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) -- Group A হলেও repository.requestWithdrawal() LOCAL balance INSTANT debit করে (ধাপ ৮-এর adminRefundEscrow-এর precedent; ধাপ ৬-এর গ্যাপ)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া উইথড্র রিকোয়েস্ট করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া উইথড্র রিকোয়েস্ট করা যাবে না")
                return@launch
            }
            val result = repository.requestWithdrawal(
                solver = user,
                amount = amount,
                method = method,
                accountNumber = accountNumber,
                bankName = bankName,
                branchName = branchName,
                accountHolderName = accountHolderName,
                role = role
            )
            result.onSuccess {
                showToast("উইথড্র রিকোয়েস্ট সফলভাবে জমা দেওয়া হয়েছে!")
                onSuccess()
            }.onFailure {
                onError(it.message ?: "উইথড্র ব্যর্থ হয়েছে।")
            }
        }
    }

    // ---------------- GATEWAY PAYMENTS & WALLET DEPOSITS ----------------

    fun depositMoneyViaGateway(
        amount: Double,
        gateway: String,
        gatewayTrxId: String,
        senderPhone: String = "",
        note: String = "",
        onSuccess: (GatewayPaymentEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onError("লগইন করা নেই")
            return
        }
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৬, 🔴 money-critical] ওয়ালেট ডিপোজিট real network call
            // (SomadhanRepository.depositMoneyViaGateway দ্রষ্টব্য) -- কিন্তু সেই ফাংশন balance
            // সাথে সাথে LOCAL-এ (optimistic) যোগ করে দেয়, cloud dual-write শুধু best-effort
            // fire-and-forget (ব্যর্থ হলে শুধু log, কোনো outbox retry queue নেই -- Group A-এ নেই)।
            // rule ২ অনুযায়ী money-critical action কখনো network confirm ছাড়া optimistic-allow করা
            // যাবে না, তাই এখানেই আটকানো হলো (আগে balance বাড়ানো, পরে ব্যর্থ dual-write -- এই
            // ঝুঁকিপূর্ণ প্যাটার্নটা এড়াতে)। onError()-ও কল করা হয়েছে (শুধু toast না) যাতে
            // UserWalletScreen-এর \"লেনদেন যাচাই হচ্ছে\" ডায়ালগ চিরস্থায়ীভাবে লোডিং-এ আটকে না থাকে।
            if (!requireOnlineOrWarn()) {
                onError("ইন্টারনেট সংযোগ ছাড়া ওয়ালেটে টাকা জমা দেওয়া যাবে না")
                return@launch
            }
            val result = repository.depositMoneyViaGateway(
                userId = user.id,
                amount = amount,
                gateway = gateway,
                gatewayTrxId = gatewayTrxId,
                senderPhone = senderPhone,
                note = note
            )
            result.fold(
                onSuccess = { payment ->
                    val freshUser = repository.getUserById(user.id)
                    if (freshUser != null) {
                        _currentUser.value = freshUser
                    } else {
                        // [ব্যালেন্স ফিক্স — ধাপ ১] fallback local echo — depositMoneyViaGateway()
                        // সবসময় addBalanceForUserRole() দিয়ে balance + balanceUser দুটোই একসাথে
                        // বাড়ায় (দেখুন SomadhanRepository.kt, "ওয়ালেট টপ-আপ সবসময় user pool-এ যায়")
                        // — এই fallback-টাও ঠিক একই দুটো কলাম বাড়াচ্ছে, শুধু legacy `balance` না
                        // (MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A)।
                        _currentUser.value = user.copy(
                            balance = user.balance + amount,
                            balanceUser = user.balanceUser + amount
                        )
                    }
                    resetTransactionsPagination(user.id)
                    showToast("৳${DistanceUtil.toBengaliDigits(amount.toInt().toString())} টাকা সফলভাবে ওয়ালেটে যোগ হয়েছে!")
                    onSuccess(payment)
                },
                onFailure = {
                    onError(it.localizedMessage ?: "টাকা যোগ করতে ব্যর্থ হয়েছে")
                }
            )
        }
    }

    // [Offline Action Gating ধাপ ৬] ইচ্ছাকৃতভাবে কোনো requireOnlineOrWarn() গার্ড বসানো হয়নি --
    // কোড পড়ে (SomadhanRepository.recordGatewayPayment) নিশ্চিত হওয়া গেছে এই ফাংশন বর্তমানে
    // সম্পূর্ণ LOCAL-only (একটা আগের বাগফিক্সে `record_gateway_payment_log` RPC dual-write
    // সম্পূর্ণ সরিয়ে ফেলা হয়েছিল, কারণ এটা duplicate-submit guard-এ ধরা পড়ে টাকা যোগ হওয়া
    // আটকে দিত -- রিপোর্টে বিস্তারিত আছে) -- অর্থাৎ এটা এখন আর কোনো নেটওয়ার্ক কল করে না, তাই
    // offline guard-এর প্রয়োজনই নেই। আসল টাকা-নড়াচড়া (escrow payment হলে acceptBid, deposit
    // হলে depositMoneyViaGateway) আলাদাভাবে গার্ড করা হয়েছে।
    fun recordGatewayPayment(
        amount: Double,
        gateway: String,
        gatewayTrxId: String,
        senderPhone: String = "",
        purpose: String = "ESCROW_PAYMENT",
        problemId: String = "",
        problemTitle: String = "",
        status: String = "SUCCESS",
        note: String = "",
        onSuccess: ((GatewayPaymentEntity) -> Unit)? = null
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val p = repository.recordGatewayPayment(
                    userId = user.id,
                    amount = amount,
                    gateway = gateway,
                    gatewayTrxId = gatewayTrxId,
                    senderPhone = senderPhone,
                    purpose = purpose,
                    problemId = problemId,
                    problemTitle = problemTitle,
                    status = status,
                    note = note
                )
                onSuccess?.invoke(p)
            } catch (e: Exception) {
                Log.e("SomadhanViewModel", "Error recording gateway payment", e)
            }
        }
    }

    // [Offline Action Gating ধাপ ৬] ইচ্ছাকৃতভাবে কোনো requireOnlineOrWarn() গার্ড বসানো হয়নি --
    // কোড পড়ে (SomadhanRepository.adminUpdateGatewayPaymentStatus) নিশ্চিত হওয়া গেছে এই ফাংশন
    // এখন সম্পূর্ণ LOCAL-only pure audit/bookkeeping status-editor (আগে চেষ্টা করা
    // `admin_confirm_gateway_deposit` RPC dual-write আসলে কখনো real কিছু করত না বলে সম্পূর্ণ
    // সরিয়ে ফেলা হয়েছিল -- ফাংশনের নিজের comment-এ বিস্তারিত আছে) -- কোনো নেটওয়ার্ক কল নেই, তাই
    // guard-এর প্রয়োজন নেই।
    fun adminUpdateGatewayPaymentStatus(
        paymentId: String,
        newStatus: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.adminUpdateGatewayPaymentStatus(paymentId, newStatus)
                showToast("গেটওয়ে পেমেন্ট স্ট্যাটাস আপডেট হয়েছে: $newStatus")
                onSuccess()
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "স্ট্যাটাস আপডেট ব্যর্থ হয়েছে"
                showToast(err)
                onError(err)
            }
        }
    }

    fun markAllNotificationsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(user.id)
            for (i in notificationsPaged.indices) {
                if (!notificationsPaged[i].isRead) {
                    notificationsPaged[i] = notificationsPaged[i].copy(isRead = true)
                }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
            val index = notificationsPaged.indexOfFirst { it.id == notificationId }
            if (index != -1 && !notificationsPaged[index].isRead) {
                notificationsPaged[index] = notificationsPaged[index].copy(isRead = true)
            }
        }
    }

    // ---------------- ADMIN CONTROLS ----------------

    fun adminApproveKyc(userId: String) {
        viewModelScope.launch {
            repository.adminApproveKyc(userId)
            showToast("KYC অনুমোদিত হয়েছে ✅ (+৫ Reputation)")
        }
    }

    fun adminRejectKyc(userId: String, reason: String) {
        viewModelScope.launch {
            repository.adminRejectKyc(userId, reason)
            showToast("সলভারের KYC আবেদন বাতিল করা হয়েছে।")
        }
    }

    fun adminRevokeKyc(userId: String, reason: String) {
        viewModelScope.launch {
            repository.adminRevokeKyc(userId, reason)
            showToast("KYC ভেরিফিকেশন বাতিল করা হয়েছে")
        }
    }

    fun adminUpdateKycInfo(
        userId: String,
        docNumber: String,
        firstName: String,
        lastName: String,
        address: String
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া KYC তথ্য আপডেট করা যাবে না")) return@launch
            repository.adminUpdateKycInfo(userId, docNumber, firstName, lastName, address)
            showToast("KYC তথ্য আপডেট হয়েছে")
        }
    }

    fun adminBulkApproveKyc(userIds: List<String>) {
        viewModelScope.launch {
            userIds.forEach { repository.adminApproveKyc(it) }
            showToast("${userIds.size}টি আবেদন অনুমোদিত হয়েছে")
        }
    }

    fun adminResetKycToPending(userId: String) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া আবেদন পেন্ডিং-এ নেওয়া যাবে না")) return@launch
            repository.adminResetKycToPending(userId)
            showToast("আবেদন পুনরায় পেন্ডিং তালিকায় নেওয়া হয়েছে")
        }
    }

    fun adminUpdateWithdrawalStatus(withdrawal: WithdrawalEntity, status: String, trxId: String? = null) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) -- Group A হলেও REJECTED হলে LOCAL optimistic refund (addBalanceForSolverRole)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া উইথড্র স্ট্যাটাস পরিবর্তন করা যাবে না")) return@launch
            // [Somadhan Bug-Fix Step 3 — গ্রুপ ২.২] আগে এখানে repository.updateWithdrawalStatus()-এর
            // রিটার্ন-ভ্যালু যাচাই না করেই unconditional "সফল" toast দেখানো হতো -- client-side
            // status-guard (terminal-state/PENDING-only) silent no-op হয়েও "সফল" দেখাতো। এখন
            // repository sealed WithdrawalUpdateResult রিটার্ন করে, তাই conditional toast।
            when (val result = repository.updateWithdrawalStatus(withdrawal, status, trxId)) {
                is WithdrawalUpdateResult.Updated -> {
                    // [Somadhan Bug-Fix Step 5 — গ্রুপ ২.৪] সফল status-update-এর পরও Admin
                    // Withdrawals ট্যাবের ডিফল্ট (unfiltered) `adminWithdrawalsPaged` snapshot
                    // non-reactive থাকায় পুরনো status-এই আটকে থাকত -- এখন resetAdminWithdrawalsPagination()
                    // কল করে সেই snapshot নতুন করে লোড করা হচ্ছে।
                    resetAdminWithdrawalsPagination()
                    showToast("উইথড্র স্ট্যাটাস আপডেট হয়েছে: $status")
                }
                is WithdrawalUpdateResult.GuardBlocked ->
                    showToast(result.reason)
            }
        }
    }

    fun adminUpdateWithdrawalTrxId(withdrawalId: String, newTrxId: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- local-first, admin_update_withdrawal_trx_id RPC best-effort।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া TrxID আপডেট করা যাবে না")) return@launch
            repository.adminUpdateWithdrawalTrxId(withdrawalId, newTrxId)
            showToast("TrxID সফলভাবে আপডেট করা হয়েছে")
        }
    }

    fun adminDeleteProblem(problemId: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- ইনভেন্টরি-গ্যাপ: local delete আগে, admin_soft_delete_problem RPC best-effort।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সমস্যা মুছে ফেলা যাবে না")) return@launch
            repository.deleteProblem(problemId)
            showToast("সমস্যাটি মুছে ফেলা হয়েছে।")
        }
    }

    fun adminDeleteUser(userId: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- userDao.deleteUser() local hard-delete আগে, admin_soft_delete_user RPC best-effort (destructive)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ব্যবহারকারী মুছে ফেলা যাবে না")) return@launch
            repository.deleteUser(userId)
            showToast("ব্যবহারকারী মুছে ফেলা হয়েছে।")
        }
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D২/D৩] তিনটাতেই নতুন ঐচ্ছিক [role]
    // প্যারামিটার ("USER"/"SOLVER"/null) — repository-তে পাস-থ্রু করা হয়। null রাখলে আগের
    // মতোই legacy shared-column আচরণ (AdminUserLookupView এখনো এভাবেই কল করে); AdminUsersView-
    // এর নতুন dual-card অ্যাকশন explicit role পাঠায়।
    fun adminSetBanned(userId: String, banned: Boolean, role: String? = null) {
        viewModelScope.launch {
            repository.adminSetBanned(userId, banned, role)
            showToast(if (banned) "ব্যবহারকারীকে ব্যান করা হয়েছে।" else "ব্যবহারকারীর ব্যান প্রত্যাহার করা হয়েছে।")
        }
    }

    fun adminSetRestricted(userId: String, restricted: Boolean, role: String? = null) {
        viewModelScope.launch {
            repository.adminSetRestricted(userId, restricted, role)
            showToast(if (restricted) "ব্যবহারকারীকে রেস্ট্রিক্ট করা হয়েছে।" else "ব্যবহারকারীর রেস্ট্রিকশন প্রত্যাহার করা হয়েছে।")
        }
    }

    fun adminSetVerifiedBadge(userId: String, verified: Boolean, role: String? = null) {
        viewModelScope.launch {
            repository.adminSetVerifiedBadge(userId, verified, role)
            showToast(if (verified) "ভেরিফাইড ব্যাজ প্রদান করা হয়েছে।" else "ভেরিফাইড ব্যাজ সরানো হয়েছে।")
        }
    }

    fun adminAdjustBalance(userId: String, amount: Double, isAddition: Boolean, reason: String, role: String? = null) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) -- Group A হলেও LOCAL balance INSTANT credit/debit (ধাপ ৮ precedent)। কোনো callback নেই।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ব্যালেন্স সমন্বয় করা যাবে না")) return@launch
            repository.adminAdjustBalance(userId, amount, isAddition, reason, role)
            showToast("ব্যালেন্স সফলভাবে সমন্বয় করা হয়েছে।")
        }
    }

    fun adminChangeRole(userId: String, newRole: String) {
        viewModelScope.launch {
            repository.adminChangeRole(userId, newRole)
            showToast("রোল পরিবর্তন সম্পন্ন হয়েছে: $newRole")
        }
    }

    fun adminResetUserPassword(userId: String, newPlainPassword: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- local bcrypt hash আগে বদলায়,
            // admin-reset-user-password Edge Function (real Supabase Auth sync) শুধু best-effort।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া পাসওয়ার্ড রিসেট করা যাবে না")) return@launch
            repository.adminResetUserPassword(userId, newPlainPassword)
            showToast("পাসওয়ার্ড সফলভাবে রিসেট করা হয়েছে।")
        }
    }

    fun adminAdjustReputation(userId: String, scoreChange: Double, note: String, role: String? = null) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- applyReputationChange() local-first, submit_reputation_event RPC best-effort।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রেপুটেশন সমন্বয় করা যাবে না")) return@launch
            repository.adminAdjustReputation(userId, scoreChange, note, role)
            showToast("রেপুটেশন স্কোর সমন্বয় সম্পন্ন হয়েছে।")
        }
    }

    fun adminUpdateProblemStatus(problemId: String, status: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না, local-first।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া স্ট্যাটাস আপডেট করা যাবে না")) {
                return@launch
            }
            repository.adminUpdateProblemStatus(problemId, status)
            showToast("সমস্যার স্ট্যাটাস আপডেট করা হয়েছে: $status")
        }
    }

    fun adminUpdateProblemBudget(problemId: String, minBudget: Double, maxBudget: Double) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না, local-first।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বাজেট আপডেট করা যাবে না")) {
                return@launch
            }
            repository.adminUpdateProblemBudget(problemId, minBudget, maxBudget)
            showToast("সমস্যার বাজেট সফলভাবে আপডেট করা হয়েছে।")
        }
    }

    fun adminReassignSolver(problemId: String, solverId: String, solverName: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- local-first, admin_reassign_solver + createNotification best-effort।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সমাধানকারী পরিবর্তন করা যাবে না")) return@launch
            repository.adminReassignSolver(problemId, solverId, solverName)
            showToast("সমাধানকারী পরিবর্তন সম্পন্ন হয়েছে: $solverName")
        }
    }

    // [Offline Action Gating ধাপ ৭] কোড পড়ে যাচাই: repository.adminRejectBid()-এ কোনো
    // টাকা/escrow জড়িত না (PENDING বিড reject, accept-এর আগে) -- rule ২ প্রযোজ্য না, কিন্তু
    // সাধারণ Group B network-write (reject_bid RPC, Outbox-এ নেই)। এই ফাংশনে (adminReassignSolver/
    // adminDeleteRating-এর মতোই) কোনো onSuccess/onError callback নেই, তাই ধাপ ৬-এর
    // adminCleanupDuplicateRefunds()-এর মতো সরল প্যাটার্ন: গার্ড ব্যর্থ হলে শুধু return@launch
    // (requireOnlineOrWarn() নিজেই toast দেখায়)।
    fun adminRejectBid(bidId: String) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিড বাতিল করা যাবে না")) return@launch
            repository.adminRejectBid(bidId)
            showToast("বিড বাতিল (Rejected) করা হয়েছে।")
        }
    }

    fun getBidsForProblem(problemId: String): Flow<List<BidEntity>> =
        repository.getBidsForProblem(problemId)

    fun getMessagesForProblem(problemId: String): Flow<List<MessageEntity>> =
        repository.getMessagesForProblem(problemId)

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.adminDeleteRating()
    // local-first ডিলিট (Room delete সরাসরি, RPC dual-write best-effort), ধাপ ৯-এর
    // adminDeleteMessage()-এর মতো একই স্ট্যান্ডার্ড হার্ড-গার্ড প্যাটার্ন।
    fun adminDeleteRating(ratingId: String) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রিভিউ মুছে ফেলা যাবে না")) return@launch
            repository.adminDeleteRating(ratingId)
            showToast("রিভিউ মুছে ফেলা হয়েছে।")
        }
    }

    fun adminDeleteMessage(messageId: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৯] সাধারণ Group B -- repository.adminDeleteMessage()
            // local-first ডিলিট (Room delete সরাসরি, RPC dual-write best-effort), ধাপ ৫-৮-এর
            // স্ট্যান্ডার্ড হার্ড-গার্ড প্যাটার্ন অনুসরণ করে এখানেও guard বসানো হলো।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া মেসেজ মুছে ফেলা যাবে না")) {
                return@launch
            }
            repository.adminDeleteMessage(messageId)
            showToast("মেসেজ মুছে ফেলা হয়েছে।")
        }
    }

    fun adminLogChatView(problemId: String, problemTitle: String, details: String = "") {
        viewModelScope.launch {
            repository.adminLogChatView(problemId, problemTitle, details)
        }
    }

    // [Offline Action Gating ধাপ ১১] নিচের admin config CRUD ফাংশনগুলো (ক্যাটাগরি, FAQ,
    // platform_settings, custom reputation event, KYC info edit) সবগুলোই local-first + best-effort
    // cloud dual-write (Outbox-এ নেই, ইনভেন্টরিতেও ছিল না -- এই ধাপে আবিষ্কৃত গ্যাপ)। কোনোটাতেই টাকা
    // জড়িত না, তাই সবগুলো সাধারণ Group B গার্ড পেল।
    fun adminAddCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া নতুন ক্যাটাগরি যোগ করা যাবে না")) return@launch
            repository.insertCategory(category)
            showToast("নতুন ক্যাটাগরি যুক্ত হয়েছে।")
        }
    }

    fun adminUpdateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ক্যাটাগরি আপডেট করা যাবে না")) return@launch
            repository.adminUpdateCategory(category)
            showToast("ক্যাটাগরি আপডেট করা হয়েছে।")
        }
    }

    fun adminSetPhysicalWorkEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এই সেটিংস পরিবর্তন করা যাবে না")) return@launch
            repository.updatePlatformSetting("physical_categories_enabled", enabled.toString())
            showToast(if (enabled) "ফিজিক্যাল কাজ সক্রিয় করা হয়েছে" else "ফিজিক্যাল কাজ বন্ধ করা হয়েছে")
        }
    }

    fun adminSetVirtualWorkEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এই সেটিংস পরিবর্তন করা যাবে না")) return@launch
            repository.updatePlatformSetting("virtual_categories_enabled", enabled.toString())
            showToast(if (enabled) "ভার্চুয়াল কাজ সক্রিয় করা হয়েছে" else "ভার্চুয়াল কাজ বন্ধ করা হয়েছে")
        }
    }

    fun adminToggleCategoryActive(categoryId: String, isActive: Boolean) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ক্যাটাগরি স্ট্যাটাস পরিবর্তন করা যাবে না")) return@launch
            repository.adminToggleCategoryActive(categoryId, isActive)
            showToast(if (isActive) "ক্যাটাগরি সক্রিয় করা হয়েছে" else "ক্যাটাগরি নিষ্ক্রিয় করা হয়েছে")
        }
    }

    fun adminDeleteCategory(categoryId: String) {
        viewModelScope.launch {
            // deleteCategory() ভেতরে admin_remove_category_from_solvers cascade RPC-ও কল করে --
            // এই একটা গার্ডই দুটোকেই কভার করে।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ক্যাটাগরি মুছে ফেলা যাবে না")) return@launch
            repository.deleteCategory(categoryId)
            showToast("ক্যাটাগরি মুছে ফেলা হয়েছে এবং সম্পর্কিত সলভারদের আপডেট করা হয়েছে।")
        }
    }

    fun adminAddFaq(
        question: String,
        answer: String,
        targetAudience: String = "USER",
        displayOrder: Int = 0,
        isActive: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া নতুন FAQ যোগ করা যাবে না")) return@launch
            repository.addFaq(
                question = question,
                answer = answer,
                targetAudience = targetAudience,
                displayOrder = displayOrder,
                isActive = isActive
            )
            showToast("নতুন FAQ সফলভাবে যুক্ত হয়েছে।")
            onSuccess()
        }
    }

    fun adminUpdateFaq(
        faq: FaqEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া FAQ আপডেট করা যাবে না")) return@launch
            repository.updateFaq(faq)
            showToast("FAQ আপডেট সম্পন্ন হয়েছে।")
            onSuccess()
        }
    }

    fun adminDeleteFaq(
        faqId: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া FAQ মুছে ফেলা যাবে না")) return@launch
            repository.deleteFaqById(faqId)
            showToast("FAQ মুছে ফেলা হয়েছে।")
            onSuccess()
        }
    }

    fun adminSendManualNotification(
        targetRole: String, // "ALL", "USER", "SOLVER"
        title: String,
        message: String,
        targetType: String = "general",
        scheduledFor: Long? = null,
        onSuccess: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (title.isBlank()) {
            onError("বিজ্ঞপ্তির শিরোনাম লিখুন।")
            return
        }
        if (message.isBlank()) {
            onError("বিজ্ঞপ্তির বার্তা লিখুন।")
            return
        }
        val isScheduled = scheduledFor != null && scheduledFor > System.currentTimeMillis()
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- notification insert + broadcast
            // সরাসরি network hit করে (admin_broadcast_notification RPC), Outbox-এ নেই।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিজ্ঞপ্তি পাঠানো যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া বিজ্ঞপ্তি পাঠানো যাবে না")
                return@launch
            }
            try {
                val count = repository.sendManualNotification(
                    targetRole = targetRole,
                    title = title,
                    message = message,
                    targetType = targetType,
                    scheduledFor = if (isScheduled) scheduledFor else null
                )
                if (count > 0) {
                    val roleLabel = when (targetRole.uppercase()) {
                        "USER" -> "সকল গ্রাহক ($count জন)"
                        "SOLVER" -> "সকল সমাধানকারী ($count জন)"
                        else -> "সকল অ্যাকাউন্ট ($count জন)"
                    }
                    if (isScheduled && scheduledFor != null) {
                        val delayMs = (scheduledFor - System.currentTimeMillis()).coerceAtLeast(0L)
                        val workTag = "scheduled_notif_${title.trim().hashCode()}_$scheduledFor"
                        val inputData = workDataOf(
                            ScheduledNotificationWorker.KEY_TITLE to title.trim(),
                            ScheduledNotificationWorker.KEY_MESSAGE to message.trim(),
                            ScheduledNotificationWorker.KEY_ROLE to targetRole,
                            ScheduledNotificationWorker.KEY_TYPE to targetType,
                            ScheduledNotificationWorker.KEY_SCHEDULED_FOR to scheduledFor
                        )
                        val workRequest = OneTimeWorkRequestBuilder<ScheduledNotificationWorker>()
                            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .addTag(workTag)
                            .addTag("scheduled_notification")
                            .build()
                        WorkManager.getInstance(getApplication()).enqueue(workRequest)

                        showToast("$roleLabel-এর জন্য বিজ্ঞপ্তি শিডিউল করা হয়েছে!")
                    } else {
                        showToast("$roleLabel-এর কাছে সফলভাবে বিজ্ঞপ্তি পাঠানো হয়েছে!")
                    }
                    onSuccess(count)
                } else {
                    onError("কোনো প্রাপক অ্যাকাউন্ট পাওয়া যায়নি।")
                }
            } catch (e: Exception) {
                val msg = e.message ?: "বিজ্ঞপ্তি প্রেরণ ব্যর্থ হয়েছে।"
                showToast(msg)
                onError(msg)
            }
        }
    }

    fun adminCancelScheduledNotification(title: String, scheduledFor: Long?, timestamp: Long) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- local delete আগে,
            // admin_delete_notification_group RPC best-effort dual-write।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া শিডিউল করা বিজ্ঞপ্তি বাতিল করা যাবে না")) return@launch
            try {
                repository.deleteScheduledNotification(title, scheduledFor, timestamp)
                if (scheduledFor != null) {
                    val workTag = "scheduled_notif_${title.trim().hashCode()}_$scheduledFor"
                    WorkManager.getInstance(getApplication()).cancelAllWorkByTag(workTag)
                }
                showToast("শিডিউল করা বিজ্ঞপ্তিটি বাতিল করা হয়েছে।")
            } catch (e: Exception) {
                showToast("বিজ্ঞপ্তি বাতিল করতে সমস্যা হয়েছে: ${e.message}")
            }
        }
    }

    fun adminDeleteManualNotification(title: String, timestamp: Long) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- local delete আগে,
            // admin_delete_notification_group RPC best-effort dual-write।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া বিজ্ঞপ্তি মুছে ফেলা যাবে না")) return@launch
            try {
                repository.deleteNotificationGroup(title, timestamp)
                showToast("বিজ্ঞপ্তি হিস্ট্রি থেকে মুছে ফেলা হয়েছে।")
            } catch (e: Exception) {
                showToast("মুছে ফেলতে সমস্যা হয়েছে: ${e.message}")
            }
        }
    }

    /**
     * [ADMIN_ROLE_PROFILE সেশন ২] মাল্টি-এডমিন মডেলে: পাসওয়ার্ড বদল = বর্তমানে লগইন-করা এডমিনের নিজের
     * Supabase Auth পাসওয়ার্ড (আর `admin_credentials`/DataStore-এ লেখা হয় না — লগইন সেগুলো আর পড়ে না)।
     * বর্তমান পাসওয়ার্ড পুনঃযাচাই = একই ফোনে আবার Auth sign-in। ফোন নম্বর লগইন-আইডি (auth.users-এর সাথে
     * বাঁধা) — এখান থেকে বদলানো যায় না ([newPhone] দিলে প্রত্যাখ্যান; UI-তে ফিল্ড নিষ্ক্রিয়)।
     * সম্পূর্ণ প্রোফাইল/পাসওয়ার্ড UI সেশন ৬-এ; এই ফাংশন শুধু বিদ্যমান সেটিংস-স্ক্রিন ভাঙা রোধ করে।
     */
    fun adminUpdateCredentials(
        newPhone: String,
        currentPassword: String,
        newPassword: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ক্রেডেনশিয়াল পরিবর্তন করা যাবে না")) {
                onResult(false, "ইন্টারনেট সংযোগ ছাড়া ক্রেডেনশিয়াল পরিবর্তন করা যাবে না")
                return@launch
            }
            try {
                val account = AdminSession.current?.account
                if (account == null) {
                    onResult(false, "অ্যাডমিন সেশন পাওয়া যায়নি — আবার লগইন করুন।")
                    return@launch
                }
                if (newPhone.isNotBlank()) {
                    onResult(false, "ফোন নম্বর লগইন আইডি — এখান থেকে বদলানো যায় না।")
                    return@launch
                }
                val trimmedNewPassword = newPassword.trim()
                if (trimmedNewPassword.isBlank()) {
                    onResult(false, "নতুন পাসওয়ার্ড লিখুন।")
                    return@launch
                }
                if (trimmedNewPassword.length < 8) {
                    onResult(false, "নতুন পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।")
                    return@launch
                }
                // বর্তমান পাসওয়ার্ড পুনঃযাচাই (একই ব্যবহারকারীর সেশন নবায়ন হয়, ব্যবহারকারী বদলায় না)
                val reauth = SupabaseAuthManager.signInWithPhonePassword(
                    com.example.util.OtpService.normalizeTarget(account.phone), currentPassword
                )
                if (reauth.isFailure) {
                    onResult(false, "বর্তমান পাসওয়ার্ড সঠিক নয়।")
                    return@launch
                }
                val upd = SupabaseAuthManager.updatePassword(trimmedNewPassword)
                if (upd.isFailure) {
                    onResult(false, "পাসওয়ার্ড বদলানো যায়নি: ${upd.exceptionOrNull()?.message ?: "অজানা কারণ"}")
                    return@launch
                }
                showToast("অ্যাডমিন পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে।")
                onResult(true, "পাসওয়ার্ড সফলভাবে আপডেট করা হয়েছে।")
            } catch (e: Exception) {
                onResult(false, e.message ?: "ক্রেডেনশিয়াল আপডেট ব্যর্থ হয়েছে।")
            }
        }
    }

    fun adminFactoryResetAllData(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user?.role != "ADMIN") {
            val errMsg = "শুধুমাত্র অ্যাডমিন অ্যাকাউন্ট থেকে ফ্যাক্টরি রিসেট করা সম্ভব।"
            showToast(errMsg)
            onError(errMsg)
            return
        }

        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] ⚠️ সবসময় সম্পূর্ণ ব্লক (ইউজার কনফার্মড, ধাপ ১) -- clearAllDatabaseAndReset() আগে local DB wipe করে, cloud wipe ব্যর্থ হলে শুধু log; অফলাইনে চালালে local ডেটা হারাত, cloud থাকত।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ফ্যাক্টরি রিসেট করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া ফ্যাক্টরি রিসেট করা যাবে না")
                return@launch
            }
            try {
                _isRefreshing.value = true
                repository.clearAllDatabaseAndReset(getApplication())
                logout()
                _isRefreshing.value = false
                showToast("সমস্ত ডেটা সফলভাবে মুছে অ্যাপ ফ্যাক্টরি রিসেট করা হয়েছে!")
                onSuccess()
            } catch (e: Exception) {
                _isRefreshing.value = false
                val err = e.message ?: "ফ্যাক্টরি রিসেট ব্যর্থ হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    fun adminUpdatePlatformSetting(key: String, value: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B। ⚠️ এই ফাংশন `strict_offline_block`
            // টগলও সেভ করে (AdminSettingsView) -- গার্ড এখানে সঠিক, কারণ অফলাইনে টগল বদলালে শুধু
            // local cache বদলাত (cloud-এ যেত না), পরের login-এ cloud মান আবার লোকাল ওভাররাইট করত।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সেটিংস পরিবর্তন করা যাবে না")) return@launch
            try {
                // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১a] আগে repository.updatePlatformSetting()-এর
                // রিটার্ন-ভ্যালু না থাকায় cloud dual-write ব্যর্থ হলেও unconditional "সফল" toast
                // দেখানো হতো -- এখন sealed PlatformSettingUpdateResult দিয়ে conditional toast
                // (Step 3-এর updateWithdrawalStatus()/WithdrawalUpdateResult precedent অনুসরণ করে)।
                when (val result = repository.updatePlatformSetting(key, value)) {
                    is PlatformSettingUpdateResult.Updated ->
                        showToast("সেটিংস সফলভাবে আপডেট করা হয়েছে")
                    is PlatformSettingUpdateResult.CloudSyncFailed ->
                        showToast("সেটিংস লোকালি সংরক্ষিত হয়েছে, কিন্তু ক্লাউডে সিঙ্ক ব্যর্থ হয়েছে: ${result.reason}")
                }
            } catch (e: Exception) {
                showToast(e.message ?: "সেটিংস আপডেট ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminBatchUpdatePlatformSettings(settings: Map<String, String>, successMessage: String = "রেপুটেশন ইঞ্জিন কনফিগারেশন সফলভাবে সংরক্ষিত হয়েছে") {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া সেটিংস সংরক্ষণ করা যাবে না")) return@launch
            try {
                settings.forEach { (key, value) ->
                    repository.updatePlatformSetting(key, value)
                }
                showToast(successMessage)
            } catch (e: Exception) {
                showToast(e.message ?: "সেটিংস সংরক্ষণ ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminSaveCustomReputationEvent(
        eventKey: String,
        title: String,
        description: String,
        isPositive: Boolean,
        score: Double,
        dailyCap: Double
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ইভেন্ট সংরক্ষণ করা যাবে না")) return@launch
            try {
                val normalizedKey = eventKey.trim().uppercase().replace(" ", "_")
                val existingListStr = allPlatformSettings.value.find { it.key == "rep_custom_events_list" }?.value ?: ""
                val currentKeys = existingListStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                currentKeys.add(normalizedKey)

                val settings = mapOf(
                    "rep_custom_events_list" to currentKeys.joinToString(","),
                    "rep_title_${normalizedKey.lowercase()}" to title,
                    "rep_desc_${normalizedKey.lowercase()}" to description,
                    "rep_type_${normalizedKey.lowercase()}" to isPositive.toString(),
                    "rep_score_${normalizedKey.lowercase()}" to score.toString(),
                    "rep_cap_daily_${normalizedKey.lowercase()}" to dailyCap.toString(),
                    "rep_enabled_${normalizedKey.lowercase()}" to "true",
                    "rep_status_${normalizedKey.lowercase()}" to "ACTIVE"
                )
                settings.forEach { (k, v) -> repository.updatePlatformSetting(k, v) }
                showToast("ইভেন্ট '$title' সফলভাবে কনফিগার ও সক্রিয় করা হয়েছে")
            } catch (e: Exception) {
                showToast(e.message ?: "ইভেন্ট সংরক্ষণ ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminDeleteCustomReputationEvent(eventKey: String) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ইভেন্ট মোছা যাবে না")) return@launch
            try {
                val normalizedKey = eventKey.trim().uppercase()
                val existingListStr = allPlatformSettings.value.find { it.key == "rep_custom_events_list" }?.value ?: ""
                val currentKeys = existingListStr.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != normalizedKey }

                repository.updatePlatformSetting("rep_custom_events_list", currentKeys.joinToString(","))
                repository.updatePlatformSetting("rep_enabled_${normalizedKey.lowercase()}", "false")
                repository.updatePlatformSetting("rep_status_${normalizedKey.lowercase()}", "DELETED")
                showToast("ইভেন্ট রুল সফলভাবে মুছে ফেলা হয়েছে")
            } catch (e: Exception) {
                showToast(e.message ?: "ইভেন্ট মোছা ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminToggleCustomReputationEventStatus(eventKey: String, isEnabled: Boolean) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া স্ট্যাটাস পরিবর্তন করা যাবে না")) return@launch
            try {
                val normalizedKey = eventKey.trim().uppercase()
                repository.updatePlatformSetting("rep_enabled_${normalizedKey.lowercase()}", isEnabled.toString())
                repository.updatePlatformSetting("rep_status_${normalizedKey.lowercase()}", if (isEnabled) "ACTIVE" else "PAUSED")
                showToast(if (isEnabled) "ইভেন্ট সক্রিয় করা হয়েছে" else "ইভেন্ট স্থগিত করা হয়েছে")
            } catch (e: Exception) {
                showToast(e.message ?: "স্ট্যাটাস পরিবর্তন ব্যর্থ হয়েছে")
            }
        }
    }

    fun triggerDynamicReputationEvent(
        userId: String,
        eventType: String,
        problemId: String? = null,
        defaultScore: Double = 0.5,
        defaultCap: Double = 2.0,
        defaultPenalty: Double = 1.0,
        isPositive: Boolean = true,
        customNote: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.triggerDynamicReputationEvent(
                    userId = userId,
                    eventType = eventType,
                    problemId = problemId,
                    defaultScore = defaultScore,
                    defaultCap = defaultCap,
                    defaultPenalty = defaultPenalty,
                    isPositive = isPositive,
                    customNote = customNote
                )
            } catch (_: Exception) {}
        }
    }

    fun adminRunMonthlyFreeQuotaReset() {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না, শুধু
            // freeJobsUsedThisMonth কাউন্টার রিসেট, local-first। (নোট: repository.runMonthlyFreeQuotaReset()
            // ScheduledNotificationWorker.kt থেকেও ব্যাকগ্রাউন্ডে কল হয় -- সেই পথ এই গার্ডের বাইরে,
            // অপরিবর্তিত, ইচ্ছাকৃতভাবে -- শুধু এই সরাসরি admin-button entry point-টাই গার্ড করা হলো।)
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া মাসিক কোটা রিসেট করা যাবে না")) {
                return@launch
            }
            try {
                repository.runMonthlyFreeQuotaReset()
                showToast("সকল সলভারের মাসিক ফ্রি কোটা সফলভাবে রিসেট করা হয়েছে")
            } catch (e: Exception) {
                showToast(e.message ?: "মাসিক কোটা রিসেট ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminResetSolverFreeQuota(solverId: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না, local-first।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া কোটা রিসেট করা যাবে না")) {
                return@launch
            }
            try {
                repository.adminResetSolverFreeQuota(solverId)
                showToast("সলভারের ব্যবহৃত ফ্রি কোটা ০-এ রিসেট করা হয়েছে")
            } catch (e: Exception) {
                showToast(e.message ?: "কোটা রিসেট ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminResetSolverMissCycle(solverId: String) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৮] সাধারণ Group B -- কোনো টাকা জড়িত না, local-first।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া মিস সাইকেল রিসেট করা যাবে না")) {
                return@launch
            }
            try {
                repository.adminResetSolverMissCycle(solverId)
                showToast("সলভারের মিস সাইকেল রিসেট করা হয়েছে")
            } catch (e: Exception) {
                showToast(e.message ?: "মিস সাইকেল রিসেট ব্যর্থ হয়েছে")
            }
        }
    }

    suspend fun searchSolverForQuota(query: String): UserEntity? {
        return repository.searchSolverDirectFromCloudOrLocal(query)
    }

    fun logAdminAction(
        actionType: String,
        targetId: String,
        targetName: String,
        details: String = ""
    ) {
        viewModelScope.launch {
            repository.logAdminCustomAction(actionType, targetId, targetName, details)
        }
    }

    // ---------------- PUBLIC PROFILE ----------------

    fun getPublicUserFlow(userId: String): Flow<UserEntity?> {
        return repository.getAllUsers().map { users -> users.find { it.id == userId } }
    }

    fun getUserByIdFlow(userId: String): Flow<UserEntity?> = repository.getUserByIdFlow(userId)

    fun getRatingsForUserFlow(userId: String): Flow<List<RatingEntity>> {
        return repository.getAllRatingsReceivedByPerson(userId)
    }

    fun getRatingsGivenBySolverFlow(solverId: String): Flow<List<RatingEntity>> {
        return repository.getRatingsGivenBySolver(solverId)
    }

    suspend fun getPublicUserById(userId: String): UserEntity? {
        return repository.getUserById(userId)
    }

    // ---------------- ESCROW ----------------

    fun getEscrowForProblem(problemId: String): Flow<EscrowEntity?> {
        return repository.getEscrowForProblem(problemId)
    }

    // [Offline Action Gating ধাপ ৮] 🔴 money-critical (rule ২, "escrow release/refund" নামোল্লেখ
    // করা আছে) -- ইনভেন্টরি-গ্যাপ: `release_escrow`/`refund_escrow_once` RPC দুটো ধাপ ১-এর
    // ইনভেন্টরিতে Group A (Outbox-retry কভার্ড) হিসেবে চিহ্নিত ছিল, অর্থাৎ মাস্টার প্রম্পটের
    // পরিকল্পনা অনুযায়ী ধাপ ১২-এ শুধু ভেরিফাই হওয়ার কথা, কোড পরিবর্তন না। কিন্তু কোড পড়ে দেখা
    // গেছে repository.adminReleaseEscrow()/adminRefundEscrow() উভয়ই ধাপ ৭-এর acceptBid()/
    // solverCancelJob()-এর হুবহু একই প্যাটার্নে LOCAL optimistic wallet write করে (payoutEscrowToSolver()/
    // refundEscrowOnce()) -- RPC dual-write শুধু best-effort, কখনো throw করে না। rule ২ স্পষ্টভাবে
    // "escrow release/refund"-কে money-critical বলে নাম নিয়ে উল্লেখ করেছে এবং বলেছে এটা
    // toggle-independent, কখনো offline optimistic allow করা যাবে না -- এটা Group A-এর "শুধু retry,
    // ব্লক না" নীতির চেয়ে বেশি কঠোর নিয়ম, আর rule ২-ই অগ্রাধিকার পায় (মাস্টার প্রম্পটে rule ২
    // স্পষ্টভাবে "toggle-independent" বলে জোর দেওয়া আছে)। তাই এখানেই এই গ্যাপ বন্ধ করা হলো --
    // ঠিক যেমন ধাপ ৭-এ acceptInstantJobBid()-এর গ্যাপ বন্ধ হয়েছিল। এই গার্ড শুধু সম্পূর্ণ-অফলাইন
    // অবস্থায় অ্যাকশন শুরু হওয়া আটকায় -- release_escrow/refund_escrow_once RPC এখনো Group A-ই
    // থাকছে (transient network glitch-এ Outbox retry এখনো কাজ করবে online অবস্থায়), শুধু guard
    // toggle OFF + সম্পূর্ণ অফলাইন কম্বিনেশনে পুরো অ্যাকশনটাই ব্লক করে। মূল স্কোপ (Problem
    // posting/Instant jobs) থেকে সামান্য বাইরে হলেও rule ১-এর "কোনো গ্যাপ রেখে দেওয়া যাবে না"
    // চেতনায় একসাথে ফিক্স করা হলো (ধাপ ৭-এর acceptInstantJobBid গ্যাপ-ফিক্সের মতোই)।
    fun adminReleaseEscrow(escrow: EscrowEntity) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এসক্রো রিলিজ করা যাবে না")) {
                return@launch
            }
            try {
                repository.adminReleaseEscrow(escrow)
                val totalAmount = Math.round(escrow.baseAmount + escrow.extraAmount)
                refreshCurrentUser()
                showToast("টাকা সফলভাবে ছেড়ে দেওয়া হয়েছে (৳${DistanceUtil.toBengaliDigits(totalAmount.toString())})!")
            } catch (e: Exception) {
                showToast(e.message ?: "এসক্রো রিলিজ ব্যর্থ হয়েছে")
            }
        }
    }

    // [Offline Action Gating ধাপ ৮] 🔴 money-critical (rule ২) -- একই কারণ ও একই সিদ্ধান্ত যেমন
    // উপরের adminReleaseEscrow()-এ বর্ণিত হয়েছে (রিফান্ড পাশ, refundEscrowOnce())।
    // admin_refund_and_reopen_problem RPC-টাও (best-effort, problem/bid reset) একই গার্ডের অধীনে
    // পড়ে যায়, আলাদা করে হ্যান্ডল করার দরকার নেই।
    fun adminRefundEscrow(escrow: EscrowEntity) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এসক্রো রিফান্ড করা যাবে না")) {
                return@launch
            }
            try {
                repository.adminRefundEscrow(escrow)
                val totalAmount = Math.round(escrow.baseAmount + escrow.extraAmount)
                refreshCurrentUser()
                showToast("টাকা সফলভাবে ফেরত দেওয়া হয়েছে (৳${DistanceUtil.toBengaliDigits(totalAmount.toString())})!")
            } catch (e: Exception) {
                showToast(e.message ?: "এসক্রো রিফান্ড ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminCleanupDuplicateRefunds(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৬, 🔴 money-critical] cleanupDuplicateRefunds()-এর কোনো
            // dryRun নেই -- কল হলেই সাথে সাথে LOCAL ব্যালেন্স সংশোধন করে (optimistic), cloud
            // dual-write শুধু best-effort fire-and-forget (SomadhanRepository.cleanupDuplicateRefunds
            // দ্রষ্টব্য)। rule ২ অনুযায়ী এখানেই আটকানো হলো। এই ফাংশনের বিদ্যমান catch(Exception)
            // ব্লকও `onComplete()` কল করে না (isCleaningDuplicates স্পিনার তখনও আটকে থাকার মতোই
            // pre-existing আচরণ, AdminTransactionsView.kt দ্রষ্টব্য) -- সামঞ্জস্য রাখতে এই গার্ডও
            // ইচ্ছাকৃতভাবে একই আচরণ অনুসরণ করছে (শুধু toast, onComplete কল হয় না); নতুন করে এই
            // pre-existing gap ফিক্স করা এই ধাপের স্কোপের বাইরে।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ডুপ্লিকেট রিফান্ড ক্লিনআপ করা যাবে না")) {
                return@launch
            }
            try {
                val count = repository.cleanupDuplicateRefunds()
                refreshCurrentUser()
                if (count > 0) {
                    showToast("$count টি ডুপ্লিকেট রিফান্ড সফলভাবে মুছে ফেলা হয়েছে ও ব্যালেন্স সমন্বয় করা হয়েছে।")
                } else {
                    showToast("কোনো ডুপ্লিকেট রিফান্ড পাওয়া যায়নি। সব ডাটা সঠিক আছে।")
                }
                onComplete(count)
            } catch (e: Exception) {
                showToast(e.message ?: "ক্লিনআপ ব্যর্থ হয়েছে")
            }
        }
    }

    fun adminRepairMissingRefunds(
        dryRun: Boolean = true,
        onComplete: (MissingRefundRepairReport) -> Unit = {}
    ) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৬, 🔴 money-critical] শুধু live-run (dryRun = false)
            // গার্ড করা হলো -- সেটাই সাথে সাথে LOCAL ব্যালেন্স ক্রেডিট করে (optimistic), cloud
            // dual-write শুধু best-effort (SomadhanRepository.repairMissingRefunds দ্রষ্টব্য),
            // rule ২ প্রযোজ্য। dryRun = true (অডিট-অনলি, কোনো ডাটা পরিবর্তন হয় না, শুধু স্ক্যান)
            // ইচ্ছাকৃতভাবে অফলাইনেও চলতে দেওয়া হলো -- local Room স্ক্যান, কোনো money movement না।
            // [Somadhan Bug-Fix Step 4 আপডেট] নিচের catch(Exception) ব্লক এখন `onComplete()` কল
            // করে (আগে করতো না, isRepairRunning স্পিনার চিরস্থায়ী আটকে থাকতো -- দেখুন নিচের ব্লকের
            // কমেন্ট)।
            if (!dryRun && !requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রিফান্ড রিপেয়ার করা যাবে না")) {
                return@launch
            }
            try {
                val report = repository.repairMissingRefunds(dryRun)
                refreshCurrentUser()
                if (dryRun) {
                    if (report.missingRefundCount > 0) {
                        showToast("অডিট সম্পন্ন: ${report.missingRefundCount}টি মিসিং রিফান্ড শনাক্ত হয়েছে (মোট ৳${report.totalAmountRepairedOrAudited.toInt()})")
                    } else {
                        showToast("অডিট সম্পন্ন: কোনো মিসিং রিফান্ড পাওয়া যায়নি ✅")
                    }
                } else {
                    if (report.repairedCount > 0) {
                        showToast("রিপেয়ার সফল: ${report.repairedCount}টি রিফান্ড পুনরুদ্ধার করা হয়েছে 🎉")
                    } else {
                        showToast("কোনো রিফান্ড প্রয়োজন হয়নি বা ডাটা ইতিমধ্যে সঠিক আছে।")
                    }
                }
                onComplete(report)
            } catch (e: Exception) {
                showToast("রিপেয়ার প্রসেসে ত্রুটি: ${e.message}")
                // [Somadhan Bug-Fix Step 4, গ্রুপ ২.৩] আগে এখানে onComplete() কল হতো না, ফলে
                // AdminEscrowView.kt-এর isRepairRunning স্পিনার চিরস্থায়ী আটকে থাকতো (সেই flag
                // শুধু onComplete callback-এর ভেতরেই false হয়)। এখন একটা zero/no-op রিপোর্ট দিয়ে
                // onComplete() কল করে UI-স্তরের running-state রিসেট করা হচ্ছে -- এরর টোস্ট আগেই
                // দেখানো হয়েছে, তাই admin বিভ্রান্ত হবেন না, শুধু বাটন আবার ব্যবহারযোগ্য হবে।
                onComplete(
                    MissingRefundRepairReport(
                        dryRun = dryRun,
                        scannedEscrowsCount = 0,
                        missingRefundCount = 0,
                        repairedCount = 0,
                        totalAmountRepairedOrAudited = 0.0,
                        items = emptyList()
                    )
                )
            }
        }
    }

    fun adminReconcileBalances(
        dryRun: Boolean = true,
        onComplete: (BalanceReconciliationReport) -> Unit = {}
    ) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ৬, 🔴 money-critical] শুধু live-run (dryRun = false)
            // গার্ড করা হলো -- একই যুক্তি adminRepairMissingRefunds()-এর মতো (উপরে দেখুন): live-run
            // সাথে সাথে LOCAL ব্যালেন্স সংশোধন করে (optimistic), cloud dual-write শুধু best-effort
            // (SomadhanRepository.reconcileUserBalances দ্রষ্টব্য), rule ২ প্রযোজ্য। dryRun = true
            // (অডিট-অনলি) অফলাইনেও চলতে দেওয়া হলো। [Somadhan Bug-Fix Step 4 আপডেট] নিচের
            // catch(Exception) ব্লক এখন `onComplete()` কল করে (আগে করতো না -- দেখুন নিচের ব্লকের
            // কমেন্ট)।
            if (!dryRun && !requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ব্যালেন্স রিকনসিলিয়েশন করা যাবে না")) {
                return@launch
            }
            try {
                val report = repository.reconcileUserBalances(dryRun)
                refreshCurrentUser()
                if (dryRun) {
                    if (report.mismatchCount > 0) {
                        showToast("অডিট সম্পন্ন: ${report.scannedUsersCount} জন ব্যবহারকারীর মধ্যে ${report.mismatchCount}টি ব্যালেন্স গরমিল শনাক্ত হয়েছে (মোট ৳${report.totalAbsoluteDifference.toInt()})")
                    } else {
                        showToast("অডিট সম্পন্ন: ${report.scannedUsersCount} জন ব্যবহারকারীর সবার ব্যালেন্স সঠিক আছে ✅")
                    }
                } else {
                    if (report.correctedCount > 0) {
                        showToast("সংশোধন সফল: ${report.correctedCount}টি ব্যালেন্স লেজার অনুযায়ী ঠিক করা হয়েছে 🎉")
                    } else {
                        showToast("কোনো সংশোধন প্রয়োজন হয়নি — সব ব্যালেন্স ইতিমধ্যে সঠিক আছে।")
                    }
                }
                onComplete(report)
            } catch (e: Exception) {
                showToast("রিকনসিলিয়েশন প্রসেসে ত্রুটি: ${e.message}")
                // [Somadhan Bug-Fix Step 4, গ্রুপ ২.৩] adminRepairMissingRefunds()-এর মতোই একই কারণে
                // -- এখন zero/no-op রিপোর্ট দিয়ে onComplete() কল করে isReconcileRunning স্পিনার
                // রিসেট করা হচ্ছে (আগে এখানে onComplete() কলই ছিল না)।
                onComplete(
                    BalanceReconciliationReport(
                        dryRun = dryRun,
                        scannedUsersCount = 0,
                        mismatchCount = 0,
                        correctedCount = 0,
                        totalAbsoluteDifference = 0.0,
                        items = emptyList()
                    )
                )
            }
        }
    }

    // ---------------- ADDITIONAL CHARGES ----------------

    // [Offline Action Gating ধাপ ১০] সাধারণ Group B -- repository.requestAdditionalCharge()
    // কোনো টাকা স্পর্শ করে না (শুধু একটা PENDING charge record তৈরি করে + ক্লায়েন্টকে
    // notification/chat-notice পাঠায়), আসল ব্যালেন্স/এসক্রো পরিবর্তন হয় respondToAdditionalCharge()-এ
    // (accept করলে)।
    fun requestAdditionalCharge(
        problem: ProblemEntity,
        reason: String,
        amount: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val solverId = _currentUser.value?.id ?: run {
            onError("অনুগ্রহ করে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিলের অনুরোধ পাঠানো যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া অতিরিক্ত বিলের অনুরোধ পাঠানো যাবে না")
                return@launch
            }
            try {
                repository.requestAdditionalCharge(problem, solverId, reason, amount)
                showToast("অতিরিক্ত বিলের অনুরোধ সফলভাবে পাঠানো হয়েছে!")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "অতিরিক্ত বিল যোগ করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    fun respondToAdditionalCharge(
        charge: AdditionalChargeEntity,
        accept: Boolean,
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Bug fix (loading-lock groundwork): this was pure fire-and-forget -- the calling screen
        // had no way to know when the accept/reject actually finished (or whether it threw),
        // which is exactly what made a double-tap on this button possible in the first place.
        // Every other action function in this file (acceptBid, solverCancelJob, raiseDispute,
        // etc.) already takes an onSuccess/onDone callback for this reason; this one didn't.
        //
        // [Offline Action Gating ধাপ ১০] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) --
        // কোড পড়ে (rule ১০) নিশ্চিত হওয়া গেছে accept=true হলে repository.respondToAdditionalCharge()
        // LOCAL wallet deduction (deductBalanceForUserRole) + escrow-এ extra amount যোগ (addToEscrow) --
        // উভয়ই optimistic Room write, ঠিক userConfirmExtraAmount()-এর একই প্যাটার্ন;
        // `respond_additional_charge` RPC শুধু best-effort dual-write। reject (accept=false)
        // শাখায় কোনো টাকা নড়ে না, কিন্তু guard আগে থেকেই বসানো হলো (accept/reject নির্বিশেষে,
        // cancelInstantJob()-এর মতোই যেখানে শুধু conditionally money move করলেও ফাংশন-লেভেলে
        // সবসময় গার্ড থাকে)।
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এই অনুরোধ প্রসেস করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া এই অনুরোধ প্রসেস করা যাবে না")
                return@launch
            }
            try {
                repository.respondToAdditionalCharge(charge, accept)
                onDone()
            } catch (e: Exception) {
                val err = e.message ?: "অনুরোধটি প্রসেস করতে সমস্যা হয়েছে।"
                showToast(err)
                onError(err)
            }
        }
    }

    fun getPendingAdditionalCharges(problemId: String): Flow<List<AdditionalChargeEntity>> =
        repository.getPendingAdditionalCharges(problemId)

    fun getAllAdditionalCharges(problemId: String): Flow<List<AdditionalChargeEntity>> =
        repository.getAllAdditionalCharges(problemId)

    // ---------------- REPUTATION EVENTS ----------------

    fun getRecentReputationEvents(userId: String, limit: Int = 20): Flow<List<com.example.data.entity.ReputationEventEntity>> =
        repository.getRecentReputationEvents(userId, limit)

    fun getReputationHistory(userId: String): Flow<List<com.example.data.entity.ReputationEventEntity>> =
        repository.getReputationHistory(userId)

    suspend fun getSumScoreChangeForProblem(
        userId: String,
        eventTypes: List<String>,
        problemId: String
    ): Double = repository.getSumScoreChangeForProblem(userId, eventTypes, problemId)

    fun reportAbuse(
        reportedUserId: String,
        reason: String,
        details: String = "",
        contextType: String? = null,
        relatedProblemId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val reporterId = _currentUser.value?.id ?: "ANONYMOUS"
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- repository.reportAbuse() শুধু
            // logAdminAction() (RPC) দিয়ে admin-এ পৌঁছায়, অফলাইনে সেটা নীরবে হারিয়ে যেত (onError নেই,
            // শুধু onSuccess) -- তাই ব্যর্থ হলে toast + return@launch।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া রিপোর্ট পাঠানো যাবে না")) return@launch
            try {
                repository.reportAbuse(
                    reporterId = reporterId,
                    reportedUserId = reportedUserId,
                    reason = reason,
                    details = details,
                    contextType = contextType,
                    relatedProblemId = relatedProblemId
                )
                showToast("আপনার রিপোর্টটি সফলভাবে জমা হয়েছে। অ্যাডমিন টিম এটি পর্যালোচনা করবে।")
                onSuccess()
            } catch (e: Exception) {
                showToast("রিপোর্ট পাঠাতে সমস্যা হয়েছে: ${e.message}")
            }
        }
    }

    fun adminUpdateDirectContractStatus(
        problemId: String,
        status: String,
        directContractStatus: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) -- COMPLETED হলে confirmReleaseAndComplete() (escrow payout), CANCELLED হলে adminCancelAndRefundDirectContract() (refund) কল করে -- ধাপ ১০-এ আবিষ্কৃত choke-point।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া চুক্তির স্ট্যাটাস পরিবর্তন করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া চুক্তির স্ট্যাটাস পরিবর্তন করা যাবে না")
                return@launch
            }
            try {
                repository.adminUpdateDirectContractStatus(problemId, status, directContractStatus)
                refreshCurrentUser()
                showToast("চুক্তির স্ট্যাটাস পরিবর্তন করা হয়েছে: $status")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "স্ট্যাটাস পরিবর্তন ব্যর্থ হয়েছে"
                showToast(err)
                onError(err)
            }
        }
    }

    fun adminCancelAndRefundDirectContract(
        problemId: String,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] 🔴 rule ২ (money-critical, সবসময়, toggle-independent) -- HELD escrow থাকলে refundEscrowOnce() দিয়ে LOCAL optimistic refund করে।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া চুক্তি বাতিল ও রিফান্ড করা যাবে না")) {
                onError("ইন্টারনেট সংযোগ ছাড়া চুক্তি বাতিল ও রিফান্ড করা যাবে না")
                return@launch
            }
            try {
                repository.adminCancelAndRefundDirectContract(problemId, reason)
                refreshCurrentUser()
                showToast("ডাইরেক্ট চুক্তি বাতিল ও রিফান্ড সম্পন্ন হয়েছে")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "বাতিল ও রিফান্ড ব্যর্থ হয়েছে"
                showToast(err)
                onError(err)
            }
        }
    }

    fun adminEscalateDirectContract(
        problemId: String,
        note: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.adminEscalateDirectContract(problemId, note)
                showToast("এসকেলেশন নোট সংরক্ষণ করা হয়েছে")
                onSuccess()
            } catch (e: Exception) {
                val err = e.message ?: "সংরক্ষণ ব্যর্থ হয়েছে"
                showToast(err)
                onError(err)
            }
        }
    }

    fun adminCleanupCorruptedCommissionRates(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            // [Offline Action Gating ধাপ ১১] সাধারণ Group B -- Postgrest update direct network call,
            // কোনো টাকা জড়িত না (শুধু OPEN জবের একটা data-cleanup ফিল্ড রিসেট করে)।
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া ক্লিনআপ করা যাবে না")) return@launch
            try {
                val count = repository.cleanupCorruptedCommissionRates()
                showToast("ক্লিনআপ সম্পন্ন: $count টি OPEN জবের কমিশন রেট ঠিক করা হয়েছে ✅")
                onComplete(count)
            } catch (e: Exception) {
                showToast("ক্লিনআপ ব্যর্থ হয়েছে: ${e.message}")
            }
        }
    }

    // [Offline Action Gating ধাপ ৮] 🔴 money-critical (rule ২)। কোড পড়ে যাচাই করা হয়েছে:
    // repository.adminForceCancelInstantJob() hadAcceptedSolver (escrow HELD) থাকলে REBROADCAST/
    // TO_NORMAL_BIDDING/CANCEL তিনটা targetAction-এই একই refundEscrowOnce() পাথে যায় (`when` ব্লকের
    // আগেই) -- তাই পুরো ফাংশনটাই money-adjacent, targetAction নির্বিশেষে গার্ড প্রযোজ্য।
    fun adminForceCancelInstantJob(
        problemId: String,
        reason: String,
        targetAction: String = "CANCEL",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এই অ্যাডমিন অ্যাকশন করা যাবে না")) {
                return@launch
            }
            try {
                // [Step 7.9] repository এখন refund_pending (Boolean) ফেরত দেয় — owner-এর USER role
                // নিষ্ক্রিয় থাকায় escrow refund করা যায়নি এমন ক্ষেত্রে (RPC Step 7.2), আগে এই তথ্যটা
                // admin-কে কোথাও জানানো হতো না।
                val refundPending = repository.adminForceCancelInstantJob(problemId, reason, targetAction)
                if (refundPending) {
                    showToast("অ্যাডমিন অ্যাকশন সফল হয়েছে, কিন্তু ইউজারের অ্যাকাউন্ট নিষ্ক্রিয় থাকায় রিফান্ড আটকে আছে — অ্যাকাউন্ট সক্রিয় হলে স্বয়ংক্রিয়ভাবে রিফান্ড হবে")
                } else {
                    showToast("জরুরি জবের উপর অ্যাডমিন অ্যাকশন সফল হয়েছে")
                }
                onSuccess()
            } catch (e: Exception) {
                showToast("অ্যাকশন ব্যর্থ হয়েছে: ${e.message}")
            }
        }
    }

    fun adminToggleCategoryInstantJob(categoryId: String, enabled: Boolean, radiusKm: Double? = null) {
        viewModelScope.launch {
            if (!requireOnlineOrWarn("ইন্টারনেট সংযোগ ছাড়া এই সেটিংস পরিবর্তন করা যাবে না")) return@launch
            try {
                val cat = repository.getCategoryById(categoryId) ?: return@launch
                val updated = cat.copy(
                    instantJobEnabled = enabled,
                    instantJobRadiusKm = radiusKm ?: cat.instantJobRadiusKm
                )
                repository.updateCategory(updated)
                showToast("ক্যাটাগরি জরুরি জব সেটিংস আপডেট করা হয়েছে")
            } catch (e: Exception) {
                showToast("আপডেট ব্যর্থ হয়েছে: ${e.message}")
            }
        }
    }
}


