package com.example.data.remote

import android.content.Context
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ১ (+ ধাপ ৩৩.৩ কাজ ৪: runtime-reconfigurable)
 *
 * এই অবজেক্টটা Supabase এর সাথে সংযোগের একমাত্র entry point। এখানে Postgrest (database),
 * Auth, Storage, আর Realtime — এই চারটা প্লাগইন ইনস্টল করা একটা [SupabaseClient] তৈরি করা হয়েছে।
 *
 * URL আর anon key ডিফল্টভাবে source code-এ hardcode করা হয়নি —
 * MAPS_API_KEY / SMS_API_KEY / SENDGRID_API_KEY গুলোর জন্য ইতিমধ্যে ব্যবহৃত হওয়া একই প্যাটার্নে
 * এই মানগুলো `.env` (local, git-ignored) / `.env.example` (default placeholder) ফাইল থেকে
 * Secrets Gradle Plugin দিয়ে অটোমেটিক্যালি [BuildConfig.SUPABASE_URL] /
 * [BuildConfig.SUPABASE_ANON_KEY] হিসেবে এক্সপোজ করা হয়।
 *
 * [com.example.util.SupabaseConfigHelper]-এর প্যাটার্নে, admin চাইলে runtime-এ (অ্যাডমিন প্যানেল থেকে) SharedPreferences
 * এ একটা override URL/key সেভ করতে পারেন — সেভ হলে সেটাই BuildConfig-এর জায়গায় ব্যবহার হবে।
 * `client` একটা rebuildable var (আগে `by lazy val` ছিল) — [reconfigure] কল হলে বর্তমান instance
 * বাতিল করে নতুন override দিয়ে পরের অ্যাক্সেসে নতুন client তৈরি হবে। সব caller (`SupabaseSyncManager`,
 * `SupabaseAuthManager`, `SupabaseRealtimeManager`, ইত্যাদি) `.client` প্রপার্টি প্রতিবার অ্যাক্সেস
 * করে (কোথাও module-level ভ্যারিয়েবলে cache করে রাখা হয়নি — যাচাই করা হয়েছে), তাই rebuild নিরাপদ।
 */
object SupabaseClientProvider {

    private const val PREF_NAME = "somadhan_supabase_config"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    @Volatile
    private var overrideUrl: String? = null

    @Volatile
    private var overrideKey: String? = null

    @Volatile
    private var cachedClient: SupabaseClient? = null

    private val lock = Any()

    /** অ্যাপ-স্টার্টআপে (Application.onCreate) একবার কল করে SharedPreferences থেকে সেভ করা override লোড করে নেওয়া উচিত। */
    fun initFromSavedConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        overrideUrl = prefs.getString(KEY_URL, null)?.takeIf { it.isNotBlank() }
        overrideKey = prefs.getString(KEY_ANON_KEY, null)?.takeIf { it.isNotBlank() }
    }

    private fun currentUrl(): String = overrideUrl ?: BuildConfig.SUPABASE_URL
    private fun currentKey(): String = overrideKey ?: BuildConfig.SUPABASE_ANON_KEY

    /**
     * এখন ব্যবহৃত হচ্ছে এমন Supabase client — override সেভ করা থাকলে সেটা, নাহলে BuildConfig।
     * থ্রেড-সেফ lazy-rebuild: [reconfigure] কল হলে `cachedClient = null` হয়ে যায়, তারপরের অ্যাক্সেসে
     * নতুন URL/key দিয়ে নতুন instance তৈরি হয়।
     */
    val client: SupabaseClient
        get() = cachedClient ?: synchronized(lock) {
            cachedClient ?: buildClient(currentUrl(), currentKey()).also { cachedClient = it }
        }

    @OptIn(SupabaseInternal::class)
    private fun buildClient(url: String, key: String): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key,
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(Realtime)
            // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "পুল-টু-রিফ্রেশ/সিঙ্ক আটকে যাচ্ছে, শেষ হচ্ছে না"]
            // আগে এই client-এ কোনো HTTP timeout সেট করা ছিল না — Ktor ডিফল্টভাবে কোনো
            // request/connect timeout চাপায় না। ফলে flaky/দুর্বল mobile network-এ কোনো
            // Postgrest কল (যেমন pullBulkDataFromSupabase()-এর ভেতরের `.select()` কলগুলো)
            // connection stall হয়ে গেলে কখনো exception ছোঁড়েনি — শুধু চিরকালের জন্য suspend
            // হয়ে থাকত। SomadhanViewModel.triggerCloudSync()-এর try/finally (যেটা
            // `_isRefreshing = false` guarantee করার জন্য আগেই এক দফা ফিক্স করা হয়েছিল, দেখো
            // সেই ফাংশনের কমেন্ট) exception ধরতে পারে, কিন্তু coroutine নিজেই যদি কখনো resume না
            // হয় (network hang, exception না) তাহলে finally ব্লকটাই কখনো চলার সুযোগ পায় না —
            // এটাই আসল root cause ছিল "স্পিনার/শিমার আটকে যাওয়া" উপসর্গের। ফিক্স: এখন প্রতিটা
            // HTTP কলে একটা hard timeout সেট করা হলো (৩০ সেকেন্ড রিকোয়েস্ট/সকেট, ১৫ সেকেন্ড
            // কানেক্ট) — এর বেশি সময় লাগলে Ktor নিজেই `HttpRequestTimeoutException` ছুঁড়বে,
            // যেটা এখন `runCatching`/try-catch-গুলো স্বাভাবিকভাবেই ধরে নিতে পারবে, আর
            // `_isRefreshing` ঠিকভাবে false-এ ফিরবে। Realtime (WebSocket)-এর নিজস্ব আলাদা
            // reconnect/heartbeat লজিক আছে, এই request-timeout তাতে হস্তক্ষেপ করে না।
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000L
                    connectTimeoutMillis = 15_000L
                    socketTimeoutMillis = 30_000L
                }
            }
        }

    /**
     * URL/key এখনো placeholder অবস্থায় আছে কিনা।
     */
    fun isConfigured(): Boolean {
        val url = currentUrl()
        val key = currentKey()
        return url.isNotBlank() &&
            key.isNotBlank() &&
            !url.contains("your_supabase", ignoreCase = true) &&
            !key.contains("your_supabase", ignoreCase = true)
    }

    fun getSavedUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_URL, "")?.takeIf { it.isNotBlank() } ?: BuildConfig.SUPABASE_URL
    }

    fun getSavedAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ANON_KEY, "")?.takeIf { it.isNotBlank() } ?: BuildConfig.SUPABASE_ANON_KEY
    }

    /**
     * নতুন URL/key SharedPreferences এ সেভ করে, cached client invalidate করে (পরের `.client`
     * অ্যাক্সেসে নতুন instance রিবিল্ড হবে)। পুরনো (এখন মুছে ফেলা) Firebase-যুগের
     * `FirebaseConfigHelper.saveAndReconfigure()`-এর মতোই একই ধরনের রিলোড-প্যাটার্ন।
     */
    fun saveAndReconfigure(context: Context, url: String, anonKey: String): Boolean {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        if (cleanUrl.isBlank() || cleanKey.isBlank()) return false
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_URL, cleanUrl)
                .putString(KEY_ANON_KEY, cleanKey)
                .apply()
            overrideUrl = cleanUrl
            overrideKey = cleanKey
            synchronized(lock) { cachedClient = null }
            true
        } catch (_: Exception) {
            false
        }
    }
}
