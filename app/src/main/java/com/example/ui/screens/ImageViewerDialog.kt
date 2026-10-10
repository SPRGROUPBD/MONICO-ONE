package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoGreen
import com.example.ui.theme.MonicoRed
import com.example.utils.ImageUtils
import com.example.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageViewerDialog(
    title: String,
    images: List<String>,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    if (images.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val rawUrl = images.getOrNull(currentIndex) ?: ""
    val isPdf = remember(rawUrl) { PdfUtils.isPdf(rawUrl) }

    // State for rendered PDF pages if it is a PDF
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isPdfLoading by remember { mutableStateOf(false) }
    var pdfLoadError by remember { mutableStateOf(false) }
    var pdfPageIndex by remember { mutableIntStateOf(0) }

    // When a PDF is encountered, render its pages
    LaunchedEffect(rawUrl, isPdf) {
        if (isPdf) {
            isPdfLoading = true
            pdfLoadError = false
            pdfPages = emptyList()
            pdfPageIndex = 0
            try {
                val rendered = PdfUtils.renderPdfPages(context, rawUrl)
                if (rendered.isNotEmpty()) {
                    pdfPages = rendered
                } else {
                    pdfLoadError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pdfLoadError = true
            } finally {
                isPdfLoading = false
            }
        }
    }

    // Reset zoom when switching page
    LaunchedEffect(currentIndex, pdfPageIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val totalPages = if (isPdf && pdfPages.isNotEmpty()) pdfPages.size else images.size
    val activePageIndex = if (isPdf && pdfPages.isNotEmpty()) pdfPageIndex else currentIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2020617))
                .testTag("image_viewer_dialog")
        ) {
            // Header bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color(0xE60F172A),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isPdf) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MonicoRed
                                ) {
                                    Text(
                                        text = "PDF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (totalPages > 1) {
                            Text(
                                text = "পৃষ্ঠা ${activePageIndex + 1} / $totalPages",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open with external app button (Works for both PDF via FileProvider and Web URLs)
                        if (isPdf) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        PdfUtils.openPdfWithExternalApp(context, rawUrl)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Open in PDF App",
                                    tint = MonicoRed
                                )
                            }
                        } else if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Open in browser",
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MonicoRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Image or PDF Content with Zoom and Pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPdf) {
                    if (isPdfLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MonicoRed)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("PDF লোড ও পেজ রেন্ডার করা হচ্ছে...", color = Color.White, fontSize = 13.sp)
                        }
                    } else if (pdfPages.isNotEmpty()) {
                        val currentBitmap = pdfPages.getOrNull(pdfPageIndex)
                        if (currentBitmap != null) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "$title PDF page ${pdfPageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        }
                    } else {
                        // PDF could not render directly as bitmaps, show open externally button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = MonicoRed,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "PDF ফাইলটি প্রস্তুত রয়েছে",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "আপনার পছন্দের PDF ভিউয়ারে দেখতে নিচের বোতামে চাপুন।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        PdfUtils.openPdfWithExternalApp(context, rawUrl)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PDF অ্যাপে খুলুন (Open in PDF App)")
                            }
                        }
                    }
                } else {
                    // Regular Image (Supports HTTP URL, content URI, and base64 ByteArray)
                    val resolvedModel = remember(rawUrl) { ImageUtils.resolveImageModel(rawUrl) }

                    SubcomposeAsyncImage(
                        model = resolvedModel,
                        contentDescription = "$title page ${currentIndex + 1}",
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MonicoBlue)
                            }
                        },
                        error = {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MonicoRed, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ছবিটি লোড হতে সমস্যা হয়েছে",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (rawUrl.startsWith("http")) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("ব্রাউজারে খুলুন")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            // Bottom Navigation Controls
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color(0xE60F172A),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (totalPages > 1) {
                        FilledTonalButton(
                            onClick = {
                                if (isPdf && pdfPages.isNotEmpty()) {
                                    if (pdfPageIndex > 0) pdfPageIndex--
                                    else pdfPageIndex = pdfPages.size - 1
                                } else {
                                    if (currentIndex > 0) currentIndex--
                                    else currentIndex = images.size - 1
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous page"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("আগের পেজ", fontSize = 12.sp)
                        }

                        Text(
                            text = "${activePageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        FilledTonalButton(
                            onClick = {
                                if (isPdf && pdfPages.isNotEmpty()) {
                                    if (pdfPageIndex < pdfPages.size - 1) pdfPageIndex++
                                    else pdfPageIndex = 0
                                } else {
                                    if (currentIndex < images.size - 1) currentIndex++
                                    else currentIndex = 0
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            )
                        ) {
                            Text("পরের পেজ", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next page"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("বন্ধ করুন (Close)")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
