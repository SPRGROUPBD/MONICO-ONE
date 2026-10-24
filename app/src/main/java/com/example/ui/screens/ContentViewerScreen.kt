package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoRed
import com.example.utils.PdfUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ContentViewerScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isPdf = remember(url) {
        PdfUtils.isPdf(url)
    }

    // --- PDF INLINE VIEWER STATE ---
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isPdfLoading by remember { mutableStateOf(isPdf) }
    var pdfLoadError by remember { mutableStateOf(false) }
    var pdfReloadKey by remember { mutableIntStateOf(0) }
    var pdfScale by remember { mutableFloatStateOf(1f) }
    var pdfOffsetX by remember { mutableFloatStateOf(0f) }
    var pdfOffsetY by remember { mutableFloatStateOf(0f) }
    val pdfListState = rememberLazyListState()

    // Load PDF pages natively using PdfRenderer
    LaunchedEffect(url, isPdf, pdfReloadKey) {
        if (isPdf) {
            isPdfLoading = true
            pdfLoadError = false
            pdfPages = emptyList()
            pdfScale = 1f
            pdfOffsetX = 0f
            pdfOffsetY = 0f
            try {
                val rendered = PdfUtils.renderPdfPages(context, url, maxPages = 40)
                if (rendered.isNotEmpty()) {
                    pdfPages = rendered
                    pdfLoadError = false
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

    // --- WEBVIEW STATE (Used when not PDF) ---
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isWebLoading by remember { mutableStateOf(true) }
    var webProgress by remember { mutableIntStateOf(0) }
    var webHasError by remember { mutableStateOf(false) }

    var uploadMessage by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Camera / storage permissions granted or denied
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        var uris: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                uris = Array(count) { i -> data.clipData!!.getItemAt(i).uri }
            } else if (data?.data != null) {
                uris = arrayOf(data.data!!)
            } else if (cameraCaptureUri != null) {
                uris = arrayOf(cameraCaptureUri!!)
            }
        }
        uploadMessage?.onReceiveValue(uris)
        uploadMessage = null
        cameraCaptureUri = null
    }

    BackHandler {
        if (!isPdf && webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    fun openInExternalApp() {
        scope.launch {
            if (isPdf) {
                PdfUtils.openPdfWithExternalApp(context, url)
            } else if (url.startsWith("http://") || url.startsWith("https://")) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (isPdf) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MonicoRed
                                ) {
                                    Text(
                                        text = "PDF ভিউয়ার",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                if (pdfPages.isNotEmpty()) {
                                    Text(
                                        text = "${pdfPages.size}টি পেজ",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isPdf) {
                        // PDF Zoom Controls in TopBar
                        if (pdfScale > 1.05f) {
                            IconButton(onClick = {
                                pdfScale = 1f
                                pdfOffsetX = 0f
                                pdfOffsetY = 0f
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FitScreen,
                                    contentDescription = "Fit to Screen",
                                    tint = MonicoBlue
                                )
                            }
                        }
                        IconButton(onClick = {
                            pdfScale = (pdfScale - 0.5f).coerceAtLeast(1f)
                            if (pdfScale <= 1.02f) {
                                pdfOffsetX = 0f
                                pdfOffsetY = 0f
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = if (pdfScale > 1f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = {
                            pdfScale = (pdfScale + 0.5f).coerceAtMost(6f)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            pdfReloadKey++
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload PDF",
                                tint = MonicoBlue
                            )
                        }
                        // Secondary convenience action: Open in external viewer if desired
                        IconButton(onClick = { openInExternalApp() }) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "অন্য অ্যাপে খুলুন",
                                tint = MonicoRed
                            )
                        }
                    } else {
                        // Webview Actions
                        IconButton(onClick = { webViewInstance?.zoomOut() }) {
                            Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }
                        IconButton(onClick = { webViewInstance?.zoomIn() }) {
                            Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }
                        IconButton(onClick = {
                            webHasError = false
                            webViewInstance?.reload()
                        }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MonicoBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                if (isPdf) {
                    // PDF Bottom Bar with Zoom Pill & Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Reset / Fit Button
                        OutlinedButton(
                            onClick = {
                                pdfScale = 1f
                                pdfOffsetX = 0f
                                pdfOffsetY = 0f
                                scope.launch { pdfListState.animateScrollToItem(0) }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("শুরুতে (Top)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Floating-style Zoom Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        pdfScale = (pdfScale - 0.4f).coerceAtLeast(1f)
                                        if (pdfScale <= 1.02f) {
                                            pdfOffsetX = 0f
                                            pdfOffsetY = 0f
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${(pdfScale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pdfScale > 1.05f) MonicoRed else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clickable {
                                            pdfScale = 1f
                                            pdfOffsetX = 0f
                                            pdfOffsetY = 0f
                                        }
                                        .padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        pdfScale = (pdfScale + 0.4f).coerceAtMost(6f)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Share or Open Externally button
                        Button(
                            onClick = { openInExternalApp() },
                            colors = ButtonDefaults.buttonColors(containerColor = MonicoRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("অন্য অ্যাপে", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Regular WebView Bottom Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                webHasError = false
                                webViewInstance?.reload()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "রিফ্রেশ করুন (Refresh)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("content_viewer_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isPdf) {
                // ==========================================
                // NATIVE IN-APP PDF DOCUMENT RENDERER
                // ==========================================
                if (isPdfLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MonicoRed.copy(alpha = 0.1f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MonicoRed,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator(
                                color = MonicoRed,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "PDF পেজ লোড ও রেন্ডার করা হচ্ছে...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করুন",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else if (pdfLoadError || pdfPages.isEmpty()) {
                    // PDF Error state with Retry and fallback
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MonicoRed,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "PDF ডকুমেন্টটি লোড করা যায়নি",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ফাইলটি ক্ষতিগ্রস্ত হতে পারে অথবা ইন্টারনেটে সংযোগ পেতে সমস্যা হচ্ছে। পুনরায় চেষ্টা করুন অথবা সরাসরি ফোনে খুলুন।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { pdfReloadKey++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("পুনরায় চেষ্টা করুন")
                                }
                                OutlinedButton(
                                    onClick = { openInExternalApp() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = MonicoRed)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ফোনের অ্যাপে খুলুন", color = MonicoRed)
                                }
                            }
                        }
                    }
                } else {
                    // Rendered PDF Pages in continuous scrollable list
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF1F5F9))
                            .clipToBounds()
                    ) {
                        val containerWidth = constraints.maxWidth.toFloat()
                        val containerHeight = constraints.maxHeight.toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pdfScale) {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            if (pdfScale > 1.2f) {
                                                pdfScale = 1f
                                                pdfOffsetX = 0f
                                                pdfOffsetY = 0f
                                            } else {
                                                pdfScale = 2.5f
                                                val centerX = containerWidth / 2f
                                                val centerY = containerHeight / 2f
                                                val maxPanX = (containerWidth * 1.5f) / 2f
                                                val maxPanY = (containerHeight * 1.5f) / 2f
                                                pdfOffsetX = ((centerX - tapOffset.x) * 1.5f).coerceIn(-maxPanX, maxPanX)
                                                pdfOffsetY = ((centerY - tapOffset.y) * 1.5f).coerceIn(-maxPanY, maxPanY)
                                            }
                                        }
                                    )
                                }
                                .pointerInput(pdfScale) {
                                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                                        val oldScale = pdfScale
                                        val targetScale = (pdfScale * zoom).coerceIn(1f, 6f)
                                        pdfScale = targetScale
                                        if (targetScale > 1.01f) {
                                            val maxPanX = (containerWidth * (targetScale - 1f)) / 2f
                                            val maxPanY = (containerHeight * (targetScale - 1f)) / 2f
                                            val newOffsetX = pdfOffsetX * (targetScale / oldScale) + pan.x
                                            val newOffsetY = pdfOffsetY * (targetScale / oldScale) + pan.y
                                            pdfOffsetX = newOffsetX.coerceIn(-maxPanX, maxPanX)
                                            pdfOffsetY = newOffsetY.coerceIn(-maxPanY, maxPanY)
                                        } else {
                                            pdfOffsetX = 0f
                                            pdfOffsetY = 0f
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = pdfScale
                                    scaleY = pdfScale
                                    translationX = pdfOffsetX
                                    translationY = pdfOffsetY
                                }
                        ) {
                            LazyColumn(
                                state = pdfListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(pdfPages) { index, pageBitmap ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            // Page Header Badge
                                            Surface(
                                                color = Color(0xFFF8FAFC),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Article,
                                                            contentDescription = null,
                                                            tint = MonicoRed,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "পৃষ্ঠা ${index + 1} / ${pdfPages.size}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF334155)
                                                        )
                                                    }
                                                    if (pdfScale > 1.05f) {
                                                        Text(
                                                            text = "জুম: ${(pdfScale * 100).toInt()}%",
                                                            fontSize = 10.sp,
                                                            color = MonicoBlue,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }

                                            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                                            // Rendered PDF Page Image
                                            Image(
                                                bitmap = pageBitmap.asImageBitmap(),
                                                contentDescription = "PDF Page ${index + 1}",
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // WEBVIEW VIEWER (HTML, Portals, & Links)
                // ==========================================
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                            setDownloadListener { downloadUrl, _, _, mimetype, _ ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    ctx.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isWebLoading = true
                                    webHasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isWebLoading = false
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                    val errorCode = error?.errorCode ?: 0
                                    if (request?.isForMainFrame == true &&
                                        (errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                                         errorCode == WebViewClient.ERROR_CONNECT ||
                                         errorCode == WebViewClient.ERROR_TIMEOUT ||
                                         errorCode == WebViewClient.ERROR_FAILED_SSL_HANDSHAKE)) {
                                        webHasError = true
                                        isWebLoading = false
                                    }
                                }

                                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                    webHasError = true
                                    isWebLoading = false
                                    try {
                                        view?.let {
                                            (it.parent as? ViewGroup)?.removeView(it)
                                            it.destroy()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    webViewInstance = null
                                    return true
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: ""
                                    if (reqUrl.startsWith("go:")) {
                                        val resolved = resolveGoUrl(reqUrl)
                                        view?.loadUrl(resolved)
                                        return true
                                    }
                                    if (!reqUrl.startsWith("http://") && !reqUrl.startsWith("https://")) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress
                                    if (newProgress >= 100) {
                                        isWebLoading = false
                                    }
                                }

                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    try {
                                        request?.grant(request.resources)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    uploadMessage?.onReceiveValue(null)
                                    uploadMessage = filePathCallback

                                    // Request camera and storage permissions if needed
                                    val permsToRequest = mutableListOf<String>()
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                        permsToRequest.add(Manifest.permission.CAMERA)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                                            permsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                                        }
                                    } else {
                                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                            permsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                    if (permsToRequest.isNotEmpty()) {
                                        permissionLauncher.launch(permsToRequest.toTypedArray())
                                    }

                                    // Camera Intent
                                    val photoFile = try {
                                        File.createTempFile("camera_upload_", ".jpg", ctx.cacheDir)
                                    } catch (e: Exception) {
                                        null
                                    }

                                    val cameraIntents = mutableListOf<Intent>()
                                    if (photoFile != null) {
                                        val photoUri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", photoFile)
                                        cameraCaptureUri = photoUri
                                        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        cameraIntents.add(takePhotoIntent)
                                    }

                                    // Gallery / Files Intent
                                    val isMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                                    val acceptTypes = fileChooserParams?.acceptTypes
                                    val mimeType = if (!acceptTypes.isNullOrEmpty() && acceptTypes[0].isNotBlank()) acceptTypes[0] else "image/*"

                                    val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = mimeType
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultiple)
                                    }

                                    val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                                        putExtra(Intent.EXTRA_INTENT, contentIntent)
                                        putExtra(Intent.EXTRA_TITLE, "ছবি বেছে নিন অথবা ক্যামেরা দিয়ে তুলুন")
                                        if (cameraIntents.isNotEmpty()) {
                                            putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
                                        }
                                    }

                                    try {
                                        fileChooserLauncher.launch(chooserIntent)
                                    } catch (e: Exception) {
                                        uploadMessage?.onReceiveValue(null)
                                        uploadMessage = null
                                        cameraCaptureUri = null
                                        return false
                                    }
                                    return true
                                }
                            }

                            loadUrl(url)
                            webViewInstance = this
                        }
                    },
                    update = { webView ->
                        webViewInstance = webView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isWebLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = MonicoBlue
                    )
                }

                // Error UI for Web Pages
                if (webHasError) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MonicoRed,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "পৃষ্ঠাটি ইন-অ্যাপে লোড করা যাচ্ছে না",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ইন্টারনেট সংযোগ চেক করুন অথবা সরাসরি ব্রাউজারে খুলুন।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { openInExternalApp() },
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ব্রাউজারে খুলুন")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = {
                                webHasError = false
                                webViewInstance?.reload()
                            }) {
                                Text("পুনরায় চেষ্টা করুন (Retry)")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveGoUrl(url: String): String {
    val clean = url.trim()
    return when (clean) {
        "go:order" -> "http://103.153.241.44/monicoapp"
        "go:Tour" -> "https://sprgroupbd.github.io/SmartManager/monicoone.html"
        "go:delivery" -> "https://sprgroupbd.github.io/SmartManager/depo_bill.html"
        "go:bill" -> "https://sprgroupbd.github.io/SmartManager/admin"
        "go:att1" -> "https://sprgroupbd.github.io/SmartManager/att.html"
        "go:dcc" -> "https://sprgroupbd.github.io/SmartManager/dcs.html"
        else -> {
            if (clean.startsWith("go:")) {
                "https://sprgroupbd.github.io/SmartManager/" + clean.removePrefix("go:").trim()
            } else {
                clean
            }
        }
    }
}
