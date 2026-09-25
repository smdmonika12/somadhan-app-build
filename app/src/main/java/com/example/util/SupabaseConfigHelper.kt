package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.withTimeout

/**
 * সমাধান (Somadhan) — ধাপ ৩৩.৩, কাজ ৪
 *
 * পুরনো `FirebaseConfigHelper`-এর Supabase-সমতুল্য: "ক্লাউড প্রজেক্ট রিকনফিগার" অ্যাডমিন টুলটা
 * ডিলিট না করে, এখন Supabase URL/anon key runtime-এ পাল্টানো ও টেস্ট করার কাজে ব্যবহৃত হয়
 * (Firebase project id/API key এর বদলে)। আসল save/rebuild লজিক [SupabaseClientProvider]-এ,
 * এই ফাইলে শুধু UI-এর জন্য সুবিধাজনক wrapper (getSaved* ও testConnection) — ঠিক
 * FirebaseConfigHelper-এর shape মিলিয়ে।
 */
object SupabaseConfigHelper {
    private const val TAG = "SupabaseConfigHelper"

    fun getSavedUrl(context: Context): String = SupabaseClientProvider.getSavedUrl(context)

    fun getSavedAnonKey(context: Context): String = SupabaseClientProvider.getSavedAnonKey(context)

    /**
     * URL/key সেভ করে ক্লায়েন্ট রিবিল্ড করে। সফল হলে সাথে সাথেই [testConnection]ও কল করা
     * ভালো (ViewModel-এই করা হচ্ছে) যাতে ভুল URL/key এর ব্যাপারে ব্যবহারকারী দ্রুত জানতে পারে।
     */
    fun saveAndReconfigure(context: Context, url: String, anonKey: String): Boolean {
        return SupabaseClientProvider.saveAndReconfigure(context, url, anonKey)
    }

    /**
     * `categories` টেবিলে একটা হালকা read (RLS: সম্পূর্ণ উন্মুক্ত SELECT পলিসি, auth লাগে না)
     * দিয়ে কানেকশন টেস্ট করে — Firebase ভার্সনের `db.collection("users").limit(10).get()`-এর
     * সমতুল্য, কিন্তু sensitive টেবিল (users) এর বদলে ইচ্ছাকৃতভাবে পাবলিক-রিডেবল টেবিল ব্যবহার
     * করা হয়েছে, যাতে ভুল/অন্য কারো anon key দিয়েও (RLS যেটাই থাকুক) টেস্ট নিরাপদে চলতে পারে।
     */
    suspend fun testConnection(context: Context): Pair<Boolean, String> {
        return try {
            withTimeout(10_000) {
                val client = SupabaseClientProvider.client
                val categories = client.postgrest.from("categories").select().decodeList<com.example.data.remote.dto.CategoryDto>()
                Pair(
                    true,
                    "সফলভাবে সংযুক্ত! Supabase URL: ${SupabaseClientProvider.getSavedUrl(context)}\n" +
                        "ক্যাটাগরি টেবিল থেকে ${categories.size} টি রো পাওয়া গেছে।"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Test connection failed: ${e.message}")
            val msg = e.localizedMessage ?: e.message ?: "সংযোগ ব্যর্থ হয়েছে"
            val help = when {
                msg.contains("401", ignoreCase = true) || msg.contains("JWT", ignoreCase = true) ->
                    "\n\nটিপস: Supabase anon key ঠিক আছে কিনা যাচাই করুন (Project Settings -> API)।"
                msg.contains("Unable to resolve host", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                    "\n\nটিপস: Supabase Project URL ঠিক আছে কিনা এবং ইন্টারনেট সংযোগ যাচাই করুন।"
                else -> ""
            }
            Pair(false, "$msg$help")
        }
    }
}
