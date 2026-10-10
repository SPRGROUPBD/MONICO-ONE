package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    val isPdf = remember(url) {
        val lower = url.lowercase()
        lower.endsWith(".pdf") ||
                lower.contains(".pdf?") ||
                lower.contains("type=pdf") ||
                lower.contains("/pdf") ||
                url.startsWith("data:application/pdf", ignoreCase = true)
    }

    val scope = rememberCoroutineScope()

    // Prepare URL for WebView: if PDF and HTTP, route via Google Docs Viewer
    val effectiveUrl = remember(url, isPdf) {
        if (isPdf && !url.contains("docs.google.com") && (url.startsWith("http://") || url.startsWith("https://"))) {
            "https://docs.google.com/gview?embedded=true&url=${Uri.encode(url)}"
        } else {
            url
        }
    }

    fun openInExternalApp() {
        scope.launch {
            try {
                if (isPdf) {
                    val opened = PdfUtils.openPdfWithExternalApp(context, url)
                    if (opened) return@launch
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                            Text(
                                text = "PDF Document Viewer",
                                fontSize = 11.sp,
                                color = MonicoRed,
                                fontWeight = FontWeight.SemiBold
                            )
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
                    IconButton(onClick = {
                        hasError = false
                        webViewInstance?.reload()
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MonicoBlue)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Only Refresh button as requested
                    Button(
                        onClick = {
                            hasError = false
                            webViewInstance?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
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

                        setDownloadListener { downloadUrl, _, _, _, _ ->
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
                                isLoading = true
                                hasError = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                val errorCode = error?.errorCode ?: 0
                                // Only show full-screen error on fatal connection/network failures for the main frame
                                if (request?.isForMainFrame == true && 
                                    (errorCode == WebViewClient.ERROR_HOST_LOOKUP || 
                                     errorCode == WebViewClient.ERROR_CONNECT || 
                                     errorCode == WebViewClient.ERROR_TIMEOUT || 
                                     errorCode == WebViewClient.ERROR_FAILED_SSL_HANDSHAKE)) {
                                    hasError = true
                                    isLoading = false
                                }
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
                                progress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }

                        loadUrl(effectiveUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = if (isPdf) MonicoRed else MonicoBlue
                )
            }

            // Error Recovery State UI
            if (hasError) {
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
                            text = "ফাইল বা পেজটি ইন-অ্যাপে সরাসরি লোড করা যাচ্ছে না",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPdf)
                                "PDF ডকুমেন্টটি ফোনের ড্রাইভ বা পিডিএফ অ্যাপে সরাসরি দেখতে নিচের বাটনে ক্লিক করুন।"
                            else
                                "পৃষ্ঠাটি ব্রাউজারে খুলতে নিচের বাটনে ক্লিক করুন।",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { openInExternalApp() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPdf) MonicoRed else MonicoBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPdf) "সরাসরি PDF অ্যাপে খুলুন" else "ব্রাউজারে খুলুন")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = {
                            hasError = false
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
