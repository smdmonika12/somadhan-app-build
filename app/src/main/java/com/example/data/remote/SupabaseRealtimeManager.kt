package com.example.data.remote

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.EscrowEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.data.remote.dto.AdditionalChargeDto
import com.example.data.remote.dto.BidDto
import com.example.data.remote.dto.EscrowDto
import com.example.data.remote.dto.GatewayPaymentDto
import com.example.data.remote.dto.MessageDto
import com.example.data.remote.dto.NotificationDto
import com.example.data.remote.dto.ProblemDto
import com.example.data.remote.dto.TransactionDto
import com.example.data.remote.dto.UserDto
import com.example.data.remote.dto.WithdrawalDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ২০-২১ (Realtime Foundation A+B)
 *
 * `FirebaseSyncManager.kt`-এর `startRealtimeListeners()` (বিশেষত `handleUsersSnapshot`/
 * `handleProblemsSnapshot`/`handleBidsSnapshot`/`handleMessagesSnapshot`/
 * `handleTransactionsSnapshot`/`handleEscrowsSnapshot`/`handleGatewayPaymentsSnapshot` + তাদের
 * `mergeAndSave*` ফাংশন, এবং `typingListener`/`setTypingStatus()`) এই ফাইলের model — একই আচরণ
 * (conflict resolution, DELETE হ্যান্ডলিং, typing সিগন্যাল) এখানে Supabase Postgres Changes +
 * Realtime Broadcast দিয়ে রেপ্লিকেট করা হয়েছে, `users`/`problems`/`bids` (ধাপ ২০) এবং
 * `messages`/`transactions`/`escrows`/`gateway_payments` + typing broadcast (ধাপ ২১) — এই ৭টা
 * টেবিল + typing সিগন্যালের জন্য।
 *
 * **ধাপ ৩৪ (ব্যবহারকারীর অনুরোধে) — ৮ম টেবিল `additional_charges` যোগ হলো।** solver extra-bill
 * request করলে সেটা আগে customer-এর app খোলা অবস্থায় কোথাও লাইভ দেখা যেত না (না
 * job-tracking/problem-detail স্ক্রিনে, না চ্যাটে) -- এখন এই টেবিলও realtime publication-এ আছে
 * (`request_additional_charge()` RPC-এর সাথে একই migration-এ, `MIGRATION_PROGRESS.md`/
 * `supabase/migrations/step34_...sql` দেখুন), আর সেই একই RPC এখন `system_event_message()` RPC
 * (ধাপ ৩২.৯৫)-ও কল করে, তাই `messages` টেবিলেও (আগে থেকেই realtime) একটা system-event bubble
 * চলে আসে -- দুটো পথেই customer সাথে সাথে জানতে পারবে।
 *
 * **ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে) — ৯ম/১০ম টেবিল `notifications`/`withdrawals` যোগ হলো।** এই
 * দুইটা টেবিল আগে সম্পূর্ণ realtime-এর বাইরে ছিল (শুধু write-time-এ local Room-এ লেখা হতো, আর
 * app restart/pull-to-refresh-এ bulk-pull হতো) -- এখন থেকে `pullBulkDataFromSupabase()`-এর
 * স্কোপেও আছে এবং নিজস্ব realtime channel আছে (`supabase/migrations/
 * step35_notifications_withdrawals_realtime.sql` দেখুন)। `notifications`-এর DELETE case-টা
 * defensive না (অন্য টেবিলের মতো) -- `admin_delete_notification_group` RPC সত্যিই row মুছে দেয়।
 *
 * **ধাপ ২২ (Realtime Cutover, Dual-Run) থেকে এই ফাইল সত্যিই wire করা হয়েছে** — `attachDatabase()`
 * এখন `FirebaseSyncManager.attachDatabase()`-এর মতোই attach হওয়ার সাথে সাথে একবার bulk-pull
 * ([pullBulkDataFromSupabase]) + `startRealtimeListeners()` চালায়, আর `SomadhanApp.kt`/
 * `SomadhanViewModel.kt`/`AdminPanelScreen.kt`-এ `FirebaseSyncManager`-এর সংশ্লিষ্ট কলগুলোর
 * **পাশে** (dual-run, প্রতিস্থাপন না) এই ম্যানেজারের কল যোগ করা হয়েছে। বিস্তারিত:
 * `MIGRATION_PROGRESS.md`-এ "ধাপ ২২" এন্ট্রি (race-condition/idempotency বিশ্লেষণ, কোন কল-সাইট
 * ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছে ও কেন)।
 *
 * **Realtime Scoping ফিক্স, ধাপ ১ (সম্পূর্ণ আলাদা, স্বতন্ত্র কাজ — `somadhan-realtime-scoping-
 * fix-master-prompt.md` দেখুন, উপরের ধাপ ২০-৩৫ migration নাম্বারিং-এর সাথে সম্পর্কিত না)।**
 * এখন পর্যন্ত টেবিল-ওয়াইড RLS-filtered `postgresChangeFlow` (Postgres Changes) ব্যবহার হচ্ছিল,
 * যেটাতে প্রতিটা DB change *সব* active user-এর কাছে delivery-attempt হয় (RLS পরে filter করে) —
 * Realtime billing fan-out ভিত্তিক বলে এটা user-সংখ্যা বাড়ার সাথে খরচ বহুগুণ বাড়ায়। এই ধাপে
 * `notifications`-এর জন্য (pilot হিসেবে) একটা **dual-run** per-user Broadcast + Realtime
 * Authorization path যোগ করা হলো ([userTopicBroadcastChannel]/[startUserTopicBroadcastSubscription])
 * — DB-সাইড trigger শুধু `user:<uuid>` topic-এই broadcast করে, RLS policy শুধু নিজের topic-ই
 * subscribe করতে দেয়। পুরনো টেবিল-ওয়াইড `notificationsChannel` **সরানো হয়নি** (dual-run, ধাপ ৬-এ
 * cleanup-এর আগ পর্যন্ত)। Admin session-এ নতুন পথ চালু হয় না (`isCurrentSessionAdmin()`) — admin
 * আগের মতোই টেবিল-ওয়াইড subscription-এর উপর নির্ভর করবে। বিস্তারিত ইতিহাস/টেস্ট-ফলাফল:
 * `/REALTIME_SCOPING_PROGRESS.md` (এই ফাইলটা `MIGRATION_PROGRESS.md` থেকে ইচ্ছাকৃতভাবে আলাদা)।
 *
 * **Realtime Scoping ফিক্স, ধাপ ২ (wallet/money গ্রুপ)।** `transactions`/`withdrawals`/
 * `gateway_payments`/`additional_charges` — এই ৪টা টেবিলও এখন একই `user:$userId` broadcast
 * channel-এ (নতুন channel না, topic-প্যাটার্ন শেয়ার্ড) table-prefixed event নামে
 * ([TransactionChangeBroadcastPayload] ইত্যাদি) dual-run subscribe করে। DB-সাইড trigger এই সেশনের
 * আগেই (একটা আলাদা সেশনে, Supabase MCP দিয়ে সরাসরি) apply হয়ে গিয়েছিল কিন্তু তখন এই Kotlin কোড আর
 * migration `.sql` ফাইল repo-তে যোগ হয়নি — এই সেশনে সেই gap পূরণ করা হলো (দেখুন
 * `/REALTIME_SCOPING_PROGRESS.md`, "ধাপ ২" এন্ট্রি, এবং duplicate-migration-check ফলাফল)।
 *
 * **Realtime Scoping ফিক্স, ধাপ ৩ (Chat: `messages`)।** Topic: `problem:<problem_id>` (resource-
 * based, notifications/wallet-এর user-based topic থেকে আলাদা) — [joinProblemMessagesBroadcastChannel]/
 * [leaveProblemMessagesBroadcastChannel], ChatScreen-এর lifecycle-এর সাথে বাঁধা (typing channel-এর
 * মতোই idempotent join/leave map)। DB-সাইড RLS policy + trigger এই সেশনেই MCP দিয়ে apply ও
 * SQL-test করা হয়েছে (migration `realtime_scoping_step3_messages_broadcast.sql`)। "Allow public
 * access" Realtime সেটিং এই সেশনে ব্যবহারকারী নিজে Dashboard থেকে বন্ধ করেছেন (নিয়ম #৭ সম্পন্ন)।
 *
 * **Realtime Scoping ফিক্স, ধাপ ৫ (ঐচ্ছিক — users/escrows)।** একই `user:$userId` broadcast
 * channel-এ (নতুন channel না) table-prefixed event নামে ([UserChangeBroadcastPayload]/
 * [EscrowChangeBroadcastPayload]) dual-run subscribe করে — `users` single-owner (`user:<id>`),
 * `escrows` dual-owner (owner+solver দুটো topic-এই, transactions-এর প্যাটার্নের মতো)। DB-সাইড
 * trigger এই সেশনেই MCP দিয়ে apply ও SQL-test করা হয়েছে। এই ধাপেই প্রথমে `users`-এর trigger bare
 * `TG_OP` event নাম ব্যবহার করছিল যেটা notifications-এর bare event-নামের সাথে collide করতো —
 * সাথে সাথেই ধরা পড়ে ঠিক করা হয়েছে (migration `fix_users_broadcast_event_naming_collision.sql`)।
 * বিস্তারিত: `/REALTIME_SCOPING_PROGRESS.md`, "ধাপ ৫" এন্ট্রি।
 *
 * `FirebaseSyncManager`-কে সরাসরি import/call করা হয়নি (যদিও `getProblemTimestamp()`/
 * `resolveIncomingEscrowStatus()` ওখানে ইতিমধ্যে আছে) — কারণ `FirebaseSyncManager.kt` পুরোটাই
 * ধাপ ৩৩-এ ডিলিট হয়ে যাবে; তাই এই ফাইলে স্বতন্ত্র নিজস্ব কপি রাখা হলো, যাতে ধাপ ৩৩-এর পর এই
 * ফাইল কোনো dangling reference ছাড়াই টিকে থাকে।
 *
 * typing_status (ধাপ ২১) ইচ্ছাকৃতভাবে DB টেবিল-ভিত্তিক না — কোনো row persist হয় না, শুধু
 * Realtime Broadcast (পিয়ার-টু-পিয়ার সিগন্যাল, per-problem channel `typing_problem_<problemId>`)
 * ব্যবহার করা হয়েছে। public interface (`typingStatusMap`/`setTypingStatus()`) ইচ্ছাকৃতভাবে
 * `FirebaseSyncManager`-এর সমতুল্য নামেই রাখা হয়েছে (একই key format `"${problemId}_${userId}"`,
 * একই `StateFlow<Map<String, Long>>` শেপ) যাতে ধাপ ২২ cutover-এ `ChatScreen.kt`/
 * `SomadhanViewModel.kt`-এ প্রায় drop-in প্রতিস্থাপন হয়। যেহেতু RLS টেবিল-ভিত্তিক না, broadcast
 * channel-এর নামেই problemId থাকায় শুধু ওই সমস্যার দুই পক্ষ (owner+solver) join করবে সেটা
 * ক্লায়েন্ট-সাইড scoping-এর দায়িত্ব (caller `joinTypingChannel(problemId)` কল করেই channel-এ
 * join করবে, অন্য কোনো problemId-র channel-এ না)।
 *
 * নোট: এই ধাপও build/compile করে verify করা যায়নি (network/Gradle সুবিধা নেই এই session-এ) —
 * supabase-kt Realtime API (BOM 3.6.0)-এর সরকারি ডকুমেন্টেশন (Kotlin: Subscribe to channel পৃষ্ঠা)
 * থেকে যাচাই করা প্যাটার্ন অনুযায়ী লেখা হয়েছে (`channel.broadcastFlow<T>(event)`,
 * `channel.broadcast(event, message)`, `channel.subscribe()`) — বিস্তারিত: `MIGRATION_PROGRESS.md`-এ
 * "ধাপ ২০"/"ধাপ ২১" এন্ট্রি। Android Studio-তে প্রথম Gradle sync/build-এ এটা confirm করে নেওয়া
 * উচিত। বিশেষভাবে অনিশ্চিত: broadcast-এর reified generic overload (`channel.broadcast(event,
 * message: T)`, JsonObject-ভিত্তিক পুরনো overload না) — ধাপ ২২ (cutover, প্রথম real wiring)-এ
 * অগ্রাধিকার দিয়ে যাচাই করা উচিত, কারণ কম্পাইল-এরর হলে সবচেয়ে আগে এখানেই ধরা পড়বে।
 */
object SupabaseRealtimeManager {
    private const val TAG = "SupabaseRealtimeManager"

    // [Somadhan Bug-Fix Step 6 — root-cause ফিক্স, Step 1-এর regression] RealtimeChannel.subscribe()-এর
    // নিজস্ব কোনো timeout নেই -- WebSocket handshake কোনো কারণে (network/firewall/Supabase-side)
    // আটকে গেলে এই suspend কল চিরকাল ঝুলে থাকতে পারে। startRealtimeListeners()/
    // startUserTopicBroadcastSubscription()-কে (Step 1-এ) `runCatching` দিয়ে ঘেরা caller
    // (completeLoginAfterOtp/switchRoleToSolver/switchRoleToUser/loginAsAdmin) এর ফলে পরের
    // onSuccess()/onReady() কখনো কল হতো না -- state (login/role) ইতিমধ্যে কমিট হয়ে গেলেও UI
    // চিরকাল loading-এ আটকে থাকত (Somadhan Bug-Fix Step 6 টেস্টের সময় real device-এ ধরা পড়েছে,
    // ২০২৬-০৯-২৩)। এই ফাইলের pullBulkDataFromSupabase()-এর `withTimeout(20_000L)` precedent
    // অনুসরণ করে প্রতিটা subscribe()-এ একটা hard timeout বসানো হলো -- timeout/fail হলে শুধু ওই
    // একটা channel-এর realtime push বন্ধ থাকবে (bulk-pull/pull-to-refresh দিয়ে ডেটা এমনিতেও
    // আসবে), কিন্তু caller (login/role-switch flow) আটকে থাকবে না।
    private const val REALTIME_SUBSCRIBE_TIMEOUT_MS = 10_000L

