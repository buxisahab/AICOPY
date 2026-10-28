package com.buxi.music.ui.screens

import com.buxi.music.utils.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buxi.music.R
import com.buxi.music.data.remote.UserProfile
import com.buxi.music.viewmodels.ProfileUiState
import com.buxi.music.viewmodels.ProfileViewModel

import com.buxi.music.LocalSyncUtils
import com.buxi.music.LocalPlayerAwareWindowInsets

// ──────────────────────────────────────────────────────────── ProfileTab
@Composable
fun ProfileTab(navController: NavController, vm: ProfileViewModel = hiltViewModel()) {
    val context   = LocalContext.current
    val state     by vm.state.collectAsStateWithLifecycle()
    val followers by vm.followers.collectAsStateWithLifecycle()
    val following by vm.following.collectAsStateWithLifecycle()
    val syncUtils = LocalSyncUtils.current

    LaunchedEffect(vm.currentUid) {
        if (vm.currentUid != null) {
            vm.ensureProfileSynced()
            vm.loadProfile()
            vm.loadSocialCounts()
            // Sync with YouTube Music library to ensure persistence
            try {
                syncUtils.syncLikedSongs()
                syncUtils.syncSavedPlaylists()
                syncUtils.syncLibrarySongs()
            } catch (e: Exception) {
                // Ignore sync errors here
            }
        }
    }
    LaunchedEffect(vm.toast) { vm.toast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    if (vm.currentUid == null) {
        // ── Guest / not-logged-in state ──────────────────────────────────────
        GuestProfileScreen(onLoginClick = { navController.navigate("login") })
    } else {
        // ── Logged-in state ──────────────────────────────────────────────────
        when (val s = state) {
            ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😞 ${s.msg}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navController.navigate("login") }) { Text("Sign In") }
                }
            }
            is ProfileUiState.Loaded -> ProfileContent(
                profile     = s.profile,
                followers   = followers,
                following   = following,
                onEditClick = { navController.navigate("edit_profile") },
                onSettingsClick = { navController.navigate("settings") },
                onSignOut   = { vm.signOut() },
                navController = navController
            )
        }
    }
}

// ──────────────────────────────────────────────────────────── GuestProfileScreen
@Composable
private fun GuestProfileScreen(onLoginClick: () -> Unit) {
    // Pulse animation for the avatar ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0A0F),
                        Color(0xFF111122),
                        Color(0xFF0D0D1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            // ── Animated avatar placeholder ──────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .drawBehind {
                        val colors = listOf(
                            Color(0xFFEA4335), Color(0xFFFBBC04),
                            Color(0xFF34A853), Color(0xFF4285F4)
                        )
                        val stroke = 4.dp.toPx()
                        val step = 360f / colors.size
                        colors.forEachIndexed { i, c ->
                            drawArc(
                                color = c.copy(alpha = glowAlpha),
                                startAngle = i * step, sweepAngle = step - 5f,
                                useCenter = false,
                                style = Stroke(width = stroke)
                            )
                        }
                    }
            ) {
                Surface(shape = CircleShape, modifier = Modifier.size(90.dp),
                    color = Color(0xFF1E1E30)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF4285F4).copy(0.7f))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Headlines ────────────────────────────────────────────────────
            Text(
                "Welcome to BuxiMusic",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to access your profile,\nfollowers, and personalized features.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.55f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(36.dp))

            // ── Login card ───────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF16162A),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Google sign-in button
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White
                        )
                    ) {
                        Image(
                            painter = painterResource(R.drawable.google),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }

                    // Divider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.1f))
                        Text("  or  ", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.35f))
                        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.1f))
                    }

                    // Email login button
                    OutlinedButton(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF4285F4).copy(0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Email, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Sign in with Email", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Feature list ─────────────────────────────────────────────────
            GuestFeatureRow(Icons.Filled.Favorite, "Save your liked songs & playlists")
            GuestFeatureRow(Icons.Filled.Person, "Follow artists and friends")
            GuestFeatureRow(Icons.Filled.Info, "Earn a verified creator badge")
        }
    }
}

@Composable
private fun GuestFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = Color(0xFF1E1E30), modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF4285F4), modifier = Modifier.size(16.dp))
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
    }
}

