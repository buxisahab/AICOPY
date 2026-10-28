package com.buxi.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buxi.music.R
import com.buxi.music.models.MediaMetadata
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary

private val RessoCoralAccent = Color(0xFFFF4D67)

/**
 * Resso Hide Song Dialog (Matches playing_dialog_hide_song.xml and playing_dialog_hide_artist.xml)
 */
@Composable
fun RessoHideSongDialog(
    mediaMetadata: MediaMetadata?,
    onConfirmHide: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedOption by remember { mutableIntStateOf(0) } // 0: this song, 1: this artist

    val artistName = mediaMetadata?.artists?.firstOrNull()?.name ?: "this artist"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E28),
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.block),
                    contentDescription = null,
                    tint = RessoCoralAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Hide from recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "We will adjust your For You feed so you hear less of this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeTextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Option 0: Hide this song
                HideOptionCard(
                    title = "Hide this song",
                    subtitle = mediaMetadata?.title ?: "Current track",
                    isSelected = selectedOption == 0,
                    onClick = { selectedOption = 0 }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 1: Hide all songs by this artist
                HideOptionCard(
                    title = "Don't recommend this artist",
                    subtitle = artistName,
                    isSelected = selectedOption == 1,
                    onClick = { selectedOption = 1 }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val msg = if (selectedOption == 0) "Hidden '${mediaMetadata?.title}'" else "Hidden songs from '$artistName'"
                    Toast.makeText(context, "$msg ✓", Toast.LENGTH_SHORT).show()
                    onConfirmHide()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RessoCoralAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun HideOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) RessoCoralAccent.copy(alpha = 0.15f) else ThemeCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) RessoCoralAccent else ThemeTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeTextSecondary
            )
        }
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