    private suspend fun RealtimeChannel.subscribeWithTimeout(label: String) {
        runCatching { withTimeout(REALTIME_SUBSCRIBE_TIMEOUT_MS) { subscribe() } }
            .onFailure { Log.w(TAG, "Realtime channel '$label' subscribe সময়মতো সম্পন্ন হয়নি/ব্যর্থ (non-fatal, bulk-pull দিয়ে ডেটা আসবে): ${it.message}") }
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ১ — কেন্দ্রীয় sync-state সিগন্যাল। এটা এমনভাবে বানানো যাতে
    // ধাপ ২-এ per-domain (categories/ratings/platform_settings/faqs/audit_logs ইত্যাদি) ডাটার
    // জন্যও এই একই enum পুনরায় ব্যবহার করা যায়, প্রতিটার নিজস্ব StateFlow<SyncPhase> দিয়ে।
    enum class SyncPhase { LOADING, LOADED, ERROR }

    // pullBulkDataFromSupabase() (users/problems/bids/messages/transactions/escrows/
    // gateway_payments/additional_charges/notifications/withdrawals -- attachDatabase()-এর
    // প্রাথমিক bulk-pull) এর real-time সাফল্য/ব্যর্থতা ট্র্যাক করে। UI স্ক্রিন এখনো এটা ব্যবহার
    // করে না (ধাপ ৩/৪-এ ব্যবহার হবে) -- শুধু signal infrastructure।
    private val _initialSyncPhase = MutableStateFlow(SyncPhase.LOADING)
    val initialSyncPhase: StateFlow<SyncPhase> = _initialSyncPhase.asStateFlow()

    // Ground Rule ২১ (ADMIN_PANEL_LOADING_MASTER_PROMPT.md), সেশন ২.১৯.৩ — realtime-এ সম্পূর্ণ
    // **নতুন** insert হওয়া row-এর id সংক্ষিপ্ত সময়ের জন্য এখানে ট্র্যাক করা হয়, যাতে UI স্তরে
    // per-item pulse শুধু genuine নতুন ডেটার জন্য দেখানো যায় — স্ক্রল করে আগে-থেকে-থাকা কোনো item
    // প্রথমবার viewport-এ আসা থেকে এভাবেই আলাদা করা সম্ভব, কারণ [pullBulkDataFromSupabase] (cold-load/
    // bulk snapshot, attachDatabase()-এর প্রথম ধাপ) এই সেট দিয়ে যায় না — শুধু [startRealtimeListeners]
    // চালু হওয়ার *পরে* আসা প্রকৃত `PostgresAction.Insert` event-ই মার্ক হয় (দেখো handleUserAction,
    // markRecentlyInserted-এর কল-সাইট)। TTL শেষে id নিজে থেকে সেট থেকে সরে যায় — Set (List না)
    // ব্যবহার করা হয়েছে কারণ membership-check per-item composable-এর প্রতি recomposition-এ হবে।
    //
    // [NEW_ITEM_PULSE_TTL_MS] ইচ্ছাকৃতভাবে [MotionToolkit.rememberFieldChangePulse]-এর ডিফল্ট
    // pulse duration (৩৫০ms)-এর চেয়ে বড় রাখা হয়েছে — নতুন কার্ড তালিকায় যুক্ত হওয়ার মুহূর্তে
    // দ্রুত ৩৫০ms ঝলকালে বেশিরভাগ ক্ষেত্রেই খেয়াল করার আগেই শেষ হয়ে যেত; ২০০০ms দেওয়া হয়েছে যাতে
    // ব্যবহারকারী আসলে "এইটা নতুন" খেয়াল করার মতো সময় পায় (চূড়ান্ত duration না — real device-এ
    // ফিল-টেস্ট করে দরকার হলে টিউন করা যাবে, দেখো ADMIN_PANEL_LOADING_PROGRESS.md-এর সেশন ২.১৯.৩)।
    //
    // স্কোপ (Ground Rule ২১): Users (এই সেশন) প্রথমে; KYC/Withdrawal/Problems/Escrow পরের
    // সেশনগুলোতে একই প্যাটার্নে নিজস্ব `_recentlyInsertedXxxIds` StateFlow + handleXxxAction-এর
    // Insert branch-এ [markRecentlyInserted] কল করে যোগ হবে (এই ফাংশনটা table-agnostic, reuse
    // করা যাবে) — একবারে সব টেবিলে বসানো হয়নি, স্কোপ অনুযায়ী ধাপে ধাপে।
    private const val NEW_ITEM_PULSE_TTL_MS = 2000L

    private val _recentlyInsertedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyInsertedUserIds: StateFlow<Set<String>> = _recentlyInsertedUserIds.asStateFlow()

    // Ground Rule ২১, সেশন ২.১৯.৪ — KYC ট্যাব। KYC-এর pending/verified/rejected তালিকা কোনো
    // আলাদা টেবিল না, `users` টেবিলেরই `kyc_status` ফিল্ড-ভিত্তিক filter — তাই এখানে "নতুন" মানে
    // genuine Insert না (নতুন user সবসময় kyc_status=NONE দিয়ে শুরু করে), বরং কোনো user-এর
    // kyc_status Update event-এ pending/verified/rejected-এ **ট্রানজিশন** করা (ব্যবহারকারীর
    // কনফার্মড ডিজাইন, দেখো ADMIN_PANEL_LOADING_PROGRESS.md সেশন ২.১৯.৪)। `users` টেবিলে
    // REPLICA IDENTITY FULL সেট করা নেই (কোনো migration-এ পাওয়া যায়নি, আর handleUserAction-এর
    // DELETE branch-এর কমেন্টও নিশ্চিত করে যে default replica identity-তে oldRecord-এ শুধু PK
    // থাকে) — তাই postgres_changes payload-এর নিজস্ব oldRecord থেকে আগের kyc_status পাওয়া যায় না।
    // বদলে handleUserAction()-এর Update branch-এ mergeAndSaveUser()-এর overwrite হওয়ার *আগে*
    // local Room থেকে আগের kycStatus পড়ে নিয়ে নতুন (mapped) মানের সাথে তুলনা করা হয়।
    private val _recentlyKycChangedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyKycChangedUserIds: StateFlow<Set<String>> = _recentlyKycChangedUserIds.asStateFlow()

    // Ground Rule ২১, সেশন ২.১৯.৫ — Withdrawal ট্যাব। **আপডেট (সেশন ২.১৯.৭):** মূলত দুটো কেসই
    // (Insert + COMPLETED/REJECTED Update) pulse করানো হয়েছিল, কিন্তু ব্যবহারকারী পরে সিদ্ধান্ত
    // বদলে জানিয়েছেন শুধু genuine নতুন PENDING রিকোয়েস্ট (Insert)-এই pulse হবে, status-update-এ
    // না — Withdrawal/Problems/Escrow তিনটাতেই একসাথে এই সিদ্ধান্ত প্রযোজ্য (নিচের handleWithdrawalAction-এর
    // Update branch এখন আর এই flow মার্ক করে না, শুধু Insert branch করে)।
    private val _recentlyChangedWithdrawalIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyChangedWithdrawalIds: StateFlow<Set<String>> = _recentlyChangedWithdrawalIds.asStateFlow()

    // Ground Rule ২১, সেশন ২.১৯.৬ — Problems ট্যাব। **আপডেট (সেশন ২.১৯.৭):** মূলত Insert+Update
    // দুটোই pulse করানো হয়েছিল, কিন্তু ব্যবহারকারী পরে সিদ্ধান্ত বদলে জানিয়েছেন শুধু genuine নতুন
    // problem post (Insert)-এই pulse হবে, status-update-এ না (Withdrawal/Escrow-এর সাথে একই
    // সিদ্ধান্ত)। `handleProblemAction`-এর Update branch এখন আর এই flow মার্ক করে না।
    private val _recentlyChangedProblemIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyChangedProblemIds: StateFlow<Set<String>> = _recentlyChangedProblemIds.asStateFlow()

    // Ground Rule ২১, সেশন ২.১৯.৬ — Escrow ট্যাব। **আপডেট (সেশন ২.১৯.৭):** মূলত Insert+Update
    // দুটোই pulse করানো হয়েছিল, কিন্তু ব্যবহারকারী পরে সিদ্ধান্ত বদলে জানিয়েছেন শুধু genuine নতুন
    // escrow তৈরি (Insert, Held তালিকায় যুক্ত হয়)-এই pulse হবে, RELEASED/REFUNDED transition
    // (Update)-এ না। `handleEscrowAction`-এর Update branch এখন আর এই flow মার্ক করে না।
    private val _recentlyChangedEscrowIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyChangedEscrowIds: StateFlow<Set<String>> = _recentlyChangedEscrowIds.asStateFlow()

    // Ground Rule ২১, সেশন ২.২০ — Transactions ট্যাব (AdminTransactionsView.kt, ইনডেক্স ৯)। এই
    // ট্যাবের তালিকায় দুই ধরনের কার্ড দুটো আলাদা টেবিল থেকে আসে (WorkTransaction = `transactions`,
    // WalletRecharge = `gateway_payments`) — তাই দুটো আলাদা StateFlow, প্রতিটা নিজের টেবিলের id
    // দিয়ে key করা (Escrow-এর তিনটা sub-tab এক সেট শেয়ার করার বিপরীতে, এখানে টেবিলই আলাদা)।
    // ব্যবহারকারীর কনফার্মড সিদ্ধান্ত (Ground Rule ১৬): শুধু genuine নতুন লেনদেন/ওয়ালেট-রিচার্জ
    // (Insert)-এই pulse হবে, status/amount আপডেটে না — Withdrawal/Problems/Escrow-এর সংশোধিত
    // (সেশন ২.১৯.৭) Insert-only ডিজাইনের সাথেই সঙ্গতিপূর্ণ, শুরু থেকেই Insert-only করে বসানো হয়েছে।
    private val _recentlyChangedTransactionIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyChangedTransactionIds: StateFlow<Set<String>> = _recentlyChangedTransactionIds.asStateFlow()

    private val _recentlyChangedGatewayPaymentIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyChangedGatewayPaymentIds: StateFlow<Set<String>> = _recentlyChangedGatewayPaymentIds.asStateFlow()

    /**
     * Ground Rule ২১ হেল্পার — [id]-কে [flow]-তে যোগ করে, [NEW_ITEM_PULSE_TTL_MS] পরে নিজে থেকে
     * সরিয়ে দেয়। Table-agnostic — পরের সেশনগুলোতে KYC/Withdrawal/Problems/Escrow-এর নিজস্ব
     * `_recentlyInsertedXxxIds` ফ্ল্যাগেও একই ফাংশন reuse হবে। [managerScope]-এ launch করা হয়
     * (writeDispatcher, single-threaded) — কিন্তু এই কাজটা কোনো DB-write না, শুধু in-memory সেট
     * বদল, তাই serialize হলেও অন্য write-এর সাথে ব্লকিং কনটেনশন হয় না (delay() suspend করে,
     * থ্রেড ধরে রাখে না)।
     */
    private fun markRecentlyInserted(flow: MutableStateFlow<Set<String>>, id: String) {
        flow.value = flow.value + id
        managerScope.launch {
            delay(NEW_ITEM_PULSE_TTL_MS)
            flow.value = flow.value - id
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled Supabase realtime error (app kept running): ${throwable.message}", throwable)
    }

    // FirebaseSyncManager-এর syncWriteScope-এর মতোই single-threaded write dispatcher — একাধিক
    // channel থেকে একসাথে আসা event গুলো Room-এ সিরিয়ালাইজড ক্রমে লেখা হয়, race এড়াতে।
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val managerScope = CoroutineScope(writeDispatcher + SupervisorJob() + exceptionHandler)

    private var localDb: AppDatabase? = null

    private var usersChannel: RealtimeChannel? = null
    private var problemsChannel: RealtimeChannel? = null
    private var bidsChannel: RealtimeChannel? = null

    // ধাপ ২১: আরও ৪টা table-backed channel (messages/transactions/escrows/gateway_payments)
    private var messagesChannel: RealtimeChannel? = null
    private var transactionsChannel: RealtimeChannel? = null
    private var escrowsChannel: RealtimeChannel? = null
    private var gatewayPaymentsChannel: RealtimeChannel? = null

    // ধাপ ৩৪ (ব্যবহারকারীর অনুরোধে): ৮ম table-backed channel (additional_charges) -- আলাদা করে
    // রাখা হলো (উপরের ৭টার সাথে না মিশিয়ে) যাতে ধাপ ২০-২১-এর মূল সেট স্পষ্ট থাকে।
    private var additionalChargesChannel: RealtimeChannel? = null

    // ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে): ৯ম ও ১০ম table-backed channel (notifications/withdrawals) --
    // এই দুইটা টেবিল এতদিন `pullBulkDataFromSupabase()`-এর ৮-টেবিল স্কোপ আর realtime publication
    // দুটোরই বাইরে ছিল (শুধু one-off CRUD/RPC দিয়ে লেখা হতো, কখনো পড়া/সিঙ্ক করা হতো না) --
    // `supabase/migrations/step35_notifications_withdrawals_realtime.sql` দেখুন।
    private var notificationsChannel: RealtimeChannel? = null
    private var withdrawalsChannel: RealtimeChannel? = null

    // Realtime Scoping ফিক্স, ধাপ ১ (`somadhan-realtime-scoping-fix-master-prompt.md`, সম্পূর্ণ
    // আলাদা কাজ -- মূল migration ধাপ নাম্বারিং-এর সাথে না মিলিয়ে) -- normal (non-admin) session-এর
    // নিজস্ব `user:<uuid>` private broadcast topic-এর জন্য একটাই channel। একাধিক টেবিলের
    // broadcast_changes trigger একই topic-এ পাঠাতে পারে -- পরের ধাপ (২, wallet/money গ্রুপ)
    // transactions/withdrawals/gateway_payments/additional_charges-এর জন্য এই একই channel-এ আরও
    // broadcastFlow listener যোগ করবে, নতুন channel বানাবে না। Admin session-এ এটা কখনো তৈরি হয়
    // না (নিয়ম #৪, [isCurrentSessionAdmin] দিয়ে branch করা হয়)।
    private var userTopicBroadcastChannel: RealtimeChannel? = null

    // ধাপ ২১: typing broadcast — table-backed না, per-problem channel; একাধিক সমস্যার চ্যাট
    // স্ক্রিন একসাথে খোলা থাকতে পারে (যদিও সাধারণত একটাই), তাই problemId-কী দিয়ে map রাখা হলো।
    private val typingChannels = mutableMapOf<String, RealtimeChannel>()
    private val _typingStatusMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** `FirebaseSyncManager.typingStatusMap`-এর সমতুল্য (একই key format, একই শেপ) — ধাপ ২২-এ cutover-এর জন্য। */
    val typingStatusMap: StateFlow<Map<String, Long>> = _typingStatusMap.asStateFlow()

    /**
     * ধাপ ২২ (Realtime Cutover, Dual-Run) — এখন `FirebaseSyncManager.attachDatabase()`-এর মতোই
     * আচরণ করে: database attach হওয়ার সাথে সাথে একবার bulk-pull + realtime listener চালু হয়।
     * আগে (ধাপ ২০-২১) শুধু [localDb] সেট হতো, কোনো actual sync হতো না — এখন সেই পার্থক্য সরানো
     * হলো, যেমনটা তখনকার কমেন্টে "পরে পুনর্বিবেচনা করা হবে" বলে রাখা ছিল।
     *
     * `FirebaseSyncManager.attachDatabase()`-এর মতোই idempotency guard (`localDb === database`
     * হলে no-op) — একই `AppDatabase` সিঙ্গেলটন একাধিকবার (SomadhanApp.onCreate() +
     * SomadhanViewModel.init{} উভয় জায়গা থেকে) attach করা স্বাভাবিক, দ্বিতীয়বার পুরো
     * pull+listener-restart আবার চালানো অপ্রয়োজনীয়/costly।
     *
     * dual-run নীতি অনুযায়ী এটা `FirebaseSyncManager.attachDatabase()`-এর **পাশে** (প্রতিস্থাপন
     * না) কল হবে — caller (SomadhanApp.kt/SomadhanViewModel.kt) দুটো ম্যানেজারকেই আলাদা
     * try/catch-এ কল করে, একটা ব্যর্থ হলে অন্যটা প্রভাবিত হবে না।
     */
    // Loading/Sync Fix Roadmap v2, ধাপ ৫ (duplicate/racing-fetch guard) — [attachDatabase],
    // [retryInitialSync], আর ধাপ ৬-এর pull-to-refresh path (`refreshData()`/`refreshWalletData()`
    // থেকে retry-if-ERROR) -- এই তিনটা উৎসই এখন একই [performInitialSync] কল করে, যেটা এই
    // AtomicBoolean দিয়ে গার্ড করা। **এই ফ্ল্যাগটা [_initialSyncPhase]-এর মান থেকে সম্পূর্ণ
    // আলাদা রাখা হয়েছে ইচ্ছাকৃতভাবে** -- `_initialSyncPhase`-এর initial value নিজেই `LOADING`
    // (কোনো fetch শুরু হওয়ার আগেই), তাই "phase == LOADING হলে স্কিপ করো" জাতীয় guard বসালে
    // একদম প্রথম কলটাই ভুলভাবে স্কিপ হয়ে যেত। তাই আলাদা "in flight" ফ্ল্যাগ, যেটা শুধু আসল
    // fetch চলাকালীন সময়েই true থাকে।
    private val initialSyncInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    // [Offline Action Gating ধাপ ১৩] শেষ bulk-pull-এ কোনো টেবিল আসলে ব্যর্থ হয়েছিল কিনা।
    // pullBulkDataFromSupabase()-এর প্রতিটা টেবিল আলাদা runCatching-এ মোড়ানো, তাই অফলাইনে
    // সব টেবিল ব্যর্থ হলেও `isSuccess=true` (শুধু `error != null`) আসে এবং [_initialSyncPhase]
    // `LOADED` হয় -- ERROR না। এই কারণে অফলাইনে app খুললে পরে reconnect হলে ERROR-retry পথ
    // (retryAllErroredSyncPhasesOnReconnect) কখনো চলত না। এই ফ্ল্যাগ [_initialSyncPhase]-এর
    // semantics না বদলে সেই ফাঁকটুকু আলাদাভাবে ট্র্যাক করে -- phase নিজে অপরিবর্তিত।
    private val initialPullIncomplete = java.util.concurrent.atomic.AtomicBoolean(false)

    fun hasIncompleteInitialPull(): Boolean = initialPullIncomplete.get()

    /**
     * [Offline Action Gating ধাপ ১৩] reconnect-এর পর নীরব catch-up: শুধু [initialPullIncomplete]
     * true থাকলে bulk-pull আবার চালায়। [_initialSyncPhase] ইচ্ছাকৃতভাবে ছোঁয়া হয় না (LOADING-এ
     * গেলে composed সব স্ক্রিন skeleton-এ ঝলকাত) -- Room Flow-ই নতুন ডেটা নিজে থেকে UI-তে আনবে।
     * একই [initialSyncInFlight] guard ব্যবহার করে, তাই attach/retry/pull-to-refresh-এর সাথে
     * সমান্তরাল দ্বিতীয় pull শুরু হবে না।
     */
    suspend fun catchUpIncompleteInitialPull() {
        if (!initialPullIncomplete.get()) return
        if (!initialSyncInFlight.compareAndSet(false, true)) return
        try {
            runCatching { withTimeout(20_000L) { pullBulkDataFromSupabase() } }
                .onSuccess { result ->
                    initialPullIncomplete.set(!result.isSuccess || result.error != null)
                }
                .onFailure { throwable ->
                    Log.w(TAG, "catchUpIncompleteInitialPull failed (non-fatal): ${throwable.message}")
                }
        } finally {
            initialSyncInFlight.set(false)
        }
    }

    private suspend fun performInitialSync() {
        if (!initialSyncInFlight.compareAndSet(false, true)) {
            Log.d(TAG, "performInitialSync: already in flight elsewhere (attach/retry/pull-to-refresh) — skipping duplicate call")
            return
        }
        try {
            // Loading/Sync Fix Roadmap v2, ধাপ ১ — pull শুরু হওয়ার ঠিক আগে LOADING সেট করা হয়,
            // আর ২০ সেকেন্ডের একটা genuine network-level timeout দেওয়া হয় (withTimeout) যাতে
            // নেটওয়ার্ক শুধু hang করে থাকলেও (না সফল হচ্ছে, না exception দিচ্ছে) এটা চিরকাল
            // LOADING-এ আটকে না থেকে ধরা-পড়ার-যোগ্য একটা ERROR-এ পরিণত হয়। ২০ সেকেন্ড এই একটা
            // কলে একসাথে ১০টা টেবিল fetch হয় বলে বেছে নেওয়া হয়েছে (দুর্বল নেটে false positive
            // এড়াতে) — দেখুন উপরের BulkPullResult/pullBulkDataFromSupabase কমেন্ট।
            _initialSyncPhase.value = SyncPhase.LOADING
            runCatching { withTimeout(20_000L) { pullBulkDataFromSupabase() } }
                .onSuccess { result ->
                    // fetch সম্পন্ন হয়েছে কিনা এটাই গুরুত্বপূর্ণ, রেজাল্ট খালি হোক বা ভরা তাতে
                    // কিছু যায় আসে না — কিন্তু isSuccess == false হলে এটাও ERROR।
                    // [Offline Action Gating ধাপ ১৩] phase LOADED হলেও কোনো টেবিল ব্যর্থ হলে (যেমন অফলাইন)
                    // সেটা আলাদাভাবে মনে রাখা -- reconnect-এ catch-up-এর জন্য। ফ্ল্যাগ phase-এর *আগে*
                    // সেট হয়, যাতে phase-collector সবসময় হালনাগাদ ফ্ল্যাগ দেখে।
                    initialPullIncomplete.set(!result.isSuccess || result.error != null)
                    _initialSyncPhase.value = if (result.isSuccess) SyncPhase.LOADED else SyncPhase.ERROR
                }
                .onFailure { throwable ->
                    // TimeoutCancellationException সহ যেকোনো exception সমানভাবে ERROR।
                    _initialSyncPhase.value = SyncPhase.ERROR
                    initialPullIncomplete.set(true)
                    val reason = if (throwable is TimeoutCancellationException) {
                        "timed out after 20s"
                    } else {
                        throwable.message ?: "unknown error"
                    }
                    Log.e(TAG, "Supabase bulk pull failed (non-fatal, local Room path unaffected): $reason", throwable)
                }
            runCatching { startRealtimeListeners() }
                .onFailure { Log.e(TAG, "Supabase realtime listeners failed to start (non-fatal): ${it.message}", it) }
        } finally {
            initialSyncInFlight.set(false)
        }
    }

    fun attachDatabase(database: AppDatabase) {
        if (localDb === database) {
            return
        }
        localDb = database
        managerScope.launch { performInitialSync() }
    }

    /**
     * Loading/Sync Fix Roadmap v2, ধাপ ৪ — [initialSyncPhase]-নির্ভর স্ক্রিনগুলোর
     * `SyncAwareContent`-এর `onRetry`-এর জন্য entry point, আর ধাপ ৬-এ pull-to-refresh-এর
     * retry-if-ERROR পথও এটাই কল করে। [attachDatabase]-এর `localDb === database` idempotency
     * guard এখানে প্রযোজ্য না -- ব্যবহারকারী স্পষ্টভাবে "আবার চেষ্টা করুন" বাটনে চাপলে বা
     * pull-to-refresh করলে, localDb আগে থেকেই attach থাকা সত্ত্বেও প্রতিবারই নতুন করে pull
     * হওয়া উচিত। ধাপ ৫-এ যোগ করা [performInitialSync]-এর ভেতরের [initialSyncInFlight] guard
     * নিশ্চিত করে যে এটা আর [attachDatabase] (বা অন্য কোনো concurrent retry) একই সাথে দুটো
     * সমান্তরাল bulk-pull শুরু করবে না।
     */
    fun retryInitialSync() {
        managerScope.launch { performInitialSync() }
    }

    // ============================================================
    // Bulk pull — ধাপ ২২: FirebaseSyncManager.pullAllCloudDataToLocal()-এর সমতুল্য, কিন্তু শুধু
    // এই ৮টা টেবিলের জন্য (users/problems/bids/messages/transactions/escrows/gateway_payments/
    // additional_charges) -- এই ৮টাই এখন পর্যন্ত (ধাপ ২০-২১, +ধাপ ৩৪-এ additional_charges) realtime
    // channel পেয়েছে, তাই attachDatabase()-এ প্রথম snapshot হিসেবে এই ৮টাই লাগবে। Firebase-এর
    // pullAllCloudDataToLocal() আরও ১২টা কালেকশন (categories/ratings/notifications/
    // platform_settings/admin_audit_logs/withdrawals/reputation_events ইত্যাদি) পুল করে --
    // সেগুলোর কোনোটারই এখনো Supabase Realtime channel নেই (মূল রোডম্যাপেও নেই), তাই এই
    // bulk-pull-এর স্কোপের বাইরে রাখা হলো;
    // সেগুলো ইতিমধ্যে `SupabaseSyncManager`-এর one-off CRUD ফাংশন দিয়ে যেভাবে পড়া হচ্ছিল সেভাবেই
    // থাকবে। এই সিদ্ধান্ত রিপোর্টে (MIGRATION_PROGRESS.md, "ধাপ ২২") স্পষ্ট করে লেখা আছে।
    //
    // 🔑 গুরুত্বপূর্ণ ডিজাইন পার্থক্য Firebase থেকে: Firestore-এ admin/non-admin scoping client-side
    // (কোন query চালানো হচ্ছে তার উপর নির্ভর করে) করতে হতো, তাই pullUsers() ইত্যাদিতে
    // isCurrentUserAdmin-ভিত্তিক আলাদা কোড-পথ ছিল। Supabase-এ RLS policy সার্ভার-সাইডে row
    // filter করে -- একই `SELECT * FROM users` কোয়েরি admin আর non-admin-এর জন্য ভিন্ন ভিন্ন
    // row-সেট ফেরত দেয় (JWT-র `auth.uid()`/`is_admin()` অনুযায়ী), client-কে আলাদা query লিখতে
    // হয় না। তাই নিচের bulk-pull ইচ্ছাকৃতভাবে unconditional `select()` -- কোনো role-branching
    // কোড নেই, কারণ RLS নিজেই সেটা করে দেয়। **তবে এটা নির্ভর করে বর্তমান ডিভাইসের Supabase Auth
    // সেশন সত্যিই সঠিক ইউজারের জন্য সাইন-ইন করা থাকার উপর** (`SupabaseAuthManager` phone-based
    // sign-in/up ব্যবহার করে) -- `loginAsAdmin()`-এর "ADMIN_SYSTEM" demo/fixed-id admin-এর
    // ক্ষেত্রে এটা সত্যি না (নিচে "যা wire করা হয়নি" অংশে বিস্তারিত)।
    // ============================================================

    data class BulkPullResult(
        val isSuccess: Boolean,
        val usersCount: Int,
        val problemsCount: Int,
        val bidsCount: Int,
        val messagesCount: Int,
        val transactionsCount: Int,
        val escrowsCount: Int,
        val gatewayPaymentsCount: Int,
        val additionalChargesCount: Int = 0,
        val notificationsCount: Int = 0,
        val withdrawalsCount: Int = 0,
        val error: String? = null
    )

    suspend fun pullBulkDataFromSupabase(): BulkPullResult {
        val database = localDb ?: return BulkPullResult(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "লোকাল ডাটাবেস প্রস্তুত নয়")
        val client = SupabaseClientProvider.client
        var lastError: String? = null

        var usersCount = 0
        var problemsCount = 0
        var bidsCount = 0
        var messagesCount = 0
        var transactionsCount = 0
        var escrowsCount = 0
        var gatewayPaymentsCount = 0
        var additionalChargesCount = 0
        var notificationsCount = 0
        var withdrawalsCount = 0

        try {
            // FirebaseSyncManager.pullAllCloudDataToLocal()-এর মতোই -- স্বাধীন টেবিলগুলো concurrently
            // fetch করা হয়। escrows বাদ (নিচে আলাদা, কারণ এটা problems/transactions-এর উপর নির্ভর করে
            // মার্জ করে -- ঠিক mergeAndSaveEscrow()-এর মতোই, তাই বাকিগুলো লেখা শেষ হওয়ার পর চালানো হয়)।
            coroutineScope {
                val usersDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("users").select().decodeList<UserDto>()
                        val entities = dtos.map { mergeAndSaveUser(database, it) }
                        if (entities.isNotEmpty()) database.userDao().insertUsers(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "users pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                val problemsDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("problems").select().decodeList<ProblemDto>()
                        val entities = dtos.mapNotNull { mergeAndSaveProblem(database, it) }
                        if (entities.isNotEmpty()) database.problemDao().insertProblems(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "problems pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                val bidsDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("bids").select().decodeList<BidDto>()
                        val entities = dtos.map { it.toBidEntity() }
                        if (entities.isNotEmpty()) database.bidDao().insertBids(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "bids pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                val messagesDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("messages").select().decodeList<MessageDto>()
                        // local-read-wins merge, handleMessageAction()-এর মতোই -- একটা ব্যাচ pull-এ
                        // একই লজিক প্রতিটা row-এর জন্য পুনরাব্যবহার করা হলো।
                        val entities = dtos.map { dto ->
                            val incoming = dto.toMessageEntity()
                            val local = database.messageDao().getMessageById(incoming.id)
                            if (local != null && local.isRead && !incoming.isRead) incoming.copy(isRead = true) else incoming
                        }
                        if (entities.isNotEmpty()) database.messageDao().insertMessages(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "messages pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                val transactionsDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("transactions").select().decodeList<TransactionDto>()
                        val entities = dtos.map { it.toTransactionEntity() }
                        if (entities.isNotEmpty()) database.transactionDao().insertTransactions(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "transactions pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                val gatewayDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("gateway_payments").select().decodeList<GatewayPaymentDto>()
                        val entities = dtos.map { it.toGatewayPaymentEntity() }
                        if (entities.isNotEmpty()) database.gatewayPaymentDao().insertPayments(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "gateway_payments pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                // ধাপ ৩৪: additional_charges — AdditionalChargeDao-তে batch-insert নেই (শুধু একক
                // insert), তাই একটা একটা করে insert করা হলো (escrows-এর মতো, তবে সেটা আলাদা কারণে
                // batch-এর বাইরে -- এখানে শুধু DAO-তে batch method না থাকার কারণে)।
                val additionalChargesDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("additional_charges").select().decodeList<AdditionalChargeDto>()
                        dtos.forEach { dto -> database.additionalChargeDao().insert(dto.toAdditionalChargeEntity()) }
                        dtos.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "additional_charges pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                // ধাপ ৩৫: notifications — NotificationDao-তে batch insert (insertNotifications)
                // আছে, তাই সরাসরি ব্যাচ upsert (additional_charges-এর মতো এক-এক করে না)।
                val notificationsDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("notifications").select().decodeList<NotificationDto>()
                        val entities = dtos.map { it.toNotificationEntity() }
                        if (entities.isNotEmpty()) database.notificationDao().insertNotifications(entities)
                        entities.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "notifications pull ব্যর্থ") }
                        .getOrDefault(0)
                }
                // ধাপ ৩৫: withdrawals — WithdrawalDao-তে batch insert নেই (শুধু একক insertWithdrawal),
                // তাই additional_charges/escrows-এর মতো এক-এক করে upsert।
                val withdrawalsDeferred = async {
                    runCatching {
                        val dtos = client.postgrest.from("withdrawals").select().decodeList<WithdrawalDto>()
                        dtos.forEach { dto -> database.withdrawalDao().insertWithdrawal(dto.toWithdrawalEntity()) }
                        dtos.size
                    }.onFailure { lastError = lastError ?: (it.message ?: "withdrawals pull ব্যর্থ") }
                        .getOrDefault(0)
                }

                usersCount = usersDeferred.await()
                problemsCount = problemsDeferred.await()
                bidsCount = bidsDeferred.await()
                messagesCount = messagesDeferred.await()
                transactionsCount = transactionsDeferred.await()
                gatewayPaymentsCount = gatewayDeferred.await()
                additionalChargesCount = additionalChargesDeferred.await()
                notificationsCount = notificationsDeferred.await()
                withdrawalsCount = withdrawalsDeferred.await()
            }

            // escrows: problems/transactions টেবিল ইতিমধ্যে লেখা হয়ে গেছে ধরে নিয়ে (উপরের ব্যাচের
            // পরেই চালানো, mergeAndSaveEscrow()-এর ডিপেন্ডেন্সি অনুযায়ী -- EscrowDao-তে batch insert
            // নেই (শুধু একক insert), তাই একটা একটা করে upsert করা হয়েছে, অন্যগুলোর মতো batch-insert না)।
            runCatching {
                val dtos = client.postgrest.from("escrows").select().decodeList<EscrowDto>()
                dtos.forEach { dto -> database.escrowDao().insertEscrow(mergeAndSaveEscrow(database, dto)) }
                escrowsCount = dtos.size
            }.onFailure { lastError = lastError ?: (it.message ?: "escrows pull ব্যর্থ") }

            return BulkPullResult(
                isSuccess = true,
                usersCount = usersCount,
                problemsCount = problemsCount,
                bidsCount = bidsCount,
                messagesCount = messagesCount,
                transactionsCount = transactionsCount,
                escrowsCount = escrowsCount,
                gatewayPaymentsCount = gatewayPaymentsCount,
                additionalChargesCount = additionalChargesCount,
                notificationsCount = notificationsCount,
                withdrawalsCount = withdrawalsCount,
                error = lastError
            )
        } catch (e: Exception) {
            Log.e(TAG, "pullBulkDataFromSupabase error: ${e.message}", e)
            return BulkPullResult(
                isSuccess = false,
                usersCount = usersCount,
                problemsCount = problemsCount,
                bidsCount = bidsCount,
                messagesCount = messagesCount,
                transactionsCount = transactionsCount,
                escrowsCount = escrowsCount,
                gatewayPaymentsCount = gatewayPaymentsCount,
                additionalChargesCount = additionalChargesCount,
                notificationsCount = notificationsCount,
                withdrawalsCount = withdrawalsCount,
                error = e.localizedMessage ?: e.message
            )
        }
    }

    // ============================================================
    // Listener health tracking — FirebaseSyncManager.checkListenerHealthAndFallbackSync()-এর
    // সমতুল্য। প্রতিটা handle*Action()-এ event আসার সময় (নিচে যোগ করা হয়েছে) এই map আপডেট হয়;
    // কোনো টেবিলের last-heard timestamp অনেক পুরনো হয়ে গেলে সেই টেবিলের জন্য targeted fallback
    // pull চলে -- ঠিক Firebase-এর ভার্সনের মতোই একই থ্রেশহোল্ড ডিফল্ট (১০ মিনিট) আর একই আচরণ:
    // কখনো "listened but gone quiet" না হলে (কখনো event-ই আসেনি) স্টেল ধরা হয় না, কারণ
    // attachDatabase() ইতিমধ্যে একটা প্রাথমিক bulk-pull চালিয়ে দেয়।
    // ============================================================
    private val listenerLastHeardMs = mutableMapOf<String, Long>()

    suspend fun checkListenerHealthAndFallbackSync(staleThresholdMs: Long = 10 * 60 * 1000L) {
        val database = localDb ?: return
        val client = SupabaseClientProvider.client
        val now = System.currentTimeMillis()

        val fallbacks = linkedMapOf<String, suspend () -> Unit>(
            "users" to {
                val dtos = client.postgrest.from("users").select().decodeList<UserDto>()
                val entities = dtos.map { mergeAndSaveUser(database, it) }
                if (entities.isNotEmpty()) database.userDao().insertUsers(entities)
            },
            "problems" to {
                val dtos = client.postgrest.from("problems").select().decodeList<ProblemDto>()
                val entities = dtos.mapNotNull { mergeAndSaveProblem(database, it) }
                if (entities.isNotEmpty()) database.problemDao().insertProblems(entities)
            },
            "bids" to {
                val dtos = client.postgrest.from("bids").select().decodeList<BidDto>()
                database.bidDao().insertBids(dtos.map { it.toBidEntity() })
            },
            "messages" to {
                val dtos = client.postgrest.from("messages").select().decodeList<MessageDto>()
                database.messageDao().insertMessages(dtos.map { it.toMessageEntity() })
            },
            "transactions" to {
                val dtos = client.postgrest.from("transactions").select().decodeList<TransactionDto>()
                database.transactionDao().insertTransactions(dtos.map { it.toTransactionEntity() })
            },
            "escrows" to {
                val dtos = client.postgrest.from("escrows").select().decodeList<EscrowDto>()
                dtos.forEach { dto -> database.escrowDao().insertEscrow(mergeAndSaveEscrow(database, dto)) }
            },
            "gateway_payments" to {
                val dtos = client.postgrest.from("gateway_payments").select().decodeList<GatewayPaymentDto>()
                database.gatewayPaymentDao().insertPayments(dtos.map { it.toGatewayPaymentEntity() })
            },
            "additional_charges" to {
                val dtos = client.postgrest.from("additional_charges").select().decodeList<AdditionalChargeDto>()
                dtos.forEach { dto -> database.additionalChargeDao().insert(dto.toAdditionalChargeEntity()) }
            },
            "notifications" to {
                val dtos = client.postgrest.from("notifications").select().decodeList<NotificationDto>()
                database.notificationDao().insertNotifications(dtos.map { it.toNotificationEntity() })
            },
            "withdrawals" to {
                val dtos = client.postgrest.from("withdrawals").select().decodeList<WithdrawalDto>()
                dtos.forEach { dto -> database.withdrawalDao().insertWithdrawal(dto.toWithdrawalEntity()) }
            }
        )

        val staleKeys = fallbacks.keys.filter { key ->
            val lastHeard = listenerLastHeardMs[key]
            lastHeard != null && (now - lastHeard) > staleThresholdMs
        }

        if (staleKeys.isEmpty()) {
            Log.d(TAG, "Supabase listener health check: সব healthy, fallback দরকার নেই")
            return
        }

        Log.w(TAG, "Supabase listener health check: ${staleKeys.joinToString()} স্টেল, targeted fallback pull চালানো হচ্ছে")
        coroutineScope {
            val jobs = staleKeys.mapNotNull { key -> fallbacks[key] }.map { fetch -> async { runCatching { fetch() } } }
            jobs.forEach { it.await() }
        }
    }

    // ============================================================
    // Problem timestamp — FirebaseSyncManager.getProblemTimestamp()-এর স্বতন্ত্র কপি (উপরের
    // ক্লাস-লেভেল কমেন্টে কারণ ব্যাখ্যা করা আছে)। দুটো ফাংশনের লজিক অবিকল একই থাকা উচিত।
    // ============================================================
    private fun getProblemTimestamp(p: ProblemEntity): Long {
        return maxOf(
            p.lastActivityAt ?: 0L,
            p.createdAt,
            p.completedAt ?: 0L,
            p.disputedAt ?: 0L,
            p.disputeResolvedAt ?: 0L,
            p.disputeSettledAt ?: 0L,
            p.solverLiveUpdatedAt ?: 0L,
            p.arrivedAt ?: 0L,
            p.onWayAt ?: 0L,
            p.jobStartedAt ?: 0L,
            p.acceptedAt2 ?: 0L,
            p.releaseRequestedAt ?: 0L,
            p.pendingExtraAmountRequestedAt ?: 0L
        )
    }

    // ============================================================
    // Merge logic — FirebaseSyncManager-এর mergeAndSaveUser/mergeAndSaveProblem-এর সমতুল্য।
    // bids-এর জন্য আলাদা mergeAndSaveBid নেই, কারণ FirebaseSyncManager.handleBidsSnapshot-ও
    // কোনো conflict-resolution ছাড়াই সরাসরি overwrite করে -- এখানে একই আচরণ রাখা হয়েছে।
    // ============================================================

    /**
     * password preserve (Supabase কখনো password পাঠায় না, `UserDto.toUserEntity()`-এর ডিফল্ট
     * আচরণ)।
     *
     * === "balance flicker guard" রিমুভ করা হলো (মানি-ফ্লো ফিক্স, ধাপ ১) ===
     * আগে এখানে `localUser.updatedAt >= cloudUpdatedAt` হলে incoming.balance বাতিল করে
     * local balance রেখে দেওয়া হতো (FirebaseSyncManager.mergeAndSaveUser-এর পুরনো লজিক,
     * উদ্দেশ্য ছিল out-of-order/stale event থেকে সুরক্ষা)। কিন্তু এই guard structurally ভুল
     * ছিল, কারণ:
     *   ১. `updatedAt` ফিল্ডটা balance ছাড়াও KYC/ban/location/notification-touch ইত্যাদি
     *      প্রায় প্রতিটা local write-এই device-এর নিজের ঘড়ি দিয়ে বসানো হয় -- তাই এই ফিল্ডটা
     *      "balance কতটা fresh" তার নির্ভরযোগ্য প্রক্সি না। কোনো অসংশ্লিষ্ট local write
     *      balance-বৃদ্ধির broadcast-এর ঠিক আগে/পরে হলেই সেই সঠিক broadcast বাতিল হয়ে যেত
     *      (রিপোর্ট করা বাগ: user fund release করলে solver-এর transaction history-তে entry
     *      আসতো কিন্তু balance আপডেট হতো না)।
     *   ২. এই guard শুধু plain `balance` ফিল্ড protect করতো -- `balanceUser`/`balanceSolver`
     *      মিরর কলাম দুটো তখনও incoming (সার্ভার) মান নিয়ে নিতো, ফলে "protect" হওয়া অবস্থাতেই
     *      `balance` আর তার নিজের মিরর কলাম একে অপরের সাথে না মিলে আরেকটা নতুন mismatch
     *      তৈরি হতো।
     *   ৩. Supabase Realtime একই row-এর জন্য commit-order অনুযায়ী delivery করে (Firebase-এর
     *      মতো out-of-order snapshot সমস্যা এখানে প্রযোজ্য না) -- তাই সার্ভার থেকে আসা `users`
     *      UPDATE/INSERT payload-ই ওই মুহূর্তের authoritative balance, client-side heuristic
     *      দিয়ে "আটকানোর" দরকার নেই। balance/balance_user/balance_solver সবসময় সার্ভারের
     *      SECURITY DEFINER RPC-ই বদলায়, তাই dto-তে যা আসছে সেটাই সত্য।
     * তাই এখন balance/balanceUser/balanceSolver-এর জন্য কোনো override ছাড়াই dto-এর মান
     * সরাসরি নেওয়া হচ্ছে। (MONEY_FLOW_TEST_CHECKLIST.md-এর সেকশন ১, ২, ৩, ৫, ৬, ৯ দিয়ে
     * regression verify করা দরকার।)
     *
     * === [Somadhan Bug-Fix — সাব-বাগ ১.৫, real root cause of the persistent balance-reset
     * bug] ===
     * উপরের রিমুভাল (money-flow ফিক্স ধাপ ১)-এর যুক্তি সাধারণভাবে ঠিক: server-এর payload-ই
     * normally authoritative, কারণ balance সবসময় SECURITY DEFINER RPC-ই বদলায়। কিন্তু এই
     * যুক্তির ৩ নং পয়েন্টের একটা অপ্রত্যাশিত ফাঁক আছে -- RPC dual-write cloud-এ *ব্যর্থ* হলে
     * (network/timeout ইত্যাদি, তখনই enqueueOutboxRetry() একটা PENDING outbox entry বানায়),
     * সেই মুহূর্তে server-এর "authoritative" balance আসলে **stale** -- ঠিক এই device-এর নিজের
     * পেন্ডিং write-টাই এখনো cloud-এ পৌঁছায়নি। এই ঠিক এই gap-টাই `SomadhanRepository.
     * refreshUserDataFromCloud()`-এ সাব-বাগ ১.৩ হিসেবে ফিক্স করা হয়েছিল -- কিন্তু সেই ফিক্স
     * শুধু login/account-switch-এর সময়কার ওই একটা call-site-কেই কভার করেছিল। এই
     * `mergeAndSaveUser()` সম্পূর্ণ আলাদা ফাইলে (SupabaseRealtimeManager.kt) থাকা তিনটা
     * ভিন্ন caller-এর (realtime `users`-table UPDATE event handler, listener-health fallback
     * pull, আর app-শুরুর bulk pull -- তিনটাই এই ফাংশন দিয়েই যায়) জন্য একই stale-overwrite
     * সমস্যাটা পুরোপুরি অরক্ষিত রেখে দিয়েছিল -- login-এর সময় ১.৩ ঠিক করে দেওয়ার পরও, ঠিক তার
     * পরপরই realtime subscription চালু হয়ে বা health-check/bulk-pull চলে আবার সেই একই stale
     * cloud balance দিয়ে local overwrite করে দিত। এটাই কেন ১.৩-এর পরেও balance বারবার "reset"
     * হয়ে যাচ্ছিল -- আগের সবগুলো ফিক্স ভুল ছিল না, শুধু সবগুলো entry point কভার করেনি।
     * ফিক্স: `updatedAt`-ভিত্তিক পুরনো heuristic (যেটা ইচ্ছাকৃতভাবে রিমুভ করা হয়েছিল, উপরের
     * ব্যাখ্যা অনুযায়ী সঠিক কারণেই) ফিরিয়ে আনা হয়নি -- বরং ১.৩-এর মতোই সরাসরি
     * `pendingSyncOutboxDao.getPending()` চেক করা হচ্ছে। কোনো PENDING/RETRYING entry থাকলে
     * শুধু money/status-critical ফিল্ডগুলো (legacy plain কলাম + role-scoped mirror কলাম দুটোই,
     * যাতে উপরের রিমুভাল-কমেন্টের ২ নং পয়েন্টে বর্ণিত mismatch আবার তৈরি না হয়) local-ই রাখা
     * হচ্ছে, বাকি সব ফিল্ড (profile/KYC/location ইত্যাদি) আগের মতোই dto থেকে নেওয়া হবে।
     */
    private suspend fun mergeAndSaveUser(database: AppDatabase, dto: UserDto): UserEntity {
        val localUser = database.userDao().getUserById(dto.id)
        val incoming = dto.toUserEntity(existingPassword = localUser?.password ?: "")
        if (localUser == null) return incoming
        val hasPendingOutboxSync = runCatching { database.pendingSyncOutboxDao().getPending() }
            .getOrDefault(emptyList())
            .isNotEmpty()
        if (!hasPendingOutboxSync) return incoming
        Log.w(
            TAG,
            "mergeAndSaveUser: pending outbox sync exists for device -- keeping local balance/status for user ${dto.id} instead of stale cloud value (সাব-বাগ ১.৫)"
        )
        return incoming.copy(
            balance = localUser.balance,
            reputationScore = localUser.reputationScore,
            isBanned = localUser.isBanned,
            isRestricted = localUser.isRestricted,
            balanceUser = localUser.balanceUser,
            balanceSolver = localUser.balanceSolver,
            reputationScoreUser = localUser.reputationScoreUser,
            reputationScoreSolver = localUser.reputationScoreSolver,
            isBannedUser = localUser.isBannedUser,
            isBannedSolver = localUser.isBannedSolver,
            isRestrictedUser = localUser.isRestrictedUser,
            isRestrictedSolver = localUser.isRestrictedSolver
        )
    }

    /**
     * FirebaseSyncManager.mergeAndSaveProblem-এর মতোই stale-snapshot-skip: local কপির timestamp
     * নতুন হলে incoming event উপেক্ষা করা হয় (দেরিতে আসা/out-of-order event পুরনো ডেটা দিয়ে
     * নতুন local state overwrite না করুক)।
     */
    private suspend fun mergeAndSaveProblem(database: AppDatabase, dto: ProblemDto): ProblemEntity? {
        val incoming = dto.toProblemEntity()
        val local = database.problemDao().getProblemById(incoming.id)
        if (local != null && getProblemTimestamp(local) > getProblemTimestamp(incoming)) {
            return null
        }
        return incoming
    }

    /** insert/update/delete event থেকে PK (`id`) বের করার হেল্পার -- DELETE event-এর oldRecord-এর জন্য। */
    private fun extractId(oldRecord: Map<String, kotlinx.serialization.json.JsonElement>): String? {
        return (oldRecord["id"] as? JsonPrimitive)?.content
    }

    // ============================================================
    // users channel
    // ============================================================
    private suspend fun handleUserAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["users"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<UserDto>() }.getOrNull() ?: return
                database.userDao().insertUser(mergeAndSaveUser(database, dto))
                // Ground Rule ২১, সেশন ২.১৯.৩ — এটা genuine realtime INSERT (bulk snapshot পুল
                // এই কোড-পাথ দিয়ে যায় না), তাই এই id-টাকে সংক্ষিপ্ত সময়ের জন্য "নতুন" হিসেবে মার্ক
                // করা হলো যাতে AdminUsersView-এর per-item pulse শুধু এই কার্ডে trigger হয়।
                markRecentlyInserted(_recentlyInsertedUserIds, dto.id)
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<UserDto>() }.getOrNull() ?: return
                // Ground Rule ২১, সেশন ২.১৯.৪ (KYC) — mergeAndSaveUser overwrite করার আগে local
                // kycStatus পড়ে রাখা হলো (oldRecord থেকে পাওয়া যায় না, উপরের
                // _recentlyKycChangedUserIds-এর কমেন্ট দেখো)। mergeAndSaveUser()-এর existing
                // লজিক/সিগনেচার স্পর্শ করা হয়নি — শুধু একটা অতিরিক্ত read।
                val oldKycStatus = database.userDao().getUserById(dto.id)?.kycStatus
                database.userDao().insertUser(mergeAndSaveUser(database, dto))
                val newKycStatus = mapKycStatusFromSupabase(dto.kycStatus)
                if (newKycStatus != oldKycStatus &&
                    (newKycStatus == "pending" || newKycStatus == "verified" || newKycStatus == "rejected")
                ) {
                    markRecentlyInserted(_recentlyKycChangedUserIds, dto.id)
                }
            }
            is PostgresAction.Delete -> {
                // ⚠️ verify করা হয়েছে (ধাপ ২০, MIGRATION_PROGRESS.md দেখুন): `users` টেবিলে কোনো
                // DELETE RLS policy নেই, তাই client-role থেকে এই event সাধারণত আসবে না -- শুধু
                // admin/service-role সরাসরি delete করলে (বিরল, যেমন GDPR cleanup) আসতে পারে।
                // Replica identity default হলে oldRecord-এ শুধু PK-ই থাকে, তাই পুরো ডিকোড না করে
                // সরাসরি "id" ফিল্ড বের করাই নিরাপদ।
                extractId(action.oldRecord)?.let { database.userDao().deleteUser(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // problems channel
    // ============================================================
    private suspend fun handleProblemAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["problems"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<ProblemDto>() }.getOrNull() ?: return
                mergeAndSaveProblem(database, dto)?.let {
                    database.problemDao().insertProblem(it)
                    // Ground Rule ২১, সেশন ২.১৯.৬ — genuine realtime INSERT (bulk-pull এই পাথ দিয়ে
                    // যায় না, Users/Withdrawal-এর প্যাটার্নেই)।
                    markRecentlyInserted(_recentlyChangedProblemIds, dto.id)
                }
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<ProblemDto>() }.getOrNull() ?: return
                // Ground Rule ২১ — ব্যবহারকারী পরে সিদ্ধান্ত বদলে জানিয়েছেন Update-এ pulse লাগবে
                // না, শুধু genuine Insert-এই (Users-এর মতো Insert-only)। তাই এখানে আর
                // markRecentlyInserted() কল হয় না, শুধু বিদ্যমান merge/save লজিক অপরিবর্তিত।
                mergeAndSaveProblem(database, dto)?.let { database.problemDao().insertProblem(it) }
            }
            is PostgresAction.Delete -> {
                // `problems` টেবিলেও কোনো DELETE RLS policy নেই (soft-delete flag
                // `is_user_deleted` ব্যবহার হয়, hard delete না) -- তাই defensively হ্যান্ডল করা,
                // স্বাভাবিক অবস্থায় আসার কথা না।
                extractId(action.oldRecord)?.let { database.problemDao().deleteProblem(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // bids channel
    // ============================================================
    private suspend fun handleBidAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["bids"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<BidDto>() }.getOrNull() ?: return
                database.bidDao().insertBid(dto.toBidEntity())
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<BidDto>() }.getOrNull() ?: return
                database.bidDao().insertBid(dto.toBidEntity())
            }
            is PostgresAction.Delete -> {
                // `bids`-এও কোনো DELETE RLS policy নেই -- defensively হ্যান্ডল করা।
                extractId(action.oldRecord)?.let { database.bidDao().deleteBid(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // messages channel — FirebaseSyncManager.handleMessagesSnapshot()-এর মডেল অনুসরণ করে।
    // local-read-wins merge: message ইতিমধ্যে locally isRead=true হয়ে থাকলে আর incoming
    // (সম্ভবত stale) isRead=false হলে, local read-receipt হারানো ঠিক না।
    // ============================================================
    private suspend fun handleMessageAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["messages"] = System.currentTimeMillis()
        suspend fun upsert(dto: MessageDto?) {
            dto ?: return
            val incoming = dto.toMessageEntity()
            val local = database.messageDao().getMessageById(incoming.id)
            val finalMsg = if (local != null && local.isRead && !incoming.isRead) {
                incoming.copy(isRead = true)
            } else incoming
            database.messageDao().insertMessage(finalMsg)
        }
        when (action) {
            is PostgresAction.Insert -> upsert(runCatching { action.decodeRecord<MessageDto>() }.getOrNull())
            is PostgresAction.Update -> upsert(runCatching { action.decodeRecord<MessageDto>() }.getOrNull())
            is PostgresAction.Delete -> {
                // `messages`-এ কোনো DELETE RLS policy নেই (ধাপ ২১-এ verify করা, MIGRATION_PROGRESS.md
                // দেখুন) -- defensively হ্যান্ডল করা, স্বাভাবিক অবস্থায় আসার কথা না।
                extractId(action.oldRecord)?.let { database.messageDao().deleteMessage(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // transactions channel — FirebaseSyncManager.handleTransactionsSnapshot()-এর মডেল অনুসরণ
    // করে। কোনো conflict-resolution নেই ওখানেও (লেজার এন্ট্রি একবার লেখা হলে immutable ধরা হয়),
    // তাই এখানেও সরাসরি overwrite।
    // ============================================================
    private suspend fun handleTransactionAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["transactions"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<TransactionDto>() }.getOrNull() ?: return
                database.transactionDao().insertTransaction(dto.toTransactionEntity())
                // Ground Rule ২১, সেশন ২.২০ — genuine realtime INSERT (bulk-pull এই পাথ দিয়ে যায়
                // না, Users/Withdrawal-এর প্যাটার্নেই)।
                markRecentlyInserted(_recentlyChangedTransactionIds, dto.id)
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<TransactionDto>() }.getOrNull() ?: return
                database.transactionDao().insertTransaction(dto.toTransactionEntity())
                // Ground Rule ২১ — ব্যবহারকারীর কনফার্মড সিদ্ধান্ত Insert-only, তাই Update-এ
                // markRecentlyInserted() কল হয় না, শুধু বিদ্যমান save লজিক অপরিবর্তিত।
            }
            is PostgresAction.Delete -> {
                // `transactions`-এ কোনো DELETE RLS policy নেই -- defensively হ্যান্ডল করা।
                extractId(action.oldRecord)?.let { database.transactionDao().deleteTransaction(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // escrows channel — FirebaseSyncManager.handleEscrowsSnapshot()-এর মডেল অনুসরণ করে, দুটো
    // merge-লজিক একসাথে রেপ্লিকেট করা হয়েছে:
    // (ক) staleness guard: local.updatedAt >= incoming.updatedAt হলে local-এর extraAmount/
    //     updatedAt রাখা হয় (in-flight cloud write-এর stale echo যেন status change হারিয়ে না
    //     ফেলে, কিন্তু extraAmount flicker না করে)।
    // (খ) resolveIncomingEscrowStatus()-এর সমতুল্য: এখনো HELD দেখানো কিন্তু problem আসলে
    //     cancelled/re-broadcast হয়ে গেছে বা escrow-scoped REFUND transaction আগেই আছে এমন
    //     escrow-কে defensively REFUNDED হিসেবে reclassify করা হয়।
    // ============================================================
    // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৫-৭] visibility private → internal ও suspend বাদ দেওয়া
    // হয়েছে (body-তে কোনো suspend কল নেই, শুধু pure local logic) — যাতে
    // `ExampleRobolectricTest.kt` (আগে `FirebaseSyncManager.resolveIncomingEscrowStatus` টেস্ট
    // করতো, FirebaseSyncManager.kt ডিলিট হওয়ায় এখন এটাই সমতুল্য টার্গেট) সরাসরি কল করতে পারে।
    // Logic/parameter/আচরণ অপরিবর্তিত।
    internal fun resolveIncomingEscrowStatus(
        escrow: EscrowEntity,
        problem: ProblemEntity?,
        hasEscrowScopedRefund: Boolean
    ): EscrowEntity {
        val belongsToCurrentCycle = problem != null &&
            !problem.acceptedSolverId.isNullOrBlank() &&
            problem.acceptedSolverId == escrow.solverId
        val isProblemCancelled = problem != null && !belongsToCurrentCycle && (
            problem.status == "CANCELLED" ||
                (problem.status == "OPEN" && problem.acceptedSolverId.isNullOrBlank())
            )
        return if (escrow.status.equals("HELD", ignoreCase = true) && (isProblemCancelled || hasEscrowScopedRefund)) {
            escrow.copy(status = "REFUNDED", releasedAt = escrow.releasedAt ?: System.currentTimeMillis())
        } else {
            escrow
        }
    }

    private suspend fun mergeAndSaveEscrow(database: AppDatabase, dto: EscrowDto): EscrowEntity {
        var incoming = dto.toEscrowEntity()
        val local = database.escrowDao().getByProblemId(incoming.problemId)
        if (local != null && local.updatedAt >= incoming.updatedAt) {
            incoming = incoming.copy(extraAmount = local.extraAmount, updatedAt = local.updatedAt)
        }
        val problem = database.problemDao().getProblemById(incoming.problemId)
        val hasRefund = database.transactionDao().getTransactionsForProblem(incoming.problemId)
            .any { it.type == "REFUND" && it.escrowId == incoming.id }
        return resolveIncomingEscrowStatus(incoming, problem, hasRefund)
    }

    private suspend fun handleEscrowAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["escrows"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<EscrowDto>() }.getOrNull() ?: return
                database.escrowDao().insertEscrow(mergeAndSaveEscrow(database, dto))
                // Ground Rule ২১, সেশন ২.১৯.৬ — genuine realtime INSERT (bulk-pull এই পাথ দিয়ে যায়
                // না, Users/Withdrawal-এর প্যাটার্নেই)।
                markRecentlyInserted(_recentlyChangedEscrowIds, dto.id)
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<EscrowDto>() }.getOrNull() ?: return
                // Ground Rule ২১ — ব্যবহারকারী পরে সিদ্ধান্ত বদলে জানিয়েছেন Update-এ pulse লাগবে
                // না, শুধু genuine Insert-এই। তাই এখানে আর markRecentlyInserted() কল হয় না, শুধু
                // বিদ্যমান merge/save লজিক অপরিবর্তিত।
                database.escrowDao().insertEscrow(mergeAndSaveEscrow(database, dto))
            }
            is PostgresAction.Delete -> {
                // `escrows`-এ কোনো DELETE RLS policy নেই -- defensively হ্যান্ডল করা।
                extractId(action.oldRecord)?.let { database.escrowDao().deleteEscrow(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // gateway_payments channel — FirebaseSyncManager.handleGatewayPaymentsSnapshot()-এর মডেল
    // অনুসরণ করে, কোনো বিশেষ merge/staleness লজিক নেই মূল ফাংশনেও।
    // ============================================================
    private suspend fun handleGatewayPaymentAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["gateway_payments"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<GatewayPaymentDto>() }.getOrNull() ?: return
                database.gatewayPaymentDao().insertPayment(dto.toGatewayPaymentEntity())
                // Ground Rule ২১, সেশন ২.২০ — genuine realtime INSERT (bulk-pull এই পাথ দিয়ে যায়
                // না)।
                markRecentlyInserted(_recentlyChangedGatewayPaymentIds, dto.id)
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<GatewayPaymentDto>() }.getOrNull() ?: return
                database.gatewayPaymentDao().insertPayment(dto.toGatewayPaymentEntity())
                // Ground Rule ২১ — ব্যবহারকারীর কনফার্মড সিদ্ধান্ত Insert-only, তাই Update-এ
                // (যেমন PENDING→SUCCESS/FAILED status বদল) markRecentlyInserted() কল হয় না, শুধু
                // বিদ্যমান save লজিক অপরিবর্তিত।
            }
            is PostgresAction.Delete -> {
                // `gateway_payments`-এ কোনো DELETE RLS policy নেই -- defensively হ্যান্ডল করা।
                extractId(action.oldRecord)?.let { database.gatewayPaymentDao().deletePayment(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // additional_charges channel (ধাপ ৩৪, ব্যবহারকারীর অনুরোধে) — gateway_payments-এর মতোই কোনো
    // বিশেষ merge/staleness লজিক নেই (status PENDING→ACCEPTED/REJECTED একবারই বদলায়, conflict
    // হওয়ার সুযোগ কম), তাই সরাসরি overwrite।
    // ============================================================
    private suspend fun handleAdditionalChargeAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["additional_charges"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<AdditionalChargeDto>() }.getOrNull() ?: return
                database.additionalChargeDao().insert(dto.toAdditionalChargeEntity())
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<AdditionalChargeDto>() }.getOrNull() ?: return
                database.additionalChargeDao().insert(dto.toAdditionalChargeEntity())
            }
            is PostgresAction.Delete -> {
                // `additional_charges`-এ কোনো DELETE RLS policy নেই -- defensively হ্যান্ডল করা।
                extractId(action.oldRecord)?.let { database.additionalChargeDao().delete(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // notifications channel (ধাপ ৩৫, ব্যবহারকারীর অনুরোধে) — gateway_payments/additional_charges-এর
    // মতোই কোনো বিশেষ merge/staleness লজিক নেই, সরাসরি overwrite। DELETE-ও হ্যান্ডল করা হলো কারণ
    // `admin_delete_notification_group` RPC আসলেই row মুছে দেয় (অন্য কিছু টেবিলের মতো শুধু
    // defensive না -- এখানে সত্যিই ব্যবহার হয়), মুছে যাওয়া notification সাথে সাথে ইউজারের ডিভাইস
    // থেকেও সরে যাওয়া উচিত।
    // ============================================================
    private suspend fun handleNotificationAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["notifications"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<NotificationDto>() }.getOrNull() ?: return
                database.notificationDao().insertNotification(dto.toNotificationEntity())
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<NotificationDto>() }.getOrNull() ?: return
                database.notificationDao().insertNotification(dto.toNotificationEntity())
            }
            is PostgresAction.Delete -> {
                extractId(action.oldRecord)?.let { database.notificationDao().deleteNotification(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // Realtime Scoping ফিক্স, ধাপ ১ (notifications pilot, dual-run) — `user:<uuid>` private
    // broadcast topic subscription। DB-সাইড (migration `realtime_scoping_step1_notifications_
    // broadcast.sql`, নিচে `supabase/migrations/`-এ যোগ করা হলো) SQL দিয়ে সরাসরি টেস্ট করে
    // যাচাই করা হয়েছে (REALTIME_SCOPING_PROGRESS.md, "ধাপ ১" এন্ট্রি) — partition-সমস্যা সমাধান
    // হওয়ার পর broadcast সঠিক topic-এই পৌঁছায় (INSERT/UPDATE/DELETE তিনটাই), ভুল topic-এ যায় না,
    // আর RLS policy অন্য ইউজারের topic subscribe করার চেষ্টা সঠিকভাবে reject করে।
    //
    // ⚠️ এই অংশের payload-ডিকোডিং ([NotificationChangeBroadcastPayload]) build-এ verify করা হয়নি
    // (network/Gradle নেই এই session-এ) — `record`/`old_record` key-নাম Supabase-এর অফিসিয়াল
    // ডকুমেন্টেশনের `realtime.broadcast_changes()` trigger-ফাংশন উদাহরণ (NEW/OLD positional args)
    // থেকে অনুমান করে লেখা, ফাইলের শীর্ষের নোটেও এই ক্যাভিয়েট আছে। প্রথম Android Studio build/
    // device test-এ অগ্রাধিকার দিয়ে verify করা উচিত।
    // ============================================================

    @Serializable
    data class NotificationChangeBroadcastPayload(
        val record: NotificationDto? = null,
        @SerialName("old_record") val oldRecord: NotificationDto? = null
    )

    /**
     * normal (non-admin) session-এর জন্য `user:<uuid>` topic-এ private broadcast channel
     * subscribe করে, আর তাতে notifications-এর INSERT/UPDATE/DELETE — তিনটা broadcast event-ই
     * আলাদাভাবে শোনে। dual-run: পুরনো টেবিল-ওয়াইড `notificationsChannel` (postgresChangeFlow,
     * উপরে) অক্ষত থাকে, এটা প্রতিস্থাপন করে না।
     *
     * idempotent — একই/ভিন্ন userId দিয়ে বারবার কল হলে (একাধিকবার attachDatabase()/
     * startRealtimeListeners(), বা role/user পরিবর্তনে) আগেরটা বন্ধ করে নতুন করে subscribe করে,
     * leak এড়াতে।
     */
    private suspend fun startUserTopicBroadcastSubscription(userId: String) {
        userTopicBroadcastChannel?.let { runCatching { it.unsubscribe() } }
        userTopicBroadcastChannel = null

        val client = SupabaseClientProvider.client
        val ch = client.realtime.channel("user:$userId") {
            isPrivate = true
        }

        // ⚠️ Step 13.3/13.4 (CI Test Suite session, ২০২৬-০৯-২২, ব্যবহারকারীর স্পষ্ট অনুমতিতে) —
        // আগে এখানে bare event ("INSERT"/"UPDATE"/"DELETE") ব্যবহার হতো, ইচ্ছাকৃতভাবেই (উপরে ও
        // নিচের ধাপ-২/ধাপ-৫ কমেন্টে পুরনো ব্যাখ্যা ছিল)। কিন্তু সেটা users-এর আগের বাগের (fix হয়ে
        // গেছে) মতোই একটা residual naming-collision ঝুঁকি রেখে দিচ্ছিল — ভবিষ্যতে এই একই
        // "user:<id>" topic-এ আরেকটা bare-event trigger যোগ হলে notifications-এর সাথে গুলিয়ে
        // যেতে পারত। তাই এখন বাকি ৬টা টেবিলের মতোই table-prefixed ("notifications_"+op) করা
        // হলো (DB-সাইড migration
        // `supabase/migrations/zz_20260913085320_notifications_broadcast_event_naming_fix.sql`
        // দেখুন, একই সাথে যোগ হয়েছে)।
        listOf("INSERT", "UPDATE", "DELETE").forEach { op ->
            ch.broadcastFlow<NotificationChangeBroadcastPayload>(event = "notifications_$op")
                .onEach { payload -> applyNotificationBroadcastChange(op, payload) }
                .launchIn(managerScope)
        }

        // Realtime Scoping ফিক্স, ধাপ ২ (wallet/money গ্রুপ, dual-run) — একই "user:$userId" channel
        // পুনর্ব্যবহার করা হচ্ছে (নতুন channel না, DB-সাইড RLS policy-ও topic-প্যাটার্নভিত্তিক, টেবিল-
        // নির্দিষ্ট না)। এই ৪টা টেবিলের event নাম table-prefixed
        // (migration `realtime_scoping_step2_wallet_event_naming_fix.sql`, DB version
        // 20260913093831 দেখুন) — notifications-এর bare "INSERT"/"UPDATE"/"DELETE" থেকে ইচ্ছাকৃতভাবে
        // আলাদা, যাতে একই topic-এ ৫টা ভিন্ন টেবিলের broadcast event নাম-সংঘর্ষে ভুল টেবিলের payload
        // হিসেবে ডিকোড হওয়ার চেষ্টা না করে (silent event-miss/misroute ঝুঁকি, নিয়ম #৮)।
        listOf("INSERT", "UPDATE", "DELETE").forEach { op ->
            ch.broadcastFlow<TransactionChangeBroadcastPayload>(event = "transactions_$op")
                .onEach { payload -> applyTransactionBroadcastChange(op, payload) }
                .launchIn(managerScope)
            ch.broadcastFlow<WithdrawalChangeBroadcastPayload>(event = "withdrawals_$op")
                .onEach { payload -> applyWithdrawalBroadcastChange(op, payload) }
                .launchIn(managerScope)
            ch.broadcastFlow<GatewayPaymentChangeBroadcastPayload>(event = "gateway_payments_$op")
                .onEach { payload -> applyGatewayPaymentBroadcastChange(op, payload) }
                .launchIn(managerScope)
            ch.broadcastFlow<AdditionalChargeChangeBroadcastPayload>(event = "additional_charges_$op")
                .onEach { payload -> applyAdditionalChargeBroadcastChange(op, payload) }
                .launchIn(managerScope)
        }

        // Realtime Scoping ফিক্স, ধাপ ৫ (ঐচ্ছিক — users/escrows, dual-run) — একই "user:$userId"
        // channel পুনর্ব্যবহার। উভয় টেবিলের event নামই table-prefixed (`users_$op`/`escrows_$op`)
        // — ধাপ ২-এর wallet-গ্রুপের মতোই bare TG_OP ব্যবহার করলে notifications-এর bare
        // "INSERT"/"UPDATE"/"DELETE"-এর সাথে collide করতো (এই সেশনেই প্রথমে এই বাগ ধরা পড়ে DB-সাইডে
        // ঠিক করা হয়েছে, migration `fix_users_broadcast_event_naming_collision.sql` দেখুন)।
        listOf("INSERT", "UPDATE", "DELETE").forEach { op ->
            ch.broadcastFlow<UserChangeBroadcastPayload>(event = "users_$op")
                .onEach { payload -> applyUserBroadcastChange(op, payload) }
                .launchIn(managerScope)
            ch.broadcastFlow<EscrowChangeBroadcastPayload>(event = "escrows_$op")
                .onEach { payload -> applyEscrowBroadcastChange(op, payload) }
                .launchIn(managerScope)
        }

        ch.subscribeWithTimeout("user_topic_broadcast")
        userTopicBroadcastChannel = ch
    }

    // ============================================================
    // Realtime Scoping ফিক্স, ধাপ ২ (wallet/money গ্রুপ: transactions/withdrawals/
    // gateway_payments/additional_charges, dual-run) — DB-সাইড migration
    // (`realtime_scoping_step2_wallet_broadcast.sql` + `realtime_scoping_step2_wallet_event_naming_
    // fix.sql`) সরাসরি realtime.broadcast_changes() কল করে দুইটা ভিন্ন test topic দিয়ে verify করা
    // হয়েছে (REALTIME_SCOPING_PROGRESS.md, "ধাপ ২" এন্ট্রি দেখুন) — table-prefixed event নাম সঠিক
    // topic-এই যাচ্ছে, কোনো কোল্লিশন নেই। dev DB-তে এই ৪টা টেবিল বর্তমানে খালি বলে real-row
    // insert/update দিয়ে trigger fire করিয়ে টেস্ট করা যায়নি (mechanism-level SQL টেস্টেই সীমাবদ্ধ) —
    // পুরনো টেবিল-ওয়াইড channel-গুলো (transactionsChannel/withdrawalsChannel/gatewayPaymentsChannel/
    // additionalChargesChannel) dual-run নীতি অনুযায়ী অক্ষত রাখা হলো, প্রতিস্থাপন করা হয়নি।
    //
    // ⚠️ এই অংশও (ধাপ ১-এর মতোই) build-এ verify করা হয়নি (network/Gradle নেই এই session-এ)।
    // ============================================================

    @Serializable
    data class TransactionChangeBroadcastPayload(
        val record: TransactionDto? = null,
        @SerialName("old_record") val oldRecord: TransactionDto? = null
    )

    @Serializable
    data class WithdrawalChangeBroadcastPayload(
        val record: WithdrawalDto? = null,
        @SerialName("old_record") val oldRecord: WithdrawalDto? = null
    )

    @Serializable
    data class GatewayPaymentChangeBroadcastPayload(
        val record: GatewayPaymentDto? = null,
        @SerialName("old_record") val oldRecord: GatewayPaymentDto? = null
    )

    @Serializable
    data class AdditionalChargeChangeBroadcastPayload(
        val record: AdditionalChargeDto? = null,
        @SerialName("old_record") val oldRecord: AdditionalChargeDto? = null
    )

    // Realtime Scoping ফিক্স, ধাপ ৫ (ঐচ্ছিক — users/escrows) payload ক্লাস, একই "user:$userId"
    // channel-এ table-prefixed event নামে (উপরে দেখুন)।
    @Serializable
    data class UserChangeBroadcastPayload(
        val record: UserDto? = null,
        @SerialName("old_record") val oldRecord: UserDto? = null
    )

    @Serializable
    data class EscrowChangeBroadcastPayload(
        val record: EscrowDto? = null,
        @SerialName("old_record") val oldRecord: EscrowDto? = null
    )

    /** [applyNotificationBroadcastChange]-এর মতোই প্যাটার্ন — postgresChangeFlow পথের
     *  [handleTransactionAction]-এর সমান আচরণ, দুই পথই idempotent upsert/delete করে বলে dual-run
     *  নিরাপদ। */
    private suspend fun applyTransactionBroadcastChange(operation: String, payload: TransactionChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["transactions_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.transactionDao().insertTransaction(dto.toTransactionEntity())
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.transactionDao().deleteTransaction(dto.id)
            }
        }
    }

    private suspend fun applyWithdrawalBroadcastChange(operation: String, payload: WithdrawalChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["withdrawals_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.withdrawalDao().insertWithdrawal(dto.toWithdrawalEntity())
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.withdrawalDao().deleteWithdrawal(dto.id)
            }
        }
    }

    private suspend fun applyGatewayPaymentBroadcastChange(operation: String, payload: GatewayPaymentChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["gateway_payments_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.gatewayPaymentDao().insertPayment(dto.toGatewayPaymentEntity())
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.gatewayPaymentDao().deletePayment(dto.id)
            }
        }
    }

    private suspend fun applyAdditionalChargeBroadcastChange(operation: String, payload: AdditionalChargeChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["additional_charges_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.additionalChargeDao().insert(dto.toAdditionalChargeEntity())
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.additionalChargeDao().delete(dto.id)
            }
        }
    }

    /** [applyNotificationBroadcastChange]-এর মতোই প্যাটার্ন — postgresChangeFlow পথের
     *  [handleUserAction]-এর সমান আচরণ (একই [mergeAndSaveUser] merge-logic পুনর্ব্যবহার করা হয়েছে,
     *  password-preserve + balance-flicker-guard দুটো পথেই একইভাবে কাজ করবে)। */
    private suspend fun applyUserBroadcastChange(operation: String, payload: UserChangeBroadcastPayload) {
        val database = localDb ?: return
        // পুরনো টেবিল-ওয়াইড usersChannel-এর health-key ("users") থেকে ইচ্ছাকৃতভাবে আলাদা রাখা হলো,
        // ঠিক notifications/wallet-গ্রুপের মতোই (নিয়ম #৯)।
        listenerLastHeardMs["users_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.userDao().insertUser(mergeAndSaveUser(database, dto))
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.userDao().deleteUser(dto.id)
            }
        }
    }

    /** [applyTransactionBroadcastChange]-এর dual-owner প্যাটার্নের মতোই — postgresChangeFlow পথের
     *  [handleEscrowAction]-এর সমান আচরণ, একই [mergeAndSaveEscrow] (stale-snapshot-skip +
     *  resolveIncomingEscrowStatus) পুনর্ব্যবহার করা হয়েছে। owner ও solver দুইজনের ডিভাইসেই এই
     *  একই event আলাদাভাবে আসবে (DB trigger দুইবার broadcast করে), দুটোই idempotent upsert তাই
     *  নিরাপদ। */
    private suspend fun applyEscrowBroadcastChange(operation: String, payload: EscrowChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["escrows_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.escrowDao().insertEscrow(mergeAndSaveEscrow(database, dto))
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.escrowDao().deleteEscrow(dto.id)
            }
        }
    }

    /** [startUserTopicBroadcastSubscription]-এর কাউন্টারপার্ট — logout/admin-এ switch/stopRealtimeListeners()-এ কল হবে। */
    private suspend fun stopUserTopicBroadcastSubscription() {
        userTopicBroadcastChannel?.let { runCatching { it.unsubscribe() } }
        userTopicBroadcastChannel = null
    }

    // ============================================================
    // Realtime Scoping ফিক্স, ধাপ ৩ (Chat: `messages`, dual-run) — Topic: `problem:<problem_id>`।
    // notifications/wallet-এর মতো "সবসময় নিজের topic-এ subscribed থাকা" প্যাটার্ন থেকে আলাদা —
    // এটা resource-scoped, screen lifecycle-এর সাথে বাঁধা (ঠিক নিচের typing channel-এর
    // [joinTypingChannel]/[leaveTypingChannel]-এর মতোই idempotent join/leave map প্যাটার্ন
    // অনুসরণ করা হয়েছে) — ChatScreen খোলা থাকা অবস্থায় শুধু সেই problemId-র channel subscribe,
    // screen ছাড়লে unsubscribe (memory/connection leak এড়াতে)। DB-সাইড trigger একবারই broadcast
    // করে (dual-broadcast লাগে না, wallet গ্রুপের মতো না, যেহেতু topic resource-based, user-based
    // না) -- owner ও accepted_solver উভয়েই একই topic থেকে পাবে। RLS policy (`problem participants
    // can receive problem-topic broadcasts`, migration `realtime_scoping_step3_messages_
    // broadcast.sql`) `public.messages`-এর বিদ্যমান `messages_select` policy-র owner/
    // accepted_solver লজিকের সাথে সামঞ্জস্যপূর্ণ রাখা হয়েছে (সাধারণ bidder, accepted হওয়ার আগে,
    // যেহেতু আসল chat message-ও দেখে না)। Event নাম table-prefixed (`messages_$op`) -- ধাপ ৪-এ
    // bids-ও এই একই topic ব্যবহার করবে বলে (roadmap অনুযায়ী), শুরু থেকেই collision এড়ানো হলো
    // (ধাপ ২-তে wallet গ্রুপে যে বাগ পাওয়া গিয়েছিল সেটা এখানে আর হবে না)।
    //
    // টেস্ট (REALTIME_SCOPING_PROGRESS.md, "ধাপ ৩" এন্ট্রি): dev DB-তে `messages`/`problems`
    // দুটোই খালি থাকায় real-row দিয়ে trigger fire করানো যায়নি -- সরাসরি realtime.broadcast_
    // changes() কল করে দুইটা ভিন্ন problem topic-এ mechanism-level verify করা হয়েছে, cross-talk
    // হয়নি।
    //
    // ⚠️ এই অংশও (আগের ধাপগুলোর মতোই) build-এ verify করা হয়নি (network/Gradle নেই এই session-এ)।
    // ============================================================

    @Serializable
    data class MessageChangeBroadcastPayload(
        val record: MessageDto? = null,
        @SerialName("old_record") val oldRecord: MessageDto? = null
    )

    // typingChannels-এর মতোই আলাদা map -- একই problemId-এর জন্য typing আর chat-message broadcast
    // দুটো আলাদা channel/topic (`typing_problem_<id>` বনাম `problem:<id>`), তাই একে অপরের
    // lifecycle-এ হস্তক্ষেপ করে না।
    private val messagesBroadcastChannels = mutableMapOf<String, RealtimeChannel>()

    /**
     * `problemId`-এর `problem:<id>` private broadcast channel এখনো join করা না থাকলে join করে
     * (idempotent) এবং messages-এর INSERT/UPDATE/DELETE তিনটা event আলাদাভাবে শুনে Room-এ upsert/
     * delete করে। ChatScreen খোলার সময় কল করার জন্য প্রস্তুত -- dual-run, পুরনো টেবিল-ওয়াইড
     * `messagesChannel` (postgresChangeFlow) অক্ষত থাকে।
     */
    suspend fun joinProblemMessagesBroadcastChannel(problemId: String): RealtimeChannel {
        messagesBroadcastChannels[problemId]?.let { return it }

        val client = SupabaseClientProvider.client
        val ch = client.realtime.channel("problem:$problemId") {
            isPrivate = true
        }

        listOf("INSERT", "UPDATE", "DELETE").forEach { op ->
            ch.broadcastFlow<MessageChangeBroadcastPayload>(event = "messages_$op")
                .onEach { payload -> applyMessageBroadcastChange(op, payload) }
                .launchIn(managerScope)
        }

        // [Somadhan Bug-Fix Step 7.6] subscribeWithTimeout(): bare subscribe() চিরকাল suspend থাকতে পারত (১.২-এর একই root cause)
        ch.subscribeWithTimeout("problem:$problemId messages")
        messagesBroadcastChannels[problemId] = ch
        return ch
    }

    /** [joinProblemMessagesBroadcastChannel]-এর কাউন্টারপার্ট — ChatScreen বন্ধ হওয়ার সময় (dispose) কল করার জন্য। */
    suspend fun leaveProblemMessagesBroadcastChannel(problemId: String) {
        messagesBroadcastChannels.remove(problemId)?.let { runCatching { it.unsubscribe() } }
    }

    // ============================================================
    // Realtime Scoping ফিক্স, ধাপ ৪ (Bids: `bids`, dual-run) — Topic: `problem:<problem_id>:bids`,
    // ইচ্ছাকৃতভাবে messages-এর `problem:<problem_id>` থেকে আলাদা রাখা হয়েছে। কারণ: bids_select
    // RLS visibility messages_select-এর চেয়ে ঢের বেশি open -- ধাপ ২৩-এ ব্যবহারকারীর স্পষ্ট নির্দেশে
    // ("Firebase-এর মতোই সম্পূর্ণ ফাংশনালিটি চাই") bids_select-কে OPEN+public problem-এর যেকোনো
    // ভিজিটরের জন্য খুলে দেওয়া হয়েছিল (দেখুন ProblemDetailScreen.kt-র সংশ্লিষ্ট কমেন্ট আর
    // migration `step23_bids_select_open_public_visibility.sql`)। messages-এর topic-এ এই broad
    // visibility যোগ করলে chat-এর privacy boundary (শুধু owner/accepted_solver) ভেঙে যেত -- তাই
    // bids-এর জন্য নিজস্ব topic + নিজস্ব RLS policy (`problem bids visibility broadcasts`,
    // migration `realtime_scoping_step4_bids_broadcast.sql`), কিন্তু policy-র শর্ত হুবহু
    // `bids_select`-এর সাথে মিলিয়ে লেখা হয়েছে -- ঠিক এই Firebase-parity সিদ্ধান্তেরই ধারাবাহিকতা।
    // ProblemDetailScreen খোলা থাকা অবস্থায় dynamic subscribe (ChatScreen-এর ধাপ ৩-প্যাটার্নের
    // মতোই lifecycle-bound), screen ছাড়লে unsubscribe। Event নাম table-prefixed (`bids_$op`)।
    //
    // টেস্ট (REALTIME_SCOPING_PROGRESS.md, "ধাপ ৪" এন্ট্রি): dev DB-তে `bids`/`problems` দুটোই খালি
    // থাকায় real-row দিয়ে trigger fire করানো যায়নি -- সরাসরি realtime.broadcast_changes() কল করে
    // দুইটা ভিন্ন problem topic-এ mechanism-level verify করা হয়েছে (INSERT ও UPDATE দুই event-টাইপ
    // দিয়েই), cross-talk হয়নি। RLS policy-র শর্ত structurally bids_select-এর সাথে মিলিয়ে
    // যাচাই করা হয়েছে (real-data দিয়ে পজিটিভ/নেগেটিভ simulation dev DB-তে সম্ভব হয়নি)।
    //
    // ⚠️ এই অংশও (আগের ধাপগুলোর মতোই) build-এ verify করা হয়নি (network/Gradle নেই এই session-এ)।
    // ============================================================

    @Serializable
    data class BidChangeBroadcastPayload(
        val record: BidDto? = null,
        @SerialName("old_record") val oldRecord: BidDto? = null
    )

    // messagesBroadcastChannels-এর মতোই আলাদা map -- একই problemId-এর জন্য bids-broadcast ও
    // chat-message-broadcast দুটো আলাদা topic (`problem:<id>:bids` বনাম `problem:<id>`), তাই একে
    // অপরের lifecycle-এ হস্তক্ষেপ করে না।
    private val bidsBroadcastChannels = mutableMapOf<String, RealtimeChannel>()

    /**
     * `problemId`-এর `problem:<id>:bids` private broadcast channel এখনো join করা না থাকলে join
     * করে (idempotent) এবং bids-এর INSERT/UPDATE/DELETE তিনটা event আলাদাভাবে শুনে Room-এ
     * upsert/delete করে -- submit/accept/reject/cancel সব ঘটনাই UPDATE/INSERT হিসেবে আসে।
     * ProblemDetailScreen খোলার সময় কল করার জন্য প্রস্তুত -- dual-run, পুরনো টেবিল-ওয়াইড
     * `bidsChannel` (postgresChangeFlow) অক্ষত থাকে।
     */
    suspend fun joinProblemBidsBroadcastChannel(problemId: String): RealtimeChannel {
        bidsBroadcastChannels[problemId]?.let { return it }

        val client = SupabaseClientProvider.client
        val ch = client.realtime.channel("problem:$problemId:bids") {
            isPrivate = true
        }

        listOf("INSERT", "UPDATE", "DELETE").forEach { op ->
            ch.broadcastFlow<BidChangeBroadcastPayload>(event = "bids_$op")
                .onEach { payload -> applyBidBroadcastChange(op, payload) }
                .launchIn(managerScope)
        }

        // [Somadhan Bug-Fix Step 7.6] subscribeWithTimeout(): bare subscribe() চিরকাল suspend থাকতে পারত (১.২-এর একই root cause)
        ch.subscribeWithTimeout("problem:$problemId bids")
        bidsBroadcastChannels[problemId] = ch
        return ch
    }

    /** [joinProblemBidsBroadcastChannel]-এর কাউন্টারপার্ট — ProblemDetailScreen বন্ধ হওয়ার সময় (dispose) কল করার জন্য। */
    suspend fun leaveProblemBidsBroadcastChannel(problemId: String) {
        bidsBroadcastChannels.remove(problemId)?.let { runCatching { it.unsubscribe() } }
    }

    /**
     * [handleBidAction] (postgresChangeFlow পথ)-এর মতোই আচরণ -- দুই পথই idempotent upsert/delete
     * করে বলে dual-run নিরাপদ।
     */
    private suspend fun applyBidBroadcastChange(operation: String, payload: BidChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["bids_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.bidDao().insertBid(dto.toBidEntity())
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.bidDao().deleteBid(dto.id)
            }
        }
    }

    /**
     * [handleMessageAction] (postgresChangeFlow পথ)-এর মতোই আচরণ, একই isRead-রেগ্রেশন-গার্ড সহ
     * (local message আগে থেকে read থাকলে আর incoming payload read না থাকলে read-ই থেকে যাবে) --
     * দুই পথই idempotent বলে dual-run নিরাপদ।
     */
    private suspend fun applyMessageBroadcastChange(operation: String, payload: MessageChangeBroadcastPayload) {
        val database = localDb ?: return
        listenerLastHeardMs["messages_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                val incoming = dto.toMessageEntity()
                val local = database.messageDao().getMessageById(incoming.id)
                val finalMsg = if (local != null && local.isRead && !incoming.isRead) {
                    incoming.copy(isRead = true)
                } else incoming
                database.messageDao().insertMessage(finalMsg)
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.messageDao().deleteMessage(dto.id)
            }
        }
    }

    /**
     * broadcast_changes payload থেকে Room-এ upsert/delete করে — ঠিক [handleNotificationAction]
     * (postgresChangeFlow পথ)-এর মতোই আচরণ, শুধু উৎস আলাদা। দুই পথই idempotent upsert/delete
     * করে বলে dual-run নিরাপদ ধরে নেওয়া হলো (একই row দুইবার আসলেও ফলাফল একই)।
     */
    private suspend fun applyNotificationBroadcastChange(operation: String, payload: NotificationChangeBroadcastPayload) {
        val database = localDb ?: return
        // পুরনো টেবিল-ওয়াইড notificationsChannel-এর health-key ("notifications") থেকে ইচ্ছাকৃতভাবে
        // আলাদা রাখা হলো — এই নতুন broadcast পথ স্বাস্থ্যবান থাকলেও পুরনো পথের নিজস্ব stale/fallback
        // হিসাব যেন প্রভাবিত না হয় (নিয়ম #৯, স্কোপের বাইরে কিছু বদলানো হয়নি)।
        listenerLastHeardMs["notifications_broadcast"] = System.currentTimeMillis()
        when (operation) {
            "INSERT", "UPDATE" -> {
                val dto = payload.record ?: return
                database.notificationDao().insertNotification(dto.toNotificationEntity())
            }
            "DELETE" -> {
                val dto = payload.oldRecord ?: return
                database.notificationDao().deleteNotification(dto.id)
            }
        }
    }

    /**
     * `startRealtimeListeners()`-এ কল করার জন্য — বর্তমান সেশন admin কিনা যাচাই করে (local Room-এর
     * `UserEntity.role` দিয়ে, ঠিক `SomadhanViewModel`-এর `user?.role == "ADMIN"` প্যাটার্নের মতোই)।
     * শুধু "কোন channel subscribe করব" সিদ্ধান্তের জন্যই — RLS-নির্ভর অন্য কোথাও client-side
     * compound role-branching নেই (নিয়ম অনুযায়ী সেটাই ঠিক)।
     */
    private suspend fun isCurrentSessionAdmin(userId: String): Boolean {
        val database = localDb ?: return false
        val user = runCatching { database.userDao().getUserById(userId) }.getOrNull()
        return user?.role == "ADMIN"
    }

    // ============================================================
    // withdrawals channel (ধাপ ৩৫, ব্যবহারকারীর অনুরোধে) — একই প্যাটার্ন, কোনো বিশেষ merge/
    // staleness লজিক নেই। solver-এর WithdrawalHistoryScreen আর admin-এর AdminWithdrawalsView দুটোই
    // Room Flow দিয়ে reactive (getWithdrawalsForSolver/getAllWithdrawals), তাই channel থেকে insert
    // হওয়া মাত্র UI নিজে থেকেই আপডেট হয়ে যাবে -- কোনো নতুন UI কোড লাগেনি।
    // ============================================================
    private suspend fun handleWithdrawalAction(action: PostgresAction) {
        val database = localDb ?: return
        listenerLastHeardMs["withdrawals"] = System.currentTimeMillis()
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching { action.decodeRecord<WithdrawalDto>() }.getOrNull() ?: return
                database.withdrawalDao().insertWithdrawal(dto.toWithdrawalEntity())
                // Ground Rule ২১, সেশন ২.১৯.৫ — genuine নতুন PENDING withdrawal request
                // (Users-এর Insert-প্যাটার্নেই, উপরের _recentlyChangedWithdrawalIds কমেন্ট দেখো)।
                markRecentlyInserted(_recentlyChangedWithdrawalIds, dto.id)
            }
            is PostgresAction.Update -> {
                val dto = runCatching { action.decodeRecord<WithdrawalDto>() }.getOrNull() ?: return
                // Ground Rule ২১ — ব্যবহারকারী পরে সিদ্ধান্ত বদলে জানিয়েছেন Update-এ pulse লাগবে
                // না, শুধু genuine Insert-এই (নতুন PENDING request)। তাই এখানে আর
                // markRecentlyInserted() কল হয় না, শুধু বিদ্যমান save লজিক অপরিবর্তিত।
                database.withdrawalDao().insertWithdrawal(dto.toWithdrawalEntity())
            }
            is PostgresAction.Delete -> {
                // `withdrawals`-এ কোনো DELETE RLS policy নেই -- defensively হ্যান্ডল করা।
                extractId(action.oldRecord)?.let { database.withdrawalDao().deleteWithdrawal(it) }
            }
            is PostgresAction.Select -> {}
        }
    }

    // ============================================================
    // typing broadcast — table-backed না, তাই কোনো mergeAndSave*/DAO নেই। প্রতিটা problemId-এর
    // নিজস্ব broadcast channel (`typing_problem_<problemId>`), শুধু সেই সমস্যার সাথে সম্পর্কিত
    // দুই পক্ষ (owner+solver) এই channel-এ join করবে -- এটা caller-সাইড scoping (কোন problemId-এর
    // জন্য joinTypingChannel() কল হচ্ছে সেটাই একমাত্র scoping, RLS/DB-ভিত্তিক না)।
    // ============================================================

    @Serializable
    data class TypingBroadcastPayload(
        @SerialName("problem_id") val problemId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("is_typing") val isTyping: Boolean,
        // FirebaseSyncManager-এর docId ("${problemId}_${userId}") timestamp document field-এর
        // মতোই -- 8-সেকেন্ড staleness guard-এর জন্য পাঠানো হয় (sender-এর device সময়)।
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * `problemId`-এর typing channel এখনো join করা না থাকলে join করে (idempotent) এবং তার
     * broadcastFlow-কে `_typingStatusMap`-এ merge করা শুরু করে। ChatScreen খোলার সময় কল করার
     * জন্য প্রস্তুত রাখা হলো (public, `sendTypingStatus()`-ও ভেতরে এটাই কল করে) -- এই ধাপে (২১)
     * কোথাও থেকে call করা হয় না।
     */
    suspend fun joinTypingChannel(problemId: String): RealtimeChannel {
        typingChannels[problemId]?.let { return it }

        val client = SupabaseClientProvider.client
        val ch = client.realtime.channel("typing_problem_$problemId")
        ch.broadcastFlow<TypingBroadcastPayload>(event = "typing")
            .onEach { payload ->
                val now = System.currentTimeMillis()
                val key = "${payload.problemId}_${payload.userId}"
                val current = _typingStatusMap.value.toMutableMap()
                if (payload.isTyping && (now - payload.timestamp) < 8000L) {
                    current[key] = payload.timestamp
                } else {
                    current.remove(key)
                }
                _typingStatusMap.value = current
            }
            .launchIn(managerScope)
        // [Somadhan Bug-Fix Step 7.6] subscribeWithTimeout(): bare subscribe() চিরকাল suspend থাকতে পারত (১.২-এর একই root cause)
        ch.subscribeWithTimeout("typing_problem_$problemId")
        typingChannels[problemId] = ch
        return ch
    }

    /** ChatScreen বন্ধ হওয়ার সময় (dispose) কল করার জন্য -- channel unsubscribe + সেই problemId-র map entry পরিষ্কার। */
    suspend fun leaveTypingChannel(problemId: String) {
        typingChannels.remove(problemId)?.let { runCatching { it.unsubscribe() } }
        val current = _typingStatusMap.value.toMutableMap()
        current.keys.filter { it.startsWith("${problemId}_") }.forEach { current.remove(it) }
        _typingStatusMap.value = current
    }

    /**
     * `FirebaseSyncManager.setTypingStatus()`-এর সমতুল্য সিগনেচার (fire-and-forget থেকে ভিন্ন --
     * এখানে suspend, কারণ broadcast পাঠানোর আগে channel join করা দরকার হতে পারে; caller-কে
     * `managerScope`/নিজস্ব coroutine থেকে কল করতে হবে)। ধাপ ২২-এ wiring-এর সময়
     * `SomadhanViewModel.setTypingStatus()`-এ এই ফাংশনটাই কল হবে।
     */
    suspend fun sendTypingStatus(problemId: String, userId: String, isTyping: Boolean) {
        val ch = joinTypingChannel(problemId)
        runCatching {
            val payload = TypingBroadcastPayload(problemId, userId, isTyping)
            ch.broadcast(event = "typing", message = Json.encodeToJsonElement(payload).jsonObject)
        }
    }

    // ============================================================
    // ⚠️ গুরুত্বপূর্ণ ডিজাইন গ্যাপ (ধাপ ২২-এ সিদ্ধান্ত নেওয়া দরকার, MIGRATION_PROGRESS.md-এ
    // বিস্তারিত): Firestore-এ admin-এর জন্য এই ৩টা listener bounded ছিল (users: শুধু
    // kycStatus=pending; problems: শুধু isDisputed=true/hasReleaseRequest=true; bids: শুধু
    // সাম্প্রতিক ৫০০টা)। Supabase Postgres Changes RLS দিয়ে "কোন row" ফিল্টার করে কিন্তু
    // Firestore-এর মতো compound/boolean subscription filter (whereEqualTo + orderBy + limit
    // একসাথে) সাপোর্ট করে না -- নিচের channel গুলো তাই আপাতত টেবিল-ওয়াইড (single global channel
    // per table), admin-এর জন্য আলাদা bounded filter এখনো নেই। এই ধাপে (ফাউন্ডেশন) ইচ্ছাকৃতভাবে
    // এই সিদ্ধান্ত পেন্ডিং রাখা হলো -- ধাপ ২২ (cutover)-এ miss না করার জন্য এই কমেন্ট এখানে
    // রাখা হয়েছে।
    // ============================================================

    /**
     * `users`/`problems`/`bids` -- এই ৩টা টেবিলের জন্য global (টেবিল-ওয়াইড) Postgres Changes
     * channel সাবস্ক্রাইব করে। কল করার আগে আগের কোনো channel active থাকলে প্রথমে বন্ধ করে দেয়
     * (leak-guard, `SupabaseSyncManager`-এর প্যাটার্ন অনুসরণ করে)।
     *
     * **এই ধাপে (২০) এই ফাংশন কোথাও থেকে call করা হয় না** -- শুধু পরের ধাপগুলোর জন্য প্রস্তুত।
     */
    suspend fun startRealtimeListeners() {
        stopRealtimeListeners()

        val client = SupabaseClientProvider.client

        val uCh = client.realtime.channel("realtime-manager-users")
        uCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "users" }
            .onEach { action -> handleUserAction(action) }
            .launchIn(managerScope)
        usersChannel = uCh

        val pCh = client.realtime.channel("realtime-manager-problems")
        pCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "problems" }
            .onEach { action -> handleProblemAction(action) }
            .launchIn(managerScope)
        problemsChannel = pCh

        val bCh = client.realtime.channel("realtime-manager-bids")
        bCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "bids" }
            .onEach { action -> handleBidAction(action) }
            .launchIn(managerScope)
        bidsChannel = bCh

        // ধাপ ২১: আরও ৪টা table-backed channel
        val mCh = client.realtime.channel("realtime-manager-messages")
        mCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "messages" }
            .onEach { action -> handleMessageAction(action) }
            .launchIn(managerScope)
        messagesChannel = mCh

        val tCh = client.realtime.channel("realtime-manager-transactions")
        tCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "transactions" }
            .onEach { action -> handleTransactionAction(action) }
            .launchIn(managerScope)
        transactionsChannel = tCh

        val eCh = client.realtime.channel("realtime-manager-escrows")
        eCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "escrows" }
            .onEach { action -> handleEscrowAction(action) }
            .launchIn(managerScope)
        escrowsChannel = eCh

        val gCh = client.realtime.channel("realtime-manager-gateway-payments")
        gCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "gateway_payments" }
            .onEach { action -> handleGatewayPaymentAction(action) }
            .launchIn(managerScope)
        gatewayPaymentsChannel = gCh

        // ধাপ ৩৪ (ব্যবহারকারীর অনুরোধে): additional_charges — এখন solver extra-bill request
        // করলে সাথে সাথেই customer-এর local Room-এ (আর তার মাধ্যমে ProblemDetailScreen-এর
        // getPendingAdditionalCharges() reactive Flow-তে) চলে আসবে।
        val acCh = client.realtime.channel("realtime-manager-additional-charges")
        acCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "additional_charges" }
            .onEach { action -> handleAdditionalChargeAction(action) }
            .launchIn(managerScope)
        additionalChargesChannel = acCh

        // ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে): notifications — আগে শুধু local Room-এ (write-time) আর
        // app-restart/pull-to-refresh-এ (bulk pull) দেখা যেত, এখন থেকে অন্য কোনো ডিভাইস/admin
        // panel থেকে notification পাঠানো/মুছে ফেলা হলে সাথে সাথে দেখা যাবে।
        val nCh = client.realtime.channel("realtime-manager-notifications")
        nCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "notifications" }
            .onEach { action -> handleNotificationAction(action) }
            .launchIn(managerScope)
        notificationsChannel = nCh

        // ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে): withdrawals — solver-এর withdraw request/admin-এর
        // approve-reject এখন সাথে সাথে WithdrawalHistoryScreen/AdminWithdrawalsView-এ লাইভ দেখা
        // যাবে, আগের মতো app restart/pull-to-refresh-এর অপেক্ষা করতে হবে না।
        val wCh = client.realtime.channel("realtime-manager-withdrawals")
        wCh.postgresChangeFlow<PostgresAction>(schema = "public") { table = "withdrawals" }
            .onEach { action -> handleWithdrawalAction(action) }
            .launchIn(managerScope)
        withdrawalsChannel = wCh

        // [Somadhan Bug-Fix — গ্রুপ ১, নতুন সাব-বাগ ১.৪, Step 6-এর regression-fix (১.২)-এর
        // নিজেরই regression] ১.২-এর ফিক্স প্রতিটা subscribe()-এ ১০ সেকেন্ড timeout দিলেও, উপরের
        // ১০টা subscribeWithTimeout() কল **sequentially (একটার পর একটা, await করে) চলছিল** --
        // network/WebSocket সমস্যায় প্রতিটা call নিজের পুরো ১০ সেকেন্ড টাইমআউট *একে একে* ব্যবহার
        // করলে মোট worst-case wait ~১০×১০=১০০ সেকেন্ড (+ নিচের broadcast subscription আরও ১০)
        // হয়ে যায় -- login আর চিরকালের জন্য আটকে থাকে না ঠিকই, কিন্তু বাস্তবে এত দীর্ঘ সময় UI
        // loading-এ আটকে থাকাটা ব্যবহারকারীর কাছে "এখনও ভাঙা"-ই মনে হয় (real-device
        // manual-verify-তে ধরা পড়েছে, ২০২৬-০৯-২৩)। ফিক্স: সবগুলো subscribeWithTimeout() কল
        // `coroutineScope { launch { ... } }` দিয়ে **সমান্তরালে (parallel)** চালানো হচ্ছে --
        // worst-case wait এখন সবচেয়ে ধীর একটা channel-এর ~১০ সেকেন্ডেই সীমাবদ্ধ, ১০০ সেকেন্ড না।
        // প্রতিটা channel-এর নিজস্ব runCatching (subscribeWithTimeout-এর ভেতরেই) অক্ষত আছে, তাই
        // একটা channel fail/timeout হলেও বাকিগুলো/পুরো coroutineScope block হয় না।
        coroutineScope {
            launch { uCh.subscribeWithTimeout("users") }
            launch { pCh.subscribeWithTimeout("problems") }
            launch { bCh.subscribeWithTimeout("bids") }
            launch { mCh.subscribeWithTimeout("messages") }
            launch { tCh.subscribeWithTimeout("transactions") }
            launch { eCh.subscribeWithTimeout("escrows") }
            launch { gCh.subscribeWithTimeout("gateway_payments") }
            launch { acCh.subscribeWithTimeout("additional_charges") }
            launch { nCh.subscribeWithTimeout("notifications") }
            launch { wCh.subscribeWithTimeout("withdrawals") }
        }

