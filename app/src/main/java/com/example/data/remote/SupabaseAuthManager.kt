package com.example.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৩ (কাঠামো তৈরি) + ধাপ ১৪ (আসল wiring)
 *
 * ## ধাপ ১৪-এ নেওয়া সবচেয়ে গুরুত্বপূর্ণ architecture decision: OTP demo-ই থাকবে, কিন্তু real
 * session কীভাবে তৈরি হবে
 *
 * Global rule #3 অনুযায়ী Login/Register/Password-Reset OTP **এখনো demo/mock** (real SMS পাঠানো
 * হয় না, [com.example.util.OtpService] আগের মতোই কাজ করে) — কিন্তু user session/profile এখন
 * **real** Supabase Auth দিয়ে তৈরি হতে হবে, কারণ RLS এর সব policy `auth.uid()` এর উপর নির্ভরশীল
 * (ধাপ ৭-১২ এ verified)।
 *
 * সমস্যা: Supabase নিজে phone-এর জন্য built-in OTP-ভিত্তিক auth (`signInWith(OTP)` +
 * `verifyPhoneOtp`) সমর্থন করে, কিন্তু সেটা ব্যবহার করলে Supabase নিজেই real SMS পাঠানোর চেষ্টা
 * করবে (একটা SMS provider যেমন Twilio configure করা লাগবে) — যেটা rule #3 ভঙ্গ করে (OTP এখনই
 * "live" করা যাবে না)।
 *
 * **সিদ্ধান্ত (ব্যবহারকারীর সাথে আলোচনা করে): phone+password ভিত্তিক Supabase Auth ব্যবহার করা
 * হয়েছে** ([Phone] provider, কিন্তু OTP grant না — password grant), Supabase-এর নিজস্ব
 * phone-verification ধাপ সম্পূর্ণ বাইপাস করে:
 * - Register: [signUpWithPhonePassword] — phone + app-এ আগে থেকেই থাকা password field দিয়ে
 *   সরাসরি sign-up। কোনো SMS পাঠানো হয় না।
 * - Login: [signInWithPhonePassword] — phone + password verify করে, সফল হলে সরাসরি session
 *   তৈরি হয়ে যায়।
 * - Demo OTP flow (`OtpService.sendOtp`/`verifyOtp`) এই দুটো কলের **আগে বা পরে না, বরং সমান্তরালে
 *   (UI-level friction হিসেবে)** থেকে যায় — কারণ Supabase-এর password check নিজেই session
 *   তৈরি করে ফেলে, "শুধু verify করো কিন্তু session বানিও না" — এটা আলাদা করে সম্ভব না। তাই মূল
 *   প্রম্পটে বর্ণিত ধারা (OTP verify হওয়ার *পরে* session তৈরি) হুবহু অনুসরণ করা possible হয়নি —
 *   বাস্তবে `SomadhanViewModel.validateLoginCredentials()`-এ (OTP পাঠানোর আগে, credential-check
 *   ধাপে) session তৈরি হয়ে যায়, demo OTP তারপর UI-তে একটা অতিরিক্ত ধাপ হিসেবে থেকে যায়। এটা
 *   নিরাপত্তার দিক থেকে সমস্যা না (real credential ইতিমধ্যে verify হয়ে গেছে), শুধু "কবে ঠিক
 *   session তৈরি হয়" তার টাইমিং প্রম্পটের বর্ণনার থেকে ভিন্ন — এই trade-off টা
 *   `MIGRATION_PROGRESS.md`-এও নথিভুক্ত করা আছে।
 *
 * **⚠️ ডিপ্লয়মেন্ট-নির্ভরতা (আমি নিজে চেক/বদলাতে পারিনি, MCP SQL tool দিয়ে auth provider সেটিংস
 * অ্যাক্সেসযোগ্য না)**: এটা কাজ করার জন্য Supabase Dashboard → Authentication → Providers →
 * Phone-এ **"Confirm phone" (phone confirmation) নিষ্ক্রিয় (OFF)** থাকতে হবে। এটা ON থাকলে
 * Supabase নিজে থেকে sign-up-কে "unconfirmed" ধরে রাখবে আর real SMS ছাড়া sign-in সফল হবে না।
 * প্রথম real device-এ test করার আগে এই সেটিংসটা অবশ্যই যাচাই করে নেবেন।
 *
 * নোট: supabase-kt phone+password auth API এই ধাপেও build/compile করে যাচাই করা যায়নি (এই
 * session এ Gradle/network সুবিধা নেই) — method নাম/signature সর্বশেষ known supabase-kt
 * (BOM 3.6.0) auth-kt API অনুযায়ী লেখা হয়েছে, Android Studio তে Gradle sync/build করে ভবিষ্যতে
 * যাচাই করে নেওয়া উচিত (এটাই এই ফাইলের প্রথম real ব্যবহার, তাই compile ত্রুটি থাকলে এখানেই
 * প্রথম ধরা পড়বে)।
 */
