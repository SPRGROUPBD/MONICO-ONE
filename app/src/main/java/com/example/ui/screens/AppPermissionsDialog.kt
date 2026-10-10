package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.MonicoAmber
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoGreen
import com.example.ui.theme.MonicoRed

data class PermissionItemState(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>,
    val isGranted: Boolean
)

@Composable
fun AppPermissionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    fun checkPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    val isLocationGranted = remember(refreshTrigger) {
        checkPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                checkPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val isNotificationGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    val isCameraGranted = remember(refreshTrigger) {
        checkPermissionGranted(Manifest.permission.CAMERA)
    }

    val isFilesImagesGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            checkPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    fun requestAllPermissions() {
        val listToRequest = mutableListOf<String>()
        listToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        listToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        listToRequest.add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            listToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        multiplePermissionsLauncher.launch(listToRequest.toTypedArray())
    }

    val allGranted = isLocationGranted && isNotificationGranted && isCameraGranted && isFilesImagesGranted

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 440.dp)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("app_permissions_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MonicoBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MonicoBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "অ্যাপ অনুমতি (App Permissions)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "কাজের সুবিধার্থে নিচের অনুমতিগুলো দিন",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Overall Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (allGranted) MonicoGreen.copy(alpha = 0.12f) else MonicoAmber.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (allGranted) MonicoGreen else MonicoAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (allGranted) "সকল প্রয়োজনীয় অনুমতি সক্রিয় রয়েছে।"
                            else "কিছু অনুমতি বাকি আছে। ফুল ফিচারের জন্য অনুমতি দিন।",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (allGranted) MonicoGreen else Color(0xFFD97706)
                        )
                    }
                }

                // Permission Item 1: Location
                PermissionCardRow(
                    title = "লোকেশন (Location)",
                    subtitle = "ফিল্ড ট্যুর, সেলস অর্ডার এবং লোকেশন ভেরিফিকেশনের জন্য",
                    icon = Icons.Default.LocationOn,
                    iconColor = MonicoBlue,
                    isGranted = isLocationGranted,
                    onRequest = {
                        multiplePermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                // Permission Item 2: Notification
                PermissionCardRow(
                    title = "নোটিফিকেশন (Notification)",
                    subtitle = "জরুরি সার্কুলার, নোটিশ, টিং সাউন্ড ও পপআপ বার্তার জন্য",
                    icon = Icons.Default.NotificationsActive,
                    iconColor = Color(0xFF8B5CF6),
                    isGranted = isNotificationGranted,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                            )
                        } else {
                            refreshTrigger++
                        }
                    }
                )

                // Permission Item 3: Camera
                PermissionCardRow(
                    title = "ক্যামেরা (Camera)",
                    subtitle = "প্রোফাইল ছবি তোলা ও প্রেসক্রিপশন বা রিপোর্ট স্ক্যান করার জন্য",
                    icon = Icons.Default.PhotoCamera,
                    iconColor = MonicoRed,
                    isGranted = isCameraGranted,
                    onRequest = {
                        multiplePermissionsLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA)
                        )
                    }
                )

                // Permission Item 4: Files and Images
                PermissionCardRow(
                    title = "ফাইল ও ছবি (Files & Images)",
                    subtitle = "পিডিএফ রিপোর্ট, সার্কুলার ও গ্যালারির ছবি ব্যবহারের জন্য",
                    icon = Icons.Default.FolderOpen,
                    iconColor = MonicoGreen,
                    isGranted = isFilesImagesGranted,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                            )
                        } else {
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Request All button & App Settings button
                if (!allGranted) {
                    Button(
                        onClick = { requestAllPermissions() },
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("grant_all_permissions_btn")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "একসঙ্গে সব অনুমতি দিন (Allow All)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("open_app_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ডিভাইস সেটিংস খুলুন (App Settings)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCardRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MonicoGreen.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MonicoGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "অনুমোদিত",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonicoGreen
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "অনুমতি দিন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
