package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object PdfUtils {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isPdf(source: String): Boolean {
        if (source.isBlank()) return false
        val s = source.trim()
        if (s.startsWith("data:application/pdf", ignoreCase = true)) return true
        if (s.startsWith("JVBERi", ignoreCase = false)) return true
        val lower = s.lowercase()
        return lower.endsWith(".pdf") ||
                lower.contains(".pdf?") ||
                lower.contains("type=pdf") ||
                lower.contains("/pdf")
    }

    suspend fun readPdfUriToBase64(context: Context, uri: Uri): Pair<String, Long>? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.readBytes()
            inputStream.close()
            if (bytes.isEmpty()) return@withContext null
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:application/pdf;base64,$base64" to bytes.size.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun preparePdfFile(context: Context, source: String): File? = withContext(Dispatchers.IO) {
        try {
            val s = source.trim()
            if (s.isBlank()) return@withContext null
            val outputFile = File(context.cacheDir, "preview_document.pdf")

            if (s.startsWith("data:application/pdf", ignoreCase = true)) {
                val base64Part = s.substringAfter("base64,")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                FileOutputStream(outputFile).use { it.write(bytes) }
                return@withContext outputFile
            } else if (s.startsWith("JVBERi", ignoreCase = false)) {
                val bytes = Base64.decode(s, Base64.DEFAULT)
                FileOutputStream(outputFile).use { it.write(bytes) }
                return@withContext outputFile
            } else if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
                val targetUrl = if (s.contains("drive.google.com/file/d/")) {
                    val fileId = s.substringAfter("/file/d/").substringBefore("/")
                    "https://drive.google.com/uc?export=download&id=$fileId"
                } else {
                    s
                }
                val req = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .build()
                val res = httpClient.newCall(req).execute()
                val body = res.body ?: return@withContext null
                val bytes = body.bytes()
                if (bytes.isNotEmpty()) {
                    FileOutputStream(outputFile).use { it.write(bytes) }
                    return@withContext outputFile
                }
            } else if (s.startsWith("content://") || s.startsWith("file://")) {
                val uri = Uri.parse(s)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                FileOutputStream(outputFile).use { out ->
                    inputStream.copyTo(out)
                }
                inputStream.close()
                return@withContext outputFile
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun renderPdfPages(context: Context, source: String, maxPages: Int = 15): List<Bitmap> = withContext(Dispatchers.IO) {
        val pdfFile = preparePdfFile(context, source) ?: return@withContext emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = minOf(renderer.pageCount, maxPages)
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val renderScale = 2.5f
                val width = (page.width * renderScale).toInt().coerceIn(720, 1800)
                val height = (page.height * renderScale).toInt().coerceIn(1000, 2600)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { renderer?.close() } catch (ignored: Exception) {}
            try { pfd?.close() } catch (ignored: Exception) {}
        }
        bitmaps
    }

    suspend fun openPdfWithExternalApp(context: Context, source: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val s = source.trim()
            val pdfFile = preparePdfFile(context, s)
            if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "PDF ডকুমেন্টটি খুলুন"))
                }
                true
            } else if (s.startsWith("http://") || s.startsWith("https://")) {
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
