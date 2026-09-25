package com.example.data.repository

/**
 * [Step 12.10d] Supabase RPC-এর ব্যর্থতা "স্থায়ী (permanent)" নাকি "সাময়িক (transient)" — সেটা আলাদা করে।
 *
 * সার্ভার-সাইড guard-গুলো (`release_escrow`, `refund_escrow_once`, `request_withdrawal`) ব্যবসায়িক নিয়ম
 * ভাঙলে `raise exception '<CODE>'` ছোড়ে — তাই client-এ এগুলো `.onFailure {}`-এ আসে, ঠিক নেটওয়ার্ক
 * error-এর মতোই। কিন্তু নেটওয়ার্ক error পরে retry করলে সফল হতে পারে; `PROBLEM_DISPUTED`/`KYC_REQUIRED`
 * ইত্যাদি হবে না — উল্টে পরে (যেমন dispute মিটে গেলে) retry সফল হলে সিদ্ধান্তের বিপরীতে টাকা নড়ে যেতে পারে।
 * তাই এই কোডগুলো outbox retry-তে পাঠানো হয় না।
 *
 * ম্যাচ করা হয় exception message-এ কোডটা `contains` করে (server-এর message-এ কোডের পরে `: <value>`
 * থাকতে পারে, যেমন `INSUFFICIENT_BALANCE: 500`)। কোড না মিললে (নেটওয়ার্ক, timeout, JWT expired, 5xx)
 * সবসময় transient ধরা হয় — অর্থাৎ আগের আচরণ (enqueue) অপরিবর্তিত।
 */
object RpcErrorClassifier {

    val PERMANENT_CODES: List<String> = listOf(
        "PROBLEM_DISPUTED",
        "NOT_AUTHORIZED",
        "KYC_REQUIRED",
        "INVALID_REFUND_PERCENTAGE",
        "ROLE_INACTIVE", // SOLVER_ROLE_INACTIVE / USER_ROLE_INACTIVE-ও এতে ধরা পড়ে
        "INVALID_ROLE",
        "INSUFFICIENT_BALANCE",
        "BELOW_MIN_WITHDRAWAL",
        // [Step 12.11] "ESCROW_NOT_FOUND" এখান থেকে সরানো হয়েছে: offline-first প্রবাহে accept_bid/accept_direct_contract
        // outbox-এ জমা থাকলে cloud-এ escrow এখনো নেই — তখন release/refund-এর ESCROW_NOT_FOUND আসলে "এখনো sync হয়নি"
        // (transient), স্থায়ী প্রত্যাখ্যান নয়। permanent ধরলে payout পুরোটাই আটকে যেত (আগের আচরণের বিপরীত)।
        // id-mismatch নিজে এখন সার্ভারে client-supplied escrow id দিয়ে মেটানো (Step 12.11)।
        "USER_NOT_FOUND"
    )

    // supabase-kt exception-এ server-এর `raise exception` টেক্সট ঠিক কোথায় (message / cause / toString) থাকে সেটা
    // যাচাই করা নেই — তাই cause-চেইনসহ সবগুলো টেক্সটেই খোঁজা হয় (ম্যাচ মিস হলে আচরণ আগের মতোই: enqueue)।
    private fun haystack(error: Throwable?): String {
        val sb = StringBuilder()
        var cur = error
        var depth = 0
        while (cur != null && depth < 5) {
            sb.append(cur.toString()).append('\n')
            cur.message?.let { sb.append(it).append('\n') }
            cur = cur.cause
            depth++
        }
        return sb.toString()
    }

    fun isPermanent(error: Throwable?): Boolean = isPermanentMessage(haystack(error))

    fun isPermanentMessage(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return PERMANENT_CODES.any { message.contains(it) }
    }

    /** escrow release ব্যর্থ হলে (server স্থায়ীভাবে প্রত্যাখ্যান করলে) ব্যবহারকারীকে দেখানোর বার্তা। */
    fun escrowReleaseUserMessage(error: Throwable?): String {
        val m = haystack(error)
        return when {
            m.contains("PROBLEM_DISPUTED") -> "এই কাজটি বিবাদাধীন (dispute), তাই পেমেন্ট রিলিজ করা যাচ্ছে না। অ্যাডমিনের সিদ্ধান্তের অপেক্ষা করুন।"
            m.contains("NOT_AUTHORIZED") -> "পেমেন্ট রিলিজের অনুমতি যাচাই করা যায়নি। আবার লগইন করে চেষ্টা করুন।"
            m.contains("ROLE_INACTIVE") -> "সমাধানকারীর অ্যাকাউন্ট এখন সক্রিয় নয়, তাই পেমেন্ট রিলিজ করা যাচ্ছে না।"
            else -> "সার্ভার এই পেমেন্ট রিলিজ গ্রহণ করেনি। কিছুক্ষণ পরে আবার চেষ্টা করুন।"
        }
    }

    /** withdrawal ব্যর্থ হলে ব্যবহারকারীকে দেখানোর বাংলা বার্তা (কোড অনুযায়ী)। */
    fun withdrawalUserMessage(error: Throwable?): String {
        val m = haystack(error)
        return when {
            m.contains("KYC_REQUIRED") -> "উইথড্র করতে আগে KYC ভেরিফিকেশন সম্পন্ন করতে হবে।"
            m.contains("ROLE_INACTIVE") -> "এই ভূমিকা (role) এখন সক্রিয় নয়, তাই উইথড্র করা যাচ্ছে না।"
            m.contains("INSUFFICIENT_BALANCE") -> "আপনার অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই।"
            m.contains("BELOW_MIN_WITHDRAWAL") -> "উইথড্রের পরিমাণ সর্বনিম্ন সীমার চেয়ে কম।"
            m.contains("NOT_AUTHORIZED") -> "অনুমতি যাচাই করা যায়নি। আবার লগইন করে চেষ্টা করুন।"
            else -> "সার্ভার এই উইথড্র রিকোয়েস্ট গ্রহণ করেনি। কিছুক্ষণ পরে আবার চেষ্টা করুন।"
        }
    }
}

/**
 * [Step 12.10d] server `release_escrow` স্থায়ীভাবে প্রত্যাখ্যান করলে (যেমন PROBLEM_DISPUTED) repository এটা ছোড়ে —
 * **কোনো local write হওয়ার আগেই** (RPC-first), তাই rollback লাগে না। ViewModel-এর বিদ্যমান try/catch
 * (confirmReleaseAndComplete/adminReleaseEscrow/adminUpdateDirectContractStatus) `e.message` toast করে।
 */
class EscrowReleaseRejectedException(message: String) : Exception(message)
