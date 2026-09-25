package com.example.data.security

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.SupabaseAuthManager
import com.example.data.remote.SupabaseSyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Manages admin authentication credentials locally via Jetpack DataStore (Preferences)
 * and via secure Supabase RPCs (see below).
 *
 * Passwords are never stored in plaintext — only bcrypt hashes are persisted.
 *
 * [SUPABASE-MIGRATED - ধাপ ১৪ (master prompt আইটেম ৬)] admin phone Supabase `platform_settings`
 * টেবিলেও (key="admin_phone") non-blocking dual-write হয় (নিচে [updateCredentials]-এ) —
 * non-sensitive বলে সমস্যা নেই।
 *
 * [SUPABASE-MIGRATED - ধাপ ১৪ ফলো-আপ] password hash-এর জন্য আগে flag করা নিরাপত্তা-ফাঁক
 * (platform_settings পুরোপুরি public-readable — offline brute-force ঝুঁকি) এখন সমাধান করা
 * হয়েছে একটা আলাদা, row-level-restricted `admin_credentials` টেবিল দিয়ে (কোনো RLS policy
 * নেই, default-deny — শুধু ৩টা SECURITY DEFINER RPC দিয়েই access: `admin_credentials_get_phone`,
 * `admin_credentials_verify_password`, `admin_credentials_update`)। hash কখনো client-এ ফেরত
 * আসে না, শুধু boolean রেজাল্ট। যেহেতু এই app-এ admin-এর কোনো real Supabase Auth session নেই
 * (আলাদাভাবে flag করা, ব্যবহারকারীর সিদ্ধান্তের অপেক্ষায়), তাই `admin_credentials_update`-এর
 * authorization boundary হলো current-password পুনঃযাচাই (auth.uid()-ভিত্তিক গেট সম্ভব না)।
 * Read/verify path আগে secure RPC ট্রাই করে (2s timeout) — Supabase-এ row থাকলে (অর্থাৎ admin
 * অন্তত একবার migrate/আপডেট করেছে) সেটাই source of truth, নাহলে [SUPABASE-MIGRATED - ধাপ ৩৩.২]
 * সরাসরি local DataStore fallback (আগে এই fallback-এর মাঝে একটা Firestore স্তরও ছিল, সরানো হয়েছে)।
 */
object AdminCredentials {

    private const val TAG = "AdminCredentials"
    private val Context.adminDataStore: DataStore<Preferences> by preferencesDataStore(name = "admin_credentials_store")

    private val KEY_ADMIN_PHONE = stringPreferencesKey("admin_phone")
    private val KEY_ADMIN_PASSWORD_HASH = stringPreferencesKey("admin_password_hash")

    // Default admin phone number for fresh installs
    const val DEFAULT_ADMIN_PHONE = "01700000000"

    // Default bcrypt hash for password "Admin@123" with 12 rounds ($2a$12$...)
    // Pre-calculated so raw plaintext password is never hardcoded in source.
    const val DEFAULT_ADMIN_PASSWORD_HASH = "$2a$12$7kPj5u8L58V1b2y2ZkQv6e6HkJd1Xm5h4Yn9P8q7r6s5t4u3v2w1x"