        // Realtime Scoping ফিক্স, ধাপ ১ (ব্যবহারকারীর অনুরোধে, dual-run) — normal (non-admin)
        // session-এর জন্য notifications-এর নতুন per-user private broadcast subscription, উপরের
        // notificationsChannel-এর পাশাপাশি (প্রতিস্থাপন না)। Admin session-এ চালু হবে না (নিয়ম #৪)।
        // [বাগ ১.৪ ফিক্সের অংশ হিসেবে নোট] এটা ইচ্ছাকৃতভাবে উপরের parallel block-এর বাইরে রাখা
        // হয়েছে -- ভেতরের startUserTopicBroadcastSubscription() নিজেই একাধিক subscribeWithTimeout
        // কল করে (নিচে দেখুন), তাই এটাকে আলাদা রাখলে worst-case বড়জোর আরও ~১০ সেকেন্ড যোগ হতে
        // পারে, কিন্তু কোড ছোট/পরিষ্কার থাকে (rule #৩, ন্যূনতম টার্গেটেড এডিট) -- ভবিষ্যতে চাইলে
        // এটাও একই coroutineScope-এর ভেতরে launch করা যায়।
        val currentUserIdForBroadcast = SupabaseAuthManager.currentUserId()
        if (currentUserIdForBroadcast != null && !isCurrentSessionAdmin(currentUserIdForBroadcast)) {
            runCatching { startUserTopicBroadcastSubscription(currentUserIdForBroadcast) }
                .onFailure { Log.e(TAG, "notifications broadcast subscription (Realtime Scoping ধাপ ১) শুরু করা যায়নি (non-fatal, পুরনো postgresChangeFlow subscription অক্ষত): ${it.message}", it) }
        } else {
            stopUserTopicBroadcastSubscription()
        }

