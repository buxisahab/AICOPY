package com.buxi.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.buxi.music.materialcolorutilities.color.dynamiccolor.DynamicScheme
import com.buxi.music.materialcolorutilities.color.hct.Hct
import com.buxi.music.materialcolorutilities.color.scheme.SchemeTonalSpot
import com.buxi.music.materialcolorutilities.color.score.Score

val DefaultThemeColor = Color(0xFF212020)
val VibrantBlueThemeColor = Color(0xFF00D4FF) // Electric blue from icon
val VibrantPurpleThemeColor = Color(0xFF8B5CF6) // Rich purple from icon
val VibrantPinkThemeColor = Color(0xFFEC4899) // Hot pink from icon
val RedThemeColor = Color(0xFFE50914) // Resso-like red
val GrayThemeColor = Color(0xFF6B7280) // Resso-like gray

// New theme colors with proper text hierarchy
val ThemeDark = Color(0xFF100F14) // Dark background
val ThemeBlack = Color(0xFF0A0A0D) // Deep black
val ThemeAccent = Color(0xFFFF385F) // Header accent color
val ThemeAccentGlow = Color(0x66FF385F) // Accent with glow
val ThemeAccentBright = Color(0xFFFF4A6A) // Bright accent
val ThemeGray = Color(0xFF100F14) // Theme gray
val ThemeLightGray = Color(0xFF16151B) // Light theme gray
val ThemeWhite = Color(0xFF16151B) // Dark theme background
val ThemeTextPrimary = Color(0xFFFFFFFF) // White text for titles
val ThemeTextSecondary = Color(0xFFFFFFFF) // White text for subtitles/singer names
val ThemeTextTertiary = Color(0xFFD3D4D8) // Lighter gray for less important text
val ThemeGlow = Color(0x4DFF385F) // Accent glow effect
val ThemeBorder = Color(0x33FF385F) // Accent border with transparency
val ThemeSurface = Color(0xFF100F14) // Surface color
val ThemeCard = Color(0xFF25252A) // Card background
val ThemeHeader = Color(0xFFFF385F) // Header color for app name, section titles

@Composable
fun AiMusicTheme(
    darkTheme: Boolean = true, // Always force dark theme
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // Always use custom dark theme colors
    val colorScheme = remember {
        androidx.compose.material3.darkColorScheme(
                                    primary = ThemeAccent,
                                    secondary = ThemeAccentBright,
                                    tertiary = ThemeGray,
            background = ThemeDark,
            surface = ThemeSurface,
            surfaceVariant = ThemeCard,
                                    primaryContainer = ThemeAccent.copy(alpha = 0.2f),
                                    secondaryContainer = ThemeAccentBright.copy(alpha = 0.2f),
                                    tertiaryContainer = ThemeGray.copy(alpha = 0.3f),
                                    outline = ThemeBorder,
            onBackground = ThemeTextPrimary,
            onSurface = ThemeTextPrimary,
                                    onPrimary = ThemeWhite,
                                    onSecondary = ThemeWhite,
            onTertiary = ThemeTextPrimary,
            onPrimaryContainer = ThemeTextPrimary,
            onSecondaryContainer = ThemeTextPrimary,
            onTertiaryContainer = ThemeTextPrimary,
            onSurfaceVariant = ThemeTextTertiary,
            onError = ThemeWhite,
            error = Color.Red
        )
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = {
            // Always use dark theme text colors
            CompositionLocalProvider(
                LocalContentColor provides ThemeTextPrimary
            ) {
                content()
            }
        }
    )
}

fun Bitmap.extractThemeColor(): Color {
    return try {
        val colorsToPopulation =
            Palette
                .from(this)
                .maximumColorCount(8)
                .generate()
                .swatches
                .associate { it.rgb to it.population }
        val rankedColors = Score.score(colorsToPopulation)
        if (rankedColors.isNotEmpty()) {
            Color(rankedColors.first())
        } else {
            DefaultThemeColor // Fallback color
        }
    } catch (e: Exception) {
        DefaultThemeColor // Safe fallback
    }
}

fun Bitmap.extractGradientColors(darkTheme: Boolean = false): List<Color> {
    return try {
        val extractedColors = Palette.from(this)
            .maximumColorCount(16)
            .generate()
            .swatches
            .associate { it.rgb to it.population }

        val orderedColors = if (darkTheme) {
            Score.order(extractedColors)
                .sortedBy { Color(it).luminance() }
                .take(2)
                .reversed()
        } else {
            Score.order(extractedColors)
                .take(2)
                .sortedByDescending { Color(it).luminance() }
        }

        if (orderedColors.size >= 2)
            listOf(Color(orderedColors[0]), Color(orderedColors[1]))
        else
            listOf(Color(0xFF595959), Color(0xFF0D0D0D))
    } catch (e: Exception) {
        // Safe fallback colors
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
    }
}

