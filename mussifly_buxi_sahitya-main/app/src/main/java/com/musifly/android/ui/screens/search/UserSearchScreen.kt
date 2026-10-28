package com.musifly.android.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.LocalSearchBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.Shadow
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.viewmodels.ProfileViewModel
import com.musifly.android.ui.component.liquidglass.LiquidHeader
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.layerBackdrop
import com.musifly.android.ui.component.AppIconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults

import com.musifly.android.sync.supabase.SupabaseProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val lazyListState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    
    val user = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }

    LaunchedEffect(query) {
        if (user != null) {
            viewModel.searchUsers(query)
        }
    }

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sign in to search users",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("google_login") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Sign In")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = if (isLiquidGlass) 100.dp else 64.dp,
                bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .layerBackdrop(backdrop)
        ) {
            if (results.isEmpty() && query.isNotEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(results) { profile ->
                UserListItem(
                    profile = profile,
                    onUserClick = { selectedProfile ->
                        onDismiss()
                        navController.navigate("profile/${selectedProfile.id}")
                    },
                    isLiquidGlass = isLiquidGlass,
                    backdrop = backdrop
                )
            }
        }

        if (isLiquidGlassActive()) {
            LiquidHeader(
                navController = navController,
                backdrop = backdrop,
                title = "Search Users: $query"
            )
        } else {
            TopAppBar(
                title = { Text("Search Users: $query") },
                navigationIcon = {
                    AppIconButton(
                        onClick = { navController.navigateUp() },
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    }
}

@Composable
fun UserListItem(
    profile: SupabaseProfile,
    onUserClick: (SupabaseProfile) -> Unit,
    isLiquidGlass: Boolean,
    backdrop: Backdrop?
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(
                if (isLiquidGlass && backdrop != null) {
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                vibrancy()
                            },
                             highlight = { Highlight.Plain },
                             shadow = { Shadow.Default },
                        )
                        .clip(shape)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape)
                        .clip(shape)
                }
            )
            .clickable {
                onUserClick(profile)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profile.avatarUrl)
                    .allowHardware(false)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.person)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.fullName ?: profile.username ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!profile.username.isNullOrBlank()) {
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!profile.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
