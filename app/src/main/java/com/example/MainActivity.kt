package com.example

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.MonicoRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class ScreenState {
    data object Dashboard : ScreenState()
    data class ContentViewer(val title: String, val url: String) : ScreenState()
    data object AdminPanel : ScreenState()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: MonicoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MonicoRepository(applicationContext)

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

            // Initial sync if logged in
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    repository.syncAll()
                }
            }

            val context = LocalContext.current
            var lastBackPressedTime by remember { mutableLongStateOf(0L) }

            // Handle 2-tap back press to close app on Dashboard or Login
            BackHandler(enabled = !showSplash && (currentUser == null || currentScreen is ScreenState.Dashboard)) {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedTime = now
                    Toast.makeText(context, "অ্যাপ বন্ধ করতে আবার ব্যাক চাপুন", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle back press from sub-screens
            BackHandler(enabled = !showSplash && currentUser != null && currentScreen !is ScreenState.Dashboard) {
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
                                    }
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
}
