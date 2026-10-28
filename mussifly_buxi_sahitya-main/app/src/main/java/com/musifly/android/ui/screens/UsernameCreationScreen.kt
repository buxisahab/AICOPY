package com.musifly.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.viewmodels.ProfileViewModel
import com.musifly.android.viewmodels.ProfileState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameCreationScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    
    // Prevent back navigation
    BackHandler(enabled = true) {
        // Do nothing, force user to create username
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusRequesterFullName = remember { FocusRequester() }
    
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val usernameAvailability by viewModel.usernameAvailability.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    
    // Auto-focus username field when screen loads
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        // Force refresh current profile to ensure we have latest data
        viewModel.loadCurrentProfile()
        
        // Auto-populate full name from Firebase if available
        if (fullName.isEmpty()) {
            fullName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
        }
    }
    
    // CRITICAL: If profile loads and user ALREADY has a username, skip this screen
    val currentProfile by viewModel.currentProfile.collectAsState()
    LaunchedEffect(currentProfile) {
        if (!currentProfile?.username.isNullOrBlank()) {
            navController.navigate(Screens.ArtistSelection.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }
    
    // Check username availability as user types
    LaunchedEffect(username) {
        if (username.length >= 3) {
            // If the user's current username (from profile) is what they typed, it's available
            if (currentProfile?.username == username) {
                return@LaunchedEffect
            }
            delay(500) // Debounce
            viewModel.checkUsername(username)
        }
    }
    
    fun validateUsername(text: String): Boolean {
        return text.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
    }
    
    fun handleCreateUsername() {
        errorMessage = null
        
        when {
            username.isBlank() -> {
                errorMessage = "Username cannot be empty"
                return
            }
            username.length < 3 -> {
                errorMessage = "Username must be at least 3 characters"
                return
            }
            username.length > 20 -> {
                errorMessage = "Username must be at most 20 characters"
                return
            }
            !validateUsername(username) -> {
                errorMessage = "Username can only contain letters, numbers, and underscores"
                return
            }
            fullName.isBlank() -> {
                errorMessage = "Full name cannot be empty"
                return
            }
            usernameAvailability == false -> {
                errorMessage = "Username is already taken"
                return
            }
        }
        
        viewModel.updateProfile(
            username = username,
            fullName = fullName.trim(),
            bio = null,
            imageUri = null
        )
    }

    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Loading -> {
                isCreating = true
            }
            is ProfileState.Success -> {
                isCreating = false
                navController.navigate(Screens.ArtistSelection.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            is ProfileState.Error -> {
                isCreating = false
                errorMessage = (profileState as ProfileState.Error).message
            }
            else -> {
                isCreating = false
            }
        }
    }
    
    val BeatsThemeColor = MaterialTheme.colorScheme.background
    val BeatsPrimaryText = MaterialTheme.colorScheme.onBackground
    val BeatsSecondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val BeatsAccentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BeatsThemeColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BeatsThemeColor)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // Title
                Text(
                    text = "Create Your Username",
                    style = androidx.compose.ui.text.TextStyle(
                        color = BeatsPrimaryText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Choose a unique username to complete your profile.\nThis is how others will find you.",
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        color = BeatsSecondaryText,
                        fontSize = 16.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Username input field
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        if (it.length <= 20) {
                            username = it.lowercase().replace(" ", "_")
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("Username") },
                    placeholder = { Text("your_username") },
                    singleLine = true,
                    enabled = !isCreating,
                    trailingIcon = {
                        when {
                            username.length < 3 -> null
                            usernameAvailability == true -> Icon(
                                Icons.Default.Check,
                                contentDescription = "Available",
                                tint = BeatsAccentColor
                            )
                            usernameAvailability == false -> Icon(
                                Icons.Default.Close,
                                contentDescription = "Taken",
                                tint = Color(0xFFB00020)
                            )
                            else -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BeatsAccentColor
                            )
                        }
                    },
                    supportingText = {
                        when {
                            username.isEmpty() -> Text("3-20 characters, letters, numbers, and underscores only")
                            username.length < 3 -> Text("At least 3 characters required")
                            !validateUsername(username) -> Text("Only letters, numbers, and underscores allowed")
                            usernameAvailability == true -> Text("Username is available!", color = BeatsAccentColor)
                            usernameAvailability == false -> Text("Username is already taken", color = Color(0xFFB00020))
                            else -> Text("Checking availability...")
                        }
                    },
                    isError = errorMessage != null || (username.length >= 3 && usernameAvailability == false),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeatsAccentColor,
                        unfocusedBorderColor = BeatsSecondaryText,
                        focusedLabelColor = BeatsAccentColor,
                        unfocusedLabelColor = BeatsSecondaryText,
                        focusedTextColor = BeatsPrimaryText,
                        unfocusedTextColor = BeatsPrimaryText,
                        cursorColor = BeatsAccentColor,
                        errorBorderColor = Color(0xFFB00020),
                        errorLabelColor = Color(0xFFB00020),
                        errorCursorColor = Color(0xFFB00020)
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusRequesterFullName.requestFocus()
                        }
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Full Name input field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { 
                        if (it.length <= 50) {
                            fullName = it
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterFullName),
                    label = { Text("Full Name") },
                    placeholder = { Text("Your full name") },
                    singleLine = true,
                    enabled = !isCreating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeatsAccentColor,
                        unfocusedBorderColor = BeatsSecondaryText,
                        focusedLabelColor = BeatsAccentColor,
                        unfocusedLabelColor = BeatsSecondaryText,
                        focusedTextColor = BeatsPrimaryText,
                        unfocusedTextColor = BeatsPrimaryText,
                        cursorColor = BeatsAccentColor,
                        errorBorderColor = Color(0xFFB00020),
                        errorLabelColor = Color(0xFFB00020),
                        errorCursorColor = Color(0xFFB00020)
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (usernameAvailability == true && validateUsername(username) && fullName.isNotBlank()) {
                                handleCreateUsername()
                            }
                        }
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Error message
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
                            style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Create button
                Button(
                    onClick = { handleCreateUsername() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isCreating && usernameAvailability == true && validateUsername(username) && fullName.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeatsAccentColor,
                        contentColor = Color.White,
                        disabledContainerColor = BeatsSecondaryText.copy(alpha = 0.3f),
                        disabledContentColor = BeatsSecondaryText
                    )
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
