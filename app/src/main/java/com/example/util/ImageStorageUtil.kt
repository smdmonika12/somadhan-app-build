package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// [SUPABASE-MIGRATED - ধাপ ১৫] Firebase Storage এর জায়গায় Supabase Storage ব্যবহার শুরু হলো।
// profile-photos bucket পাবলিক (Supabase project এ আগে থেকেই তৈরি), তাই আপলোডের পর সরাসরি
// publicUrl() দিয়ে স্থায়ী URL পাওয়া যায় (Firebase downloadUrl.await() এর সমতুল্য)।

object ImageStorageUtil {
    private const val TAG = "ImageStorageUtil"

    /**
     * Efficiently compresses and downsamples an image from a content URI to a destination File.
     * Prevents OutOfMemoryErrors, reduces multi-MB photos down to ~150-300KB with crystal clear visual quality.
     */
    fun compressImageToFile(
        context: Context,
        sourceUri: Uri,
        destFile: File,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): Boolean {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight
            while (w > maxDimension || h > maxDimension) {
                w /= 2
                h /= 2
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return false

            val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val scale = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                val scaledW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val scaledH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val res = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
                if (res != bitmap) bitmap.recycle()
                res
            } else {
                bitmap
            }

            FileOutputStream(destFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            scaledBitmap.recycle()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Image compression failed: ${e.message}", e)
            false
        }
    }

    /**
     * [SUPABASE-MIGRATED - ধাপ ১৫] কম্প্রেস করা প্রোফাইল ছবিটা Supabase Storage এর
     * `profile-photos` bucket এ `{userId}/profile_{timestamp}.jpg` পাথে আপলোড করে (৪-সেকেন্ড
     * টাইমআউটসহ)। `userId` এখানে অবশ্যই বর্তমান লগইন-করা ইউজারের নিজের id (Supabase auth.uid()
     * এর সমান, dual-row architecture-এর established pattern অনুযায়ী) — bucket এর RLS পলিসি
     * (`profile_photos_owner_write`) ঠিক এটাই দাবি করে (path এর প্রথম ফোল্ডার = auth.uid())।
     * Supabase Storage আনরিচেবল/ব্যর্থ হলে আগের মতোই persistent internal app storage এ fallback
     * করে — আচরণ অপরিবর্তিত।
     */
    suspend fun uploadProfilePhoto(
        context: Context,
        userId: String,
        sourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        val profileDir = File(context.filesDir, "profile_photos").apply { if (!exists()) mkdirs() }
        val fileName = "profile_${userId}_${UUID.randomUUID()}.jpg"
        val destFile = File(profileDir, fileName)

        // 1. First compress image locally to reduce size by up to 95%
        val compressionSuccess = compressImageToFile(context, sourceUri, destFile, maxDimension = 1024, quality = 80)
        if (!compressionSuccess) {
            // Direct stream copy fallback
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext Result.failure(IllegalStateException("ছবি রিড করা সম্ভব হয়নি"))
                inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        val localUri = Uri.fromFile(destFile)

        try {
            val publicUrl = withTimeoutOrNull(4000L) {
                val bucket = SupabaseClientProvider.client.storage.from("profile-photos")
                val remotePath = "$userId/profile_${System.currentTimeMillis()}.jpg"
                val bytes = destFile.readBytes()
                bucket.upload(remotePath, bytes) { upsert = true }
                bucket.publicUrl(remotePath)
            }
            if (!publicUrl.isNullOrBlank()) {
                Log.d(TAG, "Uploaded compressed profile photo to Supabase Storage: $publicUrl")
                return@withContext Result.success(publicUrl)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase Storage upload failed, falling back to local: ${e.message}")
        }

        // Persistent local fallback
        Log.d(TAG, "Saved profile photo to local storage fallback: $localUri")
        Result.success(localUri.toString())
    }

    /**
     * Synchronous helper to copy & compress image to internal app storage.
     */
    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val fileName = "profile_${UUID.randomUUID()}.jpg"
            val destFile = File(context.filesDir, fileName)
            if (compressImageToFile(context, sourceUri, destFile, maxDimension = 1024, quality = 80)) {
                Uri.fromFile(destFile).toString()
            } else {
                val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
                inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile).toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Defensive check: verifies if an image URI is valid and accessible on this device.
     * If it is a local file:// URI from another device that does not exist in local storage, returns false.
     */
    fun isValidDisplayUri(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        if (uriString.startsWith("file://")) {
            return try {
                val path = Uri.parse(uriString).path
                path != null && File(path).exists()
            } catch (_: Exception) {
                false
            }
        }
        return true
    }
}
