package com.example.util

/**
 * Utility to detect attempts at off-platform communication and direct unauthorized payments
 * in chat messages (e.g., phone numbers, WhatsApp, Telegram, direct bKash/Nagad solicitations).
 */
object ChatPolicyGuard {

    enum class ViolationType {
        PHONE_NUMBER,
        EXTERNAL_APP,
        DIRECT_PAYMENT,
        OFF_PLATFORM_DEAL
    }

    data class DetectionResult(
        val isViolation: Boolean,
        val violationType: ViolationType? = null,
        val detectedSnippet: String? = null,
        val warningTitle: String = "",
        val warningMessage: String = "",
        val ruleKey: String = "OFFLINE_PAYMENT_ATTEMPT"
    )

    private val bengaliToEnglishDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val externalAppKeywords = listOf(
        "whatsapp", "whats app", "what's app", "হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "হুয়াটসঅ্যাপ",
        "telegram", "টেলিগ্রাম", "imo", "ইমু", "ইমো", "viber", "ভাইবার", "wa.me", "t.me",
        "messenger", "মেসেঞ্জার", "facebook", "ফেসবুক", "ইনস্টাগ্রাম", "instagram"
    )

    private val directPaymentKeywords = listOf(
        "ব্যক্তিগত বিকাশ", "পার্সোনাল বিকাশ", "পার্সোনাল নগদ", "ব্যক্তিগত নগদ",
        "বিকাশে পাঠাও", "বিকাশে দেন", "বিকাশ দিন", "বিকাশে পাঠিয়ে", "বিকাশে পাঠাই",
        "নগদে দেন", "নগদে পাঠাও", "নগদ দিন", "নগদে পাঠিয়ে", "নগদে পাঠাই",
        "রকেটে দেন", "রকেটে পাঠাও", "রকেটে দিন",
        "bkash personal", "nagad personal", "send to bkash", "send to nagad"
    )

    private val offPlatformDealPhrases = listOf(
        "বাইরে কথা বলি", "বাইরে আসেন", "বাইরে যোগাযোগ", "বাইরে ডিল", "বাইরে কাজ", "বাইরে লেনদেন",
        "অ্যাপের বাইরে", "অ্যাপ ছাড়া", "অ্যাপ ছাড়া", "কমিশন বাঁচান", "কমিশন দিতে হবে না",
        "অ্যাপে চার্জ কাটে", "অ্যাপে অনেক চার্জ", "সরাসরি ক্যাশ", "সরাসরি যোগাযোগ", "সরাসরি কথা",
        "কল দেন", "কল দেন আমাকে", "ফোন দেন আমাকে", "ফোন দিয়েন", "ফোন দিও", "call me directly",
        "contact outside", "deal outside", "deal directly"
    )

    private val writtenDigitWords = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "জিরো", "ওয়ান", "ওয়ান", "টু", "থ্রি", "ফোর", "ফাইভ", "সিক্স", "সেভেন", "এইট", "নাইন",
        "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "নয়", "শূন্য"
    )

    /**
     * Scans message text for any off-platform policy violations.
     */
    fun scanMessage(text: String): DetectionResult {
        if (text.isBlank()) return DetectionResult(isViolation = false)

        val cleanText = text.trim()
        val lowerText = cleanText.lowercase()

        // 1. Check for Phone Numbers (English & Bengali digits with spaces/dashes evasion)
        val normalizedDigits = extractNormalizedDigits(cleanText)
        val bdPhoneRegex = Regex("(?:\\+?88)?01[3-9]\\d{8}")
        val phoneMatch = bdPhoneRegex.find(normalizedDigits)
        if (phoneMatch != null) {
            return DetectionResult(
                isViolation = true,
                violationType = ViolationType.PHONE_NUMBER,
                detectedSnippet = phoneMatch.value,
                warningTitle = "ফোন নম্বর শেয়ার করা নিষিদ্ধ",
                warningMessage = "গ্রাহক ও সলভারের নিরাপত্তা এবং প্রতারণা রোধে চ্যাটে ব্যক্তিগত ফোন নম্বর শেয়ার করা প্ল্যাটফর্মের পলিসি বিরোধী।"
            )
        }

        // 2. Check for Social / Messaging Apps solicitation
        for (keyword in externalAppKeywords) {
            if (lowerText.contains(keyword)) {
                return DetectionResult(
                    isViolation = true,
                    violationType = ViolationType.EXTERNAL_APP,
                    detectedSnippet = keyword,
                    warningTitle = "বাইরের অ্যাপে যোগাযোগ নিষিদ্ধ",
                    warningMessage = "হোয়াটসঅ্যাপ, টেলিগ্রাম বা অন্যান্য সামাজিক মাধ্যমে অ্যাপের বাইরে যোগাযোগের অনুরোধ প্ল্যাটফর্মের নিরাপত্তা নীতিমালার লঙ্ঘন।"
                )
            }
        }

        // 3. Check for Direct/Unauthorized Payment Solicitation
        for (keyword in directPaymentKeywords) {
            if (lowerText.contains(keyword)) {
                return DetectionResult(
                    isViolation = true,
                    violationType = ViolationType.DIRECT_PAYMENT,
                    detectedSnippet = keyword,
                    warningTitle = "সরাসরি ব্যক্তিগত পেমেন্ট নিষিদ্ধ",
                    warningMessage = "অ্যাপের সুরক্ষিত এসক্রো/পেমেন্ট গেটওয়ের বাইরে সরাসরি ব্যক্তিগত বিকাশ/নগদে লেনদেন নিষিদ্ধ ও ঝুঁকিপূর্ণ।"
                )
            }
        }

        // 4. Check for Off-Platform Deal Solicitation phrases
        for (phrase in offPlatformDealPhrases) {
            if (lowerText.contains(phrase)) {
                return DetectionResult(
                    isViolation = true,
                    violationType = ViolationType.OFF_PLATFORM_DEAL,
                    detectedSnippet = phrase,
                    warningTitle = "অফ-প্ল্যাটফর্ম ডিলের চেষ্টা শনাক্ত",
                    warningMessage = "অ্যাপের বাইরে সরাসরি কাজ করা বা লেনদেন করার প্রস্তাব শনাক্ত হয়েছে, যা প্ল্যাটফর্মের নিরাপত্তা চুক্তির বরখেলাপ।"
                )
            }
        }

        return DetectionResult(isViolation = false)
    }

    /**
     * Quick lightweight check for soft typing warning in UI.
     */
    fun hasSuspiciousPatterns(text: String): Boolean {
        if (text.length < 4) return false
        val lower = text.lowercase()
        return externalAppKeywords.any { lower.contains(it) } ||
                directPaymentKeywords.any { lower.contains(it) } ||
                offPlatformDealPhrases.any { lower.contains(it) } ||
                Regex("01[3-9]").containsMatchIn(extractNormalizedDigits(text))
    }

    private fun extractNormalizedDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch.isDigit()) {
                sb.append(ch)
            } else if (bengaliToEnglishDigits.containsKey(ch)) {
                sb.append(bengaliToEnglishDigits[ch])
            }
        }
        return sb.toString()
    }
}
