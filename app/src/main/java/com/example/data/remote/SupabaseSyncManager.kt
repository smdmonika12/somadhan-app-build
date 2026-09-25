@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.data.remote

import android.util.Log
import com.example.data.security.AdminActivityLogPage
import com.example.data.security.AdminSessionRecord
import com.example.BuildConfig
import com.example.data.remote.dto.AdditionalChargeDto
import com.example.data.remote.dto.AdminDashboardMetricsDto
import com.example.data.remote.dto.BidDto
import com.example.data.remote.dto.CategoryDto
import com.example.data.remote.dto.EscrowDto
import com.example.data.remote.dto.FaqDto
import com.example.data.remote.dto.MessageDto
import com.example.data.remote.dto.PlatformSettingDto
import com.example.data.remote.dto.ProblemDto
import com.example.data.remote.dto.TransactionDto
import com.example.data.remote.dto.UserDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৪ (SupabaseSyncManager: Core CRUD)
 *
 * [FirebaseSyncManager] এর প্যাটার্ন অনুসরণ করে (try/catch + [Result], আর realtime
 * listener/channel-এর জন্য cleanup ফাংশন — কারণ এই প্রজেক্টে আগে listener leak এর ইতিহাস
 * আছে) — কিন্তু Firestore এর বদলে supabase-kt (Postgrest + Realtime) দিয়ে।
 *
 * এই ধাপে শুধু users/categories/faqs/problems/bids — এই কয়টা টেবিলের CRUD আছে। money-related
 * টেবিল (escrows/transactions/withdrawals/additional_charges/gateway_payments) ইচ্ছাকৃতভাবে
 * বাদ — সেগুলো ধাপ ৫ ও ৬ এ RPC wrapper হিসেবে আসবে (client সরাসরি লিখবে না)।
 *
 * [ঐতিহাসিক নোট — তখন (ধাপ ৪) সঠিক ছিল, এখন আর না]: লেখার সময় এই ফাইল কোথাও call/wire করা
 * হয়নি বলে নোট করা ছিল। ধাপ ৭ থেকে ধাপ ৩৩.৩ পর্যন্ত ধাপে ধাপে `SomadhanRepository.kt`/UI/
 * ViewModel-এর সব কল-সাইট migrate হয়ে গেছে এবং Firebase সম্পূর্ণ অপসারিত — এই ফাইল এখন
 * সক্রিয়ভাবে ব্যবহৃত হচ্ছে।
 *
 * নোট: এই ধাপও build/compile করে verify করা যায়নি (এই session এ Gradle/network সুবিধা নেই) —
 * supabase-kt Postgrest/Realtime API (BOM 3.6.0) এর সর্বশেষ known signature অনুযায়ী লেখা হয়েছে,
 * Android Studio তে Gradle sync/build করে ভবিষ্যতে verify করে নেওয়া উচিত।
 */
object SupabaseSyncManager {

    private val client get() = SupabaseClientProvider.client

    /**
     * [ধাপ ৩০] শুধু Edge Function কল করার জন্য একটা লাইটওয়েট, আলাদা Ktor HttpClient — supabase-kt
     * এখনো Functions প্লাগইন ইনস্টল করে না (এই ধাপের স্কোপে নতুন dependency/gradle পরিবর্তন এড়ানোর
     * জন্য), তাই ইতিমধ্যে থাকা `ktor-client-android` dependency দিয়েই সরাসরি HTTPS POST করা হচ্ছে।
     */
    private val functionsHttpClient by lazy { HttpClient(Android) }

    // ============================================================
    // Users
    // ============================================================

