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
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

object PdfUtils {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun isPdf(source: String): Boolean {
        if (source.isBlank()) return false
        val s = source.trim()

        // 1. CRITICAL: If it is an image, it is NEVER a PDF
        if (ImageUtils.isImage(s)) return false
        if (s.startsWith("data:video/", ignoreCase = true) ||
            s.startsWith("data:audio/", ignoreCase = true) ||
            s.startsWith("data:text/", ignoreCase = true)) return false

        // 2. Explicit PDF base64 / data URI
        if (s.startsWith("data:application/pdf", ignoreCase = true)) return true
        if (s.startsWith("data:pdf", ignoreCase = true)) return true
        if (s.startsWith("JVBERi", ignoreCase = false)) return true
        if (s.contains(";base64,", ignoreCase = true) && s.substringBefore(";base64,").contains("pdf", ignoreCase = true)) return true

        // 3. Web / file URLs
        val cleanUrl = s.substringBefore("?").substringBefore("#").lowercase()
        if (cleanUrl.endsWith(".pdf")) return true

        if (s.startsWith("file://") || s.startsWith("/")) {
            val path = if (s.startsWith("file://")) Uri.parse(s).path ?: s.removePrefix("file://") else s
            if (path != null) {
                val f = File(path)
                if (f.name.endsWith(".pdf", ignoreCase = true)) return true
                if (f.exists() && f.length() > 0 && isValidPdfFile(f)) return true
            }
        }

        val lower = s.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            if (lower.contains(".pdf?") || lower.contains(".pdf#") || lower.contains("type=pdf") || lower.contains("format=pdf") || lower.contains(".pdf")) return true
            if (lower.contains("drive.google.com") && (lower.contains(".pdf") || lower.contains("mimetype=application/pdf") || lower.contains("/file/d/"))) return true
        }

        return false
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

            // Generate unique cache file based on content hash to prevent concurrent access locking
            val cacheKey = "pdf_" + (s.hashCode().toLong() and 0xFFFFFFFFL) + ".pdf"
            val outputFile = File(context.cacheDir, cacheKey)

            // If file already exists and is valid PDF (> 100 bytes and starts with %PDF), return directly
            if (outputFile.exists() && outputFile.length() > 100) {
                if (isValidPdfFile(outputFile)) {
                    return@withContext outputFile
                } else {
                    outputFile.delete()
                }
            }

            // 1. Base64 Data URI or raw Base64
            if (s.startsWith("data:", ignoreCase = true) || s.contains(";base64,") || s.startsWith("JVBERi")) {
                var base64Part = if (s.contains(";base64,")) {
                    s.substringAfter(";base64,").trim()
                } else if (s.startsWith("data:", ignoreCase = true)) {
                    s.substringAfter(",").trim()
                } else {
                    s.trim()
                }

                // Clean potential quotes, whitespace, and url-encoded characters safely without turning '+' into space
                base64Part = base64Part.removeSurrounding("\"", "\"").removeSurrounding("'", "'").trim()
                if (base64Part.contains("%2B", ignoreCase = true) || base64Part.contains("%2F", ignoreCase = true) || base64Part.contains("%3D", ignoreCase = true)) {
                    base64Part = base64Part
                        .replace("%2B", "+").replace("%2b", "+")
                        .replace("%2F", "/").replace("%2f", "/")
                        .replace("%3D", "=").replace("%3d", "=")
                }
                base64Part = base64Part.replace("\n", "").replace("\r", "").replace(" ", "")

                val bytes = try {
                    Base64.decode(base64Part, Base64.DEFAULT)
                } catch (e: Exception) {
                    try {
                        Base64.decode(base64Part, Base64.NO_WRAP)
                    } catch (e2: Exception) {
                        null
                    }
                }

                if (bytes != null && bytes.isNotEmpty()) {
                    FileOutputStream(outputFile).use { it.write(bytes) }
                    return@withContext outputFile
                }
            }

            // 2. Direct HTTP or HTTPS URL
            if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
                var targetUrl = s
                // Google Drive direct download link transformation
                if (s.contains("drive.google.com/file/d/")) {
                    val fileId = s.substringAfter("/file/d/").substringBefore("/")
                    targetUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
                } else if (s.contains("drive.google.com/open?id=")) {
                    val fileId = s.substringAfter("id=").substringBefore("&")
                    targetUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
                } else if (s.contains("dropbox.com") && s.contains("dl=0")) {
                    targetUrl = s.replace("dl=0", "dl=1")
                }