fun DynamicScheme.toColorScheme(): ColorScheme {
    return try {
        ColorScheme(
            primary = Color(this.primary),
            onPrimary = Color(this.onPrimary),
            primaryContainer = Color(this.primaryContainer),
            onPrimaryContainer = Color(this.onPrimaryContainer),
            inversePrimary = Color(this.inversePrimary),
            secondary = Color(this.secondary),
            onSecondary = Color(this.onSecondary),
            secondaryContainer = Color(this.secondaryContainer),
            onSecondaryContainer = Color(this.onSecondaryContainer),
            tertiary = Color(this.tertiary),
            onTertiary = Color(this.onTertiary),
            tertiaryContainer = Color(this.tertiaryContainer),
            onTertiaryContainer = Color(this.onTertiaryContainer),
            background = Color(this.background),
            onBackground = Color(this.onBackground),
            surface = Color(this.surface),
            onSurface = Color(this.onSurface),
            surfaceVariant = Color(this.surfaceVariant),
            onSurfaceVariant = Color(this.onSurfaceVariant),
            surfaceTint = Color(this.primary),
            inverseSurface = Color(this.inverseSurface),
            inverseOnSurface = Color(this.inverseOnSurface),
            error = Color(this.error),
            onError = Color(this.onError),
            errorContainer = Color(this.errorContainer),
            onErrorContainer = Color(this.onErrorContainer),
            outline = Color(this.outline),
            outlineVariant = Color(this.outlineVariant),
            scrim = Color(this.scrim),
            surfaceBright = Color(this.surfaceBright),
            surfaceDim = Color(this.surfaceDim),
            surfaceContainer = Color(this.surfaceContainer),
            surfaceContainerHigh = Color(this.surfaceContainerHigh),
            surfaceContainerHighest = Color(this.surfaceContainerHighest),
            surfaceContainerLow = Color(this.surfaceContainerLow),
            surfaceContainerLowest = Color(this.surfaceContainerLowest),
        )
    } catch (e: Exception) {
        // Fallback to safe default colors
        androidx.compose.material3.darkColorScheme(
            primary = ThemeAccent,
            secondary = ThemeAccentBright,
            background = ThemeDark,
            surface = ThemeSurface
        )
    }
}

fun ColorScheme.pureBlack(apply: Boolean): ColorScheme {
    return try {
        if (apply) {
            copy(
                surface = Color.Black,
                background = Color.Black,
            )
        } else {
            this
        }
    } catch (e: Exception) {
        // Return original scheme if copy fails
        this
    }
}

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

fun getThemeColor(themeColor: com.buxi.music.constants.ThemeColor): Color {
    return when (themeColor) {
        com.buxi.music.constants.ThemeColor.VIBRANT -> VibrantBlueThemeColor
        com.buxi.music.constants.ThemeColor.RED -> RedThemeColor
        com.buxi.music.constants.ThemeColor.GRAY -> GrayThemeColor
    }
}

fun getVibrantGradientColors(): List<Color> {
    return listOf(
        VibrantBlueThemeColor,
        VibrantPurpleThemeColor,
        VibrantPinkThemeColor
    )
}

// Theme utility functions
fun getThemeGradient(): List<Color> {
    return listOf(
        ThemeAccent,
        ThemeAccentBright,
        ThemeAccent.copy(alpha = 0.8f)
    )
}

fun getThemeGlowColor(): Color {
    return ThemeAccent
}

// Theme composable components
@Composable
fun ThemeCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDarkTheme) ThemeCard else ThemeLightGray
            )
            .border(
                1.dp, 
                ThemeBorder, 
                RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ThemeAccent.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    radius = 300f
                )
            )
    ) {
        content()
    }
}

@Composable
fun SongTitleText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = ThemeTextPrimary,
        style = style
    )
}

@Composable
fun SingerNameText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = ThemeTextPrimary,
        style = style
    )
}

@Composable
fun HeaderText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = ThemeHeader,
        style = style.copy(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = ThemeAccent,
                blurRadius = 8f,
                offset = androidx.compose.ui.geometry.Offset(0f, 0f)
            )
        )
    )
}

@Composable
fun ThemeGradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    ThemeAccent,
                    ThemeAccentBright,
                    ThemeAccent.copy(alpha = 0.8f)
                )
            ),
            shadow = androidx.compose.ui.graphics.Shadow(
                color = ThemeAccent,
                blurRadius = 12f,
                offset = androidx.compose.ui.geometry.Offset(0f, 0f)
            )
        )
    )
}

@Composable
fun ThemeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ThemeAccent)
            .border(1.dp, ThemeBorder, RoundedCornerShape(24.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ThemeAccentGlow.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    radius = 200f
                )
            ),
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ThemeWhite
        ),
        content = content
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ThemeCard)
            .border(1.dp, ThemeBorder, RoundedCornerShape(24.dp)),
        enabled = enabled,
        content = content
    )
}

@Composable
fun ThemeBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDarkTheme) ThemeDark else ThemeWhite
            )
    ) {
        // Subtle glow effect for dark theme
        if (isDarkTheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ThemeAccentGlow.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            radius = 1000f
                        )
                    )
            )
        }
        content()
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ThemeCard)
            .border(1.dp, ThemeBorder, RoundedCornerShape(24.dp)),
        color = Color.Transparent,
        content = content
    )
}
