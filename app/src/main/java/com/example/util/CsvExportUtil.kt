package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object CsvExportUtil {

    /**
     * Exports data to a CSV file in the Downloads folder or external files directory.
     * Returns true if successful, false otherwise.
     */
    fun exportToCsv(
        context: Context,
        fileName: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Boolean {
        return try {
            val csvContent = buildString {
                // BOM for UTF-8 so spreadsheet software handles Bangla characters correctly
                append("\uFEFF")
                // Headers
                append(headers.joinToString(separator = ",") { escapeCsvCell(it) })
                append("\n")
                // Rows
                rows.forEach { row ->
                    append(row.joinToString(separator = ",") { escapeCsvCell(it) })
                    append("\n")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    true
                } else {
                    writeToAppExternalDownloads(context, fileName, csvContent)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val csvContent = buildString {
                    append("\uFEFF")
                    append(headers.joinToString(",") { escapeCsvCell(it) }).append("\n")
                    rows.forEach { row ->
                        append(row.joinToString(",") { escapeCsvCell(it) }).append("\n")
                    }
                }
                writeToAppExternalDownloads(context, fileName, csvContent)
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    private fun writeToAppExternalDownloads(context: Context, fileName: String, content: String): Boolean {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
        }
        return true
    }

    private fun escapeCsvCell(cell: String): String {
        var formatted = cell.replace("\"", "\"\"")
        if (formatted.contains(",") || formatted.contains("\"") || formatted.contains("\n") || formatted.contains("\r")) {
            formatted = "\"$formatted\""
        }
        return formatted
    }
}
