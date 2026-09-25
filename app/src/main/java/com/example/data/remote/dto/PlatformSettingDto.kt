package com.example.data.remote.dto

import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.platform_settings` এর DTO — key-value টেবিল, PK হলো `key`। */
@Serializable
data class PlatformSettingDto(
    val key: String, // text, PK
    val value: String
)