                val req = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "application/pdf,application/octet-stream,*/*")
                    .build()

                val res = httpClient.newCall(req).execute()
                val body = res.body ?: return@withContext null
                val bytes = body.bytes()

                if (bytes.isNotEmpty()) {
                    // Check if bytes are a PDF (Magic bytes "%PDF-")
                    if (bytes.size >= 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte()) {
                        FileOutputStream(outputFile).use { it.write(bytes) }
                        return@withContext outputFile
                    }

                    // If not starting at offset 0, search for %PDF within the first 1024 bytes (e.g. BOM or leading spaces)
                    val pdfHeaderIndex = findPdfHeader(bytes)
                    if (pdfHeaderIndex >= 0) {
                        FileOutputStream(outputFile).use { it.write(bytes, pdfHeaderIndex, bytes.size - pdfHeaderIndex) }
                        return@withContext outputFile
                    }

                    // Check if Google Drive returned a confirmation HTML page
                    val contentString = String(bytes.take(2048).toByteArray(), Charsets.UTF_8)
                    if (contentString.contains("drive.google.com") && contentString.contains("confirm=")) {
                        val confirmToken = contentString.substringAfter("confirm=").substringBefore("&").substringBefore("\"")
                        if (confirmToken.isNotBlank()) {
                            val retryUrl = "$targetUrl&confirm=$confirmToken"
                            val retryReq = Request.Builder().url(retryUrl).build()
                            val retryRes = httpClient.newCall(retryReq).execute()
                            val retryBytes = retryRes.body?.bytes()
                            if (retryBytes != null && retryBytes.isNotEmpty()) {
                                FileOutputStream(outputFile).use { it.write(retryBytes) }
                                return@withContext outputFile
                            }
                        }
                    }

                    // Fallback: write anyway and let PdfRenderer attempt parsing
                    FileOutputStream(outputFile).use { it.write(bytes) }
                    return@withContext outputFile
                }
            }

            // 3. Direct Local File or file:// URI
            if (s.startsWith("file://")) {
                val path = Uri.parse(s).path ?: s.removePrefix("file://")
                val f = File(path)
                if (f.exists() && f.canRead() && f.length() > 0) {
                    if (isValidPdfFile(f)) return@withContext f
                    if (f.name.endsWith(".dat")) {
                        val text = try { f.readText() } catch (_: Exception) { "" }
                        if (text.isNotBlank()) return@withContext preparePdfFile(context, text)
                    }
                }
            }

            // 4. Content URI
            if (s.startsWith("content://")) {
                val uri = Uri.parse(s)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                FileOutputStream(outputFile).use { out ->
                    inputStream.copyTo(out)
                }
                inputStream.close()
                return@withContext outputFile
            }

            // 5. Direct Local File path
            val directFile = File(s)
            if (directFile.exists() && directFile.canRead() && directFile.length() > 0) {
                if (isValidPdfFile(directFile)) return@withContext directFile
                if (directFile.name.endsWith(".dat")) {
                    val text = try { directFile.readText() } catch (_: Exception) { "" }
                    if (text.isNotBlank()) return@withContext preparePdfFile(context, text)
                }
                return@withContext directFile
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findPdfHeader(bytes: ByteArray): Int {
        val maxSearch = minOf(bytes.size - 4, 1024)
        for (i in 0 until maxSearch) {
            if (bytes[i] == 0x25.toByte() && // '%'
                bytes[i + 1] == 0x50.toByte() && // 'P'
                bytes[i + 2] == 0x44.toByte() && // 'D'
                bytes[i + 3] == 0x46.toByte()    // 'F'
            ) {
                return i
            }
        }
        return -1
    }

    private fun isValidPdfFile(file: File): Boolean {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount > 0
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renderPdfPages(
        context: Context,
        source: String,
        maxPages: Int = 30
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val pdfFile = preparePdfFile(context, source) ?: return@withContext emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = minOf(renderer.pageCount, maxPages)

            for (i in 0 until pageCount) {
                try {
                    val page = renderer.openPage(i)
                    // High-quality rendering: target width ~ 1080px to ensure crystal sharp text on mobile
                    val targetWidth = 1080
                    val scaleRatio = targetWidth.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scaleRatio).toInt().coerceIn(300, 3200)

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                } catch (oom: OutOfMemoryError) {
                    oom.printStackTrace()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                    try {
                        context.startActivity(Intent.createChooser(intent, "PDF ডকুমেন্টটি খুলুন"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "ডিভাইসে কোনো PDF রিডার অ্যাপ পাওয়া যায়নি", Toast.LENGTH_LONG).show()
                    }
                }
                true
            } else if (s.startsWith("http://") || s.startsWith("https://")) {
                withContext(Dispatchers.Main) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "ব্রাউজারে লিংকটি খোলা সম্ভব হচ্ছে না", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF ফাইলটি প্রস্তুত করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF খুলতে সমস্যা হয়েছে: ${e.localizedMessage ?: "ত্রুটি"}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
