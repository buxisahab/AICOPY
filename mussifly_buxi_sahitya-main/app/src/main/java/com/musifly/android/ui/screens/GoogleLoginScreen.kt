package com.musifly.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.musifly.android.R
import com.musifly.android.auth.AuthService
import com.musifly.android.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.runtime.LaunchedEffect
import dagger.hilt.android.EntryPointAccessors

// Beats-Music theme colors (Spotify-style)
private val BeatsThemeColor = Color(0xFF000000) // Pure black background
private val BeatsAccentColor = Color(0xFF1DB954) // Spotify green
private val BeatsAccentColorLight = Color(0xFF1ED760) // Lighter Spotify green
private val BeatsPrimaryText = Color(0xFFFFFFFF) // Pure white
private val BeatsSecondaryText = Color(0xFFB3B3B3) // Light gray (white70)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = remember { AuthService.getInstance(context) }
    // SyncManager will be injected via Hilt in ViewModel or provided via DI
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { 
        context.getSharedPreferences("musifly_prefs", android.content.Context.MODE_PRIVATE) 
    }
    
    // Check if user is already logged in when screen loads
    // CRITICAL: Only navigate away if user is actually logged in (not guest mode)
    // If user manually navigated to login screen (even as guest), keep them there
    // This prevents login screen from disappearing when guest user wants to login
    LaunchedEffect(Unit) {
        // Small delay to ensure screen is fully loaded
        delay(100)
        val isLoggedIn = authService.isSignedIn()
        
        // Only navigate away if user is actually logged in
        // Don't navigate based on loginSkipped - guest users should be able to access login screen
        if (isLoggedIn) {
            // User is authenticated, navigate to home
            navController.navigate(Screens.Home.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
        // If not logged in (even if guest mode), stay on login screen
        // This allows guest users to login if they want to
    }
    
    // Launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            isLoading = true
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                
                val user = authService.handleSignInResult(account)
                if (user != null) {
                    // Reset skipped flag on successful login
                    sharedPrefs.edit().putBoolean("login_skipped", false).apply()
                    
                    // Trigger sync immediately after login
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.musifly.android.sync.SyncManagerEntryPoint::class.java
                        )
                        val likesRepository = entryPoint.likesRepository()
                        val playlistRepository = entryPoint.playlistRepository()
                        val syncManager = com.musifly.android.sync.SyncManager(
                            context.applicationContext,
                            likesRepository,
                            playlistRepository,
                            entryPoint.cachedDownloadsRepository(),
                            entryPoint.bookmarksRepository(),
                            entryPoint.migrationService()
                        )
                        
                        // Sync data: download from Firestore + upload local unsynced data
                        syncManager.syncAllUserData(scope) { result ->
                            result.onSuccess {
                                Timber.tag("GoogleLoginScreen").d("Sync complete: ${it.likedSongsCount} likes, ${it.playlistsCount} playlists")
                            }.onFailure { e ->
                                Timber.tag("GoogleLoginScreen").e(e, "Sync failed after login")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("GoogleLoginScreen").e(e, "Failed to trigger sync after login")
                    }
                    
                    // Check if user has a username in Supabase
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.musifly.android.di.ProfileRepositoryEntryPoint::class.java
                        )
                        val profileRepository = entryPoint.profileRepository()
                        val profile = profileRepository.getProfile(user.uid)
                        
                        if (profile?.username.isNullOrBlank()) {
                            // Username is mandatory - redirect to username creation
                            navController.navigate(Screens.UsernameCreation.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            // User has username - navigate to artist selection for onboarding
                            navController.navigate(Screens.ArtistSelection.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("GoogleLoginScreen").e(e, "Failed to check username, redirecting to username creation")
                        // On error, assume username is missing and redirect to creation
                        navController.navigate(Screens.UsernameCreation.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                } else {
                    errorMessage = "Sign in failed. Please try again."
                    isLoading = false
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                when (statusCode) {
                    ConnectionResult.NETWORK_ERROR -> {
                        errorMessage = "Network error. Please check your connection."
                    }
                    CommonStatusCodes.CANCELED -> {
                        errorMessage = null // User cancelled, don't show error
                    }
                    else -> {
                        errorMessage = "Sign in failed or cancelled"
                    }
                }
                isLoading = false
                Timber.e(e, "Google Sign-In failed with status: $statusCode")
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during sign-in")
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }
    
    fun handleGoogleSignIn() {
        if (!termsAccepted) {
            errorMessage = context.getString(R.string.accept_terms_error)
            return
        }
        val intent = authService.getSignInIntent()
        if (intent != null) {
            googleSignInLauncher.launch(intent)
        } else {
            errorMessage = "Google Sign-In is not configured. Please add Firebase configuration."
        }
    }
    
    fun handleGuestMode() {
        if (!termsAccepted) {
            errorMessage = context.getString(R.string.accept_terms_error)
            return
        }
        scope.launch {
            isLoading = true
            // Save skipped state to allow guest mode
            sharedPrefs.edit().putBoolean("login_skipped", true).apply()
            
            // Creating experience delay (500ms like Beats-Music)
            delay(500)
            
            // Navigate to home screen
            navController.navigate(Screens.Home.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            isLoading = false
        }
    }
    
    // Prevent back navigation on login screen (Beats-Music style)
    // User can use guest mode if they don't want to login
    BackHandler(enabled = true) {
        // Block back navigation - user must either login or use guest mode
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BeatsThemeColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BeatsThemeColor)
        ) {
            // Background decoration - Circular shadow (Beats-Music style)
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = 200.dp, y = (-100).dp)
                    .shadow(
                        elevation = 100.dp,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        ambientColor = BeatsAccentColor.copy(alpha = 0.2f),
                        spotColor = BeatsAccentColor.copy(alpha = 0.2f)
                    )
                    .background(
                        color = BeatsAccentColor.copy(alpha = 0.0f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            
            // Main content - Using Box with safeDrawingPadding instead of SafeArea (Compose doesn't have SafeArea composable)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .safeDrawingPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // App Icon - White container with rounded corners (Beats-Music style)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(30.dp),
                                spotColor = BeatsAccentColor.copy(alpha = 0.3f)
                            )
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(30.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.sahitya_h),
                            contentDescription = "MusiFly App Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Welcome text
                    Text(
                        text = "Welcome to Mussifly",
                        style = androidx.compose.ui.text.TextStyle(
                            color = BeatsPrimaryText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Your personal cloud music player.\nSync your playlists and stats across devices.",
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            color = BeatsSecondaryText,
                            fontSize = 16.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Error message (Beats-Music style - show as card if needed)
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFB00020).copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = BeatsPrimaryText,
                            )
                        }
                    }
                    
                    // Terms & Privacy Acceptance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { 
                                termsAccepted = it
                                if (it) errorMessage = null
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BeatsAccentColor,
                                uncheckedColor = BeatsSecondaryText,
                                checkmarkColor = BeatsThemeColor
                            )
                        )
                        
                        Text(
                            text = "I agree to the ",
                            color = BeatsSecondaryText,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                        
                        TextButton(
                            onClick = { navController.navigate(Screens.PrivacyPolicy.route) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Terms & Privacy Policy",
                                color = BeatsAccentColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    // Google Sign In Button or Loading
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = BeatsAccentColor
                        )
                    } else {
                        // Google Sign In Button - White background, black text (Beats-Music style)
                        Button(
                            onClick = { handleGoogleSignIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = termsAccepted,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (termsAccepted) Color.White else Color.White.copy(alpha = 0.5f),
                                contentColor = Color.Black.copy(alpha = if (termsAccepted) 1f else 0.5f)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Google "G" icon - using text for now
                                Text(
                                    text = "G",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    ),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = "Sign in with Google",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Guest Mode Button (Beats-Music style)
                    TextButton(
                        onClick = { handleGuestMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now (Guest Mode)",
                            style = androidx.compose.ui.text.TextStyle(
                                color = BeatsSecondaryText, // white60
                                fontSize = 16.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
