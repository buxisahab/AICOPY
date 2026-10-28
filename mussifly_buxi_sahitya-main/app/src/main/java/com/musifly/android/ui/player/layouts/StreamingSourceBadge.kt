package com.musifly.android.ui.player.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays a badge showing the current streaming source
 * @param streamEngine The current streaming engine (YouTube, NewPipe, JioSaavn, etc.)
 * @param modifier Modifier for customization
 */
@Composable
fun StreamingSourceBadge(
    streamEngine: String?,
    modifier: Modifier = Modifier
) {
    // Hidden permanently to declutter the UI across all themes
}
