package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.remote.AppConfigState
import com.example.data.remote.VersionComparator
import com.example.ui.theme.*

/**
 * Opens an external store page or update URL safely.
 */
fun openUpdateUrl(context: Context, url: String) {
    try {
        val targetUri = if (url.isNotBlank()) Uri.parse(url) else Uri.parse("https://play.google.com/store/apps/details?id=com.triplanai.app")
        val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.triplanai.app")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (err: Exception) {
            Toast.makeText(context, "Could not open update link.", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Sleek, responsive update dialog supporting both optional and mandatory (forced) update flows.
 */
@Composable
fun AppUpdateDialog(
    configState: AppConfigState,
    isMandatory: Boolean,
    onDismiss: () -> Unit,
    onRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .wrapContentHeight()
                .testTag("app_update_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = ArtCardBackground,
            border = BorderStroke(1.5.dp, ArtBorderDark),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(ArtSoftLavender, ArtSecondaryPurple)
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, ArtBorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMandatory) Icons.Default.Warning else Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = if (isMandatory) Color(0xFFD97706) else ArtPrimaryPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title and Version Badges
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TripPlanAI Update Available",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = ArtTextDark,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ArtSecondaryPurple,
                            border = BorderStroke(1.dp, ArtPrimaryPurple.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Current: v${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArtPrimaryPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = ArtGrayMuted,
                            modifier = Modifier.size(12.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = "New: v${configState.latestVersion}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isMandatory) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "Required Update to Continue",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Update Message Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ArtSecondaryPurple.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, ArtBorderDark.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = configState.updateMessage,
                        fontSize = 13.sp,
                        color = ArtTextDark,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                // Checking status indicator or feedback
                if (configState.isCheckingForUpdate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArtSecondaryPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = ArtPrimaryPurple
                        )
                        Text(
                            text = "Checking Remote Config...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ArtPrimaryPurple
                        )
                    }
                } else if (!configState.checkStatusFeedback.isNullOrBlank()) {
                    Text(
                        text = configState.checkStatusFeedback,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ArtGrayMuted,
                        textAlign = TextAlign.Center
                    )
                }

                // Action Buttons
                if (isMandatory) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onRefresh?.invoke() },
                            enabled = !configState.isCheckingForUpdate,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("update_refresh_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, ArtBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ArtTextDark
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Check Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                openUpdateUrl(context, configState.updateUrl)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("update_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArtPrimaryPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Update Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("update_later_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, ArtBorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ArtTextDark
                                )
                            ) {
                                Text("Later", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { onRefresh?.invoke() },
                                enabled = !configState.isCheckingForUpdate,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("update_refresh_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, ArtBorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ArtTextDark
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text("Check Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                openUpdateUrl(context, configState.updateUrl)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("update_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArtPrimaryPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Update Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen, responsive Maintenance Screen shown when maintenance_mode = true in Remote Config.
 */
@Composable
fun MaintenanceScreen(
    configState: AppConfigState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("maintenance_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Maintenance Visual Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(ArtSoftLavender, ArtSecondaryPurple)
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, ArtBorderDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = "Maintenance",
                    tint = ArtPrimaryPurple,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Maintenance Status Chip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFFCD34D))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFD97706), CircleShape)
                    )
                    Text(
                        text = "MAINTENANCE MODE ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF92400E),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Title
            Text(
                text = "TripPlanAI is temporarily unavailable",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = ArtTextDark,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            // Maintenance Message Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArtCardBackground,
                border = BorderStroke(1.5.dp, ArtBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ArtPrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "STATUS NOTICE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArtPrimaryPurple,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = configState.maintenanceMessage,
                        fontSize = 14.sp,
                        color = ArtTextDark,
                        lineHeight = 20.sp
                    )

                    HorizontalDivider(color = ArtBorderDark.copy(alpha = 0.3f))

                    Text(
                        text = "AI travel generation and network services are paused while our systems are upgraded. Please check back shortly.",
                        fontSize = 12.sp,
                        color = ArtGrayMuted,
                        lineHeight = 17.sp
                    )
                }
            }

            // Retry Button
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("maintenance_retry_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArtPrimaryPurple,
                    contentColor = Color.White
                ),
                enabled = !configState.isCheckingForUpdate
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (configState.isCheckingForUpdate) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Checking Status...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Check Status & Refresh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
