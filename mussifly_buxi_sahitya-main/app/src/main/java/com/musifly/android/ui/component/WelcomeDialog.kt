package com.musifly.android.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.musifly.android.constants.WelcomeJoinedKey
import com.musifly.android.utils.rememberPreference
import com.musifly.android.secure.SecureString
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey

/**
 * Material3 Welcome Dialog
 * Shows automatically on first app launch until user joins the Telegram channel
 */
@Composable
fun WelcomeDialog(
    onDismiss: () -> Unit,
    onJoinChannel: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var welcomeJoined by rememberPreference(WelcomeJoinedKey, defaultValue = false)

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = { /* Block dismiss */ },
            title = {
                Text(
                    text = "Welcome to MusiFly 🎧",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            content = {
                Text(
                    text = "\uD83D\uDCE2 Don’t Miss Anything!\n" +
                            "Be the first to receive latest updates, exciting new features, breaking news, and exclusive announcements.\n" +
                            "\uD83D\uDC49 Join our official channel today! \uD83D\uDE80\uD83D\uDC8E",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            },
            buttons = {
                Button(
                    onClick = {
                        SecureString.openLink(context, SecureString.LINK_TELEGRAM)
                        welcomeJoined = true
                        onJoinChannel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Join Channel",
                        style = typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    } else {
    // Use Material3 AlertDialog for proper theme support
    AlertDialog(
        onDismissRequest = { /* Block dismiss - user must choose an option */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = "Welcome to MusiFly 🎧",
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "\uD83D\uDCE2 Don’t Miss Anything!\n" +
                        "Be the first to receive latest updates, exciting new features, breaking news, and exclusive announcements.\n" +
                        "\uD83D\uDC49 Join our official channel today! \uD83D\uDE80\uD83D\uDC8E",
                style = typography.bodyMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    // Open Telegram channel
                    SecureString.openLink(context, SecureString.LINK_TELEGRAM)
                    
                    // Mark as joined
                    welcomeJoined = true
                    onJoinChannel()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Join Channel",
                    style = typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
    )
    }
}