    suspend fun getUserById(userId: String): Result<UserDto?> {
        return try {
            val user = client.postgrest.from("users")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserDto>()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByPhone(phone: String): Result<UserDto?> {
        return try {
            val user = client.postgrest.from("users")
                .select { filter { eq("phone", phone) } }
                .decodeSingleOrNull<UserDto>()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * শুধুমাত্র non-sensitive column আপডেট করে — name/address/latitude/longitude/
     * profile_image_uri/solver_categories/favorite_solver_ids/has_completed_solver_setup।
     *
     * **এখানে ইচ্ছাকৃতভাবে** balance/role/is_banned/is_kyc_verified (বা অন্য কোনো sensitive
     * column) লেখার কোনো প্যারামিটার/পথ রাখা হয়নি — Supabase এ column-level permission দিয়ে
     * এমনিতেই ব্লক করা আছে, কিন্তু কোড লেভেলেও এই ফাংশনের signature দিয়ে সেটা স্পষ্ট রাখা হলো।
     * শুধু যেসব প্যারামিটার non-null দেওয়া হবে সেগুলোই আপডেট হবে (partial update)।
     */
    suspend fun updateOwnProfile(
        userId: String,
        name: String? = null,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        profileImageUri: String? = null,
        solverCategories: String? = null,
        favoriteSolverIds: String? = null,
        hasCompletedSolverSetup: Boolean? = null,
        // ধাপ ১৪-এ যোগ: email এখানে non-sensitive column হিসেবে যোগ করা হলো (schema-তে
        // authenticated role-এর email কলামে UPDATE grant আছে, Supabase MCP দিয়ে যাচাই করা)।
        // registration flow-এ দরকার, কারণ `complete_registration_profile` RPC-এ email প্যারামিটার
        // নেই (phone+password auth ব্যবহার করায় auth.users.email ফাঁকা থাকে, trigger তাই
        // public.users.email-ও ফাঁকা রাখে — registration শেষে এই ফাংশন দিয়েই বসানো হয়)।
        email: String? = null
    ): Result<Unit> {
        return try {
            val updates = buildJsonObject {
                name?.let { put("name", JsonPrimitive(it)) }
                address?.let { put("address", JsonPrimitive(it)) }
                latitude?.let { put("latitude", JsonPrimitive(it)) }
                longitude?.let { put("longitude", JsonPrimitive(it)) }
                profileImageUri?.let { put("profile_image_uri", JsonPrimitive(it)) }
                solverCategories?.let { put("solver_categories", JsonPrimitive(it)) }
                favoriteSolverIds?.let { put("favorite_solver_ids", JsonPrimitive(it)) }
                hasCompletedSolverSetup?.let { put("has_completed_solver_setup", JsonPrimitive(it)) }
                email?.let { put("email", JsonPrimitive(it)) }
            }
            if (updates.isNotEmpty()) {
                val result = client.postgrest.from("users").update(updates) {
                    select()
                    filter { eq("id", userId) }
                }
                requireAffectedRowOrThrow(result, "updateOwnProfile(userId=$userId)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ৩৩.৩ — `updateInstantJobToggle()`-এর জন্য। RPC লাগে না — RLS-এর `users_update_own`
     * policy কলার নিজের id-তে UPDATE এমনিতেই permit করে (`syncFreeJobQuota()`-এর একই প্যাটার্ন)।
     * caller-guard repository-সাইডে (`currentUserId() == userId`) হয়।
     */
    suspend fun syncInstantJobNotificationToggle(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            val result = client.postgrest.from("users").update(
                buildJsonObject {
                    put("instant_job_notifications_enabled", JsonPrimitive(enabled))
                }
            ) {
                select()
                filter { eq("id", userId) }
            }
            requireAffectedRowOrThrow(result, "syncInstantJobNotificationToggle(userId=$userId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ৩২.৫ — `resolveCommissionRateForNewJob()`-এর free-quota কাউন্টার (`free_jobs_used_this_month`/
     * `free_jobs_month_key`) sync করার জন্য। RPC লাগে না — RLS-এর `users_update_own` policy কলার
     * নিজের id-তে UPDATE এমনিতেই permit করে, তাই সরাসরি `.update()` (caller-guard কল করার আগেই
     * repository-সাইডে করা হয়েছে, `currentUserId() == solverId` চেক)।
     */
    suspend fun syncFreeJobQuota(userId: String, usedCount: Int, monthKey: String): Result<Unit> {
        return try {
            val result = client.postgrest.from("users").update(
                buildJsonObject {
                    put("free_jobs_used_this_month", JsonPrimitive(usedCount))
                    put("free_jobs_month_key", JsonPrimitive(monthKey))
                }
            ) {
                select()
                filter { eq("id", userId) }
            }
            requireAffectedRowOrThrow(result, "syncFreeJobQuota(userId=$userId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * মানি-ফ্লো ফিক্স, ধাপ ৪ — `sync_solver_free_job_quota` RPC wrapper।
     *
     * উপরের `syncFreeJobQuota()` সরাসরি `users` টেবিলে `.update()` করে, যেটা RLS-এর কারণে শুধু
     * নিজের row-এর জন্যই কাজ করে (caller-এর `auth.uid()` আর টার্গেট `id` একই হতে হবে)।
     * `acceptBid()`-এ caller হয় job **owner** (poster), কিন্তু টার্গেট হয় **solver**-এর কোটা-
     * কাউন্টার -- তাই সেই কল-সাইট থেকে `syncFreeJobQuota()` কখনো আসলে কিছু আপডেট করতো না।
     *
     * এই নতুন SECURITY DEFINER RPC সার্ভার-সাইডে যাচাই করে caller আসলেই `p_problem_id`-এর
     * owner/accepted-solver/admin কিনা (এবং `p_solver_id` সত্যিই সেই problem-এর accepted
     * solver কিনা), তারপরই টার্গেট solver-এর `free_jobs_used_this_month`/`free_jobs_month_key`
     * আপডেট করে। এতে `acceptBid()` (caller=owner) আর `acceptDirectContractProposal()`
     * (caller=solver নিজে) -- দুই কল-সাইটই একইভাবে কাজ করে।
     *
     * ⚠️ এই RPC-র SQL সংজ্ঞা `supabase/migrations/step_money_flow_fix4_sync_solver_free_job_quota.sql`
     * ফাইলে নতুন যোগ করা হয়েছে -- Supabase-এ আগে থেকে ছিল না, তাই deploy করার আগে সেই
     * migration ফাইলটা Supabase project-এ apply করতে হবে (নাহলে এই RPC কল ব্যর্থ হবে,
     * best-effort log warning আসবে, local flow অপ্রভাবিত থাকবে)।
     */
    suspend fun syncSolverFreeJobQuota(problemId: String, solverId: String, usedCount: Int, monthKey: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "sync_solver_free_job_quota",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_solver_id", JsonPrimitive(solverId))
                    put("p_used_count", JsonPrimitive(usedCount))
                    put("p_month_key", JsonPrimitive(monthKey))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ৩২.৫ — Category-৩ গ্যাপ #১ ফিক্স। `sync_linked_account_profile` RPC wrap করে —
     * linked (User<->Solver dual-role) বা phone/email-duplicate account-এর non-sensitive
     * profile column গুলো sync করার জন্য, নিজের row বাদে অন্য কারো row লেখার একমাত্র বৈধ পথ।
     * caller ও target সত্যিই একই পরিবার কিনা RPC নিজেই (SECURITY DEFINER) সার্ভার-সাইডে verify করে।
     */
    suspend fun syncLinkedAccountProfile(
        targetUserId: String,
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        profileImageUri: String? = null,
        isVerifiedBadge: Boolean? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "sync_linked_account_profile",
                buildJsonObject {
                    put("p_target_user_id", JsonPrimitive(targetUserId))
                    put("p_name", name?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_phone", phone?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_email", email?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_address", address?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_latitude", latitude?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_longitude", longitude?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_profile_image_uri", profileImageUri?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_is_verified_badge", isVerifiedBadge?.let { JsonPrimitive(it) } ?: JsonNull)
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ৩২.৫ — Category-৩ গ্যাপ #৩ ফিক্স। `admin_get_dashboard_metrics` RPC wrap করে — admin-only
     * সার্ভার-সাইড aggregation, পুরো users/problems/bids/transactions/withdrawals টেবিল client-এ
     * না নামিয়ে headline সংখ্যাগুলো ফেরত দেয়।
     */
    suspend fun getAdminDashboardMetrics(): Result<AdminDashboardMetricsDto> {
        return try {
            val result = client.postgrest.rpc("admin_get_dashboard_metrics")
                .decodeAs<AdminDashboardMetricsDto>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ৩২.৫: `additional_charges` টেবিল আর live-listen করা হয় না (ENGINEERING_NOTES.md §৯-এর
     * Firebase-সমতুল্য কারণেই) — তাই এই explicit pull, `FirebaseSyncManager.refreshAdditionalCharges()`-এর
     * Supabase সমতুল্য হিসেবে, dual-run-এ ব্যবহারের জন্য।
     */
    suspend fun getAllAdditionalCharges(): Result<List<AdditionalChargeDto>> {
        return try {
            Result.success(client.postgrest.from("additional_charges").select().decodeList<AdditionalChargeDto>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ১৪: KYC submit (`SomadhanRepository.submitKyc`-এর Supabase সমতুল্য) — নিজের row-এর
     * kyc_* column গুলো আপডেট করে, `is_kyc_verified` সবসময় `false` আর `kyc_status` সবসময়
     * `"PENDING"` পাঠায় (কখনো caller থেকে override করা যায় না — পুরনো Firebase কোডের কমেন্টে
     * থাকা "Critical Rule 10.2: NEVER auto-verify!" নীতি এখানেও বজায় রাখা হলো)। Admin approve/
     * reject/revoke (যেগুলো `is_kyc_verified`/`kyc_status` সত্যিকারের বদলায়) এই ধাপের স্কোপে না
     * (ধাপ ১২-এর checklist অনুযায়ী পরে migrate হবে)।
     */
    // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "KYC submit করলে admin panel-এর KYC মেনুতে কোনো pending
    // KYC জমা হচ্ছে না — অনেকবার fix করার try করেছি, কোনো Claude session ঠিক করতে পারলো না"]
    // লাইভ DB সরাসরি চেক করে ধরা পড়েছে: এই পুরো project-এ **কোনো user-এর** kyc_status কখনো
    // 'NONE' ছাড়া অন্য কিছু হয়নি — অর্থাৎ cloud dual-write কখনো একবারও সফল হয়নি। কারণ: নিচের
    // পুরনো কোড সরাসরি `client.postgrest.from("users").update(...)` (raw table PATCH, RLS-নির্ভর)
    // কল করত, আর তার Result ফেরত এসে caller (repository.submitKyc()) কখনো চেক/log করত না —
    // ফলে ব্যর্থ হলেও (RLS silently ০টা row match করলে PostgREST কোনো error না দিয়েই সফল রিটার্ন
    // করে) কোনো log/warning কখনো দেখা যেত না, ডিবাগ করা অসম্ভব ছিল। ফিক্স: এই ফাইলের অন্য সব
    // সফল dual-write-এর (request_wallet_deposit, switch_role_get_or_create_linked_profile
    // ইত্যাদি) মতোই একটা SECURITY DEFINER RPC (`submit_kyc`, auth.uid()-ভিত্তিক, RLS bypass করে
    // সরাসরি নিজের row আপডেট করে, ০ rows আপডেট হলে exception raise করে) — Supabase MCP দিয়ে
    // লাইভ প্রজেক্টে তৈরি করা হয়েছে। raw table update-এর বদলে এখন এই RPC কল হচ্ছে।
    suspend fun submitKyc(
        firstName: String,
        lastName: String,
        address: String,
        documentType: String,
        documentNumber: String,
        docFrontUri: String,
        docBackUri: String,
        selfieUri: String,
        submissionDateMillis: Long
    ): Result<Unit> {
        return try {
            client.postgrest.rpc(
                "submit_kyc",
                buildJsonObject {
                    put("p_first_name", JsonPrimitive(firstName))
                    put("p_last_name", JsonPrimitive(lastName))
                    put("p_address", JsonPrimitive(address))
                    put("p_document_type", JsonPrimitive(documentType))
                    put("p_document_number", JsonPrimitive(documentNumber))
                    put("p_doc_front_url", JsonPrimitive(docFrontUri))
                    put("p_doc_back_url", JsonPrimitive(docBackUri))
                    put("p_selfie_url", JsonPrimitive(selfieUri))
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Categories & FAQs — read + admin write (ধাপ ৩২.৮ পর্যন্ত শুধু read ছিল)
    // ============================================================

    suspend fun getAllCategories(): Result<List<CategoryDto>> {
        return try {
            Result.success(client.postgrest.from("categories").select().decodeList<CategoryDto>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllFaqs(): Result<List<FaqDto>> {
        return try {
            Result.success(client.postgrest.from("faqs").select().decodeList<FaqDto>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `insertCategory()`/`adminUpdateCategory()`-এর সমতুল্য।
     * `categories` টেবিলে RLS পলিসি `categories_admin_write` (cmd=ALL, qual/with_check উভয়ই
     * `is_admin(auth.uid())`) ইতিমধ্যেই admin-only write নিশ্চিত করে (Supabase MCP দিয়ে verify
     * করা) — তাই আলাদা RPC লাগেনি, সরাসরি Postgrest `upsert()` (id PK-তে conflict হলে update,
     * না হলে insert — দুটো caller-ই এই একটা ফাংশন দিয়ে কভার হয়)।
     */
    suspend fun upsertCategory(category: CategoryDto): Result<Unit> {
        return try {
            val result = client.postgrest.from("categories").upsert(category) {
                onConflict = "id"
                select()
            }
            requireAffectedRowOrThrow(result, "upsertCategory(id=${category.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `adminToggleCategoryActive()`-এর সমতুল্য — শুধু `is_active`
     * কলাম আপডেট করে (RLS একই `categories_admin_write` পলিসি দিয়ে কভার্ড)।
     */
    suspend fun setCategoryActive(categoryId: String, isActive: Boolean): Result<Unit> {
        return try {
            val result = client.postgrest.from("categories").update(
                buildJsonObject { put("is_active", JsonPrimitive(isActive)) }
            ) {
                select()
                filter { eq("id", categoryId) }
            }
            requireAffectedRowOrThrow(result, "setCategoryActive(categoryId=$categoryId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৪] `deleteCategory()`-এর সমতুল্য — `categories` row delete করে।
     * RLS একই `categories_admin_write` (cmd=ALL) পলিসি দিয়ে কভার্ড, তাই আলাদা RPC লাগেনি।
     * `problems.category_id` FK (ON DELETE NO ACTION) এখনও কোনো problem এই category রেফার করলে
     * এই কলটা ব্যর্থ হতে পারে — সেটা expected, best-effort dual-write বলে শুধু log হবে।
     */
    suspend fun deleteCategoryRemote(categoryId: String): Result<Unit> {
        return try {
            val result = client.postgrest.from("categories").delete {
                select()
                filter { eq("id", categoryId) }
            }
            requireAffectedRowOrThrow(result, "deleteCategoryRemote(categoryId=$categoryId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৪] `deleteCategory()`-এর cascade অংশের সমতুল্য — `admin_remove_category_from_solvers`
     * RPC একবারেই (bulk) সব affected solver-এর `users.solver_categories` থেকে এই category_id বাদ
     * দেয় (N-বার loop-এর ভেতরে কল না করে, ঠিক `adminBulkResetFreeJobQuota`-র প্যাটার্নে)। RPC নিজেই
     * `is_admin(auth.uid())` চেক করে।
     */
    suspend fun adminRemoveCategoryFromSolvers(categoryId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_remove_category_from_solvers",
                buildJsonObject {
                    put("p_category_id", JsonPrimitive(categoryId))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `addFaq()`/`updateFaq()`-এর সমতুল্য। `faqs` টেবিলে RLS পলিসি
     * `faqs_admin_write` (cmd=ALL, qual/with_check উভয়ই `is_admin(auth.uid())`) ইতিমধ্যেই
     * admin-only write নিশ্চিত করে (Supabase MCP দিয়ে verify করা) — তাই এখানেও RPC লাগেনি, সরাসরি
     * `upsert()`।
     */
    suspend fun upsertFaq(faq: FaqDto): Result<Unit> {
        return try {
            val result = client.postgrest.from("faqs").upsert(faq) {
                onConflict = "id"
                select()
            }
            requireAffectedRowOrThrow(result, "upsertFaq(id=${faq.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `deleteFaq()`/`deleteFaqById()`-এর সমতুল্য — `id` দিয়ে row
     * ডিলিট করে (RLS একই `faqs_admin_write` পলিসি দিয়ে কভার্ড)।
     */
    suspend fun deleteFaqRow(faqId: String): Result<Unit> {
        return try {
            val result = client.postgrest.from("faqs").delete {
                select()
                filter { eq("id", faqId) }
            }
            requireAffectedRowOrThrow(result, "deleteFaqRow(faqId=$faqId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ৩২.৮ (বাকি অংশ) — Admin Delete + KYC/Withdrawal/Verified-Badge Edit
    // ============================================================

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `deleteProblem()` (admin) এর সমতুল্য -- কিন্তু **hard-delete
     * না, soft-delete** (`is_user_deleted = true`)। এই session-এ Supabase MCP দিয়ে verify করা
     * হয়েছে: `bids`/`escrows`/`messages`/`ratings`/`additional_charges` টেবিলের FK
     * (delete_rule='NO ACTION') `problems(id)`-কে রেফারেন্স করে -- তাই Firestore-এর মতো সরাসরি
     * হার্ড-DELETE করলে কোনো বিড/এসক্রো/মেসেজ থাকা প্রতিটা সমস্যার জন্যই FK ভায়োলেশন এরর দেবে।
     * `problems_update_admin` RLS policy (cmd=UPDATE, qual/with_check উভয়ই `is_admin(auth.uid())`,
     * কোনো column-level restriction নেই) ইতিমধ্যেই টেবিল-লেভেলে admin write কভার করে -- তাই RPC
     * ছাড়াই সরাসরি `is_user_deleted` কলাম আপডেট যথেষ্ট, নতুন কোনো RPC/migration লাগেনি।
     */
    suspend fun adminSoftDeleteProblem(problemId: String): Result<Unit> {
        return try {
            val result = client.postgrest.from("problems").update(
                buildJsonObject { put("is_user_deleted", JsonPrimitive(true)) }
            ) {
                select()
                filter { eq("id", problemId) }
            }
            requireAffectedRowOrThrow(result, "adminSoftDeleteProblem(problemId=$problemId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `deleteUser()` (admin) এর সমতুল্য -- soft-delete flag
     * (`is_deleted`), নতুন RPC `admin_soft_delete_user` (SECURITY DEFINER, is_admin() চেক)।
     * `users` টেবিলে hard-delete literal parity সম্ভব না (users(id)-এর ওপর bids/escrows/
     * transactions/withdrawals/gateway_payments/messages/ratings/notifications/
     * reputation_events/problems -- সব কটাতেই FK delete_rule='NO ACTION', verify করা হয়েছে) --
     * এই RPC শুধু flag সেট করে, auth.users থেকে মোছে না, লগইন ব্লকও করে না (ইচ্ছাকৃতভাবে খোলা রাখা
     * ভবিষ্যৎ সিদ্ধান্ত, MIGRATION_PROGRESS.md-এ নথিভুক্ত)।
     */
    suspend fun adminSoftDeleteUser(userId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_soft_delete_user",
                buildJsonObject { put("p_user_id", JsonPrimitive(userId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `adminUpdateKycInfo()`-এর সমতুল্য। `users_update_admin` RLS
     * policy (cmd=UPDATE, qual/with_check উভয়ই `is_admin(auth.uid())`, টেবিল-লেভেল, কোনো
     * column-level restriction নেই -- এই session-এ Supabase MCP দিয়ে verify করা হয়েছে) ইতিমধ্যেই
     * admin-এর জন্য KYC কলামসহ সব কলাম আপডেট কভার করে -- তাই RPC লাগেনি, সরাসরি partial `update()`।
     */
    suspend fun adminUpdateKycInfo(
        userId: String,
        docNumber: String,
        firstName: String,
        lastName: String,
        address: String
    ): Result<Unit> {
        return try {
            val result = client.postgrest.from("users").update(
                buildJsonObject {
                    put("kyc_document_number", JsonPrimitive(docNumber))
                    put("kyc_first_name", JsonPrimitive(firstName))
                    put("kyc_last_name", JsonPrimitive(lastName))
                    put("kyc_address", JsonPrimitive(address))
                }
            ) {
                select()
                filter { eq("id", userId) }
            }
            requireAffectedRowOrThrow(result, "adminUpdateKycInfo(userId=$userId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `adminResetKycToPending()`-এর সমতুল্য -- একই
     * `users_update_admin` RLS policy দিয়ে কভার্ড, RPC লাগেনি। DB-তে kyc_status uppercase
     * convention (`admin_approve_kyc`/`admin_reject_kyc` RPC সোর্স দেখে verify করা --
     * 'APPROVED'/'REJECTED') অনুসরণ করে এখানে 'PENDING' ব্যবহার করা হয়েছে।
     */
    suspend fun adminResetKycToPending(userId: String): Result<Unit> {
        return try {
            val result = client.postgrest.from("users").update(
                buildJsonObject {
                    put("kyc_status", JsonPrimitive("PENDING"))
                    put("kyc_reject_reason", JsonPrimitive(""))
                }
            ) {
                select()
                filter { eq("id", userId) }
            }
            requireAffectedRowOrThrow(result, "adminResetKycToPending(userId=$userId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮] `adminUpdateWithdrawalTrxId()`-এর সমতুল্য। `withdrawals`
     * টেবিলে কোনো admin-write RLS policy নেই (শুধু SELECT policy আছে, verify করা হয়েছে) -- তাই
     * নতুন RPC `admin_update_withdrawal_trx_id` (SECURITY DEFINER, is_admin() চেক) লাগলো। এটা
     * `process_withdrawal()`-এর থেকে আলাদা -- শুধু trx_id সংশোধন করে, status/money-movement
     * স্পর্শ করে না।
     */
    suspend fun adminUpdateWithdrawalTrxId(withdrawalId: String, newTrxId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_update_withdrawal_trx_id",
                buildJsonObject {
                    put("p_withdrawal_id", JsonPrimitive(withdrawalId))
                    put("p_new_trx_id", JsonPrimitive(newTrxId))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৮; ধাপ ৮ (MONEY_FLOW_AND_ADMIN_BUGS, বাগ D৩) এ RPC-ভিত্তিক
     * করা হলো] আগে সরাসরি Postgrest `update` দিয়ে শুধু shared `is_verified_badge` ছোঁয়া হতো।
     * এখন লাইভ প্রজেক্টে থাকা `admin_set_verified_badge(p_user_id, p_verified, p_role)` RPC
     * ব্যবহার করা হচ্ছে (`admin_set_banned`/`admin_set_restricted`-এর একই প্যাটার্নে) —
     * [role] null দিলে RPC আগের মতোই শুধু shared কলাম ছোঁবে (backward compatible), "USER"/
     * "SOLVER" দিলে role-scoped `verified_badge_user`/`verified_badge_solver` কলাম আপডেট
     * হবে, শেয়ার্ড কলাম অপরিবর্তিত থাকবে।
     */
    suspend fun adminSetVerifiedBadge(userId: String, verified: Boolean, role: String? = null): Result<Unit> {
        return try {
            client.postgrest.rpc(
                "admin_set_verified_badge",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_verified", JsonPrimitive(verified))
                    if (role != null) put("p_role", JsonPrimitive(role))
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Platform Settings (ধাপ ১২ ব্যাচ ৬) — `platform_settings` টেবিলে কোনো RPC নেই (Supabase MCP
    // দিয়ে যাচাই করা), লেখার জন্য সরাসরি Postgrest upsert ব্যবহার করা হয়েছে -- RLS পলিসি
    // `platform_settings_admin_write` (cmd=ALL, qual/with_check উভয়ই `is_admin(auth.uid())`)
    // ইতিমধ্যেই admin-only লেখা নিশ্চিত করে, তাই client-side এ আলাদা guard দরকার নেই (RPC-ভিত্তিক
    // wrapper গুলোর মতোই, RLS-ই এখানে আসল authorization boundary)। SELECT সবার জন্য উন্মুক্ত
    // (qual=true), createProblem()-এর মতো insert()-এর বদলে upsert() ব্যবহার করা হয়েছে কারণ `key`
    // PK-তে conflict হলে (setting আগে থেকেই থাকলে) insert ব্যর্থ হতো -- upsert onConflict="key"
    // দিয়ে সেই কেসটাও কভার করে।
    // ============================================================

    suspend fun upsertPlatformSetting(key: String, value: String): Result<Unit> {
        return try {
            val result = client.postgrest.from("platform_settings")
                .upsert(PlatformSettingDto(key = key, value = value)) {
                    onConflict = "key"
                    select()
                }
            requireAffectedRowOrThrow(result, "upsertPlatformSetting(key=$key)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ১৪] একটা single platform_settings row পড়ে -- `AdminCredentials.kt`
     * এর মতো non-sensitive settings dual-read এর জন্য (যেমন admin phone)। ⚠️ এই টেবিলের SELECT RLS
     * সবার জন্য উন্মুক্ত (qual=true) -- তাই এখানে কখনো password hash/secret-জাতীয় কিছু লেখা/পড়া
     * উচিত না, শুধু non-sensitive config।
     */
    suspend fun getPlatformSetting(key: String): Result<String?> {
        return try {
            val setting = client.postgrest.from("platform_settings")
                .select { filter { eq("key", key) } }
                .decodeSingleOrNull<PlatformSettingDto>()
            Result.success(setting?.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Problems
    // ============================================================

    suspend fun getProblemById(problemId: String): Result<ProblemDto?> {
        return try {
            val problem = client.postgrest.from("problems")
                .select { filter { eq("id", problemId) } }
                .decodeSingleOrNull<ProblemDto>()
            Result.success(problem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOpenProblems(): Result<List<ProblemDto>> {
        return try {
            val problems = client.postgrest.from("problems")
                .select { filter { eq("status", "OPEN") } }
                .decodeList<ProblemDto>()
            Result.success(problems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * সরাসরি client থেকে `problems` টেবিলে insert করে।
     *
     * RLS: `problems_insert_owner` policy-র `with_check` হলো `auth.uid() = user_id` — অর্থাৎ এই
     * ফাংশনটা কাজ করার জন্য client-এ একটা বৈধ Supabase Auth session থাকা আবশ্যক। ধাপ ১৪-এ Auth
     * session আসার পর থেকে এটা কাজ করে — `SomadhanRepository.createProblem()` ধাপ ৮-এ এই
     * ফাংশনটা wire করেছে (আগে এখানে লেখা ছিল এটা এখনো migrate হয়নি — সেটা এখন আর সত্যি না)।
     */
    suspend fun createProblem(problem: ProblemDto): Result<Unit> {
        return try {
            client.postgrest.from("problems").insert(problem)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Bids
    // ============================================================

    suspend fun getBidsForProblem(problemId: String): Result<List<BidDto>> {
        return try {
            val bids = client.postgrest.from("bids")
                .select { filter { eq("problem_id", problemId) } }
                .decodeList<BidDto>()
            Result.success(bids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * সরাসরি client থেকে `bids` টেবিলে insert করে (RLS policy `bids_insert_solver`:
     * `auth.uid() = solver_id AND problem.status = 'OPEN'` — Supabase MCP দিয়ে ধাপ ৮-এ যাচাই
     * করা)। insert-এর পর `handle_new_bid` trigger (ধাপ ৮-এ নতুন যোগ করা migration) স্বয়ংক্রিয়ভাবে
     * সংশ্লিষ্ট problem-এর `bids_count`/`last_activity_at` বাড়িয়ে দেয় — এই trigger ছাড়া সরাসরি
     * client থেকে সেই আপডেট করা সম্ভব হতো না (solver problem-এর owner না, তাই RLS-এ
     * `problems_update_owner` তাকে আটকাতো)।
     */
    suspend fun createBid(bid: BidDto): Result<Unit> {
        return try {
            client.postgrest.from("bids").insert(bid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Realtime — Firestore এর addSnapshotListener এর সমতুল্য, postgres_changes channel দিয়ে
    // ============================================================

    // FirebaseSyncManager এ আগে listener leak এর ইতিহাস আছে (আগের সেশনে fix করা হয়েছিল) —
    // তাই এখানে প্রতিটা active channel ট্র্যাক করা হচ্ছে, যাতে cleanup guaranteed হয়।
    private var problemsChannel: RealtimeChannel? = null
    private val bidsChannels = mutableMapOf<String, RealtimeChannel>()
    // ধাপ ১১-এ যোগ — chat/message realtime, bidsChannels এর মতোই per-problem channel
    private val messagesChannels = mutableMapOf<String, RealtimeChannel>()

    /**
     * `problems` টেবিলের সব ধরনের পরিবর্তনে (insert/update/delete) emit করে।
     * কল করার আগে যদি আগের সাবস্ক্রিপশন থেকে থাকে সেটা প্রথমে বন্ধ করে নতুন করে জয়েন করে —
     * একই সময়ে দুইটা active problems channel যাতে কখনো না থাকে।
     */
    suspend fun subscribeToProblemChanges(): Flow<PostgresAction> {
        unsubscribeFromProblemChanges()
        val channel = client.realtime.channel("problems-changes")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "problems"
        }
        problemsChannel = channel
        channel.subscribe()
        return flow
    }

    suspend fun unsubscribeFromProblemChanges() {
        problemsChannel?.let { runCatching { it.unsubscribe() } }
        problemsChannel = null
    }

    /**
     * নির্দিষ্ট একটা problem এর `bids` এ পরিবর্তনে emit করে (problem_id দিয়ে filtered)।
     * প্রতিটা problemId এর জন্য আলাদা channel রাখা হয় যাতে একাধিক স্ক্রিন থেকে একসাথে একাধিক
     * problem এর bids শোনা গেলে একটা আরেকটাকে override না করে।
     */
    suspend fun subscribeToBidsForProblem(problemId: String): Flow<PostgresAction> {
        unsubscribeFromBidsForProblem(problemId)
        val channel = client.realtime.channel("bids-changes-$problemId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "bids"
            filter("problem_id", FilterOperator.EQ, problemId)
        }
        bidsChannels[problemId] = channel
        channel.subscribe()
        return flow
    }

    suspend fun unsubscribeFromBidsForProblem(problemId: String) {
        bidsChannels.remove(problemId)?.let { runCatching { it.unsubscribe() } }
    }

    /**
     * ধাপ ১১: নির্দিষ্ট একটা problem-এর `messages` এ পরিবর্তনে (নতুন মেসেজ insert, read-status
     * update) emit করে — `subscribeToBidsForProblem()`-এর হুবহু একই প্যাটার্নে (per-problemId
     * আলাদা channel, cleanup guaranteed)।
     */
    suspend fun subscribeToMessagesForProblem(problemId: String): Flow<PostgresAction> {
        unsubscribeFromMessagesForProblem(problemId)
        val channel = client.realtime.channel("messages-changes-$problemId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
            filter("problem_id", FilterOperator.EQ, problemId)
        }
        messagesChannels[problemId] = channel
        channel.subscribe()
        return flow
    }

    suspend fun unsubscribeFromMessagesForProblem(problemId: String) {
        messagesChannels.remove(problemId)?.let { runCatching { it.unsubscribe() } }
    }

    /**
     * সব active realtime channel (problems + সব bids channel + সব messages channel) বন্ধ করে।
     * স্ক্রিন/অ্যাপ বন্ধ হওয়ার সময়, বা logout এর সময় কল করা উচিত — যাতে কোনো leak না থাকে।
     */
    suspend fun unsubscribeAll() {
        unsubscribeFromProblemChanges()
        bidsChannels.keys.toList().forEach { unsubscribeFromBidsForProblem(it) }
        messagesChannels.keys.toList().forEach { unsubscribeFromMessagesForProblem(it) }
    }

    // ============================================================
    // Money Part A (ধাপ ৫) — escrow/transactions/withdrawals/additional_charges
    //
    // এই টেবিলগুলোতে client সরাসরি balance/escrow/transaction লিখতে পারে না (RLS + column
    // permission দিয়ে ব্লক করা, ইচ্ছাকৃত ডিজাইন) — টাকা-সংক্রান্ত সব লেখা RPC function দিয়ে হয়
    // (সব RPC `SECURITY DEFINER`, `anon`/`authenticated` role থেকে callable)। প্রতিটা RPC
    // function এর parameter নাম Supabase লাইভ প্রজেক্ট থেকে সরাসরি (`pg_proc` কুয়েরি করে)
    // যাচাই করা হয়েছে — মাস্টার প্রম্পটে যা লেখা ছিল তার সাথে হুবহু মিলেছে, কোনো পরিবর্তন লাগেনি।
    // সব RPC `jsonb` রিটার্ন করে, তাই এখানে [JsonElement] হিসেবে ডিকোড করা হচ্ছে।
    // ============================================================

    // --- Escrow ---

    suspend fun getEscrowForProblem(problemId: String): Result<EscrowDto?> {
        return try {
            val escrow = client.postgrest.from("escrows")
                .select { filter { eq("problem_id", problemId) } }
                .decodeSingleOrNull<EscrowDto>()
            Result.success(escrow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ১৮] `AdminRefundDebugView`-এর read-only diagnostic টুলের জন্য —
     * escrow-এর `id` (PK) দিয়ে সরাসরি row lookup, আগে যেটা Firestore
     * `.collection("escrows").document(escId).get()` দিয়ে হতো।
     */
    suspend fun getEscrowById(escrowId: String): Result<EscrowDto?> {
        return try {
            val escrow = client.postgrest.from("escrows")
                .select { filter { eq("id", escrowId) } }
                .decodeSingleOrNull<EscrowDto>()
            Result.success(escrow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun releaseEscrow(escrowId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "release_escrow",
                buildJsonObject { put("p_escrow_id", JsonPrimitive(escrowId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refundEscrow(
        escrowId: String,
        refundType: String,
        refundPercentage: Double
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "refund_escrow_once",
                buildJsonObject {
                    put("p_escrow_id", JsonPrimitive(escrowId))
                    put("p_refund_type", JsonPrimitive(refundType))
                    put("p_refund_percentage", JsonPrimitive(refundPercentage))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Transactions ---

    /**
     * `transactions` টেবিলের SELECT পলিসি (`auth.uid() = user_id OR auth.uid() = solver_id OR
     * is_admin(...)`) অনুযায়ী কলার শুধু নিজের সাথে সম্পর্কিত transaction-ই দেখতে পারবে — তাই এখানে
     * `user_id` বা `solver_id` যেকোনো একটাতে মিললেই ফেরত দেওয়া হচ্ছে (PostgREST `or()` filter)।
     */
    suspend fun getTransactionsForUser(userId: String): Result<List<TransactionDto>> {
        return try {
            val transactions = client.postgrest.from("transactions")
                .select { filter { or { eq("user_id", userId); eq("solver_id", userId) } } }
                .decodeList<TransactionDto>()
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ১৮] `AdminRefundDebugView`-এর read-only diagnostic টুলের জন্য —
     * transaction-এর `id` (PK) দিয়ে সরাসরি row lookup, আগে যেটা Firestore
     * `.collection("transactions").document(trxDocId).get()` দিয়ে হতো। এখানে RLS caller
     * admin (is_admin() পলিসি) ধরে নেওয়া হয়েছে, তাই user_id/solver_id filter ছাড়াই id দিয়ে খোঁজা
     * নিরাপদ — non-admin caller হলে RLS নিজেই null/empty ফেরত দেবে।
     */
    suspend fun getTransactionById(transactionId: String): Result<TransactionDto?> {
        return try {
            val transaction = client.postgrest.from("transactions")
                .select { filter { eq("id", transactionId) } }
                .decodeSingleOrNull<TransactionDto>()
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Withdrawals ---

    /**
     * [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] `role` প্যারামিটার optional —
     * `null` দিলে পুরনো ৬-আর্গুমেন্ট `request_withdrawal` overload কল হয় (ডিফল্ট আচরণ, DB-সাইড
     * ডিফল্ট `'SOLVER'`)। `"USER"`/`"SOLVER"` দিলে নতুন ৭-আর্গুমেন্ট (`p_role`) overload কল হয়
     * (PostgREST নাম-ভিত্তিক resolution — কোন কী-সেট পাঠানো হচ্ছে তার উপর ভিত্তি করে সঠিক
     * overload বেছে নেয়)। `request_withdrawal` সবসময় solver-context থেকেই কল হয় (উইথড্র শুধু
     * SOLVER balance থেকেই), তাই কল-সাইট সবসময় `"SOLVER"` পাঠাবে বলে প্রত্যাশিত।
     */
    suspend fun requestWithdrawal(
        amount: Double,
        method: String,
        accountNumber: String,
        bankName: String?,
        branchName: String?,
        accountHolderName: String?,
        role: String? = null,
        // [RPC_SYNC_FIX — id-mismatch ফিক্স] caller (Repository) তার আগে থেকে বানানো local
        // withdrawId পাঠাতে পারবে — দেওয়া হলে RPC সেই id-ই cloud row-এর জন্য ব্যবহার করবে
        // (নতুন migration step38, p_client_withdrawal_id), local/cloud id সবসময় মিলবে।
        // null রাখলে আগের মতোই server-side random id (backward-compatible, আচরণ অপরিবর্তিত)।
        clientWithdrawalId: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "request_withdrawal",
                buildJsonObject {
                    put("p_amount", JsonPrimitive(amount))
                    put("p_method", JsonPrimitive(method))
                    put("p_account_number", JsonPrimitive(accountNumber))
                    put("p_bank_name", bankName?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_branch_name", branchName?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put(
                        "p_account_holder_name",
                        accountHolderName?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?)
                    )
                    if (role != null) {
                        put("p_role", JsonPrimitive(role))
                    }
                    if (!clientWithdrawalId.isNullOrBlank()) {
                        put("p_client_withdrawal_id", JsonPrimitive(clientWithdrawalId))
                    }
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * শুধু admin এর জন্য — `process_withdrawal` RPC নিজেই (`SECURITY DEFINER`) সার্ভার-সাইডে
     * `is_admin(auth.uid())` চেক করে, তাই এখানে আলাদা করে ক্লায়েন্ট-সাইড role চেক করা হয়নি (RPC
     * নিজেই non-admin কল রিজেক্ট করবে) — কিন্তু কলার (repository/ViewModel লেয়ার) নিশ্চিত করবে যে
     * শুধু admin UI থেকেই এই ফাংশন কল হয়।
     */
    suspend fun processWithdrawal(
        withdrawalId: String,
        action: String,
        trxId: String?
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "process_withdrawal",
                buildJsonObject {
                    put("p_withdrawal_id", JsonPrimitive(withdrawalId))
                    put("p_action", JsonPrimitive(action))
                    put("p_trx_id", trxId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Additional charges ---

    suspend fun requestAdditionalCharge(
        problemId: String,
        reason: String,
        amount: Double
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "request_additional_charge",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                    put("p_amount", JsonPrimitive(amount))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToAdditionalCharge(chargeId: String, accept: Boolean): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "respond_additional_charge",
                buildJsonObject {
                    put("p_charge_id", JsonPrimitive(chargeId))
                    put("p_accept", JsonPrimitive(accept))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // [ধাপ ২৯] `mark_additional_charge_settled` -- বুক-কিপিং-অনলি RPC, শুধু
    // confirmReleaseAndComplete()-এর extra-amount settlement অংশ থেকে কল করার জন্য। এটা
    // respondToAdditionalCharge()-এর মতো wallet deduct/escrow-এ যোগ করে না -- সেই money-movement
    // ততক্ষণে confirmReleaseAndComplete()-এর নিজের walletDeduction param + payoutEscrowToSolver()/
    // release_escrow RPC দিয়ে ইতিমধ্যে হয়ে গেছে। এই RPC শুধু additional_charges.status/
    // responded_at আপডেট করে -- কখনোই respondToAdditionalCharge()-এর বদলে এখানে কল করবে না,
    // তাহলে একই টাকা দ্বিতীয়বার কাটা হবে (রিপোর্ট হওয়া বাগ)।
    suspend fun markAdditionalChargeSettled(chargeId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_additional_charge_settled",
                buildJsonObject {
                    put("p_charge_id", JsonPrimitive(chargeId))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Money Part B (ধাপ ৬) — wallet deposit/gateway, bid lifecycle/dispute, admin balance, rating
    //
    // এখানেও সব RPC `SECURITY DEFINER`, `jsonb` রিটার্ন করে, আর parameter নাম Supabase লাইভ
    // প্রজেক্ট থেকে (`pg_proc` কুয়েরি) সরাসরি যাচাই করা হয়েছে — মাস্টার প্রম্পটে যা লেখা ছিল তার
    // সাথে হুবহু মিলেছে, কোনো পরিবর্তন লাগেনি।
    // ============================================================

    // --- Wallet deposit / Gateway ---

    /**
     * [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] `role` optional — `null` দিলে
     * পুরনো ৫-আর্গুমেন্ট overload (DB ডিফল্ট `'USER'`), `"USER"`/`"SOLVER"` দিলে নতুন ৬-আর্গুমেন্ট
     * (`p_role`) overload। `requestWalletDeposit` user ও solver উভয় role থেকেই হতে পারে (ওয়ালেটে
     * টাকা ঢোকানো) — কল-সাইট বর্তমান active role অনুযায়ী পাঠাবে।
     */
    suspend fun requestWalletDeposit(
        amount: Double,
        gateway: String,
        gatewayTrxId: String,
        senderPhone: String,
        note: String,
        role: String? = null,
        // [Step 12.8b] null (ডিফল্ট) = আজকের আচরণ হুবহু (পুরনো signature, কোনো user-check নেই)। non-null হলে
        // `p_expected_user_id` পাঠানো হয় -- তখন নতুন overload চলে ও auth.uid() মেলে না হলে RPC
        // NOT_AUTHORIZED ফেরত দেয়। শুধু outbox replay এটা দেয় (OutboxRpcDispatcher)।
        expectedUserId: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "request_wallet_deposit",
                buildJsonObject {
                    put("p_amount", JsonPrimitive(amount))
                    put("p_gateway", JsonPrimitive(gateway))
                    put("p_gateway_trx_id", JsonPrimitive(gatewayTrxId))
                    put("p_sender_phone", JsonPrimitive(senderPhone))
                    put("p_note", JsonPrimitive(note))
                    if (role != null) {
                        put("p_role", JsonPrimitive(role))
                    }
                    if (expectedUserId != null) {
                        put("p_expected_user_id", JsonPrimitive(expectedUserId))
                    }
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ⚠️ [SUPABASE-MIGRATED - ধাপ ১২ ফিক্স] **এই ফাংশনটা এখন আর কোথাও call করা হয় না — ব্যবহার
     * করবে না।** লাইভ প্রজেক্টে `deposit_money_via_gateway` RPC-তে anon/authenticated কারো
     * EXECUTE গ্র্যান্টই নেই (`has_function_privilege` দিয়ে সরাসরি Supabase MCP-তে যাচাই করা
     * হয়েছে, দুটোই `false`) — অর্থাৎ এই RPC client থেকে কখনো সফলভাবে কল করা যাবে না
     * (PERMISSION_DENIED)। এর বদলে [SomadhanRepository.depositMoneyViaGateway] এখন
     * [requestWalletDeposit] কল করে (auth.uid()-ভিত্তিক, EXECUTE গ্র্যান্ট আছে, একই কাজ করে)।
     * এই ফাংশনটা ইচ্ছাকৃতভাবে ডিলিট করা হয়নি (dead-code পরিষ্কার করা ধাপ ২০ এ Firebase অপসারণের
     * সাথে একসাথে হবে) — শুধু dead/unused অবস্থায় রাখা হলো।
     */
    suspend fun depositMoneyViaGateway(
        userId: String,
        amount: Double,
        gateway: String,
        gatewayTrxId: String,
        senderPhone: String,
        note: String
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "deposit_money_via_gateway",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_amount", JsonPrimitive(amount))
                    put("p_gateway", JsonPrimitive(gateway))
                    put("p_gateway_trx_id", JsonPrimitive(gatewayTrxId))
                    put("p_sender_phone", JsonPrimitive(senderPhone))
                    put("p_note", JsonPrimitive(note))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে। */
    suspend fun adminConfirmGatewayDeposit(paymentId: String, action: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_confirm_gateway_deposit",
                buildJsonObject {
                    put("p_payment_id", JsonPrimitive(paymentId))
                    put("p_action", JsonPrimitive(action))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Bid lifecycle + dispute ---

    // [SUPABASE-MIGRATED - ধাপ ৮] `accept_bid` RPC এখন ৫টা প্যারামিটার নেয় (লাইভ প্রজেক্টে
    // migration `accept_bid_partial_wallet_gateway_topup` দিয়ে আগেই আপডেট হয়ে গেছে, এই session
    // এ RPC সোর্স পড়ে যাচাই করা হয়েছে) — পুরনো Firebase/local লজিকের মতোই ওয়ালেট থেকে যতটুকু
    // balance আছে ততটুকু deduct করে (partial), আর ঘাটতি (shortfall) থাকলে caller-কে অবশ্যই
    // gateway trx info (p_gateway_trx_id/p_gateway/p_gateway_amount) দিতে হবে -- না দিলে RPC
    // exception না ছুঁড়ে ফলাফলে `result: "INSUFFICIENT_BALANCE"` ফেরত দেয় (caller কে জানিয়ে
    // দেয় যে gateway payment আগে করাতে হবে)। gatewayTrxId/gateway/gatewayAmount ঐচ্ছিক
    // (default null) -- পুরো wallet balance দিয়ে accept করা গেলে এই তিনটা লাগেই না।
    // [Step 12.11] escrowId: client-এর local escrow id ("ESCROW_<৮>") — দিলে সার্ভার সেই id-তেই cloud escrow খোলে
    // (না দিলে/id আগে থেকে থাকলে আগের মতো "ESC_<uuid>"), যাতে release/refund RPC local id দিয়ে escrow খুঁজে পায়।
    suspend fun acceptBid(
        problemId: String,
        bidId: String,
        gatewayTrxId: String? = null,
        gateway: String? = null,
        gatewayAmount: Double? = null,
        escrowId: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "accept_bid",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_bid_id", JsonPrimitive(bidId))
                    put("p_gateway_trx_id", gatewayTrxId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_gateway", gateway?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_gateway_amount", gatewayAmount?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as Double?))
                    if (escrowId != null) put("p_escrow_id", JsonPrimitive(escrowId))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBid(bidId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "cancel_bid",
                buildJsonObject { put("p_bid_id", JsonPrimitive(bidId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectBid(bidId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "reject_bid",
                buildJsonObject { put("p_bid_id", JsonPrimitive(bidId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun solverCancelJob(
        problemId: String,
        reason: String,
        reopenAsOpen: Boolean
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "solver_cancel_job",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                    put("p_reopen_as_open", JsonPrimitive(reopenAsOpen))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ২৩ — লাইভ GPS: সলভারের বর্তমান লোকেশন `problems.solver_live_lat`/`solver_live_lng`/
     * `solver_live_updated_at`-এ লেখে। এই ৩টা কলাম RLS-এর সাধারণ UPDATE policy দিয়ে সলভারের
     * জন্য লেখা যায় না (`problems_update_owner` শুধু পোস্টদাতাকে অনুমতি দেয়) -- তাই
     * `update_solver_live_location` নামে একটা SECURITY DEFINER RPC এই session-এ Supabase
     * MCP দিয়ে সরাসরি তৈরি/apply করা হয়েছে (দেখুন `supabase/migrations/
     * step23_update_solver_live_location_rpc.sql` ও `MIGRATION_PROGRESS.md`-এর "ধাপ ২৩"
     * এন্ট্রি), যেটা ভেতরে `auth.uid() = accepted_solver_id` চেক করে শুধু এই ৩টা কলামই লেখে।
     */
    suspend fun updateSolverLiveLocation(problemId: String, lat: Double, lng: Double): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "update_solver_live_location",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_lat", JsonPrimitive(lat))
                    put("p_lng", JsonPrimitive(lng))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun raiseDispute(problemId: String, reason: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "raise_dispute",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে।
     *
     * ⚠️ [স্কোপ-বাইরে আবিষ্কার — ধাপ ২৯.৫] এই `resolve_dispute` RPC ও এই wrapper প্রজেক্টের একদম
     * শুরুর দিকের schema-bootstrap migration-এ (`rpc_dispute`, ২০২৬-০৯-১০) তৈরি হয়েছিল, কিন্তু
     * কোথাও থেকে **কল করা হয় না** (dead code — গ্রেপ করে যাচাই করা হয়েছে)। এটা এখানেও **ইচ্ছাকৃতভাবে
     * touch/wire করা হয়নি** — এর SPLIT_SETTLEMENT শাখা owner-refund নিজেই আবার করে (যেটা
     * `refundEscrowOnce()`/`refund_escrow_once` দিয়ে ইতিমধ্যে আলাদাভাবে migrate হয়ে গেছে — এটা কল
     * করলে ডাবল-রিফান্ড হতো) আর কমিশন হিসাবও Kotlin-সাইডের `calculateCommissionBreakdown()`-এর
     * promo/free-quota/extra-discount লজিক থেকে আলাদা (`resolve_commission_rate()` ব্যবহার করে)।
     * তাই ধাপ ২৯.৫-এ এর বদলে নিচের নতুন, সংকীর্ণভাবে-scoped `resolve_dispute_split()` বানানো হলো।
     * এই dead ফাংশনটা ধাপ ৩২ (Active-Path পুনঃনিরীক্ষা)-এ চিহ্নিত/পরিষ্কার করার জন্য রেখে দেওয়া হলো।
     */
    suspend fun resolveDispute(
        problemId: String,
        resolution: String,
        decisionNote: String,
        solverPercent: Double?
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "resolve_dispute",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_resolution", JsonPrimitive(resolution))
                    put("p_decision_note", JsonPrimitive(decisionNote))
                    put("p_solver_percent", solverPercent?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as Double?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ২৯.৫] Admin only — RPC সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে।
     *
     * শুধু dispute-এর SPLIT_SETTLEMENT/CUSTOM_SPLIT/SETTLE শাখার **সলভার-পেআউট + problem-row
     * আপডেট + notification** অংশটুকু dual-write করে — owner-refund এখানে করা হয় না (সেটা
     * `refundEscrowOnce()`-এর মধ্য দিয়ে আগে থেকেই আলাদাভাবে migrate করা `refund_escrow_once` RPC
     * দিয়ে যায়, এখানে আবার করলে ডাবল-রিফান্ড হতো)। সলভারের gross/commission/net amount
     * Kotlin-সাইডের `calculateCommissionBreakdown()` থেকে **pre-calculated** পাঠানো হয় — RPC নিজে
     * নতুন করে commission recompute করে না, শুধু bounds re-verify করে (percent অনুযায়ী প্রত্যাশিত
     * gross-এর কাছাকাছি কিনা, net<=gross<=escrow-total) — এটাই নিশ্চিত করে cloud balance ঠিক তা-ই
     * পাবে যা local-এ ইতিমধ্যে ক্রেডিট হয়েছে, একটা দ্বিতীয় কমিশন-ফর্মুলা তৈরি না করেই।
     *
     * `userRefundAmount` শুধু bookkeeping/notification-এর জন্য পাঠানো হয় (owner-কে টাকা দেওয়ার
     * জন্য না) — RPC এটা ব্যবহার করে বোঝে escrow ইতিমধ্যে `refund_escrow_once` দিয়ে বন্ধ হয়ে গেছে
     * কিনা (>0 হলে) নাকি সলভার ১০০% পেয়েছে বলে escrow কখনো refund পথে যায়ইনি (<=0 হলে, তখন এই RPC
     * নিজেই escrow status RELEASED করে দেয়)।
     */
    suspend fun resolveDisputeSplit(
        problemId: String,
        escrowId: String,
        splitSolverPercent: Double,
        solverGrossAmount: Double,
        commissionAmount: Double,
        solverNetAmount: Double,
        userRefundAmount: Double,
        resolutionDecision: String,
        decisionNote: String,
        progressAtSettlement: Int?
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "resolve_dispute_split",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_escrow_id", JsonPrimitive(escrowId))
                    put("p_split_solver_percent", JsonPrimitive(splitSolverPercent))
                    put("p_solver_gross_amount", JsonPrimitive(solverGrossAmount))
                    put("p_commission_amount", JsonPrimitive(commissionAmount))
                    put("p_solver_net_amount", JsonPrimitive(solverNetAmount))
                    put("p_user_refund_amount", JsonPrimitive(userRefundAmount))
                    put("p_resolution_decision", JsonPrimitive(resolutionDecision))
                    put("p_decision_note", JsonPrimitive(decisionNote))
                    put("p_progress_at_settlement", progressAtSettlement?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as Int?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Admin balance ---

    /**
     * Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে।
     *
     * [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] `role` optional — `null` দিলে
     * পুরনো ৪-আর্গুমেন্ট overload (DB ডিফল্ট `'USER'`), `"USER"`/`"SOLVER"` দিলে নতুন ৫-আর্গুমেন্ট
     * (`p_role`) overload। কোন role-এর balance adjust করা হচ্ছে সেটা টার্গেট `UserEntity`-এর
     * নিজস্ব `.role` ফিল্ড থেকেই জানা যায় (dual-row architecture-এ প্রতি role-এর নিজস্ব row) —
     * Admin panel-এ আলাদা role-selector UI নেই, দরকারও নেই।
     */
    suspend fun adminAdjustBalance(
        userId: String,
        amount: Double,
        isAddition: Boolean,
        reason: String,
        role: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_adjust_balance",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_amount", JsonPrimitive(amount))
                    put("p_is_addition", JsonPrimitive(isAddition))
                    put("p_reason", JsonPrimitive(reason))
                    if (role != null) {
                        put("p_role", JsonPrimitive(role))
                    }
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Admin: user status (KYC/ban/restrict/role) — ধাপ ১২ (মূল কাজ, ব্যাচ ১) ---
    //
    // এই ৬টা RPC এই session-এ নতুন migration (`step12_admin_user_status_rpcs_batch1`) দিয়ে
    // লাইভ প্রজেক্টে তৈরি করা হয়েছে (আগে ছিল না)। প্রতিটাই SECURITY DEFINER, `is_admin(auth.uid())`
    // চেক করে, `users` টেবিলের সংশ্লিষ্ট column আপডেট করে, আর নিজেই `notifications` টেবিলে insert
    // করে (Kotlin-সাইডের local NotificationEntity টেক্সটের সাথে হুবহু মিলিয়ে লেখা হয়েছে)। ⚠️
    // **জানা গুরুত্বপূর্ণ পার্থক্য**: `kyc_status` কলামে Supabase-এর check constraint শুধু
    // UPPERCASE মান মানে (`NONE`/`PENDING`/`APPROVED`/`REJECTED`) — লাইভে গিয়ে
    // `pg_get_constraintdef` দিয়ে যাচাই করে এটা ধরা পড়েছে — কিন্তু local Room-এ lowercase
    // (`"none"`/`"pending"`/`"verified"`/`"rejected"`) ব্যবহার হয় (`"verified"`-এর Supabase
    // সমতুল্য `"APPROVED"`, নাম-ও আলাদা)। তাই এই wrapper গুলো কোনো kyc-status string parameter
    // Kotlin থেকে নেয় না — RPC নিজেই সঠিক uppercase মান hardcode করে সেট করে, ভুল কেসিং/মান
    // পাঠানোর কোনো সুযোগ নেই। `admin_audit_logs` insert এখানে হয় না (সেটা `logAdminAction()` →
    // `log_admin_action` RPC দিয়ে Kotlin-সাইড থেকে আলাদাভাবে dual-write হয়, ডুপ্লিকেট এড়াতে)।

    suspend fun adminApproveKyc(userId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_approve_kyc",
                buildJsonObject { put("p_user_id", JsonPrimitive(userId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRejectKyc(userId: String, reason: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_reject_kyc",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRevokeKyc(userId: String, reason: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_revoke_kyc",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D২] [role] প্যারামিটার null দিলে RPC-এর
    // ডিফল্ট আচরণ (শুধু shared `is_banned`/`is_restricted` কলাম, backward compatible —
    // AdminUserLookupView-এর মতো যেসব caller এখনো role-aware split করা হয়নি) অপরিবর্তিত।
    // "USER"/"SOLVER" দিলে RPC শুধু সংশ্লিষ্ট role-scoped কলাম আপডেট করে (AdminUsersView-এর
    // নতুন dual-card অ্যাকশন এভাবেই কল করে)।
    suspend fun adminSetBanned(userId: String, banned: Boolean, role: String? = null): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_set_banned",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_banned", JsonPrimitive(banned))
                    if (role != null) put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminSetRestricted(userId: String, restricted: Boolean, role: String? = null): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_set_restricted",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_restricted", JsonPrimitive(restricted))
                    if (role != null) put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** `p_new_role` অবশ্যই `"USER"`/`"SOLVER"`/`"ADMIN"` (uppercase) — RPC নিজে যাচাই করে, নাহলে `INVALID_ROLE` exception। */
    suspend fun adminChangeRole(userId: String, newRole: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_change_role",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_new_role", JsonPrimitive(newRole))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Role switching — ধাপ ১৪.৫খ ---
    //
    // `switch_role_get_or_create_linked_profile` RPC এই ধাপে single-row মডেলে rewrite করা
    // হয়েছে (আগে ছিল dual-row: টার্গেট role-এর জন্য নতুন users row insert করতো)। এখন এটা
    // caller-এর নিজের root row-ই (id = auth.uid()) আপডেট করে -- has_user_role/has_solver_role
    // flag flip, আর SOLVER হলে solver_categories/has_completed_solver_setup সেট করে। কোনো
    // নতুন row তৈরি হয় না। জবাবে role-scoped balance/reputation/ban/restrict কলামগুলো ফেরত
    // আসে যাতে caller (SomadhanRepository.switchRole()) local active-role row-এ সেগুলো
    // প্রতিফলিত করতে পারে -- Kotlin-সাইড এখনো নিজস্ব dual-row (per-role আলাদা UserEntity)
    // মডেল ব্যবহার করে (global rule #২: চালু ফিচার ভাঙা যাবে না, তাই local architecture এই
    // ধাপে বদলানো হয়নি), এই RPC শুধু cloud-সাইডকে সঠিক single-row বানায় আর সেই role-scoped
    // সত্য মান local row-এ sync করার উপায় দেয়। `p_solver_categories` খালি/null পাঠালে RPC
    // বিদ্যমান মান ধরে রাখে (coalesce)।
    suspend fun switchRole(targetRole: String, solverCategoriesCsv: String?): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "switch_role_get_or_create_linked_profile",
                buildJsonObject {
                    put("p_target_role", JsonPrimitive(targetRole))
                    put(
                        "p_solver_categories",
                        solverCategoriesCsv?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?)
                    )
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Direct Contract flow — ধাপ ১২ (মূল কাজ, ব্যাচ ২) ---
    //
    // এই ৫টা RPC এই session-এ নতুন migration (`step12_batch2_direct_contract_rpcs`) দিয়ে
    // লাইভ প্রজেক্টে তৈরি করা হয়েছে (আগে ছিল না)। `accept_direct_contract`/`decline_direct_contract`
    // solver-actor (owner না) কর্তৃক কল হয় বলে RPC লাগে (RLS-এ problems_update_owner শুধু owner-কেই
    // allow করে)। `admin_*` তিনটা admin-actor, money/notification জড়িত বলে RPC-ভিত্তিক (established
    // প্যাটার্ন — কোথাও raw problems update ব্যবহার হয় না)। প্রতিটাই নিজে notification insert করে।

    // [Step 12.11] escrowId: client-এর local escrow id — accept_bid()-এর মতোই (দেখুন ওপরের মন্তব্য)।
    suspend fun acceptDirectContract(problemId: String, escrowId: String? = null): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "accept_direct_contract",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    if (escrowId != null) put("p_escrow_id", JsonPrimitive(escrowId))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineDirectContract(problemId: String, reason: String = ""): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "decline_direct_contract",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে। */
    suspend fun adminUpdateDirectContractStatus(problemId: String, status: String, directContractStatus: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_update_direct_contract_status",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_status", JsonPrimitive(status))
                    put("p_direct_contract_status", JsonPrimitive(directContractStatus))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে। */
    /**
     * [SUPABASE-MIGRATED - ধাপ ১] `admin_refund_and_reopen_problem` RPC — is_admin() চেক-সহ,
     * problem.status='OPEN' + accepted_bid_id/accepted_solver_id/accepted_solver_name/
     * accepted_amount=NULL সেট করে, এবং সংশ্লিষ্ট bid-কে CANCELLED (resolution_type=
     * ADMIN_MANUAL_REFUND) করে। adminRefundEscrow()-এর local problemDao.updateProblem()/
     * bidDao.updateBid()-এর ঠিক পরে dual-write হিসেবে কল হবে।
     */
    suspend fun adminRefundAndReopenProblem(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_refund_and_reopen_problem",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে। */
    suspend fun adminUpdateProblemStatus(problemId: String, status: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_update_problem_status",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_status", JsonPrimitive(status))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ২] `admin_update_problem_budget` RPC — is_admin() চেক-সহ, শুধু
     * `problems.min_budget/max_budget` (+ last_activity_at) আপডেট করে। owner-notify RPC নিজে করে
     * না — existing local+cloud (createNotification) dual-write আলাদাই থাকে।
     */
    suspend fun adminUpdateProblemBudget(problemId: String, minBudget: Double, maxBudget: Double): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_update_problem_budget",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_min_budget", JsonPrimitive(minBudget))
                    put("p_max_budget", JsonPrimitive(maxBudget))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩] `admin_reassign_solver` RPC — is_admin() চেক-সহ, শুধু
     * `problems.accepted_solver_id/accepted_solver_name` (+ last_activity_at) আপডেট করে।
     * notification নিজে করে না — existing local+cloud (createNotification, দুই পক্ষকে)
     * dual-write আলাদাই থাকে, ঠিক ধাপ ২-এর adminUpdateProblemBudget-এর মতো।
     */
    suspend fun adminReassignSolver(problemId: String, solverId: String, solverName: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_reassign_solver",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_solver_id", JsonPrimitive(solverId))
                    put("p_solver_name", JsonPrimitive(solverName))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Job release / dispute flow — ধাপ ১২ (মূল কাজ, ব্যাচ ৩) ---
    //
    // এই ৭টা RPC এই session-এ নতুন migration (`step12_batch3_job_release_dispute_rpcs`) দিয়ে
    // লাইভ প্রজেক্টে তৈরি করা হয়েছে (আগে ছিল না)। solver-actor (`requestJobRelease`,
    // `cancelJobReleaseRequest`) আর owner-actor (`rejectJobReleaseRequest`) সবগুলোই RPC লাগে
    // (RLS-এ `problems_update_owner` শুধু owner-কেই raw update allow করে, আর
    // `rejectJobReleaseRequest`-এ escrow/additional_charges-জড়িত multi-table লজিকও আছে)।
    // `adminManuallyFlagDispute` admin-actor। `withdrawDispute`/`settleDispute`-এ dispute-এর
    // যেকোনো পক্ষ actor হতে পারে। প্রতিটাই নিজে notification insert করে (raise_dispute/
    // admin_set_banned প্যাটার্নের মতো)।
    //
    // জানা সীমাবদ্ধতা: `admin_manually_flag_dispute` RPC-তে `dispute_initiator_id` (uuid কলাম)
    // local-এর মতো স্ট্রিং `"ADMIN"` রাখতে পারে না (uuid cast ব্যর্থ হবে) — তাই cloud-এ এই কলাম
    // null থাকে, role কলামে শুধু `'ADMIN'` বসে। এর ফলে `withdrawDispute` RPC-তে admin-ফ্ল্যাগ করা
    // dispute কখনো auth.uid() দিয়ে match করবে না (initiator null) — যা আসলে সঠিক আচরণ, কারণ local
    // সাইডেও normal user/solver session থেকে requesterId="ADMIN" পাঠানো সম্ভব না।

    /** Actor = accepted solver. RPC নিজেই solver-অথরাইজেশন চেক করে, প্রয়োজনে pending additional_charge তৈরি করে, আর owner-কে notify করে। */
    suspend fun requestJobRelease(problemId: String, extraAmount: Double, note: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "request_job_release",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_extra_amount", JsonPrimitive(extraAmount))
                    put("p_note", JsonPrimitive(note))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = accepted solver। RPC নিজেই dispute একসাথে ছিল কিনা যাচাই করে সেই অনুযায়ী রিসেট করে। */
    suspend fun cancelJobReleaseRequest(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "cancel_job_release_request",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = problem owner। RPC নিজেই escrow.extra_amount ACCEPTED additional_charges যোগফল দিয়ে রিসেট করে। */
    suspend fun rejectJobReleaseRequest(problemId: String, reason: String = ""): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "reject_job_release_request",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin only — RPC নিজেই সার্ভার-সাইডে `is_admin(auth.uid())` চেক করে। */
    suspend fun adminManuallyFlagDispute(problemId: String, reason: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_manually_flag_dispute",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = dispute-এর initiator নিজে (owner অথবা solver)। RPC নিজেই initiator মিলিয়ে দেখে। */
    suspend fun withdrawDispute(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "withdraw_dispute",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = dispute-এর যেকোনো পক্ষ (owner অথবা solver)। */
    suspend fun settleDispute(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "settle_dispute",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actor = problem-এর owner অথবা solver পক্ষ। RPC problem flag আপডেট করে আর party-to-party
     * নোটিস মেসেজ insert করে। admin-broadcast নোটিফিকেশন এখানে নেই — সেটার জন্য আলাদাভাবে
     * (আগে থেকেই থাকা) [notifyAdmins] কল করতে হবে, কারণ notifications.user_id একটা uuid কলাম আর
     * local-এর সেন্টিনেল ভ্যালু `"admin_broadcast"` uuid না।
     */
    suspend fun requestAdminAssistance(problemId: String, requesterRole: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "request_admin_assistance",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_requester_role", JsonPrimitive(requesterRole))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Instant Job Lifecycle (ধাপ ১২ ব্যাচ ৪ক) ---

    /** Actor = accepted solver। RPC নিজেই accepted_solver_id মিলিয়ে দেখে, owner-কে notify করে। */
    suspend fun markSolverOnWay(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_solver_on_way",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = accepted solver। RPC উভয় পক্ষকে notify করে (owner + solver)। */
    suspend fun markSolverArrived(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_solver_arrived",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = accepted solver। RPC জব স্ট্যাটাস IN_PROGRESS করে, উভয় পক্ষকে notify করে। */
    suspend fun markJobStarted(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_job_started",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actor = problem owner। RPC problem/bids রিসেট করে, উভয় পক্ষকে notify করে — কিন্তু escrow
     * refund করে না (সেটা caller-সাইডে আলাদা, ইতিমধ্যে-migrate-করা `refundEscrowOnce()` পথে হয়,
     * ধাপ ৯ — এখানে ডুপ্লিকেট করলে ডাবল-রিফান্ডের ঝুঁকি তৈরি হতো)। `progressStep` client-সাইডে
     * `ProblemEntity.calculateProgressStep()` দিয়ে হিসাব করা মান, RPC নিজে recompute করে না।
     */
    suspend fun cancelInstantJob(problemId: String, reason: String, progressStep: Int): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "cancel_instant_job",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                    put("p_progress_step", JsonPrimitive(progressStep))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ১২ ব্যাচ ৪গ। Actor = problem owner (RPC-এর মধ্যেই `auth.uid() = problems.user_id` চেক
     * হয়, আর `is_instant_job = true` না হলেও ব্যর্থ হবে)। RPC নিজেই matched solver খুঁজে বের করে
     * (category match + `instant_job_notifications_enabled` + radius, Kotlin-এর
     * `createInstantJob()`-এর broadcast-filter লজিক মিরর করে) আর তাদের প্রত্যেককে notification
     * insert করে। Problem row নিজে এই RPC insert করে না — সেটা আগে থেকেই migrate করা
     * `createProblem()` (raw insert, ধাপ ৮) দিয়ে হয়, এই RPC শুধু broadcast notification অংশটা
     * কভার করে (owner এর নিজের insert করা problem row-এর উপর ভিত্তি করে)।
     */
    suspend fun broadcastInstantJob(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "broadcast_instant_job",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ১২ ব্যাচ ৪গ। Actor = admin (RPC নিজেই `is_admin(auth.uid())` চেক করে)। REBROADCAST/
     * TO_NORMAL_BIDDING/CANCEL — তিনটা targetAction-ই এক RPC-তে হ্যান্ডেল হয় (problem/bids রিসেট
     * + দুই পক্ষকে notify, reason সহ)। escrow refund এই RPC করে না — সেটা caller-সাইডে আলাদাভাবে
     * ইতিমধ্যে-migrate-করা `refundEscrowOnce()` (ধাপ ৯) দিয়ে আগে থেকেই হয়ে যায়। `progressStep`
     * client-সাইডে হিসাব করা মান (RPC recompute করে না, `cancelInstantJob()`-এর মতোই প্যাটার্ন)।
     *
     * নোট: এই RPC-টা DB-তে এই session শুরুর আগেই (একটা আগের, `MIGRATION_PROGRESS.md`-এ
     * অ-লিপিবদ্ধ session-এ) তৈরি হয়ে ছিল — উদ্ধার করে/audit করে সঠিক পাওয়া গেছে বলেই এখানে wire
     * করা হলো, নতুন করে লেখা হয়নি।
     */
    suspend fun adminForceCancelInstantJob(
        problemId: String,
        reason: String,
        targetAction: String,
        progressStep: Int
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_force_cancel_instant_job",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_reason", JsonPrimitive(reason))
                    put("p_target_action", JsonPrimitive(targetAction))
                    put("p_progress_step", JsonPrimitive(progressStep))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ধাপ ১২ ব্যাচ ৪গ। Actor = problem owner-ই শুধু (RPC-এর মধ্যেই `auth.uid() = problems.user_id`
     * চেক হয়) — জেনে-বুঝেই per-problem/owner-scoped ডিজাইন (Kotlin-এর
     * `checkAndExpireInstantJobs()`-এর মতো "যেকোনো logged-in user অন্য যে কারো broadcasting job
     * expire করতে পারবে" global sweep না — এটাও আগের অ-লিপিবদ্ধ session-এর সিদ্ধান্ত, নিরাপত্তার
     * দিক থেকে ভালো তাই বহাল রাখা হলো)।
     *
     * **সীমাবদ্ধতা (গুরুত্বপূর্ণ):** যেহেতু এই RPC শুধু owner নিজেই কল করতে পারে, তাই
     * `checkAndExpireInstantJobs()`-এর মধ্যে যখন অন্য কোনো ইউজারের broadcasting job (বর্তমান
     * logged-in ইউজারের নিজের না) time-out হয়ে expire হয়, সেই ক্ষেত্রে local Room এ ঠিকই
     * expire হবে কিন্তু Supabase dual-write স্কিপ হয়ে যাবে (caller ইউজার owner না)। ওই
     * problem-টা Supabase-এ dual-write হবে শুধু তখনই যখন কোনো এক সময় তার নিজের owner-এর ডিভাইস
     * নিজেই এই sweep চালাবে আর নিজের এই job-টা expired পাবে। এটা একটা known gap — future ধাপে
     * (server-side cron/Edge Function) পুরোপুরি ঠিক করা যেতে পারে, কিন্তু এখনকার owner-scoped RPC
     * ডিজাইন অক্ষত রেখেই।
     */
    suspend fun expireBroadcastingInstantJob(problemId: String, progressStep: Int): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "expire_broadcasting_instant_job",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_progress_step", JsonPrimitive(progressStep))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = accepted solver। RPC নিজেই solver-অথরাইজেশন চেক করে pending-extra-amount সেট করে, owner-কে notify করে। */
    suspend fun requestExtraAmount(problemId: String, amount: Double, note: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "request_extra_amount",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_amount", JsonPrimitive(amount))
                    put("p_note", JsonPrimitive(note))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actor = problem owner। RPC নিজেই wallet_deduction = least(balance, pending_extra_amount)
     * স্বাধীনভাবে হিসাব করে (client-এর walletDeducted প্যারামিটারের উপর নির্ভর করে না) — escrow ও
     * confirmed_extra_amount_total বাড়ে সম্পূর্ণ pending amount দিয়ে, wallet থেকে শুধু আংশিক কাটা
     * হতে পারে (বাকিটা demo payment gateway দিয়ে "পরিশোধ" ধরা হয়, নিয়ম #৩ অনুযায়ী)।
     *
     * [Step 12.8c] `expectedAmount` ঐচ্ছিক (ব্যবহারকারীর সম্মতিতে যোগ করা, ২০২৬-০৯-২১) — null (ডিফল্ট)
     * = আজকের আচরণ হুবহু (পুরনো ১-arg RPC signature resolve হয়, কোনো guard নেই)। non-null হলে
     * `p_expected_amount` পাঠানো হয় -- তখন নতুন ২-arg overload চলে, যেটা retry-উইন্ডোতে pending amount
     * বদলে গেলে non-OK `AMOUNT_CHANGED` ফেরত দেয় (deposit-এর `expectedUserId`-এর হুবহু প্যাটার্নে,
     * 12.8b দেখো)। শুধু outbox replay (`OutboxRpcDispatcher`) এটা পাঠায়।
     */
    suspend fun userConfirmExtraAmount(problemId: String, expectedAmount: Double? = null): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "user_confirm_extra_amount",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    if (expectedAmount != null) {
                        put("p_expected_amount", JsonPrimitive(expectedAmount))
                    }
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = problem owner। RPC pending-extra-amount ফিল্ড ক্লিয়ার করে, সলভারকে notify করে। */
    suspend fun userRejectExtraAmount(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "user_reject_extra_amount",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Rating ---

    /**
     * `submit_rating` RPC সোর্স পড়ে যাচাই করা হয়েছে (ধাপ ১১) — server-side নিজেই problem row
     * থেকে problem_title/user_id/solver_id বের করে নেয় আর নিজের rating_id বানায় (client থেকে id
     * পাঠানো যায় না, রিটার্ন করা `rating_id` ব্যবহার করার দরকার নেই — কারণ ratings-এর জন্য
     * additional_charges-এর মতো পরের কোনো dual-write ফাংশন cloud id দিয়ে পরে lookup করে না,
     * প্রতিটা rating submit একবারই হয়, পরে update/respond করার কিছু নেই)। authorization RPC নিজেই
     * করে: raterRole="USER" হলে caller-কে অবশ্যই problem.user_id হতে হবে, raterRole="SOLVER" হলে
     * problem.accepted_solver_id — তাই caller-সাইডে এখানে আলাদা guard যোগ করার দরকার নেই, তবে
     * repository লেয়ারে dual-write কল করার আগে caller নিজে থেকেই মিলিয়ে নেবে (best-effort)।
     */
    suspend fun submitRating(
        problemId: String,
        stars: Int,
        comment: String,
        raterRole: String
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "submit_rating",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_stars", JsonPrimitive(stars))
                    put("p_comment", JsonPrimitive(comment))
                    put("p_rater_role", JsonPrimitive(raterRole))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ১১ (Repository Migration D1) — Messages (chat) + Reputation events
    //
    // `messages`/`ratings` টেবিলের মতোই client সরাসরি লিখতে পারে (RLS দিয়ে সীমিত — sender নিজে,
    // আর নিজের problem-এর party হতে হবে); কোনো RPC লাগে না, তাই Money Part A/B-এর মতো RPC-wrapper
    // প্যাটার্ন এখানে না — বরং problems/bids (ধাপ ৪)-এর মতো সরাসরি Postgrest insert/update
    // প্যাটার্ন। reputation_events-এর জন্য RPC আছে (`submit_reputation_event`, লাইভ প্রজেক্টে
    // Supabase MCP দিয়ে সোর্স পড়ে যাচাই করা হয়েছে) — client সরাসরি reputation_events টেবিলে
    // insert/update করতে পারে না (শুধু SELECT পলিসি আছে, INSERT/UPDATE নেই)।
    // ============================================================

    // --- Messages ---

    /**
     * সরাসরি client থেকে `messages` টেবিলে insert করে। RLS policy `messages_insert`-এর
     * `with_check`: `auth.uid() = sender_id AND (সেই problem-এর owner অথবা accepted_solver
     * caller)` — Supabase MCP দিয়ে যাচাই করা। তাই caller (repository) নিশ্চিত করবে dual-write
     * কল করার আগে `SupabaseAuthManager.currentUserId() == senderId`।
     */
    suspend fun sendMessage(message: MessageDto): Result<Unit> {
        return try {
            client.postgrest.from("messages").insert(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * RLS policy `messages_update`-এর `with_check`: `auth.uid() = receiver_id` — অর্থাৎ শুধু
     * receiver নিজের পাওয়া মেসেজগুলোই is_read=true করতে পারবে। `markMessagesAsReadForProblem()`
     * (SomadhanRepository/messageDao) এর সাথে হুবহু মেলা ফাংশন — এই problem-এর, এই receiver-এর
     * সব unread মেসেজ একসাথে read মার্ক করে।
     */
    suspend fun markMessagesAsReadForProblem(problemId: String, userId: String): Result<Unit> {
        return try {
            client.postgrest.from("messages").update(
                buildJsonObject { put("is_read", JsonPrimitive(true)) }
            ) {
                filter {
                    eq("problem_id", problemId)
                    eq("receiver_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Reputation events ---

    /**
     * `submit_reputation_event` RPC সোর্স পড়ে যাচাই করা হয়েছে (ধাপ ১১, আর ধাপ ১২-প্রি-ফিক্সে
     * authorization মডেল আপডেট হয়েছে) — এটা [applyReputationChange]-এর server-side সমতুল্য,
     * কিন্তু **হুবহু mirror না**:
     *
     * - `ADMIN_ADJUSTMENT` ইভেন্টে caller-কে অবশ্যই admin হতে হবে (`is_admin`), তখনই
     *   `p_score_change` সরাসরি ব্যবহার হয়।
     * - `BID_WON`/`JOB_COMPLETED`/`PROBLEM_POSTED`/`RATING_BONUS`/`WITHDRAWAL_COMPLETED`/
     *   `EXTRA_CHARGE_VIA_APP`/`EXTRA_CHARGE_ACCEPTED` — এই সাতটার জন্য RPC **নিজেই** সংশ্লিষ্ট
     *   টেবিল (bids/problems/ratings/withdrawals/additional_charges) দেখে score independently
     *   হিসাব করে platform_settings থেকে — `p_score_change` তখন আদৌ ব্যবহারই হয় না (শুধু
     *   ADMIN_ADJUSTMENT-এ ব্যবহার হয়)। তাই local score আর cloud score সামান্য ভিন্ন হতে পারে যদি
     *   local/remote platform_settings sync-এ সাময়িক পার্থক্য থাকে — এটা Kotlin-সাইডের অন্যান্য
     *   RPC-wrapper (যেমন acceptBid) এর মতোই ইচ্ছাকৃত ডিজাইন (server স্বাধীনভাবে পুনঃগণনা করে,
     *   client-এর মান blindly কপি করে না)।
     * - **ধাপ ১২-প্রি-ফিক্স (গুরুত্বপূর্ণ পরিবর্তন):** আগে `BID_WON`/`JOB_COMPLETED`/
     *   `PROBLEM_POSTED`/`RATING_BONUS`/`WITHDRAWAL_COMPLETED`-এর জন্য RPC একটা ব্ল্যাংকেট চেক
     *   করত `p_user_id == caller`, ফলে counterparty/admin অন্য কারো পক্ষে (যেমন problem
     *   owner-এর session থেকে solver-এর BID_WON) কল করলে সবসময় `NOT_ELIGIBLE` পেত। এখন প্রতিটা
     *   event type-এর জন্য আলাদা authorization চেক আছে — eligibility `p_user_id` (target)-এর
     *   ভিত্তিতে যাচাই হয়, আর caller নিজে সেই target, অথবা প্রাসঙ্গিক counterparty (problem owner/
     *   accepted_solver/rater), অথবা admin হলেই authorized। তাই এখন `applyReputationChange()`
     *   থেকে dual-write attempt local guard ছাড়াই (session থাকলেই) পাঠানো হয় — RPC নিজেই
     *   authorization/eligibility চূড়ান্তভাবে যাচাই করে, ব্যর্থ হলে `NOT_ELIGIBLE` শুধু log হয়।
     * - **[ধাপ ৩৩.১ আপডেট]** আগে এই সাতটার বাইরের event type (RATING_PENALTY, INACTIVE_7_DAYS,
     *   INACTIVE_30_DAYS ইত্যাদি) পাঠালে RPC `UNSUPPORTED_EVENT_TYPE` ছুঁড়ত। এখন **`INACTIVE_7_DAYS`
     *   এবং `INACTIVE_30_DAYS` সমর্থিত** — migration `step33_1_reputation_event_inactive_decay_support_6arg`
     *   দিয়ে ৬-আর্গুমেন্ট overload-এ নতুন ব্র্যাঞ্চ যোগ করা হয়েছে (Supabase MCP দিয়ে লাইভ `pg_proc`
     *   পড়ে যাচাই করা হয়েছে — এই migration আগে থেকেই DB-তে প্রয়োগ করা ছিল)। এই দুইটাতে RPC
     *   server-side নিজেই independently `updated_at` (inactivity) আর
     *   `last_reputation_decay_check_at` (৭-দিনের rate-limit) চেক করে — client-এর দাবির ওপর ভরসা
     *   করে না। বাকি (RATING_PENALTY, UNRESPONSIVE_CHAT, EXTRA_PAYMENT_MISS_CYCLE_PENALTY, বা
     *   Admin Panel-এর dynamic custom event) এখনো `UNSUPPORTED_EVENT_TYPE` — এটা শুধু log হবে
     *   (best-effort dual-write, caller Result.failure পাবে), local reputation flow সম্পূর্ণ
     *   অপ্রভাবিত থাকবে।
     *
     * [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] `role` optional — `null` দিলে
     * পুরনো ৫-আর্গুমেন্ট overload কল হয় (আগের আচরণ অপরিবর্তিত)। `"USER"`/`"SOLVER"` দিলে নতুন
     * ৬-আর্গুমেন্ট (`p_role`) overload কল হয়। **গুরুত্বপূর্ণ**: RPC সোর্স পড়ে যাচাই করা হয়েছে —
     * `BID_WON`/`JOB_COMPLETED`/`PROBLEM_POSTED`/`RATING_BONUS`/`WITHDRAWAL_COMPLETED`/
     * `EXTRA_CHARGE_VIA_APP`/`EXTRA_CHARGE_ACCEPTED` — এই সাতটা event type-এ RPC নিজেই
     * (structurally বা contextually) role derive করে, `p_role` পাঠালেও সেটা এই path-এ **ignore**
     * হয় (শুধু ভবিষ্যৎ-সামঞ্জস্যের জন্য পাঠানো, ক্ষতিকর না)। **`ADMIN_ADJUSTMENT`, `INACTIVE_7_DAYS`,
     * এবং `INACTIVE_30_DAYS`** — এই তিনটা event type-এ RPC আসলে `p_role` ব্যবহার করে এবং
     * **বাধ্যতামূলক** — না দিলে যথাক্রমে `ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT` বা
     * `ROLE_REQUIRED_FOR_INACTIVE_DECAY` exception (শুধু log হবে, local flow অপ্রভাবিত)।
     * `applyReputationChange()` কলার-এর নিজের `user.role` থেকে role derive করে সবসময় পাঠায়
     * (কখনো `null` না, `runInactivityReputationDecay()`-সহ), তাই এই দুইটা নতুন branch-এও
     * dual-write ইতিমধ্যে কার্যকরভাবে কাজ করে — আলাদা কোনো call-site পরিবর্তন লাগেনি।
     */
    suspend fun submitReputationEvent(
        userId: String,
        eventType: String,
        refId: String?,
        scoreChange: Double?,
        note: String?,
        role: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "submit_reputation_event",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_event_type", JsonPrimitive(eventType))
                    put("p_ref_id", refId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_score_change", scoreChange?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as Double?))
                    put("p_note", note?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    if (role != null) {
                        put("p_role", JsonPrimitive(role))
                    }
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ১২ প্রি-ফিক্স — Admin delete message/rating + admin-send-to-chat RPC wrapper
    //
    // `messages`/`ratings` টেবিলে কোনো DELETE RLS policy নেই আর `messages_insert` পলিসি admin-কে
    // অন্যের chat-এ insert করতে দেয় না (Supabase MCP দিয়ে যাচাই করা, ধাপ ১১-এর রিপোর্টে গ্যাপ
    // হিসেবে চিহ্নিত হয়েছিল) — তাই তিনটা নতুন SECURITY DEFINER RPC বানানো হলো
    // (`admin_delete_message`, `admin_delete_rating`, `admin_send_message_to_problem_chat`),
    // প্রতিটাই ভিতরে `is_admin(auth.uid())` চেক করে।
    // ============================================================

    /**
     * `admin_delete_message` RPC wrapper — কোনো message id `messages` টেবিল থেকে মুছে দেয়,
     * caller admin না হলে RPC নিজেই `ADMIN_ONLY` exception ছোঁড়ে।
     */
    suspend fun adminDeleteMessage(messageId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_delete_message",
                buildJsonObject { put("p_message_id", JsonPrimitive(messageId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `admin_delete_rating` RPC wrapper — কোনো rating id `ratings` টেবিল থেকে মুছে দেয়,
     * caller admin না হলে RPC নিজেই `ADMIN_ONLY` exception ছোঁড়ে।
     */
    suspend fun adminDeleteRating(ratingId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_delete_rating",
                buildJsonObject { put("p_rating_id", JsonPrimitive(ratingId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `admin_send_message_to_problem_chat` RPC wrapper — admin-এর পক্ষ থেকে কোনো problem-এর
     * chat-এ "Support Manager 🛡️" নামে একটা `is_admin_message=true` মেসেজ insert করে।
     * (Admin Action bug-fix master prompt, ধাপ ৭ — RPC generalize করা হয়েছে) receiver ঐচ্ছিক
     * `p_receiver_id`-এ resolve হয়: না দিলে (NULL) আগের মতোই problem owner, দিলে সেটা owner বা
     * problem-এর accepted_solver_id-এর একটা হতেই হবে (RPC নিজেই validate করে, নাহলে
     * INVALID_RECEIVER exception)। আর problem-এর `is_admin_involved_in_chat=true` করে দেয়।
     * Notification পাঠানো এই RPC-এর স্কোপে নেই — সেটা notification migration-এর অংশ (ধাপ ১২-এর
     * মূল কাজ)।
     */
    suspend fun adminSendMessageToProblemChat(
        problemId: String,
        content: String,
        receiverId: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_send_message_to_problem_chat",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_content", JsonPrimitive(content))
                    put("p_receiver_id", receiverId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ১২ (মূল কাজ) — Notifications
    //
    // `notifications` টেবিলের RLS: SELECT (নিজের row অথবা admin), UPDATE (শুধু নিজের row,
    // is_read=true করার জন্য যথেষ্ট) -- কিন্তু কোনো client-side INSERT/DELETE policy নেই। তাই
    // নতুন notification তৈরি (admin_broadcast_notification/admin_notify_user) আর group-delete
    // (admin_delete_notification_group) দুটোই SECURITY DEFINER RPC দিয়ে হয় (ধাপ ১২ প্রি-ফিক্সে
    // Supabase MCP দিয়ে এই ৩টা RPC-এর সোর্স পড়ে যাচাই করা হয়েছে -- এগুলো এই session-এর আগেই
    // লাইভ প্রজেক্টে ছিল, master prompt-এর RPC তালিকায় ছিল না)। markAsRead/markAllAsRead সরাসরি
    // Postgrest update দিয়ে হয় (RLS নিজেই own-row এ সীমাবদ্ধ করে)।
    // ============================================================

    /**
     * Postgres `timestamp with time zone` কলামে পাঠানোর জন্য epoch-millis কে ISO-8601 (UTC)
     * string এ রূপান্তর করে। `java.time.Instant` এড়ানো হয়েছে (এই প্রজেক্টে core library
     * desugaring configured আছে কিনা নিশ্চিত না, তাই সব API level এ নিরাপদ `SimpleDateFormat`
     * ব্যবহার করা হলো)।
     */
    private fun epochMillisToIsoUtc(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(millis))
    }

    /**
     * ধাপ ৪.৫ — RLS silent-noop audit ফিক্স।
     *
     * postgrest-kt-এর raw `update`/`upsert`/`delete` কল ডিফল্টে `Prefer: return=minimal`
     * হেডার ব্যবহার করে, তাই RLS পলিসি ০ row ম্যাচ করলেও (ভুল id, বা পলিসি ভবিষ্যতে বদলে
     * গেলে) কোনো error না দিয়েই "সফল" রিটার্ন করে -- write টা আসলে কার্যকর হয়নি তা কখনো
     * ধরা পড়ে না (`RPC_SYNC_FIX_PROGRESS.md` Step 1-এ ডকুমেন্টেড, আগে `submitKyc()`-এ এই
     * বাগ বাস্তবে পাওয়া গিয়েছিল)।
     *
     * এই হেল্পার কল-সাইটের builder lambda-তে `select()` যোগ করার পর রেসপন্স থেকে affected
     * row সংখ্যা গুনে ০ হলে exception থ্রো করে, যা বিদ্যমান `catch (e: Exception)` ব্লক ধরে
     * `Result.failure(e)` রিটার্ন করবে -- আগের সাইলেন্ট-সাকসেস আচরণ বন্ধ হয়ে যায়। ১+ row
     * ম্যাচ হলে (স্বাভাবিক/সফল কেস) এই হেল্পার কিছুই বদলায় না, শুধু pass-through করে।
     *
     * দ্রষ্টব্য: এই হেল্পার তখনই কাজ করবে যখন কল-সাইটের builder lambda-তে `select()` যোগ করা
     * থাকবে -- নাহলে রেসপন্স বডি খালি আসবে, decode করলে false-positive ০-row এরর হবে।
     */
    private fun requireAffectedRowOrThrow(
        result: io.github.jan.supabase.postgrest.result.PostgrestResult,
        context: String
    ) {
        val affected = result.decodeList<JsonObject>()
        if (affected.isEmpty()) {
            throw IllegalStateException(
                "RLS silent-noop guard: 0 rows affected for $context -- write did not match " +
                    "any row (check RLS policy / target id). See RPC_SYNC_FIX_PROGRESS.md Step 4.5."
            )
        }
    }

    /**
     * নিজের একটা notification read মার্ক করে (RLS: `auth.uid() = user_id` চেক হয়ে যায়
     * automatically -- অন্য কারো notification হলে ০ row আপডেট হবে, কোনো error না, কিন্তু কিছু
     * বদলাবেও না)।
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            client.postgrest.from("notifications").update(
                buildJsonObject { put("is_read", JsonPrimitive(true)) }
            ) {
                filter { eq("id", notificationId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** নিজের সব notification read মার্ক করে (RLS: `auth.uid() = user_id`)। */
    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            client.postgrest.from("notifications").update(
                buildJsonObject { put("is_read", JsonPrimitive(true)) }
            ) {
                filter { eq("user_id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `admin_broadcast_notification` RPC wrapper -- targetRole ("ALL"/"USER"/"SOLVER") অনুযায়ী
     * সব মিলে যাওয়া user-দের জন্য একসাথে notification insert করে। caller admin না হলে RPC নিজেই
     * `ADMIN_ONLY` exception ছোঁড়ে।
     */
    suspend fun adminBroadcastNotification(
        targetRole: String,
        title: String,
        message: String,
        targetType: String = "general",
        scheduledForMillis: Long? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_broadcast_notification",
                buildJsonObject {
                    put("p_target_role", JsonPrimitive(targetRole))
                    put("p_title", JsonPrimitive(title))
                    put("p_message", JsonPrimitive(message))
                    put("p_target_type", JsonPrimitive(targetType))
                    put("p_scheduled_for", scheduledForMillis?.let { JsonPrimitive(epochMillisToIsoUtc(it)) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `admin_notify_user` RPC wrapper -- একজন নির্দিষ্ট user-কে single notification পাঠায়।
     * এই ধাপে Kotlin-সাইড কোনো কল-সাইট থেকে wire করা হয়নি (SomadhanRepository-তে এখন পর্যন্ত
     * শুধু broadcast/all-role notification পাঠানোর ফাংশন আছে) -- future ব্যবহারের জন্য ready
     * রাখা হলো, ঠিক `admin_delete_notification_group`-এর মতোই এই session-এ লাইভ প্রজেক্টে
     * discover হওয়া RPC।
     */
    suspend fun adminNotifyUser(
        userId: String,
        title: String,
        message: String,
        targetType: String = "general",
        targetId: String? = null,
        relatedProblemId: String? = null,
        // [cloud-sync role গ্যাপ ফিক্স] লাইভ DB-তে `admin_notify_user`-এর ৭-প্যারামিটার
        // p_role-সহ overload আগে থেকেই আছে (`notifications.role` কলাম-সহ) -- এতদিন এই wrapper
        // শুধু ৬-প্যারামিটার সেট পাঠাত বলে PostgREST পুরনো role-বিহীন overload resolve করত।
        role: String = ""
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_notify_user",
                buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_title", JsonPrimitive(title))
                    put("p_message", JsonPrimitive(message))
                    put("p_target_type", JsonPrimitive(targetType))
                    put("p_target_id", targetId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_related_problem_id", relatedProblemId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `create_notification` RPC wrapper -- ধাপ ১২ notification-audit এ discover হওয়া একটা
     * জেনেরিক RPC (master prompt-এর তালিকায় ছিল না, লাইভ প্রজেক্টে আগে থেকেই ছিল, Supabase MCP
     * দিয়ে সোর্স পড়ে verify করা হয়েছে)। SECURITY DEFINER, `notifications` টেবিলে সরাসরি insert
     * করে। Authorization RPC নিজেই চেক করে: caller admin হলে যেকোনো target-এ পাঠাতে পারে, caller
     * নিজেকে notify করলে (self-notify) অনুমোদিত, অথবা `p_related_problem_id` দেওয়া থাকলে caller
     * ও target উভয়কেই সেই problem-এর party (owner/accepted_solver/bidder) হতে হবে -- নইলে RPC
     * `NOT_AUTHORIZED` exception ছোঁড়ে (তাই caller ভুল party-কে notify করার চেষ্টা করলে এই কল
     * ব্যর্থ হবে, সেটা প্রত্যাশিত)। `acceptBid`/`solverCancelJob`/`respondToAdditionalCharge`/
     * `adminSendMessageToProblemChat`-এর notification-গ্যাপ বন্ধ করতে এই ফাংশনটা এখন সবগুলো
     * কল-সাইটেই wire করা হয়েছে (SomadhanRepository.kt-তে দেখুন `// [SUPABASE-MIGRATED - ধাপ ১২
     * ফিক্স]` কমেন্টগুলো)।
     */
    suspend fun createNotification(
        targetUserId: String,
        title: String,
        message: String,
        targetType: String,
        targetId: String? = null,
        relatedProblemId: String? = null,
        // [cloud-sync role গ্যাপ ফিক্স] লাইভ DB-তে `create_notification`-এর ৭-প্যারামিটার
        // p_role-সহ overload আগে থেকেই আছে -- আগে এই wrapper পুরনো ৬-প্যারামিটার (role-বিহীন)
        // overload-এ resolve হতো, তাই cloud row সবসময় role="" পেত। কল-সাইটে (SomadhanRepository.kt)
        // পাশের local NotificationEntity.role থেকেই মান পাস করতে হবে।
        role: String = ""
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "create_notification",
                buildJsonObject {
                    put("p_target_user_id", JsonPrimitive(targetUserId))
                    put("p_title", JsonPrimitive(title))
                    put("p_message", JsonPrimitive(message))
                    put("p_target_type", JsonPrimitive(targetType))
                    put("p_target_id", targetId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_related_problem_id", relatedProblemId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `system_notify_48hour_auto_release` RPC wrapper -- ধাপ ২৭ (Notification RPC
     * Caller-Scoping ফিক্স)-এ বানানো একটা সংকীর্ণভাবে-scoped RPC। `create_notification`-এর
     * caller-check (caller admin/self/problem-party হতে হবে) শিথিল করা হয়নি -- তাহলে অন্য কেউ
     * arbitrary notification পাঠাতে পারার মতো নতুন নিরাপত্তা ঝুঁকি তৈরি হতো। এর বদলে এই আলাদা RPC
     * caller-এর identity না দেখে, বরং টার্গেট problem-টা আসলেই ৪৮-ঘণ্টা-পার-হওয়া auto-release
     * শর্ত পূরণ করে কিনা (status IN_PROGRESS/OPEN, accepted_solver_id আছে, has_release_request,
     * release_requested_at ৪৮ ঘণ্টার বেশি আগে, is_disputed না) তা সার্ভার-সাইডে নিজেই যাচাই করে,
     * আর টার্গেট ইউজারকে সেই problem-এর owner/accepted-solver হতেই হয় (নাহলে `NOT_AUTHORIZED`)।
     * ফলে caller যে-ই হোক (app startup/pull-to-refresh-এ যেকোনো logged-in user), শুধু এই একটা
     * নির্দিষ্ট, business-rule-bound ক্ষেত্রেই notification পাঠাতে পারবে।
     * `checkAndProcess48HourAutoReleases()`-এ ব্যবহৃত (SomadhanRepository.kt, `// [SUPABASE-MIGRATED
     * - ধাপ ২৭]` কমেন্ট দেখুন)।
     */
    suspend fun systemNotify48HourAutoRelease(
        problemId: String,
        targetUserId: String,
        title: String,
        message: String,
        targetType: String = "problem",
        targetId: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "system_notify_48hour_auto_release",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_target_user_id", JsonPrimitive(targetUserId))
                    put("p_title", JsonPrimitive(title))
                    put("p_message", JsonPrimitive(message))
                    put("p_target_type", JsonPrimitive(targetType))
                    put("p_target_id", targetId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `notify_admins` RPC wrapper -- ধাপ ১২ notification-audit এ discover হওয়া আরেকটা জেনেরিক RPC
     * (একই session/উৎস হিসেবে `create_notification`-এর মতো)। SECURITY DEFINER, `role='ADMIN'`
     * সব user-কে notification পাঠায়। caller-এর উপর কোনো admin-check নেই (শুধু auth থাকলেই
     * চলে) -- কারণ normal user/solver session থেকেও (যেমন `requestAdminAssistance`-জাতীয় ফ্লো)
     * অ্যাডমিনদের সতর্ক করার দরকার পড়ে। এখনো কোনো কল-সাইট থেকে wire করা হয়নি।
     */
    suspend fun notifyAdmins(
        title: String,
        message: String,
        relatedProblemId: String? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "notify_admins",
                buildJsonObject {
                    put("p_title", JsonPrimitive(title))
                    put("p_message", JsonPrimitive(message))
                    put("p_related_problem_id", relatedProblemId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `admin_delete_notification_group` RPC wrapper -- title মিলিয়ে (আর ঐচ্ছিকভাবে
     * scheduledFor/timestamp মিলিয়ে) notification batch delete করে। `deleteScheduledNotification`/
     * `deleteNotificationGroup` (SomadhanRepository) দুটোই এটা ব্যবহার করে, exactly কোন প্যারামিটার
     * (scheduledFor বনাম timestamp) পাঠাতে হবে সেটা caller ঠিক করে -- RPC নিজে p_scheduled_for
     * থাকলে সেটা প্রাধান্য দেয়, না থাকলে p_timestamp, দুটোই null হলে শুধু title দিয়ে মিলিয়ে delete
     * করে (caller-রা সবসময় দুটোর একটা পাঠায়, তাই এই ফলব্যাক কখনো hit হওয়ার কথা না)।
     */
    suspend fun adminDeleteNotificationGroup(
        title: String,
        scheduledForMillis: Long? = null,
        timestampMillis: Long? = null
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_delete_notification_group",
                buildJsonObject {
                    put("p_title", JsonPrimitive(title))
                    put("p_scheduled_for", scheduledForMillis?.let { JsonPrimitive(epochMillisToIsoUtc(it)) } ?: JsonPrimitive(null as String?))
                    put("p_timestamp", timestampMillis?.let { JsonPrimitive(epochMillisToIsoUtc(it)) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ১২ (মূল কাজ) — Admin audit log
    // ============================================================

    /**
     * `log_admin_action` RPC wrapper। নাম সত্ত্বেও এই RPC শুধু `auth.uid() is not null` চেক করে,
     * `is_admin()` না -- কারণ `SomadhanRepository.logAdminAction()` শুধু admin panel থেকে না,
     * যেকোনো normal user/solver সেশন থেকেও (যেমন `reconcileEscrowStates()`-এর self-heal alert)
     * ট্রিগার হতে পারে, আর সেই log admin-এর নিজের ডিভাইসে পৌঁছানো দরকার। তাই guard শুধু "session
     * আছে কিনা" -- `is_admin` না।
     */
    suspend fun logAdminAction(
        actionType: String,
        targetId: String,
        targetName: String,
        details: String,
        // [cloud-sync role গ্যাপ ফিক্স] লাইভ DB-তে `log_admin_action`-এর ৫-প্যারামিটার
        // p_role-সহ overload আগে থেকেই আছে (`admin_audit_logs.role` কলাম-সহ)।
        role: String = ""
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "log_admin_action",
                buildJsonObject {
                    put("p_action_type", JsonPrimitive(actionType))
                    put("p_target_id", JsonPrimitive(targetId))
                    put("p_target_name", JsonPrimitive(targetName))
                    put("p_details", JsonPrimitive(details))
                    put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Admin Credentials (secure, row-restricted) — ধাপ ১৪ ফলো-আপ
    // ============================================================
    // [SUPABASE-MIGRATED - ধাপ ১৪ ফলো-আপ] নতুন `admin_credentials` টেবিলে কোনো RLS policy নেই
    // (default-deny) — password hash কখনো client-এ ফেরত আসে না, শুধু এই ৩টা SECURITY DEFINER
    // RPC দিয়েই access হয়। `platform_settings` (পুরোপুরি public-readable) থেকে সম্পূর্ণ আলাদা
    // এই টেবিল, যাতে admin phone (non-sensitive, platform_settings-এ dual-write অক্ষত আছে)
    // আর password hash (sensitive) কখনো একই জায়গায় মিশে না যায়।

    suspend fun getAdminPhoneSecure(): Result<String?> {
        return try {
            val phone = client.postgrest.rpc("admin_credentials_get_phone")
                .decodeAs<String?>()
            Result.success(phone)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAdminPasswordSecure(rawPassword: String): Result<Boolean> {
        return try {
            val ok = client.postgrest.rpc(
                "admin_credentials_verify_password",
                buildJsonObject { put("p_password", JsonPrimitive(rawPassword)) }
            ).decodeAs<Boolean>()
            Result.success(ok)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [p_currentPassword] বাধ্যতামূলক -- real Supabase Auth admin session না থাকায় (আলাদাভাবে
     * flag করা, ব্যবহারকারীর সিদ্ধান্তের অপেক্ষায়) `auth.uid()`-ভিত্তিক গেট করা সম্ভব না, তাই
     * সার্ভার-সাইডে current-password পুনঃযাচাইটাই এই RPC-র authorization boundary। রিটার্ন
     * `false` মানে হয় current password ভুল, নাহলে RPC কল ব্যর্থ হয়েছে (দুটোই আলাদা করে হ্যান্ডল
     * করা উচিত caller-এ)।
     */
    suspend fun updateAdminCredentialsSecure(
        currentPassword: String,
        newPhone: String?,
        newPasswordHash: String?
    ): Result<Boolean> {
        return try {
            val ok = client.postgrest.rpc(
                "admin_credentials_update",
                buildJsonObject {
                    put("p_current_password", JsonPrimitive(currentPassword))
                    put("p_new_phone", newPhone?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_new_password_hash", newPasswordHash?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<Boolean>()
            Result.success(ok)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Multi-admin login/session RPCs — ADMIN_ROLE_PROFILE সেশন ২
    // ============================================================
    // সেশন ১-এর `admin_me`/`admin_session_start`/`admin_heartbeat`/`admin_session_end` + সেশন ২-এর
    // pre-auth `admin_login_check`। প্রথম চেকটা বাদে বাকি সব real Supabase Auth সেশন (auth.uid()) চায় —
    // সার্ভারই জানে কে কল করছে, ক্লায়েন্ট কোনো admin-id পাঠায় না। এরর মেসেজে সার্ভারের raise-করা কোড
    // (NOT_AN_ADMIN / ACCOUNT_INACTIVE / AUTH_REQUIRED) থাকে — caller `message.contains(...)` দিয়ে ধরে।

    /** লগইনের আগে (anon): এই ফোনটা কি কোনো এডমিন অ্যাকাউন্টের? true হলে এডমিন-লগইন পথ, নাহলে সাধারণ পথ। */
    suspend fun adminLoginCheck(phone: String): Result<Boolean> {
        return try {
            val ok = client.postgrest.rpc(
                "admin_login_check",
                buildJsonObject { put("p_phone", JsonPrimitive(phone)) }
            ).decodeAs<Boolean>()
            Result.success(ok)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** নতুন এডমিন-সেশন খোলে (last_login আপডেট + ADMIN_LOGIN অডিট লগ)। ফেরত: (sessionId, account-JSON)। */
    suspend fun adminSessionStart(device: String): Result<Pair<String?, JsonObject>> {
        return try {
            val el = client.postgrest.rpc(
                "admin_session_start",
                buildJsonObject {
                    put("p_device", JsonPrimitive(device))
                    put("p_ip", JsonPrimitive(""))
                    put("p_location", JsonPrimitive(""))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject ?: return Result.failure(IllegalStateException("admin_session_start: অপ্রত্যাশিত রেসপন্স"))
            val sid = (obj["session_id"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            val acct = obj["account"] as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_session_start: account নেই"))
            Result.success(sid to acct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ~৩০সে পরপর: last_seen আপডেট + সর্বশেষ অ্যাকাউন্ট-মান (active/flagged/রোল/পারমিশন) ফেরত। */
    suspend fun adminHeartbeat(sessionId: String?): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_heartbeat",
                buildJsonObject {
                    put("p_session_id", sessionId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject ?: return Result.failure(IllegalStateException("admin_heartbeat: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** লগআউটে সেশন বন্ধ করে (sessionId null হলে নিজের সব খোলা সেশন)। */
    suspend fun adminSessionEnd(sessionId: String?): Result<Boolean> {
        return try {
            val ok = client.postgrest.rpc(
                "admin_session_end",
                buildJsonObject {
                    put("p_session_id", sessionId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<Boolean>()
            Result.success(ok)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Role management RPCs — ADMIN_ROLE_PROFILE সেশন ৩ (অংশ ২.১, নতুন — এই সেশনে প্রথমবার)
    // ============================================================
    // `admin_roles_list`/`admin_role_upsert`/`admin_role_delete` (সেশন ১-এ migration-এ তৈরি, এই
    // ফাইলে আগে কল-সাইট ছিল না)। সবগুলো সুপার-অনলি — RPC নিজেই ভেতরে `_admin_require_super()`
    // দিয়ে গার্ড করে (caller কোনো is_super চেক নিজে থেকে করে না)। ব্যর্থ হলে সার্ভারের raise-করা
    // কোড exception message-এ থাকে (যেমন ROLE_NAME_REQUIRED/ROLE_NAME_TAKEN/ROLE_NOT_FOUND/
    // SUPER_ROLE_IMMUTABLE/ROLE_IN_USE) — caller `message?.contains(...)` দিয়ে বাংলা বার্তায়
    // ম্যাপ করবে (দেখুন `AdminRoleManagementView.kt`-এর `roleErrorMessage`)।

    /** সব রোলের তালিকা (super সহ), প্রতিটায় account_count। সুপার-অনলি। */
    suspend fun adminRolesList(): Result<List<JsonObject>> {
        return try {
            val el = client.postgrest.rpc("admin_roles_list").decodeAs<JsonElement>()
            val arr = el as? JsonArray
                ?: return Result.failure(IllegalStateException("admin_roles_list: অপ্রত্যাশিত রেসপন্স"))
            Result.success(arr.mapNotNull { it as? JsonObject })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** নতুন রোল তৈরি ([id] null) অথবা বিদ্যমান রোল আপডেট। সুপার রোল (`is_super`) এডিট করা যায় না — সার্ভার ব্লক করে। */
    suspend fun adminRoleUpsert(id: String?, name: String, permissions: Set<String>): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_role_upsert",
                buildJsonObject {
                    put("p_id", id?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_name", JsonPrimitive(name))
                    put("p_permissions", JsonArray(permissions.map { JsonPrimitive(it) }))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_role_upsert: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** রোল ডিলিট। সুপার রোল বা কোনো অ্যাকাউন্টে অ্যাসাইনড রোল ডিলিট করা যায় না — সার্ভার ব্লক করে (ROLE_IN_USE)। */
    suspend fun adminRoleDelete(id: String): Result<Boolean> {
        return try {
            val ok = client.postgrest.rpc(
                "admin_role_delete",
                buildJsonObject { put("p_id", JsonPrimitive(id)) }
            ).decodeAs<Boolean>()
            Result.success(ok)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Admin account RPCs — ADMIN_ROLE_PROFILE সেশন ৪ (এডমিন অ্যাকাউন্ট ম্যানেজমেন্ট, সুপার-অনলি)
    // ============================================================
    // `admin_accounts_list`/`admin_account_set_role`/`admin_account_set_active`/`admin_account_set_flagged`
    // সেশন ১-এ, `admin_account_create`/`admin_account_reset_password` সেশন ৪-এ (migration
    // `zz_20260924170000_admin_account_create_session4.sql`) তৈরি। সবগুলো সুপার-অনলি — RPC নিজেই ভেতরে
    // `_admin_require_super()` দিয়ে গার্ড করে। ব্যর্থ হলে সার্ভারের raise-করা কোড exception message-এ থাকে
    // (যেমন ADMIN_PHONE_TAKEN/PHONE_IN_USE_BY_USER/INVALID_PASSWORD/SUPER_ADMIN_IMMUTABLE/CANNOT_CHANGE_SELF) —
    // caller `message?.contains(...)` দিয়ে বাংলা বার্তায় ম্যাপ করবে (`AdminAccountsView.kt`-এর `accountErrorMessage`)।
    // ⚠️ পাসওয়ার্ড শুধু RPC-র প্যারামিটারে যায় (HTTPS) — কোথাও লগ/সংরক্ষণ করা হয় না।

    /** সব এডমিন অ্যাকাউন্টের তালিকা (রোল-নাম, পারমিশন, active/flagged/অনলাইনসহ)। সুপার-অনলি। */
    suspend fun adminAccountsList(): Result<List<JsonObject>> {
        return try {
            val el = client.postgrest.rpc("admin_accounts_list").decodeAs<JsonElement>()
            val arr = el as? JsonArray
                ?: return Result.failure(IllegalStateException("admin_accounts_list: অপ্রত্যাশিত রেসপন্স"))
            Result.success(arr.mapNotNull { it as? JsonObject })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগের এক পেজ (সব এডমিনের, ক্লাউড থেকে)। সুপার-অনলি — সার্ভার
     * `_admin_require_super()` দিয়ে আটকায়। ফিল্টার + পেজিনেশন দুটোই সার্ভারে (পেজ ছোট, ফিল্টার-সহ সঠিক)।
     * কার্সর = ([beforeTimestamp], [beforeId]) — আগের পেজের শেষ সারির মান, timestamp সার্ভারের ISO স্ট্রিং হুবহু
     * (মাইক্রোসেকেন্ড অক্ষত রাখতে; দেখো `AdminActivityLogEntry`-র ডক)। প্রথম পেজে দুটোই null।
     */
    suspend fun adminActivityLogsList(
        adminId: String?,
        nameQuery: String,
        unattributedOnly: Boolean,
        beforeTimestamp: String?,
        beforeId: String?,
        limit: Int
    ): Result<AdminActivityLogPage> {
        return try {
            val el = client.postgrest.rpc(
                "admin_activity_logs_list",
                buildJsonObject {
                    put("p_admin_id", adminId?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_name_query", JsonPrimitive(nameQuery))
                    put("p_unattributed_only", JsonPrimitive(unattributedOnly))
                    put("p_before", beforeTimestamp?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_before_id", beforeId?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_limit", JsonPrimitive(limit))
                }
            ).decodeAs<JsonElement>()
            val page = AdminActivityLogPage.fromJson(el)
                ?: return Result.failure(IllegalStateException("admin_activity_logs_list: অপ্রত্যাশিত রেসপন্স"))
            Result.success(page)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [ADMIN_ROLE_PROFILE সেশন ৬] প্রোফাইল আপডেট (`admin_profile_update`)। [targetId] null = নিজের; অন্যের হলে
     * সার্ভার সুপার-অনলি (`_admin_require_super`) আটকায়। ফোন/রোল এখান থেকে বদলায় না (সার্ভারেও নেই)।
     * সার্ভারে null প্যারামিটার = "অপরিবর্তিত", তাই: নাম/পদবি/ইমেইল/bio সবসময় পূর্ণ মান পাঠানো হয়
     * (ইমেইল `""` = মুছে ফেলা); [photoUrl] null = ছবি অপরিবর্তিত, `""` = ছবি সরানো। ফেরত: হালনাগাদ অ্যাকাউন্ট JSON।
     */
    suspend fun adminProfileUpdate(
        targetId: String?,
        name: String,
        designation: String,
        email: String,
        bio: String,
        photoUrl: String?
    ): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_profile_update",
                buildJsonObject {
                    put("p_id", targetId?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_name", JsonPrimitive(name))
                    put("p_designation", JsonPrimitive(designation))
                    put("p_email", JsonPrimitive(email))
                    put("p_bio", JsonPrimitive(bio))
                    put("p_photo_url", photoUrl?.let { JsonPrimitive(it) } ?: JsonNull)
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_profile_update: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [ADMIN_ROLE_PROFILE সেশন ৬] একজন এডমিনের সাম্প্রতিক লগইন-সেশন (নতুন আগে)। `admin_sessions`-এর RLS:
     * নিজের সেশন সবাই দেখে, সুপার সবার — তাই আলাদা RPC ছাড়াই সরাসরি select (সার্ভার-সাইড ফিল্টার আর সীমা)।
     */
    suspend fun adminSessionsList(adminId: String, limit: Int): Result<List<AdminSessionRecord>> {
        return try {
            val rows = client.postgrest.from("admin_sessions")
                .select {
                    filter { eq("admin_id", adminId) }
                    order("logged_in_at", Order.DESCENDING)
                    range(0L, (limit.coerceAtLeast(1) - 1).toLong())
                }
                .decodeList<JsonObject>()
            Result.success(rows.mapNotNull { AdminSessionRecord.fromJson(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** নতুন এডমিন (Supabase Auth user + অ্যাকাউন্ট একসাথে, অ্যাটমিক)। ফেরত: নতুন অ্যাকাউন্টের JSON। */
    suspend fun adminAccountCreate(
        name: String,
        phone: String,
        password: String,
        roleId: String,
        designation: String,
        email: String?
    ): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_account_create",
                buildJsonObject {
                    put("p_name", JsonPrimitive(name))
                    put("p_phone", JsonPrimitive(phone))
                    put("p_password", JsonPrimitive(password))
                    put("p_role_id", JsonPrimitive(roleId))
                    put("p_designation", JsonPrimitive(designation))
                    put("p_email", email?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_account_create: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** এডমিনের রোল বদল। সুপার রোল অ্যাসাইন করা যায় না, সুপার অ্যাকাউন্টের রোলও বদলানো যায় না — সার্ভার ব্লক করে। */
    suspend fun adminAccountSetRole(id: String, roleId: String): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_account_set_role",
                buildJsonObject {
                    put("p_id", JsonPrimitive(id))
                    put("p_role_id", JsonPrimitive(roleId))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_account_set_role: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** সক্রিয়/নিষ্ক্রিয়। সুপার অ্যাকাউন্ট ও নিজেকে নিষ্ক্রিয় করা যায় না — সার্ভার ব্লক করে। [reason] অডিট লগের details-এ যায়। */
    suspend fun adminAccountSetActive(id: String, active: Boolean, reason: String): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_account_set_active",
                buildJsonObject {
                    put("p_id", JsonPrimitive(id))
                    put("p_active", JsonPrimitive(active))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_account_set_active: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ফ্ল্যাগ/আনফ্ল্যাগ (view অক্ষত, কোনো অ্যাকশন নয়)। সুপার অ্যাকাউন্ট ও নিজেকে ফ্ল্যাগ করা যায় না — সার্ভার ব্লক করে। */
    suspend fun adminAccountSetFlagged(id: String, flagged: Boolean, reason: String): Result<JsonObject> {
        return try {
            val el = client.postgrest.rpc(
                "admin_account_set_flagged",
                buildJsonObject {
                    put("p_id", JsonPrimitive(id))
                    put("p_flagged", JsonPrimitive(flagged))
                    put("p_reason", JsonPrimitive(reason))
                }
            ).decodeAs<JsonElement>()
            val obj = el as? JsonObject
                ?: return Result.failure(IllegalStateException("admin_account_set_flagged: অপ্রত্যাশিত রেসপন্স"))
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** অন্য এডমিনের (সুপার বাদে) পাসওয়ার্ড রিসেট; পুরনো পাসওয়ার্ডের সেশন বন্ধ হয়। */
    suspend fun adminAccountResetPassword(id: String, newPassword: String): Result<Boolean> {
        return try {
            val ok = client.postgrest.rpc(
                "admin_account_reset_password",
                buildJsonObject {
                    put("p_id", JsonPrimitive(id))
                    put("p_new_password", JsonPrimitive(newPassword))
                }
            ).decodeAs<Boolean>()
            Result.success(ok)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Generic Admin Explorer (ধাপ ১৯) — AdminSupabaseExplorerView-এর জন্য
    // ============================================================
    // [SUPABASE-MIGRATED - ধাপ ১৯] পুরনো AdminFirestoreExplorerView.kt কোনো নির্দিষ্ট collection-এ
    // বাঁধা ছিল না — যেকোনো Firestore collection browse/edit/delete করতে পারতো, কারণ Firestore
    // schemaless। Postgrest-এ সেই একই "generic" আচরণ পেতে হলে টেবিলের নাম runtime-এ String
    // parameter হিসেবে নিয়ে সাধারণ (untyped) [JsonObject] row হিসেবে ডিকোড করা হচ্ছে — উপরের
    // বাকি সব ফাংশনের মতো নির্দিষ্ট DTO টাইপে না বেঁধে, কারণ এই একটা explorer-ই একসাথে ১৭টা
    // টেবিলের বিপরীতে কাজ করবে।
    //
    // ⚠️ নোট (build-verify করা যায়নি, Gradle sync-এ রিভিউ করা উচিত): এই ফাইলের বাকি ফাংশনগুলো
    // (উপরে) শুধু `select { filter { ... } }` আর `rpc(...)` প্যাটার্ন ব্যবহার করে, যেগুলো আগের
    // ধাপগুলোতে ইতিমধ্যে এই কোডবেসে ব্যবহৃত ও নথিভুক্ত। নিচের ৪টা ফাংশনে supabase-kt Postgrest
    // এর `range()`/`order()` (select scope) এবং জেনেরিক `insert()`/`update()`/`delete()` (নন-DTO,
    // [JsonObject] দিয়ে) — এই কোডবেসে এই প্রথমবার ব্যবহৃত হচ্ছে, তাই আগের ফাংশনগুলোর মতো
    // "codebase-এ আগে থেকে verified প্যাটার্ন" না — supabase-kt (BOM 3.6.0) এর সরকারি ডকুমেন্টেশন
    // অনুযায়ী লেখা হয়েছে, কিন্তু Android Studio তে Gradle sync/build করে একবার আলাদাভাবে verify
    // করে নেওয়া উচিত।

    /**
     * যেকোনো টেবিল থেকে একটা page row ফেরত দেয় ([from]/[to] হলো ০-ইনডেক্সড, দুই প্রান্ত-ইনক্লুসিভ
     * PostgREST Range — Firestore-এর `limit()+startAfter()` এর সমতুল্য)। [orderByColumn] দিয়ে স্থিতিশীল
     * (deterministic) pagination নিশ্চিত করা হয় — নাহলে পেজ থেকে পেজে row-অর্ডার নড়াচড়া করলে
     * duplicate/skip হতে পারে।
     */
    suspend fun explorerFetchPage(
        table: String,
        orderByColumn: String,
        from: Long,
        to: Long
    ): Result<List<JsonObject>> {
        return try {
            val rows = client.postgrest.from(table)
                .select {
                    order(orderByColumn, Order.ASCENDING)
                    range(from, to)
                }
                .decodeList<JsonObject>()
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** [pkColumn]/[pkValue] দিয়ে একটা row-এর একটা field আপডেট করে ([updates]-এ শুধু বদলানো ফিল্ড থাকবে)। */
    suspend fun explorerUpdateRow(
        table: String,
        pkColumn: String,
        pkValue: String,
        updates: JsonObject
    ): Result<Unit> {
        return try {
            val result = client.postgrest.from(table).update(updates) {
                select()
                filter { eq(pkColumn, pkValue) }
            }
            requireAffectedRowOrThrow(result, "explorerUpdateRow(table=$table, $pkColumn=$pkValue)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** [pkColumn]/[pkValue] দিয়ে একটা row ডিলিট করে। */
    suspend fun explorerDeleteRow(
        table: String,
        pkColumn: String,
        pkValue: String
    ): Result<Unit> {
        return try {
            val result = client.postgrest.from(table).delete {
                select()
                filter { eq(pkColumn, pkValue) }
            }
            requireAffectedRowOrThrow(result, "explorerDeleteRow(table=$table, $pkColumn=$pkValue)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** [values]-এ দেওয়া কলাম/মান দিয়ে একটা নতুন row insert করে (bare RLS/DB constraint অনুযায়ী)। */
    suspend fun explorerInsertRow(
        table: String,
        values: JsonObject
    ): Result<Unit> {
        return try {
            client.postgrest.from(table).insert(values)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Admin: password reset (Edge Function) — [ধাপ ৩০]
    // ============================================================

    /**
     * [ধাপ ৩০] admin-এর পক্ষ থেকে অন্য কোনো ব্যবহারকারীর Supabase Auth পাসওয়ার্ড রিসেট করে —
     * `admin-reset-user-password` Edge Function কল করে (service_role key কখনো client-এ আসে না,
     * শুধু Edge Function-এর সার্ভার-সাইড env-এ থাকে)।
     *
     * এই Edge Function-টা এই ধাপের আগেই (একটা আলাদা/আনলগড সেশনে) লাইভ প্রজেক্টে deploy হয়ে
     * গিয়েছিল পাওয়া গেছে (`admin-reset-user-password`, ACTIVE, version 2) — কোড রিভিউ করে
     * নিশ্চিত হওয়া গেছে এটা ঠিক এই কাজটাই (caller-এর JWT দিয়ে identity + is_admin() RPC দিয়ে
     * server-side admin-check, তারপর service-role client দিয়ে auth.admin.updateUserById) সঠিকভাবে
     * করে — তাই নতুন করে এই ফাংশন redeploy না করে বিদ্যমানটাই ব্যবহার করা হলো। (এই সেশনে আলাদাভাবে
     * `admin-reset-password` নামেও একটা কার্যত-অভিন্ন ফাংশন ভুলবশত deploy হয়ে গিয়েছিল — সেটা এখন
     * ব্যবহৃত/wire করা হচ্ছে না, dead/unused artifact হিসেবে থেকে যাবে, MIGRATION_PROGRESS.md-এ
     * নথিভুক্ত করা আছে।)
     *
     * Request/response shape বিদ্যমান Edge Function-এর সোর্স থেকে হুবহু মিলিয়ে নেওয়া হয়েছে:
     * body `{ "target_user_id": ..., "new_password": ... }`, সফল হলে `{ "result": "OK" }`,
     * ব্যর্থ হলে HTTP non-2xx status-এ `{ "error": "<code>" }`।
     */
    suspend fun adminResetUserPasswordViaEdgeFunction(
        targetUserId: String,
        newPassword: String
    ): Result<String> {
        val accessToken = SupabaseAuthManager.currentAccessToken()
            ?: return Result.failure(IllegalStateException("NO_ACTIVE_SUPABASE_SESSION"))
        return try {
            val response = functionsHttpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/admin-reset-user-password") {
                header("Authorization", "Bearer $accessToken")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("target_user_id", JsonPrimitive(targetUserId))
                        put("new_password", JsonPrimitive(newPassword))
                    }.toString()
                )
            }
            val bodyText: String = response.body()
            val json = Json.parseToJsonElement(bodyText).let { it as? JsonObject }
            val result = json?.get("result")?.jsonPrimitive?.content
            val error = json?.get("error")?.jsonPrimitive?.content
            if (result == "OK") {
                Result.success("OK")
            } else {
                Result.failure(Exception(error ?: "UNKNOWN_EDGE_FUNCTION_ERROR"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // Admin: Financial Reconciliation tools — [ধাপ ৩২.৬]
    // ============================================================
    // ৪টা RPC-ই SECURITY DEFINER, ভেতরে is_admin(auth.uid()) চেক করে (non-admin কল করলে
    // NOT_AUTHORIZED exception ছুঁড়বে)। p_dry_run=true (default) হলে শুধু jsonb রিপোর্ট রিটার্ন
    // করে, কোনো ডেটা বদলায় না। p_dry_run=false হলে আসল UPDATE/INSERT চালায়।

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৬] `reconcileEscrowStates()`-এর সমতুল্য: HELD escrow স্ক্যান করে
     * status-mismatch (already-refunded/completed-but-unpaid/cancelled-but-unrefunded) ঠিক করে।
     * Kotlin-সাইড ফাংশনের মতোই প্রতি-রান সর্বোচ্চ ৫টা case-2/case-3 রিপেয়ার (max_repairs_per_run)।
     */
    suspend fun adminReconcileEscrowStates(dryRun: Boolean = true): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_reconcile_escrow_states",
                buildJsonObject { put("p_dry_run", JsonPrimitive(dryRun)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৬] `reconcileUserBalances()`-এর সমতুল্য: প্রতিটা ইউজারের
     * transaction ledger থেকে balance recompute করে stored balance-এর সাথে মিলিয়ে mismatch
     * রিপোর্ট করে (dryRun=true) বা সংশোধন করে (dryRun=false, BALANCE_RECONCILIATION transaction
     * তৈরিসহ)। শুধু shared/deprecated `users.balance` কলাম স্পর্শ করে (role-scoped
     * balance_user/balance_solver না) — ledger diff কোন role-এর তা নির্ভুলভাবে জানা সম্ভব না বলে,
     * ঠিক Kotlin-সাইড ফাংশনের বর্তমান আচরণের সাথেই মিলিয়ে।
     */
    suspend fun adminReconcileUserBalances(dryRun: Boolean = true): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_reconcile_user_balances",
                buildJsonObject { put("p_dry_run", JsonPrimitive(dryRun)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৬] `cleanupDuplicateRefunds()`-এর সমতুল্য: একই escrow-এর জন্য
     * একাধিক refund transaction পাওয়া গেলে canonical/earliest একটা রেখে বাকিগুলো মুছে ব্যালেন্স
     * অ্যাডজাস্ট করে। fallback-id escrow (`ESC_%`) থাকলে নিরাপত্তার জন্য skip করে (Kotlin-সাইডের
     * মতোই)।
     */
    suspend fun adminCleanupDuplicateRefunds(dryRun: Boolean = true): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_cleanup_duplicate_refunds",
                buildJsonObject { put("p_dry_run", JsonPrimitive(dryRun)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৬] `repairMissingRefunds()`-এর সমতুল্য: escrow status='REFUNDED'
     * কিন্তু কোনো REFUND transaction নেই এমন সব escrow খুঁজে বের করে — problem-এর state যাচাই করে
     * (CANCELLED/OPEN/dispute-resolved-in-user's-favor) সঠিক refund-scenario হলে মিসিং transaction
     * তৈরি করে ও ইউজারকে ক্রেডিট করে।
     */
    suspend fun adminRepairMissingRefunds(dryRun: Boolean = true): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_repair_missing_refunds",
                buildJsonObject { put("p_dry_run", JsonPrimitive(dryRun)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------- [ধাপ ৩২.৭] Escrow Increment + Gateway Payment Bookkeeping +
    // Free-Quota/Miss-Cycle Admin&Bulk RPC ----------------
    // ৬টা RPC-ই Supabase MCP দিয়ে ইতিমধ্যে লাইভ প্রজেক্টে apply ও verify করা হয়েছে (migration নাম:
    // step32_7_increment_escrow_extra_amount, step32_7_record_gateway_payment_log,
    // step32_7_admin_reset_free_job_quota(_fix), step32_7_admin_reset_miss_cycle,
    // step32_7_admin_bulk_reset_free_job_quota, step32_7_system_track_extra_payment_miss)।

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৭] `FirebaseSyncManager.incrementEscrowExtraAmount()`-এর
     * সমতুল্য: `addToEscrow()`-এর atomic increment (পুরো row ওভাররাইট না করে
     * `extra_amount = extra_amount + p_amount`)। RPC ভেতরে caller escrow-এর সাথে জড়িত
     * (user/solver/admin) কিনা চেক করে।
     */
    suspend fun incrementEscrowExtraAmount(escrowId: String, amount: Double): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "increment_escrow_extra_amount",
                buildJsonObject {
                    put("p_escrow_id", JsonPrimitive(escrowId))
                    put("p_amount", JsonPrimitive(amount))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৭] `FirebaseSyncManager.syncGatewayPayment()`-এর সমতুল্য:
     * `recordGatewayPayment()`-এর bookkeeping/audit-log টাইপ `gateway_payments` row। RPC-এ
     * `on conflict (id) do nothing` থাকায় idempotent — রিট্রাই হলেও ডুপ্লিকেট row হবে না।
     */
    suspend fun recordGatewayPaymentLog(
        id: String,
        gatewayTrxId: String,
        userId: String,
        userName: String,
        userPhone: String,
        amount: Double,
        gateway: String,
        purpose: String = "ESCROW_PAYMENT",
        problemId: String = "",
        problemTitle: String = "",
        status: String = "SUCCESS",
        note: String = "",
        role: String = "USER"
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "record_gateway_payment_log",
                buildJsonObject {
                    put("p_id", JsonPrimitive(id))
                    put("p_gateway_trx_id", JsonPrimitive(gatewayTrxId))
                    put("p_user_id", JsonPrimitive(userId))
                    put("p_user_name", JsonPrimitive(userName))
                    put("p_user_phone", JsonPrimitive(userPhone))
                    put("p_amount", JsonPrimitive(amount))
                    put("p_gateway", JsonPrimitive(gateway))
                    put("p_purpose", JsonPrimitive(purpose))
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_problem_title", JsonPrimitive(problemTitle))
                    put("p_status", JsonPrimitive(status))
                    put("p_note", JsonPrimitive(note))
                    put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৭] `adminResetSolverFreeQuota(solverId)`-এর সমতুল্য — admin
     * অন্য একজন solver-এর মাসিক ফ্রি-কোটা কাউন্টার রিসেট করে। RPC ভেতরে `is_admin(auth.uid())`
     * চেক করে।
     */
    suspend fun adminResetFreeJobQuota(solverId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_reset_free_job_quota",
                buildJsonObject { put("p_solver_id", JsonPrimitive(solverId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৭] `runMonthlyFreeQuotaReset()`-এর সমতুল্য — cron/admin-triggered
     * bulk reset, সব ইউজারের ওপর client-side loop না করে একটা কলেই সবার ফ্রি-কোটা রিসেট করে।
     */
    suspend fun adminBulkResetFreeJobQuota(monthKey: String? = null): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_bulk_reset_free_job_quota",
                buildJsonObject { put("p_month_key", monthKey?.let { JsonPrimitive(it) } ?: JsonNull) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৭] `adminResetSolverMissCycle(solverId)`-এর সমতুল্য — admin
     * অন্য একজন solver-এর Extra Bill মিস-সাইকেল কাউন্টার (cycleJobCount/cycleMissCount) রিসেট
     * করে। RPC ভেতরে `is_admin(auth.uid())` চেক করে।
     */
    suspend fun adminResetMissCycle(solverId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "admin_reset_miss_cycle",
                buildJsonObject { put("p_solver_id", JsonPrimitive(solverId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ৩২.৭] `trackExtraPaymentMissCycle(solverId, ...)`-এর সমতুল্য —
     * সিস্টেম-ট্রিগার্ড bookkeeping-only sync (caller solver নিজে নাও হতে পারে, job-completion
     * flow problem-owner-এর ডিভাইস থেকেও ট্রিগার হতে পারে)। RPC caller==solver ম্যাচ করে না,
     * শুধু authenticated + solver row বাস্তব ও active কিনা যাচাই করে (নিয়ম #১১,
     * `system_notify_48hour_auto_release`-এর caller-scoping প্যাটার্ন অনুসরণ করে)।
     */
    suspend fun systemTrackExtraPaymentMiss(
        solverId: String,
        newJobCount: Int,
        newMissCount: Int
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "system_track_extra_payment_miss",
                buildJsonObject {
                    put("p_solver_id", JsonPrimitive(solverId))
                    put("p_new_job_count", JsonPrimitive(newJobCount))
                    put("p_new_miss_count", JsonPrimitive(newMissCount))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ধাপ ৩২.৮৫ (৩২.৮/৩২.৯-এর মাঝে, আগের session-এর অডিটে চিহ্নিত "১৪-১৫ নম্বর" গ্যাপ) ---
    // ⚠️ এই ৫টা RPC-র জন্য এই session-এ Supabase MCP দিয়ে লাইভ RLS re-verify করা যায়নি
    // (MCP কানেক্টেড ছিল না) — MIGRATION_PROGRESS.md-এ নথিভুক্ত আগের schema/RLS তথ্যের ওপর
    // ভিত্তি করে RPC-ভিত্তিক প্যাটার্ন বেছে নেওয়া হয়েছে (step23-এর মতোই)। পরের session প্রথমে
    // এই RPC গুলো `pg_proc`/`get_advisors` দিয়ে re-verify করবে (rule #১১)।

    /** Actor = problem owner অথবা accepted solver (isUser দিয়ে নির্ধারিত)। RPC caller যাচাই করে। */
    suspend fun markDisputeResultSeen(problemId: String, isUser: Boolean): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_dispute_result_seen",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_is_user", JsonPrimitive(isUser))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actor = problem owner অথবা accepted solver (isUser দিয়ে নির্ধারিত)। RPC caller যাচাই করে। */
    suspend fun markCompletionResultSeen(problemId: String, isUser: Boolean): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_completion_result_seen",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_is_user", JsonPrimitive(isUser))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actor = problem owner অথবা accepted solver (role দিয়ে নির্ধারিত)। RPC caller যাচাই করে।
     * "role" আসে caller-এর বর্তমান UI role থেকে (Kotlin-সাইড, "SOLVER" বা "USER"), spoof হলেও
     * RPC-এর ভেতরের auth.uid() চেকই আসল নিরাপত্তা — role শুধু কোন কলাম লেখা হবে তা বাছাই করে।
     */
    suspend fun markProblemSeen(problemId: String, role: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "mark_problem_seen",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_role", JsonPrimitive(role))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actor = problem owner (RPC-এর ভেতরেই auth.uid() দিয়ে যাচাই হয়, caller-এর পাঠানো ownerId
     * ব্যবহার হয় না -- spoof করা যাবে না)। RPC নিজেই escrow-locked-amount গার্ড আবার চেক করে
     * (client-সাইড guard-কে বিশ্বাস করে না, কারণ এটা টাকা-সংক্রান্ত reset)।
     * ফলাফল: "OK" | "NOT_ACCEPTED" | "NOT_ELIGIBLE" | "FUNDS_LOCKED"।
     */
    suspend fun ownerResetOrphanedAcceptedBid(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "owner_reset_orphaned_accepted_bid",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actor = problem owner (auth.uid()-ভিত্তিক, spoof করা যাবে না)। RPC problem soft-delete
     * করে (is_user_deleted=true, status=CANCELLED) আর সেই পোস্টের সব বিড CANCELLED করে দেয়,
     * একই transaction-এ (bids টেবিলে owner-scoped UPDATE policy নেই বলে, bids-এর অন্য সব
     * status-পরিবর্তনের মতোই RPC-ভিত্তিক)। নোট: bid-এর `progress_at_cancel` কলাম এই RPC সেট
     * করে না (Kotlin-সাইড `calculateProgressStep()`-এর মতো recompute এখানে করা হয়নি -- একটা
     * ছোট, নথিভুক্ত display-only বিচ্যুতি, পরের session চাইলে p_progress_step প্যারামিটার
     * যোগ করে ঠিক করতে পারে)।
     */
    suspend fun userDeleteProblem(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "user_delete_problem",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ৩২.৯ — Clear-All-Reset + Solver Cloud Search + Corrupted-Commission Cleanup
    // ============================================================

    /**
     * `FirebaseSyncManager.clearAllCloudData()`-এর সমতুল্য -- নতুন RPC `admin_wipe_all_data()`
     * (SECURITY DEFINER, is_admin() চেক)। Firebase-এর প্যারিটি অনুসরণ করে: users/problems/bids/
     * categories/faqs/withdrawals/transactions/escrows/ratings/reputation_events/messages/
     * notifications/platform_settings/additional_charges/admin_audit_logs সব টেবিলের row মোছে,
     * কিন্তু `auth.users`/`auth.identities` টাচ করে না (Firebase Auth account যেমন persist করে,
     * Supabase Auth account-ও তাই করে -- শুধু profile ডেটা মোছে)। `admin_credentials`/
     * `idempotency_keys` ইচ্ছাকৃতভাবে বাদ (এগুলোর Firebase-সমতুল্য কোনো collection নেই)।
     */
    suspend fun adminWipeAllData(): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc("admin_wipe_all_data").decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `cleanupCorruptedCommissionRates()`-এর জন্য -- `problems_update_admin` RLS policy
     * (cmd=UPDATE, qual/with_check `is_admin(auth.uid())`, কোনো column-level restriction নেই)
     * আগে থেকেই টেবিল-লেভেলে admin write কভার করে (`adminSoftDeleteProblem()`-এর মতোই), তাই
     * নতুন RPC ছাড়াই সরাসরি `applied_commission_rate` কলাম আপডেট যথেষ্ট।
     *
     * [Step 12.8c] outbox-replay গার্ড: filter-এ `status = 'OPEN'` ও `accepted_solver_id is null`
     * যোগ হলো (Kotlin-সাইড `cleanupCorruptedCommissionRates()`-এর ঠিক একই দুটো শর্তের সাথে মিলিয়ে) --
     * replay-এর সময় ততক্ষণে কোনো সলভার accept করে ফেললে (status/acceptedSolverId বদলে গেছে) এই
     * আপডেট আর কোনো row ম্যাচ করবে না, তাই বৈধ (এখন প্রাসঙ্গিক নয় এমন) commission rate ভুলবশত
     * null করে দেওয়ার ঝুঁকি নেই।
     *
     * এই কারণেই ০-row ম্যাচ এখানে একটা সম্পূর্ণ **স্বাভাবিক/বেনাইন** ফলাফল (RLS bug না, একটা বৈধ
     * race) -- তাই এই ফাংশন `requireAffectedRowOrThrow()`-এর সাধারণ "০ row = ছুঁড়ে দাও" আচরণ অনুসরণ
     * করে না (করলে outbox worker `MAX_RETRY_COUNT`-এ না পৌঁছানো পর্যন্ত বৃথা retry করতেই থাকবে,
     * শেষে `FAILED_PERMANENT`)। বদলে ০ row হলে সরাসরি `Result.success(Unit)` রিটার্ন হয়, log-এ শুধু
     * একটা নোট থাকে -- এই decision **শুধু এই ফাংশনের জন্য**, বাকি সব RPC/update এখনো
     * `requireAffectedRowOrThrow()`-এর কড়া guard-ই ব্যবহার করে।
     */
    suspend fun adminUpdateProblemCommissionRate(problemId: String, rate: Double?): Result<Unit> {
        return try {
            val result = client.postgrest.from("problems").update(
                buildJsonObject {
                    put("applied_commission_rate", rate?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as Double?))
                }
            ) {
                select()
                filter {
                    eq("id", problemId)
                    eq("status", "OPEN")
                    exact("accepted_solver_id", null)
                }
            }
            val affected = result.decodeList<JsonObject>()
            if (affected.isEmpty()) {
                Log.w(
                    "SupabaseSyncManager",
                    "adminUpdateProblemCommissionRate(problemId=$problemId): 0 rows matched " +
                        "(problem no longer OPEN/unassigned -- benign race, not retried further)"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ধাপ ৩২.৯৫ (ব্যবহারকারীর অনুরোধে, ৩৩ শুরুর আগে) — sendSystemEventMessage() +
    // clearSolverCancelledNotice() + acceptInstantJobBid()-এর শেষ field-reset গ্যাপ ফিক্স
    // ============================================================

    /**
     * `sendSystemEventMessage()`-এর সমতুল্য — নতুন RPC `system_event_message()`
     * (SECURITY DEFINER)। `messages_insert` RLS policy `auth.uid() = sender_id` দাবি করে বলে
     * সিস্টেম-জেনারেটেড মেসেজ (sender_id কোনো real uuid না) সরাসরি insert দিয়ে সম্ভব না —
     * RPC নিজে `sender_id = null` রেখে `is_system_event=true`/`system_event_type` দিয়ে insert
     * করে + problem-এর `last_activity_at` আপডেট করে। Guard: caller admin, অথবা owner, অথবা
     * accepted_solver হতে হবে (এই ফাংশনের ৭টা call-site যাচাই করে এই তিনটাই পাওয়া গেছে)।
     */
    suspend fun systemEventMessage(
        problemId: String,
        receiverId: String?,
        eventType: String,
        content: String,
        senderName: String = "সিস্টেম"
    ): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "system_event_message",
                buildJsonObject {
                    put("p_problem_id", JsonPrimitive(problemId))
                    put("p_receiver_id", receiverId?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                    put("p_event_type", JsonPrimitive(eventType))
                    put("p_content", JsonPrimitive(content))
                    put("p_sender_name", JsonPrimitive(senderName))
                }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `clearSolverCancelledNotice()`-এর সমতুল্য — নতুন RPC `clear_solver_cancelled_notice()`
     * (SECURITY DEFINER, caller must equal problem.user_id)। problem-এর accepted-বিড ফিল্ড
     * রিসেট করে + broadcast টাইমার নতুন করে শুরু করে, আর আগের accepted/cancelled সলভারদের বিড
     * strictly CANCELLED করে দেয় (Kotlin লজিকের সাথে হুবহু মিলিয়ে) — bids টেবিলে owner-scoped
     * UPDATE RLS policy নেই বলে (bids-এর সব status-পরিবর্তন RPC দিয়েই হয়), problem + bids একই
     * transaction-এ বদলানো হয়েছে (userDeleteProblem()-এর মতোই প্যাটার্ন)।
     */
    suspend fun clearSolverCancelledNotice(problemId: String): Result<JsonElement> {
        return try {
            val result = client.postgrest.rpc(
                "clear_solver_cancelled_notice",
                buildJsonObject { put("p_problem_id", JsonPrimitive(problemId)) }
            ).decodeAs<JsonElement>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * `acceptInstantJobBid()`-এর শেষের unconditional field-reset (jobStatus="ON_WAY" +
     * on-way/arrived/started/live-location/dispute/extra-amount ফিল্ড ক্লিয়ার) দুই পাশে
     * mirror করার জন্য — কোনো নতুন RPC লাগেনি, কারণ `problems_update_owner` RLS policy
     * (qual/with_check `auth.uid() = user_id`, কোনো column-level restriction নেই) ইতিমধ্যেই
     * owner-এর জন্য পুরো row আপডেট কভার করে (ঠিক `acceptBid()`-এর `accept_bid` RPC bridge-এর
     * caller-guard-এর মতোই, যেটা এই একই ফাংশনের শুরুতে কল হয়)। পুরো DTO পাঠানো হয় (partial না)
     * যাতে local `updated` ProblemEntity-এর সাথে হুবহু মিলে যায়, ঠিক `syncProblem()`-এর মতোই।
     */
    suspend fun updateProblemFull(problem: ProblemDto): Result<Unit> {
        return try {
            val result = client.postgrest.from("problems").update(problem) {
                select()
                filter { eq("id", problem.id) }
            }
            requireAffectedRowOrThrow(result, "updateProblemFull(problemId=${problem.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
