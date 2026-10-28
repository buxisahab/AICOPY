package com.buxi.music.ui.screens

import android.net.Uri
import com.buxi.music.utils.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.buxi.music.viewmodels.ProfileUiState
import com.buxi.music.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel()
) {
    val context  = LocalContext.current
    val state    by vm.state.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()

    // Pre-fill fields from loaded profile
    val profile = (state as? ProfileUiState.Loaded)?.profile
    var displayName by remember(profile) { mutableStateOf(profile?.display_name ?: "") }
    var bio         by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var pickedUri   by remember { mutableStateOf<Uri?>(null) }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { pickedUri = it; vm.uploadAvatar(it) } }

    LaunchedEffect(vm.toast) {
        vm.toast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { vm.saveProfile(displayName, bio) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar picker
            Box(contentAlignment = Alignment.BottomEnd) {
                GoogleRingAvatar(
                    avatarUrl = profile?.avatar_url,
                    displayName = displayName,
                    size = 96.dp,
                    showCameraIcon = true
                )
                Surface(
                    onClick = { imagePicker.launch("image/*") },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).offset((-2).dp, (-2).dp)
                ) {
                    Icon(Icons.Filled.Edit, null,
                        tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            Text("Tap to change profile picture",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f))

            // Username (read-only)
            profile?.username?.let { uname ->
                Surface(shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.AccountCircle, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Username", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Text("@$uname", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Can't change", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
                    }
                }
            }

            // Display name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                placeholder = { Text("Your name") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Bio
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Bio") },
                placeholder = { Text("Tell people about yourself…") },
                leadingIcon = { Icon(Icons.Filled.Info, null) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("${bio.length}/160") }
            )

            // Verification badge info
            if (profile?.is_verified == true) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1D9BF0).copy(0.1f),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        VerificationBadge(
                            badgeType = null,       // fetched via ProfileViewModel if needed
                            isVerified = true,
                            size = 24.dp
                        )
                        Column {
                            Text("Verified Account", fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D9BF0), style = MaterialTheme.typography.bodyMedium)
                            Text("This badge was granted by the app creator",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = { vm.saveProfile(displayName, bio) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
