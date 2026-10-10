package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a data:image base64 string or raw base64 to a Bitmap so Coil and Compose
     * can render it reliably and instantly without fetcher errors.
     */
    fun resolveImageModel(data: String): Any {
        if (data.isBlank()) return ""
        val s = data.trim()
        if (s.startsWith("data:image", ignoreCase = true) || s.startsWith("/9j/") || s.startsWith("iVBORw")) {
            val base64Str = if (s.contains("base64,")) s.substringAfter("base64,") else s
            return try {
                val bytes = Base64.decode(base64Str, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap ?: java.nio.ByteBuffer.wrap(bytes)
            } catch (e: Exception) {
                s
            }
        }
        return s
    }
}
