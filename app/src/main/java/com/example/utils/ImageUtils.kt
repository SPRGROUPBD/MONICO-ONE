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
     * Converts a data:image base64 string to a ByteArray so Coil can render it reliably and quickly.
     */
    fun resolveImageModel(data: String): Any {
        if (data.isBlank()) return ""
        val s = data.trim()
        if (s.startsWith("data:image", ignoreCase = true)) {
            val idx = s.indexOf("base64,")
            if (idx != -1) {
                val base64Str = s.substring(idx + 7)
                return try {
                    Base64.decode(base64Str, Base64.DEFAULT)
                } catch (e: Exception) {
                    s
                }
            }
        }
        return s
    }
}