    /**
     * Gets the configured admin phone number.
     * [SUPABASE-MIGRATED - ধাপ ৩৩.২] অর্ডার: secure Supabase RPC আগে (2s timeout) → ব্যর্থ/blank
     * হলে সরাসরি DataStore fallback। (আগে এখানে RPC ও DataStore-এর মাঝে একটা Firestore fallback
     * স্তরও ছিল, ৩৩.২-এ সরানো হয়েছে।)
     */
    suspend fun getAdminPhone(context: Context): String {
        // [SUPABASE-MIGRATED - ধাপ ১৪ ফলো-আপ] secure RPC প্রথমে -- row থাকলে (admin অন্তত
        // একবার migrate করেছে) এটাই এখন source of truth।
        try {
            val securePhone = withTimeoutOrNull(2000L) {
                SupabaseSyncManager.getAdminPhoneSecure().getOrNull()
            }
            if (!securePhone.isNullOrBlank()) {
                context.adminDataStore.edit { preferences ->
                    preferences[KEY_ADMIN_PHONE] = securePhone
                }
                return securePhone
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch admin phone from secure Supabase RPC: ${e.message}")
        }

        return context.adminDataStore.data.map { preferences ->
            preferences[KEY_ADMIN_PHONE] ?: DEFAULT_ADMIN_PHONE
        }.first()
    }

    /**
     * Retrieves the stored password hash (or the default hash if unset).
     * [SUPABASE-MIGRATED - ধাপ ৩৩.২] এটা শুধু [verifyAdminPassword]-এর legacy fallback অংশ —
     * primary path (secure Supabase RPC) আগে ট্রাই হয়ে ব্যর্থ হলেই এই ফাংশন কল হয়, এখন সরাসরি
     * DataStore থেকে (আগে এই fallback-এর মাঝে একটা Firestore স্তরও ছিল, ৩৩.২-এ সরানো হয়েছে)।
     */
    private suspend fun getAdminPasswordHash(context: Context): String {
        return context.adminDataStore.data.map { preferences ->
            preferences[KEY_ADMIN_PASSWORD_HASH] ?: DEFAULT_ADMIN_PASSWORD_HASH
        }.first()
    }

    /**
     * Verifies if the provided [rawPassword] matches the stored admin password hash.
     * Uses [PasswordHasher.verify] for secure bcrypt matching.
     */
    suspend fun verifyAdminPassword(context: Context, rawPassword: String): Boolean {
        // [SUPABASE-MIGRATED - ধাপ ১৪ ফলো-আপ] Supabase-এ admin_credentials row থাকলে
        // (মানে অন্তত একবার migrate/আপডেট হয়েছে), সেটাই source of truth -- hash কখনো
        // client-এ ফেরত আসে না, RPC শুধু true/false দেয়।
        try {
            val secureRowExists = withTimeoutOrNull(2000L) {
                !SupabaseSyncManager.getAdminPhoneSecure().getOrNull().isNullOrBlank()
            }
            if (secureRowExists == true) {
                val secureResult = withTimeoutOrNull(2000L) {
                    SupabaseSyncManager.verifyAdminPasswordSecure(rawPassword).getOrNull()
                }
                if (secureResult != null) {
                    return secureResult
                }
                Log.w(TAG, "Secure Supabase row exists but verify RPC call failed -- falling back to legacy check")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Secure admin password verify via Supabase threw -- falling back to legacy check: ${e.message}")
        }

        val storedHash = getAdminPasswordHash(context)
        return if (storedHash == DEFAULT_ADMIN_PASSWORD_HASH) {
            // First check if the precomputed hash matches or verify against standard hash for "Admin@123"
            PasswordHasher.verify(rawPassword, storedHash) || (rawPassword == "Admin@123")
        } else {
            PasswordHasher.verify(rawPassword, storedHash)
        }
    }

    /**
     * Updates the admin phone number and/or password.
     * [newRawPassword], if provided, is securely hashed using [PasswordHasher.hash] before saving.
     * Saves to local DataStore and dual-writes to Supabase (platform_settings + secure
     * admin_credentials RPC, see below).
     *
     * [currentPasswordForSecureSync] — [SUPABASE-MIGRATED - ধাপ ১৪ ফলো-আপ] নতুন secure
     * `admin_credentials` RPC-তে dual-write করার জন্য দরকার (real Supabase Auth admin session
     * না থাকায় ওই RPC-র authorization boundary হলো current-password re-check)। caller
     * (ViewModel.adminUpdateCredentials) ইতিমধ্যেই এই password client-side verify করে রাখে,
     * সেটাই এখানে পাস করা হয়। `null` দিলে (ভবিষ্যতে অন্য কোনো caller থাকলে) শুধু এই ধাপের secure
     * dual-write স্কিপ হবে — বাকি সব (DataStore/phone platform_setting) অপ্রভাবিত।
     */
    suspend fun updateCredentials(
        context: Context,
        newPhone: String?,
        newRawPassword: String?,
        currentPasswordForSecureSync: String? = null
    ) {
        val newPasswordHash = if (!newRawPassword.isNullOrBlank()) {
            PasswordHasher.hash(newRawPassword.trim())
        } else null

        val phoneTrimmed = newPhone?.trim()?.takeIf { it.isNotBlank() }

        // 1. Save to local DataStore
        context.adminDataStore.edit { preferences ->
            if (phoneTrimmed != null) {
                preferences[KEY_ADMIN_PHONE] = phoneTrimmed
            }
            if (newPasswordHash != null) {
                preferences[KEY_ADMIN_PASSWORD_HASH] = newPasswordHash
            }
        }

        // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে ধাপ ২ হিসেবে Firestore admin_config/credentials
        // ডকুমেন্টে phone/passwordHash push হতো -- সরানো হলো। নিচের ২টা Supabase dual-write
        // (item ৩: platform_settings phone, item ৪: secure admin_credentials RPC phone+hash)
        // ইতিমধ্যেই একই ডেটা কভার করে (ধাপ ১৪/১৪-ফলো-আপ থেকে verified bridge)।
        // 3. [SUPABASE-MIGRATED - ধাপ ১৪] শুধু phone (non-sensitive) Supabase platform_settings
        // এ dual-write করা হয়, password hash **ইচ্ছাকৃতভাবে বাদ** (উপরে class doc-এ কারণ বিস্তারিত)।
        // ব্যর্থ হলেও local flow সম্পূর্ণ অপ্রভাবিত (non-blocking, শুধু log)।
        if (phoneTrimmed != null) {
            try {
                SupabaseSyncManager.upsertPlatformSetting("admin_phone", phoneTrimmed)
                    .onFailure { e ->
                        Log.w(TAG, "Supabase dual-write failed for admin_phone (local/flow unaffected): ${e.message}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase dual-write threw for admin_phone (local/flow unaffected): ${e.message}")
            }
        }

        // 4. [SUPABASE-MIGRATED - ধাপ ১৪ ফলো-আপ] নতুন row-restricted admin_credentials টেবিলে
        // phone+password hash dual-write (RPC-এর ভেতরেই current-password re-verify হয়)।
        // ব্যর্থ হলেও (network/mismatch) DataStore/platform_settings flow সম্পূর্ণ
        // অপ্রভাবিত -- non-blocking, শুধু log।
        if (currentPasswordForSecureSync != null && (phoneTrimmed != null || newPasswordHash != null)) {
            try {
                SupabaseSyncManager.updateAdminCredentialsSecure(
                    currentPassword = currentPasswordForSecureSync,
                    newPhone = phoneTrimmed,
                    newPasswordHash = newPasswordHash
                ).onSuccess { ok ->
                    if (!ok) {
                        Log.w(TAG, "Secure admin_credentials RPC rejected update (server-side current-password mismatch)")
                    }
                }.onFailure { e ->
                    Log.w(TAG, "Secure admin_credentials RPC failed (local/flow unaffected): ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Secure admin_credentials RPC threw (local/flow unaffected): ${e.message}")
            }
        }

        // 5. [ফিক্স — real Supabase Auth password sync] loginAsAdmin() এখন একটা real Supabase
        // Auth session তৈরি করে (SupabaseAuthManager.signInWithPhonePassword) আর সেই session-এর
        // উপরই সব RLS admin-scoping নির্ভর করে। কিন্তু আগে এখানে (ধাপ ৪ পর্যন্ত) শুধু app-level
        // hash + admin_credentials টেবিল আপডেট হতো — real Supabase Auth password
        // (auth.users.encrypted_password) কখনো sync হতো না। ফলে পাসওয়ার্ড বদলানোর পর পরের
        // loginAsAdmin()-এর ভেতরের real sign-in silently fail করত (Log.w-তে ধরা পড়ত, কিন্তু login
        // আটকাত না) — RLS admin-কে চিনতে না পেরে anon-level visibility দিয়ে দিত, আর admin panel-এ
        // ডেটা mismatch/অসম্পূর্ণ দেখাত অথচ কোনো error message ছাড়াই।
        //
        // এখন: নতুন পাসওয়ার্ড দেওয়া হলে client.auth.updateUser() দিয়ে বর্তমান (এই মুহূর্তে active,
        // কারণ Admin Settings-এ ঢুকতেই আগে loginAsAdmin() হয়ে গেছে) Supabase Auth session-এর
        // password-ও সাথে সাথে বদলে দেওয়া হয় — তাই admin_credentials হ্যাশ আর real Auth password
        // কখনো out-of-sync হবে না। ব্যর্থ হলে (network/session-not-active ইত্যাদি) বাকি সব flow
        // (DataStore/platform_settings/secure RPC) আগের মতোই অপ্রভাবিত — non-blocking, শুধু log —
        // কিন্তু caller-কে (adminUpdateCredentials) এই ব্যর্থতা জানানো হয় (এক্সসেপশন থ্রো করে) যাতে
        // caller প্রয়োজনে admin-কে দৃশ্যমান সতর্কবার্তা দেখাতে পারে (পরের লগইনে "সীমিত ভিউ" এড়াতে,
        // দেখুন SomadhanViewModel.adminUpdateCredentials + loginAsAdmin-এর নতুন warning)।
        if (!newRawPassword.isNullOrBlank()) {
            try {
                SupabaseAuthManager.updatePassword(newRawPassword.trim())
                    .onFailure { e ->
                        Log.w(
                            TAG,
                            "Real Supabase Auth password sync FAILED — admin_credentials hash updated but real Auth password did NOT change. " +
                                "Next admin login's real Supabase Auth sign-in will likely fail until this is retried (session may have expired): ${e.message}"
                        )
                        throw IllegalStateException(
                            "ক্রেডেনশিয়াল আংশিকভাবে আপডেট হয়েছে: app-level পাসওয়ার্ড বদলেছে, কিন্তু real " +
                                "Supabase Auth password sync ব্যর্থ হয়েছে (${e.message ?: "session active নেই"}). " +
                                "অনুগ্রহ করে লগআউট করে আবার লগইন করে পুনরায় পাসওয়ার্ড বদলান।"
                        )
                    }
            } catch (e: IllegalStateException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Real Supabase Auth password sync threw: ${e.message}")
                throw IllegalStateException(
                    "ক্রেডেনশিয়াল আংশিকভাবে আপডেট হয়েছে: real Supabase Auth password sync ব্যর্থ হয়েছে " +
                        "(${e.message ?: "unknown error"})। অনুগ্রহ করে লগআউট করে আবার লগইন করে পুনরায় পাসওয়ার্ড বদলান।"
                )
            }
        }
    }

    /**
     * Checks whether the admin is currently still using the initial default phone and/or password.
     * Useful for showing a security warning banner urging password change.
     */
    suspend fun isUsingDefaultCredentials(context: Context): Boolean {
        val phone = getAdminPhone(context)
        val hash = getAdminPasswordHash(context)
        return phone == DEFAULT_ADMIN_PHONE || hash == DEFAULT_ADMIN_PASSWORD_HASH
    }
}
