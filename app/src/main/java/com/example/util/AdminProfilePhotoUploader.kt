package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.remote.SupabaseAuthManager
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * [ADMIN_ROLE_PROFILE সেশন ৬] এডমিন প্রোফাইল ছবি → Supabase Storage `admin-profile-photos` (public) bucket।
 *
 * ⚠️ বিদ্যমান [ImageStorageUtil.uploadProfilePhoto]-এর সাথে ইচ্ছাকৃত পার্থক্য: ওটা আপলোড ব্যর্থ হলে লোকাল
 * `file://` URI ফেরত দেয় (সাধারণ ইউজারের নিজের ফোনে দেখানোর জন্য ঠিক আছে)। এডমিনের ছবির URL সার্ভারে
 * (`admin_accounts.photo_url`) সেভ হয় আর **অন্য এডমিনের/সুপারের ফোনে** দেখা যায় — `file://` সেখানে ভাঙা।
 * তাই এখানে কোনো লোকাল fallback নেই: ব্যর্থ হলে `Result.failure` (কোড: PHOTO_*), ভিউ কারণসহ জানায় ও ছবি
 * ছাড়া কিছুই সেভ হয় না।
 *
 * পাথ: `{আপলোডকারীর auth.uid()}/photo_{timestamp}.jpg` — bucket-এর RLS ঠিক এটাই দাবি করে (প্রথম ফোল্ডার =
 * `auth.uid()`; migration `zz_20260925120000_admin_profile_photos_session6.sql`)। ধ্রুবক নাম থাকায় upsert
 * লাগে না (`upsert = false`)। সুপার অন্যের ছবি বদলালেও ফাইল সুপারের ফোল্ডারে যায়।
 * ⚠️ পুরনো ছবির ফাইল মোছা হয় না (URL থেকে path বের করে delete করা এই সেশনের স্কোপে না) — ছোট ফাইল, ঝুঁকি কম।
 */
object AdminProfilePhotoUploader {
    private const val TAG = "AdminProfilePhotoUpl"
    private const val BUCKET = "admin-profile-photos"
    private const val UPLOAD_TIMEOUT_MS = 20_000L
    private const val MAX_BYTES = 2 * 1024 * 1024 // bucket-এর file_size_limit-এর সমান

    suspend fun upload(context: Context, sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val uid = SupabaseAuthManager.currentUserId()
            ?: return@withContext Result.failure(IllegalStateException("AUTH_REQUIRED"))
        val tmp = File(context.cacheDir, "admin_photo_${System.currentTimeMillis()}.jpg")
        try {
            val ok = ImageStorageUtil.compressImageToFile(context, sourceUri, tmp, maxDimension = 1024, quality = 82)
            if (!ok || !tmp.exists() || tmp.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("PHOTO_READ_FAILED"))
            }
            val bytes = tmp.readBytes()
            if (bytes.size > MAX_BYTES) {
                return@withContext Result.failure(IllegalStateException("PHOTO_TOO_LARGE"))
            }
            val url = withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
                val bucket = SupabaseClientProvider.client.storage.from(BUCKET)
                val path = "$uid/photo_${System.currentTimeMillis()}.jpg"
                bucket.upload(path, bytes) { upsert = false }
                bucket.publicUrl(path)
            }
            if (url.isNullOrBlank()) {
                Result.failure(IllegalStateException("PHOTO_UPLOAD_TIMEOUT"))
            } else {
                Log.d(TAG, "Uploaded admin profile photo: $url")
                Result.success(url)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Admin profile photo upload failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            runCatching { tmp.delete() }
        }
    }
}
