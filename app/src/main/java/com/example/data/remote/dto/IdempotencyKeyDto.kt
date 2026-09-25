package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase টেবিল `public.idempotency_keys` এর DTO — PK হলো `key`।
 *
 * `result` কলাম `jsonb` টাইপ; এই ধাপে সেটা raw JSON `String?` হিসেবে রাখা হয়েছে (JsonElement এর
 * মতো টাইপড parsing পরের ধাপে দরকার হলে repository লেয়ারে যোগ করা যাবে, এখানে scope বাড়ানো হয়নি)।
 */
@Serializable
data class IdempotencyKeyDto(
    val key: String, // text, PK
    @SerialName("request_type") val requestType: String,
    val result: String? = null, // jsonb, nullable
    @SerialName("created_at") val createdAt: String? = null // timestamptz
)