        // typing broadcast এখানে অন্তর্ভুক্ত না -- table-backed না, per-problem channel
        // (ChatScreen open/close অনুযায়ী ensureTypingChannelJoined()/leaveTypingChannel() দিয়ে
        // আলাদাভাবে লাইফসাইকেল-বাউন্ড, users/problems/bids-এর মতো একটা গ্লোবাল সেশন-ওয়াইড
        // channel না)।
    }

    /** সব active channel বন্ধ করে (leak-guard) -- `startRealtimeListeners()`-এর কাউন্টারপার্ট, cutover/logout-এর সময় দরকার হবে। */
    suspend fun stopRealtimeListeners() {
        usersChannel?.let { runCatching { it.unsubscribe() } }
        usersChannel = null
        problemsChannel?.let { runCatching { it.unsubscribe() } }
        problemsChannel = null
        bidsChannel?.let { runCatching { it.unsubscribe() } }
        bidsChannel = null
        messagesChannel?.let { runCatching { it.unsubscribe() } }
        messagesChannel = null
        transactionsChannel?.let { runCatching { it.unsubscribe() } }
        transactionsChannel = null
        escrowsChannel?.let { runCatching { it.unsubscribe() } }
        escrowsChannel = null
        gatewayPaymentsChannel?.let { runCatching { it.unsubscribe() } }
        gatewayPaymentsChannel = null
        additionalChargesChannel?.let { runCatching { it.unsubscribe() } }
        additionalChargesChannel = null
        notificationsChannel?.let { runCatching { it.unsubscribe() } }
        notificationsChannel = null
        withdrawalsChannel?.let { runCatching { it.unsubscribe() } }
        withdrawalsChannel = null
        // Realtime Scoping ফিক্স, ধাপ ১ — notifications-এর নতুন per-user broadcast channel-ও বন্ধ।
        userTopicBroadcastChannel?.let { runCatching { it.unsubscribe() } }
        userTopicBroadcastChannel = null
        // typing channels-ও বন্ধ করা হলো (logout-এর সময় সব খোলা চ্যাট-স্ক্রিন channel সহ)।
        typingChannels.keys.toList().forEach { pid -> leaveTypingChannel(pid) }
        // Realtime Scoping ফিক্স, ধাপ ৩ — খোলা থাকা সব problem:<id> message-broadcast channel-ও
        // বন্ধ (logout-এর সময় leak এড়াতে, typing channels-এর মতোই)।
        messagesBroadcastChannels.keys.toList().forEach { pid -> runCatching { leaveProblemMessagesBroadcastChannel(pid) } }
        // Realtime Scoping ফিক্স, ধাপ ৪ — খোলা থাকা সব problem:<id>:bids bid-broadcast channel-ও
        // বন্ধ (logout-এর সময় leak এড়াতে, messagesBroadcastChannels-এর মতোই)।
        bidsBroadcastChannels.keys.toList().forEach { pid -> runCatching { leaveProblemBidsBroadcastChannel(pid) } }
        // ধাপ ২২: heartbeat map ক্লিয়ার -- না হলে stopRealtimeListeners() (লগআউট) এর পরে নতুন
        // সেশনে আবার startRealtimeListeners() কল হলে পুরনো "last heard" timestamp দিয়ে ভুলভাবে
        // "healthy" ধরে নেওয়া হতো, যদিও নতুন সেশনে এখনো কোনো event আসেনি।
        listenerLastHeardMs.clear()
    }
}
