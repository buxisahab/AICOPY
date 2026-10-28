package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.PauseListenHistoryKey
import com.musifly.android.constants.PauseSearchHistoryKey
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.PreferenceEntry
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.component.liquidglass.AdaptiveSwitch
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberPreference
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val database = LocalDatabase.current
    val (pauseListenHistory, onPauseListenHistoryChange) = rememberPreference(
        key = PauseListenHistoryKey,
        defaultValue = false
    )
    val (pauseSearchHistory, onPauseSearchHistoryChange) = rememberPreference(
        key = PauseSearchHistoryKey,
        defaultValue = false
    )

    var showClearListenHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }

    val isLiquidGlass = isLiquidGlassActive()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy)) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            
            SettingsGroup(
                title = stringResource(R.string.listen_history)
            ) {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.pause_listen_history)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    iconBackgroundColor = Color(0xFF2196F3), // Blue
                    trailingContent = {
                        AdaptiveSwitch(
                            checked = pauseListenHistory,
                            onCheckedChange = onPauseListenHistoryChange
                        )
                    },
                    onClick = { onPauseListenHistoryChange(!pauseListenHistory) }
                )
                
                
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_listen_history)) },
                    icon = { Icon(painterResource(R.drawable.delete_history), null) },
                    iconBackgroundColor = Color(0xFFF44336), // Red
                    onClick = { showClearListenHistoryDialog = true }
                )
            }

            SettingsGroup(
                title = stringResource(R.string.search_history)
            ) {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.pause_search_history)) },
                    icon = { Icon(painterResource(R.drawable.search_off), null) },
                    iconBackgroundColor = Color(0xFF9C27B0), // Purple
                    trailingContent = {
                        AdaptiveSwitch(
                            checked = pauseSearchHistory,
                            onCheckedChange = onPauseSearchHistoryChange
                        )
                    },
                    onClick = { onPauseSearchHistoryChange(!pauseSearchHistory) }
                )
                
                
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_search_history)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    iconBackgroundColor = Color(0xFF673AB7), // Deep Purple
                    onClick = { showClearSearchHistoryDialog = true }
                )
            }


            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearListenHistoryDialog) {
        ConfirmationDialog2(
            title = stringResource(R.string.clear_listen_history),
            message = stringResource(R.string.clear_listen_history_confirm),
            onDismiss = { showClearListenHistoryDialog = false },
            onConfirm = {
                showClearListenHistoryDialog = false
                database.query { clearListenHistory() }
            }
        )
    }

    if (showClearSearchHistoryDialog) {
        ConfirmationDialog2(
            title = stringResource(R.string.clear_search_history),
            message = stringResource(R.string.clear_search_history_confirm),
            onDismiss = { showClearSearchHistoryDialog = false },
            onConfirm = {
                showClearSearchHistoryDialog = false
                database.query { clearSearchHistory() }
            }
        )
    }
}


@Composable
private fun ConfirmationDialog2(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            },
            content = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}