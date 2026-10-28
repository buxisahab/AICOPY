package com.buxi.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buxi.music.R
import com.buxi.music.playback.PlayerConnection
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary
import com.buxi.music.ui.theme.ThemeTextTertiary

private val RessoCoralAccent = Color(0xFFFF4D67)

/**
 * Resso-style Playback Speed selector bottom sheet.
 *
 * Options: 0.5×, 0.75×, 1.0× (Normal), 1.25×, 1.5×, 2.0×
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RessoSpeedSheet(
    onDismiss: () -> Unit,
    playerConnection: PlayerConnection
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSpeed by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }

    val speedOptions = listOf(
        0.5f to "0.5×",
        0.75f to "0.75×",
        1.0f to "1.0× (Normal)",
        1.25f to "1.25×",
        1.5f to "1.5×",
        2.0f to "2.0×"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ThemeDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                )
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.speed),
                        contentDescription = null,
                        tint = RessoCoralAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = ThemeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Adjust the tempo of audio playback",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeTextTertiary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Speed options list
            speedOptions.forEach { (speedVal, label) ->
                val isSelected = kotlin.math.abs(currentSpeed - speedVal) < 0.05f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) RessoCoralAccent.copy(alpha = 0.15f) else ThemeCard
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) RessoCoralAccent else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            currentSpeed = speedVal
                            playerConnection.player.setPlaybackSpeed(speedVal)
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) RessoCoralAccent else ThemeTextPrimary
                    )
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = RessoCoralAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
