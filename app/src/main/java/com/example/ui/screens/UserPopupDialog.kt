package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.PopupNotice
import com.example.ui.theme.MonicoAmber
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoGreen
import com.example.utils.ImageUtils

@Composable
fun UserPopupDialog(
    popup: PopupNotice,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .testTag("user_popup_dialog"),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // If image exists, show image
                if (popup.image.isNotBlank()) {
                    AsyncImage(
                        model = ImageUtils.resolveImageModel(popup.image),
                        contentDescription = popup.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    val isProduct = popup.type == "product"
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isProduct) MonicoGreen.copy(alpha = 0.15f) else MonicoAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isProduct) Icons.Default.RocketLaunch else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isProduct) MonicoGreen else MonicoAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = popup.title.ifEmpty { "জরুরি ঘোষণা" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )

                if (popup.body.isNotBlank()) {
                    Text(
                        text = popup.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("বুঝেছি (OK)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
