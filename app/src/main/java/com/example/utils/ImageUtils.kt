package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {

    suspend fun uriToBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        quality: Int = 65
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return@withContext null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxWidth) {
                val newHeight = (height.toFloat() * (maxWidth.toFloat() / width.toFloat())).toInt()
                Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, maxWidth: Int = 600, quality: Int = 65): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxWidth) {
                val newHeight = (height.toFloat() * (maxWidth.toFloat() / width.toFloat())).toInt()
                Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
            } else {
                bitmap
            }
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    /**
     * Accurately determines if a string represents an image (data URI, base64 header, or image URL).
     * NEVER returns true for a PDF.
     */
    fun isImage(data: String): Boolean {
        if (data.isBlank()) return false
        val s = data.trim()
        if (s.startsWith("data:image/", ignoreCase = true)) return true
        if (s.startsWith("/9j/") || s.startsWith("iVBORw0KGgo") || s.startsWith("R0lGOD")) return true
        
        // Strip query params and hash for clean extension check
        val clean = s.substringBefore("?").substringBefore("#").lowercase()
        return clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") ||
               clean.endsWith(".webp") || clean.endsWith(".gif") || clean.endsWith(".bmp") || clean.endsWith(".svg")
    }

    /**
     * Converts a data:image base64 string or raw base64 to a Bitmap so Coil and Compose
     * can render it reliably and instantly without fetcher errors.
     */
    fun resolveImageModel(data: String): Any {
        if (data.isBlank()) return ""
        val s = data.trim()
        if (s.startsWith("file://")) {
            val path = Uri.parse(s).path ?: s.removePrefix("file://")
            val f = File(path)
            if (f.exists() && f.length() > 0) return f
        } else if (s.startsWith("/") && File(s).exists()) {
            return File(s)
        }
        if (s.startsWith("data:image", ignoreCase = true) || s.startsWith("/9j/") || s.startsWith("iVBORw") || s.startsWith("R0lGOD")) {
            val base64Str = if (s.contains("base64,")) s.substringAfter("base64,") else s
            return try {
                val bytes = Base64.decode(base64Str, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap ?: s
            } catch (t: Throwable) {
                s
            }
        }
        return s
    }
}
