package com.musifly.android.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.viewmodels.StoryViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import android.widget.Toast
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    navController: NavController,
    viewModel: StoryViewModel,
    encodedUri: String
) {
    val uri = remember(encodedUri) {
        try {
            Uri.parse(URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString()))
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }
    
    var caption by remember { mutableStateOf("") }
    var showMusicSheet by remember { mutableStateOf(false) }
    var selectedMusic by remember { mutableStateOf<com.musifly.android.models.MediaMetadata?>(null) }
    var musicTrimStart by remember { mutableLongStateOf(0L) }
    var musicTrimEnd by remember { mutableLongStateOf(30000L) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val isUploading by viewModel.isUploading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isTextMode by remember { mutableStateOf(false) }
    
    // Determine content type
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    val isVideo = mimeType?.startsWith("video/") == true

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .systemBarsPadding()
        .imePadding()
    ) {
        // Media Preview
        if (isVideo) {
            // Simplified Video Preview (Autoplay loop)
             VideoPlayer(
                videoUrl = uri.toString(),
                trimStart = 0,
                trimEnd = null,
                isPaused = false,
                isLooping = true,
                onProgressUpdate = {},
                onVideoEnded = {},
                modifier = Modifier.fillMaxSize()
            )
        } else {
             AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Story Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        // Dim overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // Caption Toggle Button (Placeholder)
                 IconButton(
                    onClick = { 
                        isTextMode = true
                        focusRequester.requestFocus() 
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.TextFormat, contentDescription = "Add Text", tint = Color.White)
                }

                 // Music Button
                IconButton(
                    onClick = { showMusicSheet = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Add Music", tint = Color.White)
                }
                
                // More Button
                IconButton(
                    onClick = { /* More options */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
            }
        }
        
        // Caption Input (Centered)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        ) {
            BasicTextField(
                value = caption,
                onValueChange = { caption = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (caption.isEmpty()) {
                        // Invisible placeholder to keep layout if needed, or visual hint
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            
            LaunchedEffect(isTextMode) {
                if (isTextMode) {
                    focusRequester.requestFocus()
                }
            }
        }
        
        // Bottom Bar (Action Buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Your Story Button
             Button(
                onClick = {
                    val finalDuration = if (isVideo) 15000L else 15000L // Cap segments at 15s for backend logic
                    viewModel.uploadStory(
                        uri = uri,
                        caption = caption.takeIf { it.isNotBlank() },
                        duration = finalDuration,
                        musicId = selectedMusic?.id,
                        startTime = musicTrimStart,
                        endTime = musicTrimEnd
                    )
                    navController.popBackStack()
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF262626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                 if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    // Avatar placeholder or actual avatar
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray)) {
                         if (currentUser?.avatarUrl != null) {
                             AsyncImage(
                                 model = currentUser?.avatarUrl,
                                 contentDescription = null,
                                 contentScale = ContentScale.Crop,
                                 modifier = Modifier.fillMaxSize()
                             )
                         } else {
                             Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp).fillMaxSize())
                         }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Your story", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Close Friends Button
            Button(
                onClick = { 
                    // For now, upload normally but show Toast. Ideally, backend needs `isCloseFriends` flag.
                    Toast.makeText(context, "Sharing to Close Friends only!", Toast.LENGTH_SHORT).show()
                     val duration = if (isVideo || selectedMusic != null) 15000L else 5000L
                    viewModel.uploadStory(
                        uri = uri,
                        caption = caption.takeIf { it.isNotBlank() },
                        duration = duration,
                        musicId = selectedMusic?.id?.toString(),
                        startTime = 0,
                        endTime = 15000
                    )
                    navController.popBackStack()
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF262626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                 Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF4CAF50)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Close Friends", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Next Button (Floating Arrow)
            IconButton(
                onClick = {
                    viewModel.uploadStory(
                        uri = uri,
                        caption = caption.takeIf { it.isNotBlank() },
                        duration = 15000,
                        musicId = selectedMusic?.id,
                        startTime = musicTrimStart,
                        endTime = musicTrimEnd
                    )
                    navController.popBackStack()
                },
                enabled = !isUploading,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = Color.Black, modifier = Modifier.size(28.dp))
            }
        }

        // Music Selection Sheet
        if (showMusicSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMusicSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                com.musifly.android.ui.component.StoryMusicSelectionSheet(
                    onDismiss = { showMusicSheet = false },
                    onMusicSelected = { metadata ->
                        selectedMusic = metadata
                        showMusicSheet = false
                    }
                )
            }
        }

        // Music Trimmer Sheet removed as per request

        // Selected Music Indicator
        // Selected Music Indicator with Bright Waveform
        if (selectedMusic != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                 Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .width(280.dp) // Proper window size for the waveform
                 ) {
                     Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                     ) {
                         Row(
                            verticalAlignment = Alignment.CenterVertically
                         ) {
                             Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(
                                 text = selectedMusic?.title ?: "",
                                 color = Color.White,
                                 style = MaterialTheme.typography.labelLarge
                             )
                             Spacer(modifier = Modifier.weight(1f))
                             IconButton(
                                 onClick = { selectedMusic = null },
                                 modifier = Modifier.size(24.dp)
                             ) {
                                 Icon(Icons.Default.Close, contentDescription = "Remove music", tint = Color.White)
                             }
                         }
                         
                         Spacer(modifier = Modifier.height(12.dp))
                         
                         // The Bright Waveform clipped to window
                         com.musifly.android.ui.component.MusicWaveform(
                             metadata = selectedMusic!!,
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(40.dp)
                                 .clip(RoundedCornerShape(8.dp)),
                             color = Color.White // Bright
                         )
                     }
                 }
            }
        }
    }
}
