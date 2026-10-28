package com.buxi.music.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.buxi.music.R
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextTertiary
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun VoiceSearchDialog(
    state: VoiceSearchState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = ThemeDark,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Voice animation
                when (state) {
                    VoiceSearchState.LISTENING -> ListeningAnimation()
                    VoiceSearchState.PROCESSING -> ProcessingAnimation()
                    VoiceSearchState.ERROR -> ErrorAnimation()
                    else -> IdleAnimation()
                }

                // Status text
                Text(
                    text = when (state) {
                        VoiceSearchState.LISTENING -> "Listening..."
                        VoiceSearchState.PROCESSING -> "Processing..."
                        VoiceSearchState.ERROR -> "Error occurred"
                        else -> "Tap to speak"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                // Hint text
                if (state == VoiceSearchState.LISTENING) {
                    Text(
                        text = "Speak now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeTextTertiary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "listening")
    
    // Pulsing circles
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulseScale1)
                .alpha(pulseAlpha * 0.3f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ThemeAccent.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Middle pulse circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(pulseScale2)
                .alpha(pulseAlpha * 0.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ThemeAccent.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Center icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(ThemeAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Waveform bars
        WaveformBars()
    }
}

@Composable
private fun BoxScope.WaveformBars() {
    val barCount = 5
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(barCount) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = Random.nextInt(20, 40).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = Random.nextInt(300, 600),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(ThemeAccent, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun ProcessingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating gradient circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ThemeAccent,
                            ThemeAccent.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Center icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(ThemeDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = "Processing",
                tint = ThemeAccent,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ErrorAnimation() {
    val scale = remember { Animatable(0.5f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale.value)
                .background(Color.Red.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun IdleAnimation() {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(ThemeAccent.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = "Microphone",
                tint = ThemeAccent,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
