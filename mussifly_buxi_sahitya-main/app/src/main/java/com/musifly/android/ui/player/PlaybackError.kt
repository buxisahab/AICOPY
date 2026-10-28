package com.musifly.android.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import com.musifly.android.R
import com.musifly.android.playback.PlayerErrorMapper

/**
 * Enhanced PlaybackError UI component using PlayerErrorMapper
 * 
 * IMPROVEMENTS:
 * - Uses PlayerErrorMapper to categorize errors intelligently
 * - Shows user-friendly messages instead of generic "Unknown error: Bad stream player response"
 * - Handles all stream types: YouTube, JioSaavn, Local files
 */
@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
    streamType: String? = null, // Optional: "youtube", "saavn", "local" for context
) {
    // Use PlayerErrorMapper to categorize error and get user-friendly message
    val errorCategory = PlayerErrorMapper.categorizeError(error, streamType)
    val userMessage = PlayerErrorMapper.getUserFriendlyMessage(errorCategory, streamType)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Text(
            text = userMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        
        // Show retry button - always available for manual retry
        Button(
            onClick = retry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
