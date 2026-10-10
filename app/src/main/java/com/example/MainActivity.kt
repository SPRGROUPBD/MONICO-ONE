package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.repository.MonicoRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class ScreenState {
    data object Dashboard : ScreenState()
    data class ContentViewer(val title: String, val url: String) : ScreenState()
    data object AdminPanel : ScreenState()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: MonicoRepository
    private val notificationIntentTrigger = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MonicoRepository(applicationContext)

        if (intent?.getBooleanExtra("open_notifications", false) == true) {
            notificationIntentTrigger.value = true
        }

        setContent {
            val prefs = remember { getSharedPreferences("monico_app_settings", Context.MODE_PRIVATE) }
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember {
                mutableStateOf(prefs.getBoolean("is_dark_mode", systemDark))
            }

            val currentUser by repository.currentUser.collectAsState()
            var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Dashboard) }
            var showSplash by remember { mutableStateOf(true) }

            var loginLoading by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }

            // Active image viewer modal state
            var viewerTitle by remember { mutableStateOf("") }
            var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
            var isImageViewerOpen by remember { mutableStateOf(false) }

            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            // Notification permission request on startup (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        NotificationHelper.initNotificationChannel(applicationContext)
                    }
                }
                LaunchedEffect(Unit) {
                    val status = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (status != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Notification tap trigger listener
            val shouldOpenNotifsFromIntent by notificationIntentTrigger.collectAsState()
            var triggerNotificationsDialog by remember { mutableStateOf(false) }

            LaunchedEffect(shouldOpenNotifsFromIntent) {
                if (shouldOpenNotifsFromIntent) {
                    currentScreen = ScreenState.Dashboard
                    triggerNotificationsDialog = true
                    notificationIntentTrigger.value = false
                }
            }

            // Initial sync if logged in
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    repository.syncAll()
                }
            }

            var lastBackPressedTime by remember { mutableLongStateOf(0L) }

            // Handle 2-tap back press to close app ONLY on root Dashboard or Login (and not when modal viewer is open)
            BackHandler(enabled = !showSplash && !isImageViewerOpen && (currentUser == null || currentScreen is ScreenState.Dashboard)) {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedTime = now
                    Toast.makeText(context, "অ্যাপ বন্ধ করতে আবার ব্যাক চাপুন", Toast.LENGTH_SHORT).show()
                }
            }

            // Dismiss image viewer if open on back press
            BackHandler(enabled = isImageViewerOpen) {
                isImageViewerOpen = false
            }

            // Handle back press from sub-screens
            BackHandler(enabled = !showSplash && !isImageViewerOpen && currentUser != null && currentScreen !is ScreenState.Dashboard) {
                currentScreen = ScreenState.Dashboard
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen(
                            onFinish = {
                                showSplash = false
                            }
                        )
                    } else if (currentUser == null) {
                        LoginScreen(
                            isLoading = loginLoading,
                            errorMessage = loginError,
                            onLogin = { empId, pass ->
                                scope.launch {
                                    loginLoading = true
                                    loginError = null
                                    val result = repository.login(empId, pass)
                                    loginLoading = false
                                    if (result.isFailure) {
                                        loginError = result.exceptionOrNull()?.message ?: "লগইন ব্যর্থ হয়েছে!"
                                    }
                                }
                            }
                        )
                    } else {
                        when (val screen = currentScreen) {
                            is ScreenState.Dashboard -> {
                                DashboardScreen(
                                    repository = repository,
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = {
                                        isDarkMode = !isDarkMode
                                        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
                                    },
                                    onOpenUrl = { title, url ->
                                        currentScreen = ScreenState.ContentViewer(title, url)
                                    },
                                    onOpenImageViewer = { title, images ->
                                        viewerTitle = title
                                        viewerImages = images
                                        isImageViewerOpen = true
                                    },
                                    onOpenAdminPanel = {
                                        currentScreen = ScreenState.AdminPanel
                                    },
                                    initialShowNotifications = triggerNotificationsDialog
                                )
                            }
                            is ScreenState.ContentViewer -> {
                                ContentViewerScreen(
                                    title = screen.title,
                                    url = screen.url,
                                    onBack = { currentScreen = ScreenState.Dashboard }
                                )
                            }
                            is ScreenState.AdminPanel -> {
                                AdminPanelScreen(
                                    repository = repository,
                                    onBack = { currentScreen = ScreenState.Dashboard }
                                )
                            }
                        }

                        // Fullscreen Image Gallery Modal
                        if (isImageViewerOpen) {
                            ImageViewerDialog(
                                title = viewerTitle,
                                images = viewerImages,
                                onDismiss = { isImageViewerOpen = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_notifications", false)) {
            notificationIntentTrigger.value = true
        }
    }
}