object SupabaseAuthManager {

    private val client get() = SupabaseClientProvider.client

    /**
     * [Balance-reset race ফিক্স] `SomadhanViewModel.logout()` ইচ্ছাকৃতভাবে `SupabaseAuthManager
     * .signOut()`-কে fire-and-forget কোরুটিনে রাখে (যাতে অফলাইনে logout আটকে না যায় — দেখুন
     * ViewModel-এর ধাপ ১৪ কমেন্ট)। সমস্যা: দ্রুত অন্য account-এ login করলে ওই দেরি-হওয়া signOut()
     * পরে গিয়ে ফায়ার হয়ে **নতুন** session-টাই মুছে দিতে পারত — ফলে ঠিক তার পরপরই [currentUserId]
     * null/ভুল হয়ে যেত, আর সেটার উপর নির্ভরশীল money-critical guard (যেমন acceptBid()-এর dual-write
     * + outbox-enqueue block) নীরবে skip হয়ে যেত। এই [pendingSignOutJob] সেই fire-and-forget
     * Job-টা ট্র্যাক করে, আর [signInWithPhonePassword] শুরুতেই সেটা শেষ হওয়া পর্যন্ত (bounded
     * timeout সহ, যাতে কখনো hang না করে) অপেক্ষা করে — logout তবুও সাথে সাথেই local-এ সম্পন্ন হয়,
     * শুধু *পরের* login সামান্য (সাধারণত instant, network থাকলে) অপেক্ষা করে যাতে দুটো session
     * race না করে।
     */
    @Volatile
    private var pendingSignOutJob: Job? = null

    /** [Balance-reset race ফিক্স] logout()-এর fire-and-forget signOut Job রেজিস্টার করে। */
    fun trackPendingSignOut(job: Job) {
        pendingSignOutJob = job
    }

    /** [Balance-reset race ফিক্স] আগের কোনো signOut এখনো চলমান থাকলে (bounded wait), শেষ হওয়া পর্যন্ত অপেক্ষা করে। */
    private suspend fun awaitPendingSignOutIfAny() {
        val job = pendingSignOutJob ?: return
        if (job.isActive) {
            withTimeoutOrNull(5000) { job.join() }
        }
        pendingSignOutJob = null
    }

