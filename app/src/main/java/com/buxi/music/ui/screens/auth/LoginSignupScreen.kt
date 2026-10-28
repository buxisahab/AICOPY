package com.buxi.music.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.buxi.music.R
import com.buxi.music.auth.AuthResult
import com.buxi.music.auth.AuthState
import com.buxi.music.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSignupScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                coroutineScope.launch {
                    isLoading = true
                    val authResult = viewModel.signInWithGoogle(account)
                    isLoading = false
                    when (authResult) {
                        is AuthResult.Success -> {
                            navController.popBackStack()
                        }
                        is AuthResult.Error -> {
                            errorMessage = authResult.message
                        }
                    }
                }
            } catch (e: ApiException) {
                errorMessage = "Google sign-in failed: ${e.message}"
            }
        }
    }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.popBackStack()
            }
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                isLoading = false
            }
            is AuthState.Loading -> {
                isLoading = true
            }
            else -> {
                isLoading = false
            }
        }
    }
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ThemeDark,
                        ThemeAccent.copy(alpha = 0.1f),
                        ThemeDark
                    ),
                    startY = gradientOffset,
                    endY = gradientOffset + 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            
            // Logo and Title
            Icon(
                painter = painterResource(R.drawable.ia_icon),
                contentDescription = "App Logo",
                tint = ThemeAccent,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "AI Music",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ThemeTextPrimary
            )
            
            Text(
                text = if (authMode == AuthMode.LOGIN) "Welcome back!" else "Create your account",
                style = MaterialTheme.typography.bodyLarge,
                color = ThemeTextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(40.dp))
            
            // Auth Form
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = ThemeDark.copy(alpha = 0.6f),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ThemeDark),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        ModeButton(
                            text = "Login",
                            isSelected = authMode == AuthMode.LOGIN,
                            onClick = { authMode = AuthMode.LOGIN },
                            modifier = Modifier.weight(1f)
                        )
                        ModeButton(
                            text = "Sign Up",
                            isSelected = authMode == AuthMode.SIGNUP,
                            onClick = { authMode = AuthMode.SIGNUP },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Display Name (Sign Up only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SIGNUP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = ThemeAccent,
                                    unfocusedIndicatorColor = ThemeTextTertiary,
                                    focusedTextColor = ThemeTextPrimary,
                                    unfocusedTextColor = ThemeTextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = ThemeAccent,
                            unfocusedIndicatorColor = ThemeTextTertiary,
                            focusedTextColor = ThemeTextPrimary,
                            unfocusedTextColor = ThemeTextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    painter = painterResource(
                                        if (passwordVisible) R.drawable.visibility_off else R.drawable.visibility
                                    ),
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = ThemeTextSecondary
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = ThemeAccent,
                            unfocusedIndicatorColor = ThemeTextTertiary,
                            focusedTextColor = ThemeTextPrimary,
                            unfocusedTextColor = ThemeTextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (authMode == AuthMode.SIGNUP) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                focusManager.clearFocus()
                                if (authMode == AuthMode.LOGIN) {
                                    coroutineScope.launch {
                                        isLoading = true
                                        val result = viewModel.signInWithEmail(email, password)
                                        isLoading = false
                                        when (result) {
                                            is AuthResult.Error -> errorMessage = result.message
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        )
                    )
                    
                    // Confirm Password (Sign Up only)
                    AnimatedVisibility(
                        visible = authMode == AuthMode.SIGNUP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = ThemeAccent,
                                    unfocusedIndicatorColor = ThemeTextTertiary,
                                    focusedTextColor = ThemeTextPrimary,
                                    unfocusedTextColor = ThemeTextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (password == confirmPassword) {
                                            coroutineScope.launch {
                                                isLoading = true
                                                val result = viewModel.signUpWithEmail(email, password, displayName)
                                                isLoading = false
                                                when (result) {
                                                    is AuthResult.Error -> errorMessage = result.message
                                                    else -> {}
                                                }
                                            }
                                        } else {
                                            errorMessage = "Passwords don't match"
                                        }
                                    }
                                )
                            )
                        }
                    }
                    
                    // Error Message
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Submit Button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (authMode == AuthMode.LOGIN) {
                                coroutineScope.launch {
                                    isLoading = true
                                    val result = viewModel.signInWithEmail(email, password)
                                    isLoading = false
                                    when (result) {
                                        is AuthResult.Error -> errorMessage = result.message
                                        else -> {}
                                    }
                                }
                            } else {
                                if (password == confirmPassword) {
                                    coroutineScope.launch {
                                        isLoading = true
                                        val result = viewModel.signUpWithEmail(email, password, displayName)
                                        isLoading = false
                                        when (result) {
                                            is AuthResult.Error -> errorMessage = result.message
                                            else -> {}
                                        }
                                    }
                                } else {
                                    errorMessage = "Passwords don't match"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeAccent,
                            contentColor = Color.White
                        ),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (authMode == AuthMode.LOGIN) "Login" else "Sign Up",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = ThemeTextTertiary)
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = ThemeTextSecondary,
                            fontSize = 12.sp
                        )
                        Divider(modifier = Modifier.weight(1f), color = ThemeTextTertiary)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            val signInIntent = viewModel.getGoogleSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = ThemeTextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(ThemeAccent, ThemeAccent.copy(alpha = 0.5f))
                            )
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.google),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Skip Button
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now",
                            color = ThemeTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ThemeAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else ThemeTextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}
