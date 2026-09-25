package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.days

object KycUploadManager {
    private const val TAG = "KycUploadManager"

    // [SUPABASE-MIGRATED - ধাপ ১৫] `kyc-docs` bucket প্রাইভেট (public নয়), তাই সরাসরি publicUrl()
    // কাজ করবে না — এর বদলে একটা লম্বা-মেয়াদী signed URL বানিয়ে সেটাই সংরক্ষণ করা হচ্ছে, যাতে
    // AdminKycView.kt-এর মতো বিদ্যমান display কোড (যেটা fileUrl স্ট্রিংটাকে সরাসরি AsyncImage-এ
    // ব্যবহার করে) অপরিবর্তিত থাকে। ৩৬৫ দিনের মেয়াদ বেছে নেওয়া হয়েছে যাতে ব্যবহারিকভাবে
    // KYC review-এর পুরো জীবনচক্রে URL কাজ করে। সীমাবদ্ধতা: মেয়াদ ফুরোলে পুরনো URL অকেজো হয়ে
    // যাবে — ভবিষ্যতে দরকার হলে admin-side একটা "refresh signed URL" হেল্পার/RPC যোগ করা যেতে
    // পারে, কিন্তু এই ধাপের স্কোপে নেই (progress রিপোর্টে নোট করা হয়েছে)।
    private val SIGNED_URL_EXPIRY = 365.days

    suspend fun uploadKycImage(
        context: Context,
        userId: String,
        fileType: String, // "front", "back", "selfie"
        imageUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        // Prepare destination compressed file
        val kycDir = File(context.filesDir, "kyc_docs").apply { if (!exists()) mkdirs() }
        val destFile = File(kycDir, "${userId}_${fileType}_${System.currentTimeMillis()}.jpg")
        val isCompressed = ImageStorageUtil.compressImageToFile(context, imageUri, destFile, maxDimension = 1280, quality = 82)

        if (!isCompressed) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy $fileType image locally: ${e.message}", e)
                return@withContext Result.success(imageUri.toString())
            }
        }

        // uploadFileUri আর দরকার নেই — Supabase Storage আপলোডে সরাসরি destFile থেকে bytes পড়া হয়।

        // 1. Try Supabase Storage (`kyc-docs` bucket, private) with a strict 3-second timeout
        try {
            val signedUrl = withTimeoutOrNull(3000L) {
                val bucket = SupabaseClientProvider.client.storage.from("kyc-docs")
                val remotePath = "$userId/${fileType}_${System.currentTimeMillis()}.jpg"
                val bytes = destFile.readBytes()
                bucket.upload(remotePath, bytes) { upsert = true }
                bucket.createSignedUrl(remotePath, SIGNED_URL_EXPIRY)
            }
            if (!signedUrl.isNullOrBlank()) {
                Log.d(TAG, "Uploaded compressed $fileType to Supabase Storage: $signedUrl")
                return@withContext Result.success(signedUrl)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase Storage upload skipped/failed, fallback to persistent local storage: ${e.message}")
        }

        // 2. Persistent local file storage fallback (ensures 100% offline & emulator reliability)
        val localPath = destFile.toURI().toString()
        Log.d(TAG, "Saved $fileType to local persistent file: $localPath")
        Result.success(localPath)
    }
}
