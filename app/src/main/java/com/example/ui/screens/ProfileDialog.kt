package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.theme.MonicoRed

@Composable
fun ProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onUpdateProfile: (name: String, newPassword: String) -> Unit,
    onLogout: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf(user.fullName) }
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passError by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

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

                // Profile Avatar & Role
                Box(contentAlignment = Alignment.Center) {
                    if (user.profilePic.isNotBlank() && (user.profilePic.startsWith("http") || user.profilePic.startsWith("data:"))) {
                        AsyncImage(
                            model = user.profilePic,
                            contentDescription = user.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.monico_one_icon_1791604475434),
                            contentDescription = "Profile Pic",
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                        )
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

                // Actions: Edit Name
                AnimatedVisibility(visible = !isEditingName && !isChangingPassword) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        onUpdateProfile(newName, "")
                                        isEditingName = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("সংরক্ষণ")
                            }
                        }
                    }
                }

                // Change Password Form
                AnimatedVisibility(visible = isChangingPassword) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "পাসওয়ার্ড পরিবর্তন:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        if (passError != null) {
                            Text(
                                text = passError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedTextField(
                            value = oldPass,
                            onValueChange = { oldPass = it },
                            placeholder = { Text("বর্তমান পাসওয়ার্ড") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            placeholder = { Text("নতুন পাসওয়ার্ড") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            placeholder = { Text("নতুন পাসওয়ার্ড নিশ্চিত করুন") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    isChangingPassword = false
                                    passError = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("বাতিল")
                            }
                            Button(
                                onClick = {
                                    if (oldPass != user.password) {
                                        passError = "বর্তমান পাসওয়ার্ড ভুল!"
                                    } else if (newPass.length < 4) {
                                        passError = "নতুন পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে!"
                                    } else if (newPass != confirmPass) {
                                        passError = "পাসওয়ার্ড দুটি মিলছে না!"
                                    } else {
                                        onUpdateProfile(user.fullName, newPass)
                                        isChangingPassword = false
                                        passError = null
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoAmber),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("আপডেট", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Logout confirmation dialog
                if (showLogoutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showLogoutConfirm = false },
                        title = { Text("লগআউট নিশ্চিতকরণ") },
                        text = { Text("আপনি কি নিশ্চিত আপনার অ্যাকাউন্ট থেকে লগআউট করতে চান?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLogoutConfirm = false
                                    onDismiss()
                                    onLogout()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MonicoRed)
                            ) {
                                Text("লগআউট")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutConfirm = false }) {
                                Text("ক্যানসেল")
                            }
                        }
                    )
                }
            }
        }
    }
}
