package com.musifly.android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.musifly.android.BuildConfig
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.utils.backToMain
import java.util.Calendar
import com.musifly.android.secure.SecureString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showCustomAmountDialog by remember { mutableStateOf(false) }
    var showPaymentGateway by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("50") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                // scrollBehavior = scrollBehavior, // Pinned toolbar
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlassActive()) Color.Transparent else MaterialTheme.colorScheme.background,
                    scrolledContainerColor = if (isLiquidGlassActive()) Color.Transparent else MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { paddingValues ->
        val isLiquidGlass = isLiquidGlassActive()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Logo with shadow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.sahitya_h),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "MusiFly",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Version
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your personal music companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Developer Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp), // Shifted to left
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val avatarUrl = SecureString.s(SecureString.LINK_GITHUB_AVATAR)
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(avatarUrl)
                                .allowHardware(false)
                                .build(),
                            contentDescription = "Developer Avatar",
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { showAvatarDialog = true },
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Developed by",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "SAHITYA",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Social Links - Using FlowRow to prevent clipping on small screens
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        SocialButton(
                            icon = R.drawable.instagram,
                            label = "Instagram",
                            onClick = { SecureString.openLink(context, SecureString.LINK_INSTA_WEB) }
                        )
                        SocialButton(
                            icon = R.drawable.telegram,
                            label = "Telegram",
                            onClick = { SecureString.openLink(context, SecureString.LINK_TELEGRAM) }
                        )
                        SocialButton(
                            icon = R.drawable.language,
                            label = "Website",
                            onClick = { SecureString.openLink(context, SecureString.LINK_OFFICIAL_WEBSITE) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Special Thanks Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Special Thanks",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CreditTile(
                        name = "MussiFly",
                        role = "Official Music Project",
                        icon = R.drawable.sahitya_h
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "Shahdat Buxi",
                        role = "Ui Enhancer",
                        icon = R.drawable.person
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    CreditTile(
                        name = "Aadith",
                        role = "Helper",
                        icon = R.drawable.telegram
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "OpenTune",
                        role = "Open Source Project",
                        icon = R.drawable.language
                    )



                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Beta Testers",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "JPGRS_Gaming09",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "Sujal kanojiya",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    CreditTile(
                        name = "Aryan Morya",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "Sumit Prajapati",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )
                    Spacer(modifier = Modifier.height(12.dp))



                    CreditTile(
                        name = "Ayush kanojiya",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    CreditTile(
                        name = "Shuvohowlader",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "Ayan Sorkar",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "Talha Shaikh",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CreditTile(
                        name = "Dhruv603",
                        role = "Beta Tester",
                        icon = R.drawable.person
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Support Section Title
            Text(
                text = "Support Development",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your support helps keep MusiFly free and ad-free",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // UPI Payment Button (India)
            val context = LocalContext.current
            Button(
                onClick = {
                    // Show custom amount dialog
                    showCustomAmountDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.payments),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Support via UPI",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // PayPal Button (International)
            Button(
                onClick = { SecureString.openLink(context, SecureString.LINK_PAYPAL_ME) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.paypal),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Support via PayPal",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Footer
            Text(
                text = "© $currentYear MussiFly Team",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Avatar Preview Dialog
        if (showAvatarDialog) {
            Dialog(
                onDismissRequest = { showAvatarDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(SecureString.s(SecureString.LINK_GITHUB_AVATAR))
                            .allowHardware(false)
                            .build(),
                        contentDescription = "Full Size Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        // Custom Amount Dialog for UPI Payment
        if (showCustomAmountDialog) {
            Dialog(
                onDismissRequest = { showCustomAmountDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Support Development",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan with any UPI App",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Professional QR Box
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.sahitya_pay_qr),
                                contentDescription = "Payment QR",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Copy UPI ID
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val upiId = "sahitya000@ptyes"
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f))
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(upiId))
                                    Toast.makeText(context, "UPI ID Copied", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.copy_all),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = upiId,
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Close Button
                        Button(
                            onClick = { showCustomAmountDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun SocialButton(
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when(label) {
                            "Instagram" -> Color(0xFFE1306C).copy(alpha = 0.1f)
                            "Telegram" -> Color(0xFF0088CC).copy(alpha = 0.1f)
                            "Website" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when(label) {
                        "Instagram" -> Color(0xFFE1306C)
                        "Telegram" -> Color(0xFF0088CC)
                        "Website" -> Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CreditTile(
    name: String,
    role: String,
    icon: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when(icon) {
                        R.drawable.sahitya_h -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        R.drawable.person -> Color(0xFF673AB7).copy(alpha = 0.1f)
                        R.drawable.telegram -> Color(0xFF0088CC).copy(alpha = 0.1f)
                        R.drawable.language -> Color(0xFF2196F3).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when(icon) {
                    R.drawable.sahitya_h -> Color(0xFFFF9800)
                    R.drawable.person -> Color(0xFF673AB7)
                    R.drawable.telegram -> Color(0xFF0088CC)
                    R.drawable.language -> Color(0xFF2196F3)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
