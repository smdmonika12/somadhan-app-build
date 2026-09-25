package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    fun roundTaka(amount: Double): Long {
        return Math.round(amount)
    }

    fun formatTaka(amount: Double): String {
        val rounded = Math.round(amount)
        return "৳ ${DistanceUtil.toBengaliDigits(rounded.toString())}"
    }

    fun formatTakaRange(min: Double, max: Double): String {
        val rMin = Math.round(min)
        val rMax = Math.round(max)
        return "৳ ${DistanceUtil.toBengaliDigits(rMin.toString())} - ৳ ${DistanceUtil.toBengaliDigits(rMax.toString())}"
    }

    fun formatDateBengali(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
        val formatted = sdf.format(Date(timestamp))
        val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val banglaMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        
        var result = formatted
        for (i in englishMonths.indices) {
            result = result.replace(englishMonths[i], banglaMonths[i])
        }
        return DistanceUtil.toBengaliDigits(result)
    }

    fun formatDateTimeBengali(timestamp: Long): String {
        val sdfDate = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val dateFormatted = sdfDate.format(Date(timestamp))
        val timeFormatted = sdfTime.format(Date(timestamp))
            .replace("AM", "AM")
            .replace("PM", "PM")

        val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val banglaMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        
        var dateResult = dateFormatted
        for (i in englishMonths.indices) {
            dateResult = dateResult.replace(englishMonths[i], banglaMonths[i])
        }
        return "${DistanceUtil.toBengaliDigits(dateResult)} • ${DistanceUtil.toBengaliDigits(timeFormatted)}"
    }

    fun formatTimeExact(timestamp: Long): String {
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        return DistanceUtil.toBengaliDigits(sdfTime.format(Date(timestamp)))
    }

    fun formatTimeWithRelative(timestamp: Long): String {
        val relative = formatTimeAgo(timestamp)
        val exact = formatDateTimeBengali(timestamp)
        return "$relative ($exact)"
    }

    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            minutes < 1 -> "এইমাত্র"
            minutes < 60 -> "${DistanceUtil.toBengaliDigits(minutes.toString())} মিনিট আগে"
            hours < 24 -> "${DistanceUtil.toBengaliDigits(hours.toString())} ঘণ্টা আগে"
            days < 7 -> "${DistanceUtil.toBengaliDigits(days.toString())} দিন আগে"
            else -> formatDateBengali(timestamp)
        }
    }

    /**
     * [SUPABASE-MIGRATED - E.164 ফলো-আপ ফিক্স ১]
     * `public.users.phone` (এবং তাই local `UserEntity.phone`) এখন সবসময় E.164 ফরম্যাটে
     * (`+8801712345678`) সেভ থাকে, কারণ `handle_new_auth_user()` DB trigger `auth.users.phone`
     * সরাসরি কপি করে। কিন্তু বাংলাদেশি ব্যবহারকারীরা লোকাল ফরম্যাটে (`01712345678`) টাইপ করেছিল,
     * তাই শুধু READ-ONLY DISPLAY-এর জন্য (profile/admin স্ক্রিন) এই ফাংশন দিয়ে E.164 -> লোকাল
     * ফরম্যাটে রূপান্তর করে দেখানো হয়। input/edit ফিল্ডে এটা ব্যবহার করা যাবে না।
     */
    fun toLocalDisplayFormat(e164OrLocalPhone: String): String {
        val p = e164OrLocalPhone.trim()
        return if (p.startsWith("+880") && p.length == 14) {
            "0" + p.substring(4) // +8801712345678 -> 01712345678
        } else {
            p // ইতিমধ্যে লোকাল ফরম্যাটে বা অচেনা ফরম্যাটে থাকলে অপরিবর্তিত (no-op)
        }
    }
}
