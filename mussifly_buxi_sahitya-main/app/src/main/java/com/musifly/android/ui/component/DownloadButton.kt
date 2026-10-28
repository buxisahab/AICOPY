package com.musifly.android.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.musifly.android.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadButton(
    download: Download?,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val isDownloading = download != null && download.state != Download.STATE_COMPLETED && download.state != Download.STATE_FAILED
    val isDownloaded = download != null && download.state == Download.STATE_COMPLETED
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (isDownloading) {
            val progress = if (download!!.percentDownloaded > 0) download.percentDownloaded / 100f else 0f
            
            // Background track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(iconSize + 8.dp), // Slightly larger than icon
                color = tint.copy(alpha = 0.2f),
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
            
            // Progress
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(iconSize + 8.dp),
                color = tint,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
                trackColor = Color.Transparent,
            )
            
            // Close/Pause icon in center usually, or just the arrow?
            // User requested "download arrow button ke circular progress me show hona chahiye"
            // Suggesting standard arrow but with progress circle around it.
            // Or maybe a "Stop" icon if downloading? Standard pattern is Stop/Pause.
            // But user specifically said "download arrow button". So I'll keep the Download icon if uncertain, 
            // or better, standard is separate loading indicator or filled circle.
            // Let's keep the Download icon but maybe simpler or different indication?
            // If strictly following "download arrow button... circular progress me show", 
            // I'll show the arrow inside the circle.
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = "Downloading",
                tint = tint.copy(alpha = 0.5f), // Dimmed to show it's active/background
                modifier = Modifier.size(iconSize)
            )
        } else if (isDownloaded) {
            Icon(
                painter = painterResource(R.drawable.done),
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary, // Highlight downloaded state? Or keeping user tint? User tint usually.
                // But typically downloaded means "done".
                // I'll use the passed tint but maybe the icon changes to "download_done".
                modifier = Modifier.size(iconSize)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = "Download",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
