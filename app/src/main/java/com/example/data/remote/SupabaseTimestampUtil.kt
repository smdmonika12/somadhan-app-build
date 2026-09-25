package com.example.data.remote

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ২০ (Realtime Foundation A)
 *
 * `timestamptz` কলাম (Supabase/PostgREST/Realtime থেকে ISO-8601 `String` হিসেবে আসে, যেমন
 * `"2026-09-12T10:15:30.123456+00:00"` বা `"...Z"`) কে local `Long` (epoch millis) এ পার্স করার
 * জন্য একমাত্র জায়গা।
 *
 * **কেন `java.time.Instant`/`OffsetDateTime` ব্যবহার করা হয়নি**: `java.time` API ৮ Android API
 * ২৬+ এ পাওয়া যায় (অথবা core library desugaring লাগে)। এই প্রজেক্টের `minSdk = 24` -- ধাপ ২০-এ
 * (এই একই সেশনে, `SupabaseRealtimeManager.kt`-এর জন্য কাজ করার সময়) desugaring যোগ করা হয়েছে
 * (`app/build.gradle.kts` দেখুন, `isCoreLibraryDesugaringEnabled = true`), কিন্তু এই ফাইলটা
 * সেই dependency-র উপর নির্ভর না করে সব সময় নিরাপদ থাকতে ইচ্ছাকৃতভাবে পুরনো `SimpleDateFormat`-ই
 * ব্যবহার করে (`CsvImportUtil.kt`-এ আগে থেকেই ব্যবহৃত হওয়া একই প্যাটার্ন অনুসরণ করে) -- API 24
 * থেকেই সমর্থিত।
 *
 * **কেন fractional-seconds আগে normalize করা হয়**: Postgres `timestamptz` এর fractional
 * seconds precision ০ থেকে ৬ ডিজিট (microseconds) পর্যন্ত হতে পারে, কিন্তু Java-র পুরনো
 * `SimpleDateFormat`-এর `S` প্যাটার্ন letter একটা fixed-width field না -- এটা যতগুলো ডিজিট পায়
 * ততগুলোকেই raw সংখ্যা হিসেবে parse করে (৩-ডিজিট "SSS" প্যাটার্ন দিয়ে ".123456" parse করলে ভুলভাবে
 * ১২৩৪৫৬ মিলিসেকেন্ড হিসেবে ধরবে, ১২৩ না)। তাই parse করার আগে সবসময় ঠিক ৩ ডিজিটে
 * pad/truncate করা হয়।
 */
object SupabaseTimestampUtil {

    private val fractionRegex = Regex("""\.(\d+)""")
    private val offsetRegex = Regex("""[+-]\d{2}:?\d{2}$""")

    /**
     * Supabase `timestamptz` স্ট্রিং কে epoch millis এ পার্স করে। পার্স করা না গেলে (null, ফাঁকা,
     * বা অপ্রত্যাশিত ফরম্যাট) `null` রিটার্ন করে -- caller-কে decide করতে হবে fallback কী হবে
     * (যেমন `System.currentTimeMillis()`, নাকি null-ই রেখে দেওয়া উচিত সেটা field-ভেদে ভিন্ন)।
     */
    fun parseTimestamptz(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            val normalized = fractionRegex.replace(raw) { m ->
                ".${m.groupValues[1].padEnd(3, '0').take(3)}"
            }
            val hasOffset = normalized.endsWith("Z") || offsetRegex.containsMatchIn(normalized)
            val hasFraction = normalized.contains('.')
            val pattern = when {
                hasFraction && hasOffset -> "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                hasFraction && !hasOffset -> "yyyy-MM-dd'T'HH:mm:ss.SSS"
                !hasFraction && hasOffset -> "yyyy-MM-dd'T'HH:mm:ssXXX"
                else -> "yyyy-MM-dd'T'HH:mm:ss"
            }
            // "Z" কে SimpleDateFormat-এর "XXX" প্যাটার্ন সরাসরি বোঝে (ISO 8601 "Z" = UTC), তাই
            // আলাদা করে replace করার দরকার নেই -- Android API 24+ এই আচরণ সমর্থন করে।
            val sdf = SimpleDateFormat(pattern, Locale.US)
            if (!hasOffset) sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(normalized)?.time
        } catch (e: Exception) {
            null
        }
    }
}
