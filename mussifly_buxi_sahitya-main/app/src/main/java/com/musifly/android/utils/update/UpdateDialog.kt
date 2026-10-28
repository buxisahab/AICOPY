package com.musifly.android.utils.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.musifly.android.R

@Composable
fun UpdateDialog(
    updateData: UpdateData,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit
) {
    val context = LocalContext.current
    val brandOrange = Color(0xFFFF5722)
    val surfaceColor = Color(0xFF121212) // Midnight Black for premium feel

    Dialog(
        onDismissRequest = {
            if (!updateData.forceUpdate) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateData.forceUpdate,
            dismissOnClickOutside = !updateData.forceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = surfaceColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Highly Refined Header (Precision Color Match for Lottie) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFFEBF4FF)), // Refined light blue to match rocket's cloud/circle
                    contentAlignment = Alignment.Center
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.sahitya_rc))
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )
                    
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    )
                }

                // --- Content Body ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Update Available",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "v${updateData.latestVersionName}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = brandOrange,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Release Notes Container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = updateData.updateDescription
                                    .replace(Regex("Note\\s*:\\s*the apk is being downloaded from the official github repository\\s*,\\s*not from f-\\s*droid", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("Note\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                                    .trim(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 20.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // --- Dynamic Actions ---
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!updateData.forceUpdate) {
                            // Glass Cancel Button
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onDismiss() },
                                color = Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Later",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                        
                        // Vivid Update Button
                        Surface(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { 
                                     onUpdateClick()
                                     try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                        intent.data = android.net.Uri.parse(updateData.downloadUrl)
                                        context.startActivity(intent)
                                     } catch (e: Exception) {
                                        android.util.Log.e("UpdateDialog", "Failed to open download URL", e)
                                     }
                                },
                            color = brandOrange,
                            contentColor = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Update Now",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
