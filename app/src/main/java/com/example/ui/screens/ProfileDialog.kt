package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.User
import com.example.ui.theme.MonicoAmber
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoGreen
import com.example.ui.theme.MonicoRed
import com.example.utils.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun ProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onUpdateProfile: (name: String, newPassword: String, newProfilePic: String?) -> Unit,
    onLogout: () -> Unit,
    onOpenPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isEditingName by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    var currentPic by remember(user.profilePic) { mutableStateOf(user.profilePic) }
    var isUploadingPic by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf(user.fullName) }
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passError by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingPic = true
                val base64 = ImageUtils.uriToBase64(context, it, maxWidth = 380, quality = 65)
                if (base64 != null) {
                    currentPic = base64
                    onUpdateProfile(user.fullName, "", base64)
                }
                isUploadingPic = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch {
                isUploadingPic = true
                val base64 = ImageUtils.bitmapToBase64(it, maxWidth = 380, quality = 65)
                if (base64 != null) {
                    currentPic = base64
                    onUpdateProfile(user.fullName, "", base64)
                }
                isUploadingPic = false
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .testTag("profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ব্যবহারকারী প্রোফাইল (Profile)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Profile Avatar & Camera button
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(3.dp, MonicoBlue, CircleShape)
                            .clickable { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentPic.isNotBlank()) {
                            AsyncImage(
                                model = ImageUtils.resolveImageModel(currentPic),
                                contentDescription = user.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.monico_one_icon_1791604475434),
                                contentDescription = "Profile Pic",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Floating camera badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MonicoBlue)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Edit photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isUploadingPic) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("ছবি আপলোড হচ্ছে...", fontSize = 11.sp, color = MonicoBlue)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = user.fullName.ifEmpty { user.employeeId },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "আইডি: ${user.employeeId}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Surface(
                        shape = CircleShape,
                        color = MonicoBlue.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "রোল: ${user.role}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MonicoBlue,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Actions: Edit Name, Change Photo, Change Password, Logout
                AnimatedVisibility(visible = !isEditingName && !isChangingPassword) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPhotoSourceDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MonicoGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("প্রোফাইল ছবি পরিবর্তন (ক্যামেরা / গ্যালারি)")
                        }

                        OutlinedButton(
                            onClick = { isEditingName = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MonicoBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("নাম পরিবর্তন করুন (Edit Name)")
                        }

                        OutlinedButton(
                            onClick = { isChangingPassword = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MonicoAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("পাসওয়ার্ড পরিবর্তন (Change Password)")
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenPermissions()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MonicoBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("অ্যাপ পারমিশন (App Permissions)")
                        }

                        Button(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MonicoRed.copy(alpha = 0.12f),
                                contentColor = MonicoRed
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("লগআউট (Logout)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Edit Name Form
                AnimatedVisibility(visible = isEditingName) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "আপনার পূর্ণ নাম লিখুন:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { isEditingName = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("বাতিল")
                            }
                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        onUpdateProfile(newName, "", null)
                                        isEditingName = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("সংরক্ষণ করুন")
                            }
                        }
                    }
                }

                // Change Password Form
                AnimatedVisibility(visible = isChangingPassword) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "পাসওয়ার্ড পরিবর্তন করুন:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedTextField(
                            value = oldPass,
                            onValueChange = { oldPass = it; passError = null },
                            label = { Text("বর্তমান পাসওয়ার্ড") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it; passError = null },
                            label = { Text("নতুন পাসওয়ার্ড") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it; passError = null },
                            label = { Text("নতুন পাসওয়ার্ড নিশ্চিত করুন") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        passError?.let {
                            Text(text = it, color = MonicoRed, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    isChangingPassword = false
                                    oldPass = ""
                                    newPass = ""
                                    confirmPass = ""
                                    passError = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("বাতিল")
                            }
                            Button(
                                onClick = {
                                    if (oldPass != user.password) {
                                        passError = "বর্তমান পাসওয়ার্ড সঠিক নয়।"
                                    } else if (newPass.length < 4) {
                                        passError = "নতুন পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে।"
                                    } else if (newPass != confirmPass) {
                                        passError = "উভয় পাসওয়ার্ড মেলেনি।"
                                    } else {
                                        onUpdateProfile(user.fullName, newPass, null)
                                        isChangingPassword = false
                                        oldPass = ""
                                        newPass = ""
                                        confirmPass = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("পরিবর্তন করুন")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("লগআউট নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই অ্যাকাউন্ট থেকে লগআউট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MonicoRed)
                ) {
                    Text("হ্যাঁ, লগআউট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("না")
                }
            }
        )
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("প্রোফাইল ছবি পরিবর্তন", fontWeight = FontWeight.Bold) },
            text = { Text("ছবি তোলার জন্য সরাসরি ক্যামেরা ব্যবহার করুন অথবা গ্যালারি/ফাইল থেকে বেছে নিন।") },
            confirmButton = {
                Button(
                    onClick = {
                        showPhotoSourceDialog = false
                        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ক্যামেরা (Camera)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPhotoSourceDialog = false
                        photoPickerLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("গ্যালারি/ফাইল")
                }
            }
        )
    }
}
