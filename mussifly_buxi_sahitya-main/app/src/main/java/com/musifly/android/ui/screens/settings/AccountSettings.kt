package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.App.Companion.forgetAccount
import com.musifly.android.R
import com.musifly.android.auth.AuthService
import com.musifly.android.constants.AccountChannelHandleKey
import com.musifly.android.constants.AccountEmailKey
import com.musifly.android.constants.AccountNameKey
import com.musifly.android.constants.DataSyncIdKey
import com.musifly.android.constants.InnerTubeCookieKey
import com.musifly.android.constants.UseLoginForBrowse
import com.musifly.android.constants.VisitorDataKey
import com.musifly.android.constants.YtmSyncKey
import com.musifly.android.sync.SyncManager
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.InfoLabel
import com.musifly.android.ui.component.PreferenceEntry
import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.SwitchPreference
import com.musifly.android.ui.component.TextFieldDialog
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberPreference
import com.musifly.innertube.YouTube
import com.musifly.innertube.utils.parseCookieString
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authService = remember { AuthService.getInstance(context) }
    val isFirebaseLoggedIn = remember { FirebaseAuth.getInstance().currentUser != null }

    val (accountName, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(
        AccountChannelHandleKey,
        ""
    )
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

    // Función para obtener el nombre de cuenta de manera segura
    val getAccountDisplayName =
        remember(accountName, accountEmail, accountChannelHandle, isLoggedIn) {
            when {
                !isLoggedIn -> ""
                accountName.isNotBlank() -> accountName
                accountEmail.isNotBlank() -> accountEmail.substringBefore("@")
                accountChannelHandle.isNotBlank() -> accountChannelHandle
                else -> "Usuario sin nombre" // Fallback para evitar crashes
            }
        }

    // Función para obtener la descripción de la cuenta de manera segura
    val getAccountDescription = remember(accountEmail, accountChannelHandle, isLoggedIn) {
        when {
            !isLoggedIn -> null
            accountEmail.isNotBlank() -> accountEmail
            accountChannelHandle.isNotBlank() -> accountChannelHandle
            else -> null
        }
    }

    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, defaultValue = true)

    var showToken: Boolean by remember {
        mutableStateOf(false)
    }
    var showTokenEditor by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlassActive()) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = if (isLiquidGlassActive()) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        }
        ) { innerPadding ->
            val isLiquidGlass = isLiquidGlassActive()
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier)
                    .verticalScroll(rememberScrollState())
            ) {

            SettingsGroup(title = stringResource(R.string.google)) {

            PreferenceEntry(
                title = {
                    Text(
                        if (isLoggedIn) {
                            getAccountDisplayName.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.login)
                        } else {
                            stringResource(R.string.login)
                        }
                    )
                },
                description = if (isLoggedIn) getAccountDescription else null,
                icon = { Icon(painterResource(R.drawable.login), null) },
                iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
                trailingContent = {
                    if (isLoggedIn || isFirebaseLoggedIn) {
                        OutlinedButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    Timber.tag("AccountSettings").d("🔄 Starting logout process...")
                                    
                                    // Get repositories to clear user data
                                    val entryPoint = EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        com.musifly.android.sync.SyncManagerEntryPoint::class.java
                                    )
                                    
                                    // Clear Firebase user data (playlists, likes) if logged in
                                    if (isFirebaseLoggedIn) {
                                        Timber.tag("AccountSettings").d("Clearing Firebase user data...")
                                        entryPoint.likesRepository().clearUserData()
                                        entryPoint.playlistRepository().clearUserData()
                                        
                                        // Sign out from Firebase
                                        authService.signOut()
                                        Timber.tag("AccountSettings").d("Signed out from Firebase")
                                        
                                        // Cancel periodic sync
                                        val syncManager = SyncManager(
                                            context,
                                            entryPoint.likesRepository(),
                                            entryPoint.playlistRepository(),
                                            entryPoint.cachedDownloadsRepository(),
                                            entryPoint.bookmarksRepository(),
                                            entryPoint.migrationService()
                                        )
                                        syncManager.cancelPeriodicSync()
                                    }
                                    
                                    // Clear YouTube account data
                                    Timber.tag("AccountSettings").d("Clearing YouTube account data...")
                                    onInnerTubeCookieChange("")
                                    onAccountNameChange("")
                                    onAccountEmailChange("")
                                    onAccountChannelHandleChange("")
                                    onVisitorDataChange("")
                                    onDataSyncIdChange("")
                                    forgetAccount(context)
                                    
                                    // Clear login_skipped flag - set to false so login screen shows on next start
                                    val sharedPrefs = context.getSharedPreferences(
                                        "musifly_prefs",
                                        android.content.Context.MODE_PRIVATE
                                    )
                                    sharedPrefs.edit().putBoolean("login_skipped", false).apply()
                                    
                                    // CRITICAL: Clear ALL app data (preferences, cache)
                                    Timber.tag("AccountSettings").d("🧹 Clearing all app data...")
                                    
                                    // Clear shared preferences
                                    sharedPrefs.edit().clear().apply()
                                    
                                    // Clear cache directory
                                    try {
                                        val cacheDir = context.cacheDir
                                        if (cacheDir.exists() && cacheDir.isDirectory) {
                                            cacheDir.listFiles()?.forEach { it.delete() }
                                        }
                                        Timber.tag("AccountSettings").d("✅ Cache cleared")
                                    } catch (e: Exception) {
                                        Timber.tag("AccountSettings").w(e, "⚠️ Could not clear cache")
                                    }
                                    
                                    // CRITICAL: Stop player and clear media items BEFORE stopping service
                                    // This prevents mini player from showing on login screen
                                    try {
                                        val playerConnection = com.musifly.android.playback.PlayerConnection.instance
                                        if (playerConnection != null) {
                                            Timber.tag("AccountSettings").d("🛑 Stopping player and clearing all state...")
                                            
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
                                            
                                            Timber.tag("AccountSettings").d("✅ Player stopped, media items cleared, and automix cleared")
                                        } else {
                                            Timber.tag("AccountSettings").w("⚠️ PlayerConnection instance is null")
                                        }
                                    } catch (e: Exception) {
                                        Timber.tag("AccountSettings").w(e, "⚠️ Could not stop player")
                                    }
                                    
                                    // Unbind and stop MusicService
                                    try {
                                        // Try to unbind service connection if MainActivity is available
                                        // Note: We can't directly access MainActivity's serviceConnection,
                                        // but stopping the service will unbind it automatically
                                        
                                        val serviceIntent = android.content.Intent(context, com.musifly.android.playback.MusicService::class.java)
                                        
                                        // Stop the service (this will also unbind any connections)
                                        context.stopService(serviceIntent)
                                        
                                        Timber.tag("AccountSettings").d("✅ MusicService stopped and unbound")
                                    } catch (e: Exception) {
                                        Timber.tag("AccountSettings").w(e, "⚠️ Could not stop MusicService")
                                    }
                                    
                                    Timber.tag("AccountSettings").d("✅ All app data cleared, restarting app...")
                                    
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
                                            Timber.tag("AccountSettings").e(e, "❌ Error restarting app")
                                            // Fallback: navigate to login
                                            navController.navigate("google_login") {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.tag("AccountSettings").e(e, "❌ Error during logout")
                                    // Still try to restart
                                    withContext(Dispatchers.Main) {
                                        try {
                                            val intent = android.content.Intent(context, com.musifly.android.MainActivity::class.java)
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            context.startActivity(intent)
                                            kotlin.system.exitProcess(0)
                                        } catch (ex: Exception) {
                                            navController.navigate("google_login") {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ) {
                            Text(stringResource(R.string.logout))
                        }
                    }
                },
                onClick = { 
                    if (!isLoggedIn) {
                        navController.navigate("google_login") {
                            // Prevent going back to previous screens
                            launchSingleTop = true
                        }
                    }
                }
            )

            if (showTokenEditor) {
                val text =
                    "***INNERTUBE COOKIE*** =${innerTubeCookie}\n\n***VISITOR DATA*** =${visitorData}\n\n***DATASYNC ID*** =${dataSyncId}\n\n***ACCOUNT NAME*** =${accountName}\n\n***ACCOUNT EMAIL*** =${accountEmail}\n\n***ACCOUNT CHANNEL HANDLE*** =${accountChannelHandle}"
                TextFieldDialog(
                    modifier = Modifier,
                    initialTextFieldValue = TextFieldValue(text),
                    onDone = { data ->
                        data.split("\n").forEach {
                            when {
                                it.startsWith("***INNERTUBE COOKIE*** =") -> {
                                    val cookie =
                                        it.substringAfter("***INNERTUBE COOKIE*** =").trim()
                                    onInnerTubeCookieChange(cookie)
                                }

                                it.startsWith("***VISITOR DATA*** =") -> {
                                    val visitorDataValue =
                                        it.substringAfter("***VISITOR DATA*** =").trim()
                                    onVisitorDataChange(visitorDataValue)
                                }

                                it.startsWith("***DATASYNC ID*** =") -> {
                                    val dataSyncIdValue =
                                        it.substringAfter("***DATASYNC ID*** =").trim()
                                    onDataSyncIdChange(dataSyncIdValue)
                                }

                                it.startsWith("***ACCOUNT NAME*** =") -> {
                                    val name = it.substringAfter("***ACCOUNT NAME*** =").trim()
                                    onAccountNameChange(name)
                                }

                                it.startsWith("***ACCOUNT EMAIL*** =") -> {
                                    val email = it.substringAfter("***ACCOUNT EMAIL*** =").trim()
                                    onAccountEmailChange(email)
                                }

                                it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> {
                                    val handle =
                                        it.substringAfter("***ACCOUNT CHANNEL HANDLE*** =").trim()
                                    onAccountChannelHandleChange(handle)
                                }
                            }
                        }
                    },
                    onDismiss = { showTokenEditor = false },
                    singleLine = false,
                    maxLines = 20,
                    isInputValid = { input ->
                        input.isNotEmpty() &&
                                try {
                                    val cookieLine = input.lines()
                                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                                    if (cookieLine != null) {
                                        val cookie =
                                            cookieLine.substringAfter("***INNERTUBE COOKIE*** =")
                                                .trim()
                                        cookie.isEmpty() || "SAPISID" in parseCookieString(cookie)
                                    } else {
                                        false
                                    }
                                } catch (e: Exception) {
                                    false
                                }
                    },
                    extraContent = {
                        InfoLabel(text = stringResource(R.string.token_adv_login_description))
                    }
                )
            }

            PreferenceEntry(
                title = {
                    if (!isLoggedIn) {
                        Text(stringResource(R.string.advanced_login))
                    } else {
                        if (showToken) {
                            Text(stringResource(R.string.token_shown))
                        } else {
                            Text(stringResource(R.string.token_hidden))
                        }
                    }
                },
                icon = { Icon(painterResource(R.drawable.token), null) },
                iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF607D8B), // Blue Grey
                onClick = {
                    if (!isLoggedIn) {
                        showTokenEditor = true
                    } else {
                        if (!showToken) {
                            showToken = true
                        } else {
                            showTokenEditor = true
                        }
                    }
                },
            )

            if (isLoggedIn) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.use_login_for_browse)) },
                    description = stringResource(R.string.use_login_for_browse_desc),
                    icon = { Icon(painterResource(R.drawable.person), null) },
                    iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
                    checked = useLoginForBrowse,
                    onCheckedChange = {
                        YouTube.useLoginForBrowse = it
                        onUseLoginForBrowseChange(it)
                    }
                )
            }

            if (isLoggedIn) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.ytm_sync)) },
                    icon = { Icon(painterResource(R.drawable.cached), null) },
                    iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF9C27B0), // Purple
                    checked = ytmSync,
                    onCheckedChange = onYtmSyncChange,
                    isEnabled = isLoggedIn
                )
            }
            }
            
            if (isFirebaseLoggedIn) {
                SettingsGroup(title = "Legacy Data Migration") {
                    var isMigrating by remember { mutableStateOf(false) }

                    PreferenceEntry(
                        title = { Text(stringResource(R.string.migrate_to_supabase)) },
                        description = stringResource(R.string.migrate_to_supabase_desc),
                        icon = { 
                            if (isMigrating) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(painterResource(R.drawable.backup), null)
                            }
                        },
                        iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFFE91E63), // Pink
                        onClick = {
                            if (!isMigrating) {
                                scope.launch(Dispatchers.IO) {
                                    isMigrating = true
                                    try {
                                        val entryPoint = EntryPointAccessors.fromApplication(
                                            context.applicationContext,
                                            com.musifly.android.sync.SyncManagerEntryPoint::class.java
                                        )
                                        
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Migrating Playlists & Likes...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        
                                        // Migrate Playlists
                                        val playlistResult = entryPoint.playlistRepository().migrateFirestoreToSupabase()
                                        
                                        // Migrate Likes
                                        val likesResult = entryPoint.likesRepository().migrateFirestoreToSupabase()
                                        
                                        withContext(Dispatchers.Main) {
                                            if (playlistResult.isSuccess && likesResult.isSuccess) {
                                                android.widget.Toast.makeText(context, "✅ Full migration successful!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (playlistResult.isSuccess) {
                                                android.widget.Toast.makeText(context, "⚠️ Playlists migrated, Likes failed", android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (likesResult.isSuccess) {
                                                android.widget.Toast.makeText(context, "⚠️ Likes migrated, Playlists failed", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "❌ Migration failed", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            isMigrating = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            isMigrating = false
                                        }
                                    }
                                }
                            }
                        },
                        isEnabled = !isMigrating
                    )
                }
            }


        }
    }
}