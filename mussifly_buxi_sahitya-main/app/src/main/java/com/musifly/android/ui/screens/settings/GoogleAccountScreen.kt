package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import timber.log.Timber
import androidx.navigation.NavController
import android.content.Context
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.auth.AuthService
import com.musifly.android.sync.SyncManager
import com.musifly.android.ui.component.IconButton
import dagger.hilt.android.EntryPointAccessors
import com.musifly.android.ui.utils.backToMain
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey

// Theme adaptive - using MaterialTheme colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val authService = remember { AuthService.getInstance(context) }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    // Get SyncManager via EntryPoint for sync operations
    val syncManager = remember {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                com.musifly.android.sync.SyncManagerEntryPoint::class.java
            )
            SyncManager(
                context,
                entryPoint.likesRepository(),
                entryPoint.playlistRepository(),
                entryPoint.cachedDownloadsRepository(),
                entryPoint.bookmarksRepository(),
                entryPoint.migrationService()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with theme-adaptive background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Profile Picture or Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser?.photoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentUser.photoUrl)
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Default icon
                            Icon(
                                painter = painterResource(R.drawable.person),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Display Name
                    Text(
                        text = currentUser?.displayName ?: "Guest User",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Email
                    Text(
                        text = currentUser?.email ?: "Sign in to sync data",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Account Information Section
            if (currentUser != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Display Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Display Name",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentUser.displayName ?: "Not set",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Email
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Email",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentUser.email ?: "Not set",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // User ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "User ID",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentUser.uid.take(20) + "...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sync Button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            syncManager?.triggerManualSync(scope) { result ->
                                result.onSuccess { syncResult ->
                                    // Show success message
                                }.onFailure { error ->
                                    // Show error message
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Sync Data",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sign Out Button
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                // Not signed in - Show sign in prompt
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Not signed in",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sign in to sync your playlists and stats across devices",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("google_login") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
    
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        val dialogTitle: @Composable () -> Unit = {
            Text(
                text = "Sign Out",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        val dialogContent: @Composable ColumnScope.() -> Unit = {
            Text(
                text = "Are you sure you want to sign out?",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        val buttonsContent: @Composable RowScope.() -> Unit = {
            com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                onClick = { showSignOutDialog = false },
                text = "Cancel"
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Get repositories to clear user data
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.musifly.android.sync.SyncManagerEntryPoint::class.java
                            )
                            
                            // Clear user data from local storage (likes and playlists)
                            Timber.tag("GoogleAccountScreen").d("Clearing user data from local storage on logout...")
                            entryPoint.likesRepository().clearUserData()
                            entryPoint.playlistRepository().clearUserData()
                            Timber.tag("GoogleAccountScreen").d("User data cleared from local storage")
                            
                            // Sign out from Firebase
                            authService.signOut()
                            
                            // Cancel periodic sync
                            syncManager?.cancelPeriodicSync()
                            
                            // Clear login_skipped flag and all shared preferences
                            val sharedPrefs = context.getSharedPreferences(
                                "musifly_prefs",
                                Context.MODE_PRIVATE
                            )
                            sharedPrefs.edit().clear().apply()
                            
                            // Clear cache directory
                            try {
                                val cacheDir = context.cacheDir
                                if (cacheDir.exists() && cacheDir.isDirectory) {
                                    cacheDir.listFiles()?.forEach { it.delete() }
                                }
                                Timber.tag("GoogleAccountScreen").d("✅ Cache cleared")
                            } catch (e: Exception) {
                                Timber.tag("GoogleAccountScreen").w(e, "⚠️ Could not clear cache")
                            }
                            
                            // CRITICAL: Stop player and clear media items BEFORE stopping service
                            // This prevents mini player from showing on login screen
                            try {
                                val playerConnection = com.musifly.android.playback.PlayerConnection.instance
                                if (playerConnection != null) {
                                    Timber.tag("GoogleAccountScreen").d("🛑 Stopping player and clearing all state...")
                                    
                                    // Clear automix
                                    playerConnection.service.clearAutomix()
                                    
                                    // Stop playback
                                    playerConnection.player.stop()
                                    
                                    // Clear all media items
                                    playerConnection.player.clearMediaItems()
                                    
                                    // Pause player and reset play state
                                    playerConnection.player.pause()
                                    playerConnection.player.playWhenReady = false
                                    
                                    // Small delay to ensure state is cleared
                                    kotlinx.coroutines.delay(100)
                                    
                                    Timber.tag("GoogleAccountScreen").d("✅ Player stopped, media items cleared, and automix cleared")
                                } else {
                                    Timber.tag("GoogleAccountScreen").w("⚠️ PlayerConnection instance is null")
                                }
                            } catch (e: Exception) {
                                Timber.tag("GoogleAccountScreen").w(e, "⚠️ Could not stop player")
                            }
                            
                            // Unbind and stop MusicService
                            try {
                                // Try to unbind service connection if MainActivity is available
                                // Note: We can't directly access MainActivity's serviceConnection,
                                // but stopping the service will unbind it automatically
                                
                                val serviceIntent = android.content.Intent(context, com.musifly.android.playback.MusicService::class.java)
                                
                                // Stop the service (this will also unbind any connections)
                                context.stopService(serviceIntent)
                                
                                Timber.tag("GoogleAccountScreen").d("✅ MusicService stopped and unbound")
                            } catch (e: Exception) {
                                Timber.tag("GoogleAccountScreen").w(e, "⚠️ Could not stop MusicService")
                            }
                            
                            showSignOutDialog = false
                            
                            Timber.tag("GoogleAccountScreen").d("✅ All app data cleared, restarting app...")
                            
                            // Restart the app
                            withContext(Dispatchers.Main) {
                                try {
                                    // Show toast message
                                    android.widget.Toast.makeText(
                                        context,
                                        "Logging out... App will restart",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Small delay to show toast
                                    kotlinx.coroutines.delay(500)
                                    
                                    // Restart app
                                    val intent = android.content.Intent(context, com.musifly.android.MainActivity::class.java)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    context.startActivity(intent)
                                    
                                    // Exit current process
                                    kotlin.system.exitProcess(0)
                                } catch (e: Exception) {
                                    Timber.tag("GoogleAccountScreen").e(e, "❌ Error restarting app")
                                    // Fallback: navigate to login
                                    navController.navigate("google_login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag("GoogleAccountScreen").e(e, "Error during logout")
                            // Still sign out even if clearing data fails
                            authService.signOut()
                            withContext(Dispatchers.Main) {
                                showSignOutDialog = false
                                navController.navigate("google_login") {
                                    popUpTo(0) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.error
            ) {
                Text("Sign Out")
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = { showSignOutDialog = false },
                title = dialogTitle,
                content = dialogContent,
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = dialogTitle,
                text = { Column { dialogContent() } },
                confirmButton = {
                    // confirmButton is actually the Sign Out button in the original code
                    // But standard AlertDialog expects confirmButton and dismissButton.
                    // I will use buttonsContent in LiquidGlassDialog and put both buttons there.
                    // For standard AlertDialog, I'll map them accordingly.
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // ... copy of logout logic ...
                                    val entryPoint = EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        com.musifly.android.sync.SyncManagerEntryPoint::class.java
                                    )
                                    entryPoint.likesRepository().clearUserData()
                                    entryPoint.playlistRepository().clearUserData()
                                    authService.signOut()
                                    syncManager?.cancelPeriodicSync()
                                    val sharedPrefs = context.getSharedPreferences("musifly_prefs", Context.MODE_PRIVATE)
                                    sharedPrefs.edit().clear().apply()
                                    try {
                                        val cacheDir = context.cacheDir
                                        if (cacheDir.exists() && cacheDir.isDirectory) {
                                            cacheDir.listFiles()?.forEach { it.delete() }
                                        }
                                    } catch (_: Exception) {}
                                    try {
                                        val playerConnection = com.musifly.android.playback.PlayerConnection.instance
                                        if (playerConnection != null) {
                                            playerConnection.service.clearAutomix()
                                            playerConnection.player.stop()
                                            playerConnection.player.clearMediaItems()
                                            playerConnection.player.pause()
                                            playerConnection.player.playWhenReady = false
                                            kotlinx.coroutines.delay(100)
                                        }
                                    } catch (_: Exception) {}
                                    try {
                                        val serviceIntent = android.content.Intent(context, com.musifly.android.playback.MusicService::class.java)
                                        context.stopService(serviceIntent)
                                    } catch (_: Exception) {}
                                    showSignOutDialog = false
                                    withContext(Dispatchers.Main) {
                                        try {
                                            android.widget.Toast.makeText(context, "Logging out... App will restart", android.widget.Toast.LENGTH_SHORT).show()
                                            kotlinx.coroutines.delay(500)
                                            val intent = android.content.Intent(context, com.musifly.android.MainActivity::class.java)
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            context.startActivity(intent)
                                            kotlin.system.exitProcess(0)
                                        } catch (e: Exception) {
                                            navController.navigate("google_login") {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    authService.signOut()
                                    withContext(Dispatchers.Main) {
                                        showSignOutDialog = false
                                        navController.navigate("google_login") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}