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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Coral accent matching Resso's sleep timer design */
private val RessoCoralAccent = Color(0xFFFF4D67)

/**
 * Resso-style Sleep Timer bottom sheet.
 *
 * - Presets: Off, 15 min, 30 min, 45 min, 60 min, End of song
 * - When a timer is active: shows live countdown + Cancel button
 * - Pauses playback via [playerConnection].player when the timer expires
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RessoSleepTimerSheet(
    onDismiss: () -> Unit,
    playerConnection: PlayerConnection
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sleepTimer = playerConnection.service.sleepTimer

    // Live countdown value (ms remaining), updated every second while timer is active
    var remainingMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimer.triggerTime) {
        while (isActive) {
            val trigger = sleepTimer.triggerTime
            remainingMs = if (trigger != -1L) (trigger - System.currentTimeMillis()).coerceAtLeast(0L)
            else 0L
            delay(1_000)
        }
    }

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
            // ── Header ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        tint = RessoCoralAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sleep Timer",
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

            Spacer(modifier = Modifier.height(4.dp))

            // ── Active countdown banner ───────────────────────────────
            if (sleepTimer.isActive) {
                val displayText = when {
                    sleepTimer.pauseWhenSongEnd -> "Pausing at end of song"
                    remainingMs > 0 -> {
                        val totalSecs = (remainingMs / 1000).toInt()
                        val m = totalSecs / 60
                        val s = totalSecs % 60
                        "Pausing in %02d:%02d".format(m, s)
                    }
                    else -> "Pausing soon…"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RessoCoralAccent.copy(alpha = 0.12f))
                        .border(1.dp, RessoCoralAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = RessoCoralAccent
                        )
                        Button(
                            onClick = { sleepTimer.clear() },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RessoCoralAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Preset chips ─────────────────────────────────────────
            Text(
                text = "Choose a preset",
                style = MaterialTheme.typography.labelMedium,
                color = ThemeTextTertiary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val presets = listOf(
                "Off" to 0,
                "15 min" to 15,
                "30 min" to 30,
                "45 min" to 45,
                "60 min" to 60,
                "End of song" to -1
            )

            presets.forEach { (label, minutes) ->
                val isSelected = when (minutes) {
                    0 -> !sleepTimer.isActive
                    -1 -> sleepTimer.pauseWhenSongEnd
                    else -> {
                        sleepTimer.isActive && !sleepTimer.pauseWhenSongEnd &&
                            remainingMs in (minutes * 60_000L - 5_000L)..(minutes * 60_000L + 5_000L)
                    }
                }

                SleepTimerPresetRow(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        when (minutes) {
                            0 -> sleepTimer.clear()
                            -1 -> {
                                sleepTimer.clear()
                                sleepTimer.start(-1)
                            }
                            else -> {
                                sleepTimer.clear()
                                sleepTimer.start(minutes)
                            }
                        }
                        if (minutes == 0) onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SleepTimerPresetRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
            .clickable(onClick = onClick)
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
}
