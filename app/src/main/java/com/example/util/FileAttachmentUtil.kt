package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

data class ProcessedAttachment(
    val uriString: String,
    val fileName: String,
    val fileType: String, // "image", "document", "file"
    val sizeBytes: Long,
    val formattedSize: String
)

object FileAttachmentUtil {
    private const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024L // 15 MB

    fun processAttachment(context: Context, sourceUri: Uri): Result<ProcessedAttachment> {
        return try {
            val contentResolver = context.contentResolver
            var rawFileName = "attachment_${UUID.randomUUID().toString().take(8)}"
            var fileSize = 0L

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) rawFileName = name
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Fallback for file size
            if (fileSize <= 0L) {
                fileSize = contentResolver.openAssetFileDescriptor(sourceUri, "r")?.use {
                    it.length
                } ?: 0L
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return Result.failure(Exception("ফাইলের আকার সর্বোচ্চ ১৫ মেগাবাইট (15MB) হতে পারবে।"))
            }

            // Determine mime type and file extension
            val mimeType = contentResolver.getType(sourceUri)
                ?: getMimeTypeFromFileName(rawFileName)
                ?: "application/octet-stream"

            val fileType = when {
                mimeType.startsWith("image/") || isImageExtension(rawFileName) -> "image"
                mimeType.startsWith("application/pdf") || mimeType.contains("word") ||
                mimeType.contains("text") || mimeType.contains("document") ||
                isDocumentExtension(rawFileName) -> "document"
                else -> "file"
            }

            val extension = getExtension(rawFileName).ifBlank {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "dat"
            }

            val targetFileName = "chat_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension"
            val chatDir = File(context.filesDir, "chat_attachments").apply { if (!exists()) mkdirs() }
            val destFile = File(chatDir, targetFileName)

            var copied = false
            if (fileType == "image") {
                copied = ImageStorageUtil.compressImageToFile(context, sourceUri, destFile, maxDimension = 1280, quality = 80)
            }
            if (!copied) {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return Result.failure(Exception("ফাইলটি কপি করা যায়নি।"))
            }

            val finalUri = Uri.fromFile(destFile).toString()
            val formatted = formatFileSize(destFile.length().takeIf { it > 0 } ?: fileSize)

            Result.success(
                ProcessedAttachment(
                    uriString = finalUri,
                    fileName = rawFileName,
                    fileType = fileType,
                    sizeBytes = destFile.length(),
                    formattedSize = formatted
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isImageExtension(name: String): Boolean {
        val ext = getExtension(name).lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
    }

    private fun isDocumentExtension(name: String): Boolean {
        val ext = getExtension(name).lowercase()
        return ext in listOf("pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx", "ppt", "pptx", "csv")
    }

    private fun getExtension(name: String): String {
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < name.length - 1) {
            name.substring(dotIndex + 1)
        } else ""
    }

    private fun getMimeTypeFromFileName(name: String): String? {
        val ext = getExtension(name)
        return if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        } else null
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "০ KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            "${DistanceUtil.toBengaliDigits(String.format(java.util.Locale.US, "%.1f", mb))} MB"
        } else {
            "${DistanceUtil.toBengaliDigits(kb.toInt().toString())} KB"
        }
    }

    fun openFile(context: Context, fileUriString: String, fileName: String? = null) {
        try {
            val uri = Uri.parse(fileUriString)
            val file = if (uri.scheme == "file") {
                File(uri.path ?: "")
            } else null

            val viewUri = if (file != null && file.exists()) {
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (_: Exception) {
                    uri
                }
            } else {
                uri
            }

            val mime = fileName?.let { getMimeTypeFromFileName(it) } ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(viewUri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "ফাইল খুলুন"))
        } catch (e: Exception) {
            // If cannot open, ignore or toast
        }
    }
}