// ──────────────────────────────────────────────────────────── ProfileContent
@Composable
private fun ProfileContent(
    profile: UserProfile,
    followers: List<UserProfile>,
    following: List<UserProfile>,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOut: () -> Unit,
    navController: NavController
) {
    var showSocialList by remember { mutableStateOf<String?>(null) } // null, "followers", "following"
    var selectedFilter by remember { mutableStateOf("Playlists") }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = {
                Text(
                    "Log Out",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Are you sure you want to log out of your account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4D67),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = Color.White.copy(0.6f))
                }
            },
            containerColor = Color(0xFF1E1E28),
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) {
        // ── 1. Profile Header (Avatar + Action) ──────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar with Google-style colorful ring
                    GoogleRingAvatar(
                        avatarUrl = profile.avatar_url,
                        displayName = profile.display_name,
                        size = 90.dp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Name + Verification + Bio (Below Header/Avatar)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.display_name ?: profile.username ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        VerificationBadge(profile.badge_type, profile.is_verified)
                    }
                    
                    Text(
                        text = profile.email ?: "@${profile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (!profile.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
                
                // Header buttons (Settings + Logout)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLogoutConfirmation = true }
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    IconButton(
                        onClick = onSettingsClick
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // ── 2. Stats Row (Clickable) ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Followers Stat
                Surface(
                    onClick = { showSocialList = if (showSocialList == "followers") null else "followers" },
                    color = Color.Transparent
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(profile.followers_count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Followers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
                
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                
                // Following Stat
                Surface(
                    onClick = { showSocialList = if (showSocialList == "following") null else "following" },
                    color = Color.Transparent
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(profile.following_count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Following", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Edit profile button
                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // ── 3. Conditional Social Lists ───────────────────────────────────────
        if (showSocialList == "followers") {
            item {
                SocialBanner("Your Followers", followers)
            }
        }
        if (showSocialList == "following") {
            item {
                SocialBanner("People You Follow", following)
            }
        }

        // ── 4. Library Filter Chips ───────────────────────────────────────────
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Playlists", "Songs", "Albums", "Artists")
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }
        }

        // ── 5. Main Library Items ───────────────────────────────────────────
        item {
            LibraryOptionItem(
                icon = Icons.Filled.Favorite,
                title = "Liked Songs",
                subtitle = "Your favorite tracks",
                iconColor = Color(0xFFE91E63),
                onClick = { navController.navigate("auto_playlist/liked") }
            )
        }
        
        item {
            LibraryOptionItem(
                icon = Icons.Filled.CheckCircle,
                title = "Downloaded",
                subtitle = "Offline music",
                iconColor = Color(0xFF4CAF50),
                onClick = { navController.navigate("auto_playlist/downloaded") }
            )
        }

        item {
            LibraryOptionItem(
                icon = Icons.Filled.Star,
                title = "My Top 50",
                subtitle = "Most played songs",
                iconColor = Color(0xFFFFC107),
                onClick = { navController.navigate("top_playlist/50") }
            )
        }

        item {
            LibraryOptionItem(
                icon = Icons.Filled.List,
                title = "All Playlists",
                subtitle = "Your custom collections",
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate("library") }
            )
        }

        item {
            LibraryOptionItem(
                icon = Icons.Filled.ExitToApp,
                title = "Log Out",
                subtitle = "Sign out of your account",
                iconColor = Color(0xFFFF4D67),
                onClick = { showLogoutConfirmation = true }
            )
        }
    }
}

@Composable
private fun SocialBanner(title: String, users: List<UserProfile>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (users.isEmpty()) {
            Text("Nothing to show here yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users) { user -> UserPill(user) }
            }
        }
    }
}

@Composable
private fun LibraryOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
        }
    }
}

// ──────────────────────────────────────────────────────────── Avatar with Google ring
@Composable
fun GoogleRingAvatar(avatarUrl: String?, displayName: String?, size: Dp, showCameraIcon: Boolean = false) {
    val ringStroke = 3.5.dp
    val colors = listOf(
        Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853), Color(0xFF4285F4)
    )
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size + ringStroke * 2)
                .drawBehind {
                    val stroke = ringStroke.toPx()
                    val step = 360f / colors.size
                    colors.forEachIndexed { i, c ->
                        drawArc(
                            color = c, startAngle = i * step, sweepAngle = step - 4f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                        )
                    }
                }
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(avatarUrl)
                        .crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            } else {
                Surface(shape = CircleShape, modifier = Modifier.size(size),
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (displayName?.firstOrNull() ?: "U").toString().uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        if (showCameraIcon) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(24.dp).offset((-2).dp, (-2).dp)) {
                Icon(Icons.Filled.Edit, null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
private fun StatChip(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}

@Composable
private fun UserPill(user: UserProfile) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(36.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!user.avatar_url.isNullOrBlank()) {
                AsyncImage(model = user.avatar_url, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(22.dp).clip(CircleShape))
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text((user.display_name?.firstOrNull() ?: "U").toString().uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Text(user.display_name ?: user.username ?: "User",
                style = MaterialTheme.typography.labelMedium, maxLines = 1)
            // Small badge in pills too
            if (user.is_verified) VerificationBadge(badgeType = null, isVerified = true, size = 12.dp)
        }
    }
}

// ──────────────────────────────────────────────────────────── VerificationBadge
// Uses custom SVG drawables:
//   creator  → ic_badge_creator.xml  (gold star seal)
//   official / default → ic_verified_badge.xml  (blue checkmark seal)
@Composable
fun VerificationBadge(badgeType: String?, isVerified: Boolean, size: Dp = 18.dp) {
    if (!isVerified && badgeType == null) return
    val drawableRes = when (badgeType) {
        "creator" -> R.drawable.ic_badge_creator
        else      -> R.drawable.ic_verified_badge   // official, artist, partner, default
    }
    Image(
        painter = painterResource(drawableRes),
        contentDescription = badgeType ?: "Verified",
        modifier = Modifier.size(size)
    )
}