    /**
     * নতুন account তৈরি করে (phone + password, real SMS ছাড়াই — উপরের ক্লাস-লেভেল কমেন্ট দেখুন)।
     * সফল হলে `handle_new_auth_user` trigger (DB-তে আগে থেকেই আছে) `public.users`-এ একটা bare
     * row (id, name from metadata, phone, email=null) তৈরি করে দেয় — বাকি প্রোফাইল তথ্য পরে
     * [completeRegistrationProfile] দিয়ে বসাতে হবে (caller-এর দায়িত্ব, এই ফাংশনের ভেতরে না)।
     *
     * `name` এখানে user metadata হিসেবে পাঠানো হচ্ছে যাতে trigger এটা সরাসরি ব্যবহার করতে পারে —
     * এই প্যারামিটার trigger-এর `new.raw_user_meta_data->>'name'` reference-এর সাথে মিলিয়ে
     * (`handle_new_auth_user` সোর্স Supabase MCP দিয়ে পড়ে যাচাই করা হয়েছে)।
     */
    suspend fun signUpWithPhonePassword(phone: String, password: String, name: String): Result<Unit> {
        return try {
            client.auth.signUpWith(Phone) {
                this.phone = phone
                this.password = password
                data = buildJsonObject {
                    put("name", JsonPrimitive(name))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * বিদ্যমান account দিয়ে sign-in করে (phone + password)। সফল হলে Supabase Auth session তৈরি
     * হয়ে যায় ([currentUserId] দিয়ে পরে অ্যাক্সেসযোগ্য)। Supabase ভুল credential-এর জন্য একটা
     * generic error দেয় (নিরাপত্তার কারণে "phone নেই" বনাম "password ভুল" আলাদা করে বলে না) —
     * caller-কে এই generic error handle করতে হবে (দেখুন `SomadhanRepository.loginWithPhonePassword`)।
     */
    suspend fun signInWithPhonePassword(phone: String, password: String): Result<Unit> {
        // [Balance-reset race ফিক্স] আগের account-এর fire-and-forget signOut() তখনও চলমান থাকতে
        // পারে -- সেটা এই নতুন sign-in সম্পন্ন হওয়ার পরে ফায়ার হলে নতুন session মুছে যেত। নতুন
        // sign-in attempt শুরুর আগেই সেটা resolve হওয়া নিশ্চিত করা হলো।
        awaitPendingSignOutIfAny()
        return try {
            client.auth.signInWith(Phone) {
                this.phone = phone
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * সাইনআপের পর `complete_registration_profile` RPC কল করে বাকি প্রোফাইল তথ্য সেভ করে।
     * প্যারামিটার নাম/সংখ্যা Supabase MCP দিয়ে `pg_get_function_identity_arguments()` কুয়েরি
     * করে **সরাসরি ডাটাবেস থেকে verify করা** (অনুমান নয়): `p_name, p_address, p_latitude,
     * p_longitude, p_role, p_solver_categories, p_has_solver_role, p_has_user_role`।
     *
     * নোট: এই RPC-এর প্যারামিটারে `email` নেই (ধাপ ৩-এ যেভাবে verify হয়েছিল, ধাপ ১৪-এও আবার
     * সোর্স রিভিউ করে reconfirm করা হলো) — তাই email এই ফাংশনের বাইরে, registration flow-এর
     * পরের ধাপে, সরাসরি `users` টেবিলে update করে বসাতে হয় (দেখুন
     * `SupabaseSyncManager.updateOwnProfile(email = ...)`)।
     */
    suspend fun completeRegistrationProfile(
        name: String,
        address: String,
        latitude: Double?,
        longitude: Double?,
        role: String,
        solverCategories: String = "",
        hasSolverRole: Boolean,
        hasUserRole: Boolean
    ): Result<Unit> {
        return try {
            client.postgrest.rpc(
                function = "complete_registration_profile",
                parameters = buildJsonObject {
                    put("p_name", JsonPrimitive(name))
                    put("p_address", JsonPrimitive(address))
                    put("p_latitude", latitude?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_longitude", longitude?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("p_role", JsonPrimitive(role))
                    put("p_solver_categories", JsonPrimitive(solverCategories))
                    put("p_has_solver_role", JsonPrimitive(hasSolverRole))
                    put("p_has_user_role", JsonPrimitive(hasUserRole))
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** বর্তমান logged-in ব্যবহারকারীর Supabase Auth UUID (থাকলে) — repository layer user_id হিসেবে ব্যবহার করে। */
    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    /**
     * [ধাপ ৩০] বর্তমান সেশনের access token (JWT) — Edge Function কল করার সময় Authorization
     * header-এ পাঠানোর জন্য দরকার (Edge Function নিজে এই token যাচাই করে caller-এর পরিচয়/admin
     * status নিশ্চিত হয়)। Session না থাকলে null।
     */
    fun currentAccessToken(): String? = client.auth.currentAccessTokenOrNull()

    /**
     * বর্তমান Supabase Auth session-এর password বদলায় (Supabase Auth নিজেই password owner —
     * `public.users`-এ কোনো password column নেই)। Session না থাকলে ব্যর্থ হয় — caller (
     * `SomadhanRepository.updateUserPassword`) session-বিহীন (এখনো migrate-না-হওয়া/demo)
     * account-এর জন্য পুরনো local bcrypt পথে fallback করে।
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            client.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** সাইন-আউট করে বর্তমান Supabase Auth সেশন ক্লিয়ার করে। */
    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
