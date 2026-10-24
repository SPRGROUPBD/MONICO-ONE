package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NotificationItem
import com.example.data.model.User
import com.example.ui.theme.MonicoAmber
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoDarkBlue
import com.example.ui.theme.MonicoGreen

@Composable
fun NotificationsDialog(
    currentUser: User?,
    notifications: List<NotificationItem>,
    readNotificationIds: Set<String>,
    onDismiss: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onSendNotification: (title: String, message: String, targetRole: String, type: String, targetUrl: String) -> Unit,
    onOpenUrl: ((title: String, url: String) -> Unit)? = null
) {
    val empId = currentUser?.employeeId?.uppercase() ?: ""
    val userRole = currentUser?.role?.uppercase() ?: "MPO"
    val isSuperAdmin = userRole == "SUPER_ADMIN" || userRole == "ADMIN"

    // Filter notifications relevant to current user
    val userNotifications = remember(notifications, userRole) {
        notifications.filter { notif ->
            notif.targetRole.equals("ALL", ignoreCase = true) ||
                    notif.targetRole.contains(userRole, ignoreCase = true)
        }
    }

    var showSendDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("notifications_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MonicoBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MonicoBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "নোটিফিকেশন সেন্টার",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val unreadCount = userNotifications.count {
                                !readNotificationIds.contains(it.id) && (it.readBy[empId] != true)
                            }
                            Text(
                                text = if (unreadCount > 0) "$unreadCount টি নতুন নোটিফিকেশন অমীমাংসিত" else "সকল নোটিফিকেশন পঠিত",
                                fontSize = 12.sp,
                                color = if (unreadCount > 0) MonicoBlue else MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(14.dp))

                // Actions row: Mark all as read & (if Super Admin) Send notification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onMarkAllAsRead,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("mark_all_read_btn")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সব পঠিত করুন", fontSize = 12.sp)
                    }

                    if (isSuperAdmin) {
                        Button(
                            onClick = { showSendDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                            modifier = Modifier.testTag("admin_send_notif_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন পাঠান", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Notifications List
                if (userNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "কোনো নোটিফিকেশন নেই",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(userNotifications, key = { it.id }) { notif ->
                            val isRead = readNotificationIds.contains(notif.id) || (notif.readBy[empId] == true)
                            NotificationCardItem(
                                notification = notif,
                                isRead = isRead,
                                onClick = {
                                    if (!isRead) {
                                        onMarkAsRead(notif.id)
                                    }
                                    if (notif.targetUrl.isNotBlank() && onOpenUrl != null) {
                                        onDismiss()
                                        onOpenUrl(notif.title, notif.targetUrl)
                                    }
                                },
                                onOpenFile = if (notif.targetUrl.isNotBlank() && onOpenUrl != null) {
                                    {
                                        if (!isRead) onMarkAsRead(notif.id)
                                        onDismiss()
                                        onOpenUrl(notif.title, notif.targetUrl)
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSendDialog) {
        SendNotificationDialog(
            onDismiss = { showSendDialog = false },
            onSend = { title, message, targetRole, type, targetUrl ->
                onSendNotification(title, message, targetRole, type, targetUrl)
                showSendDialog = false
            }
        )
    }
}

@Composable
fun NotificationCardItem(
    notification: NotificationItem,
    isRead: Boolean,
    onClick: () -> Unit,
    onOpenFile: (() -> Unit)? = null
) {
    val (typeColor, typeIcon, typeLabel) = when (notification.type.lowercase()) {
        "urgent" -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Warning, "জরুরি")
        "update" -> Triple(MonicoAmber, Icons.Default.Campaign, "আপডেট")
        "offer" -> Triple(MonicoGreen, Icons.Default.CheckCircle, "অফার")
        "notice" -> Triple(MonicoBlue, Icons.Default.Campaign, "নোটিশ")
        else -> Triple(MonicoBlue, Icons.Default.Info, "তথ্য")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (!isRead) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(typeColor.copy(alpha = 0.5f))
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = typeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        }
                    }

                    if (notification.targetRole != "ALL") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = notification.targetRole,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Unread dot / Status
                if (!isRead) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MonicoBlue)
                    )
                } else {
                    Text(
                        text = "পঠিত",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = notification.title,
                fontWeight = if (!isRead) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (notification.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Attached URL / file button
            if (notification.targetUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MonicoBlue.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { onOpenFile?.invoke() ?: onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MonicoBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "সংযুক্ত ফাইল/নোটিশ খুলুন (Open Attachment)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonicoBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Date and Sender
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (notification.dateFormatted.isNotBlank()) notification.dateFormatted else "সম্প্রতি",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (notification.senderName.isNotBlank()) {
                    Text(
                        text = "প্রেরক: ${notification.senderName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationDialog(
    onDismiss: () -> Unit,
    onSend: (title: String, message: String, targetRole: String, type: String, targetUrl: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("ALL") }
    var type by remember { mutableStateOf("info") }
    var targetUrl by remember { mutableStateOf("") }
    var targetExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val roleOptions = listOf(
        "ALL" to "সকল ব্যবহারকারী (All Users)",
        "MPO" to "শুধুমাত্র MPO",
        "AM" to "শুধুমাত্র AM",
        "RSM" to "শুধুমাত্র RSM",
        "PMD" to "শুধুমাত্র PMD",
        "DIC" to "শুধুমাত্র DIC"
    )

    val typeOptions = listOf(
        "info" to "সাধারণ তথ্য (Information)",
        "urgent" to "জরুরি নোটিশ (Urgent Alert)",
        "update" to "সিস্টেম আপডেট (System Update)",
        "offer" to "নতুন অফার / স্কিম (Offer/Scheme)",
        "notice" to "অফিস সার্কুলার (Circular)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("send_notification_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ব্যবহারকারীদের নোটিফিকেশন পাঠান",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorText = null },
                    label = { Text("নোটিফিকেশনের শিরোনাম (Title)*") },
                    placeholder = { Text("যেমন: নতুন পলিসি সার্কুলার...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notif_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; errorText = null },
                    label = { Text("বিস্তারিত বিবরণ / বার্তা (Message)*") },
                    placeholder = { Text("সকল ফিল্ড ফোর্সকে নির্দেশিকা দেখার জন্য...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .testTag("notif_message_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetUrl,
                    onValueChange = { targetUrl = it },
                    label = { Text("সংযুক্ত ফাইল বা রিপোর্ট URL (ঐচ্ছিক)") },
                    placeholder = { Text("https://example.com/doc.pdf বা go:order") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notif_url_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Target Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = !targetExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = roleOptions.find { it.first == targetRole }?.second ?: targetRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("প্রাপক (Target Audience)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false }
                    ) {
                        roleOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp) },
                                onClick = {
                                    targetExpanded = false
                                    targetRole = key
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = typeOptions.find { it.first == type }?.second ?: type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("নোটিফিকেশনের ধরন (Category)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp) },
                                onClick = {
                                    typeExpanded = false
                                    type = key
                                }
                            )
                        }
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            targetExpanded = false
                            typeExpanded = false
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorText = "অনুগ্রহ করে শিরোনাম লিখুন।"
                            } else if (message.isBlank()) {
                                errorText = "অনুগ্রহ করে বার্তা বিবরণ লিখুন।"
                            } else {
                                targetExpanded = false
                                typeExpanded = false
                                onSend(title.trim(), message.trim(), targetRole, type, targetUrl.trim())
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        modifier = Modifier.testTag("submit_send_notif_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাঠান")
                    }
                }
            }
        }
    }
}
