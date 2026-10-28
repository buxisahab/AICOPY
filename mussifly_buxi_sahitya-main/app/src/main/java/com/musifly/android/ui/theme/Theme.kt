package com.musifly.android.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.MotionScheme // Removed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import com.google.material.color.score.Score
import com.musifly.android.constants.AppTheme
import com.musifly.android.constants.ThemeMode

val DefaultThemeColor = Color(0xFF4285F4)

// Light Theme Colors
val LightPrimary = Color(0xFF4285F4)
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F5F5)
val LightOnBackground = Color(0xFF1A1A1A)        // 10% black - excellent contrast
val LightOnSurface = Color(0xFF1A1A1A)           // 10% black - excellent contrast
val LightOnSurfaceVariant = Color(0xFF666666)    // 60% black - professional secondary text

// Dark Theme Colors (OLED)
val DarkPrimary = Color(0xFF90CDF4)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF0A0A0A)
val DarkOnBackground = Color(0xFFF5F5F5)         // 96% white - softer for OLED, reduces eye strain
val DarkOnSurface = Color(0xFFF5F5F5)            // 96% white - softer for OLED
val DarkOnSurfaceVariant = Color(0xFFB3B3B3)     // 70% white - professional secondary text

// Liquid Glass Theme Colors - Premium iOS-like glassmorphism
// Soft neon accents (not harsh, professional)
val LiquidNeonCyan = Color(0xFF00E5FF)        // Soft cyan (primary accent)
val LiquidNeonBlue = Color(0xFF0099FF)        // Soft blue (secondary)
val LiquidNeonViolet = Color(0xFF9D00FF)      // Soft violet (tertiary)
val LiquidNeonPink = Color(0xFFFF0080)        // Soft pink (error/highlight)

// Background gradients - Deep dark with subtle color
val LiquidBackgroundTop = Color(0xFF000000)    // Pure black
val LiquidBackgroundMid = Color(0xFF0A0A14)    // Dark blue-black
val LiquidBackgroundBottom = Color(0xFF0F0A1E) // Dark purple-black
val LiquidBackground = Color(0xE6000000)       // Semi-transparent black

// Glass surfaces - Low opacity for true glassmorphism
val LiquidGlassLight = Color(0x1FFFFFFF)       // 12% white (light glass)
val LiquidGlassMedium = Color(0x26FFFFFF)      // 15% white (medium glass)
val LiquidGlassDark = Color(0x0DFFFFFF)        // 5% white (subtle glass)

// Text colors - Professional standards for readability
val LiquidOnBackground = Color(0xFFF5F5F5)     // 96% white - softer for OLED
val LiquidOnSurface = Color(0xFFF5F5F5)        // 96% white - softer for OLED
val LiquidOnSurfaceVariant = Color(0xFFB3B3B3) // 70% white - better contrast for secondary text

// Flutter Music Theme Colors
val FlutterMusicThemeColor = Color(0xFF0A040C) // themeColor
val FlutterMusicPrimary1 = Color(0xFFDAEAF7) // primaryColor1
val FlutterMusicPrimary2 = Color(0xFFF2E7F0) // primaryColor2
val FlutterMusicAccent1 = Color(0xFF0EA5E0) // accentColor1
val FlutterMusicAccent2 = Color(0xFFFE385E) // accentColor2
val FlutterMusicSuccess = Color(0xFF5EFF43) // successColor

@Composable
fun OpenTuneTheme(
    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    expressive: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    customPrimaryColor: Color? = null,
    customSecondaryColor: Color? = null,
    appTheme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(darkTheme, pureBlack, themeColor, appTheme, themeMode, customPrimaryColor, customSecondaryColor) {
        val baseScheme = if (themeMode == ThemeMode.LIQUID_GLASS) {
            // Liquid Glass Theme (Keeping existing logic)
            if (darkTheme) {
                 // Dark Liquid Glass
                 val primaryColor = if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) customPrimaryColor else LiquidNeonBlue
                 val secondaryColor = if (appTheme == AppTheme.CUSTOM && customSecondaryColor != null) customSecondaryColor else if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) customPrimaryColor else LiquidNeonCyan
                 val tertiaryColor = if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) customPrimaryColor else LiquidNeonViolet 

                 ColorScheme(
                    primary = primaryColor,
                    onPrimary = Color(0xFFFFFFFF),              // Pure white for accent contrast
                    primaryContainer = primaryColor.copy(alpha = 0.2f),
                    onPrimaryContainer = Color(0xFFF5F5F5),     // Softer white
                    secondary = secondaryColor,
                    onSecondary = Color(0xFF000000),            // Pure black for neon contrast
                    secondaryContainer = secondaryColor.copy(alpha = 0.2f),
                    onSecondaryContainer = secondaryColor,
                    tertiary = tertiaryColor,
                    onTertiary = Color(0xFFFFFFFF),             // Pure white
                    tertiaryContainer = tertiaryColor.copy(alpha = 0.2f),
                    onTertiaryContainer = tertiaryColor,
                    background = LiquidBackgroundTop,
                    onBackground = Color(0xFFF5F5F5),           // Professional: 96% white
                    surface = LiquidGlassDark,
                    onSurface = Color(0xFFF5F5F5),              // Professional: 96% white
                    surfaceVariant = LiquidGlassMedium,
                    onSurfaceVariant = Color(0xFFB3B3B3),       // Professional: 70% white
                    error = LiquidNeonPink,
                    onError = Color(0xFFFFFFFF),                // Pure white
                    outline = primaryColor.copy(alpha = 0.5f),
                    outlineVariant = primaryColor.copy(alpha = 0.2f),
                    scrim = Color.Black,
                    inverseSurface = Color(0xFFF5F5F5),
                    inverseOnSurface = LiquidBackgroundTop,
                    inversePrimary = primaryColor,
                    surfaceTint = primaryColor,
                    surfaceBright = LiquidGlassLight,
                    surfaceDim = LiquidBackgroundBottom,
                    surfaceContainer = LiquidBackgroundMid,
                    surfaceContainerHigh = LiquidBackground,
                    surfaceContainerHighest = LiquidGlassMedium,
                    surfaceContainerLow = LiquidGlassDark,
                    surfaceContainerLowest = LiquidBackgroundTop,
                    errorContainer = LiquidNeonPink.copy(alpha = 0.2f),
                    onErrorContainer = LiquidNeonPink,
                )
            } else {
                 // Light Liquid Glass
                 val primaryColor = if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) customPrimaryColor else LiquidNeonBlue
                 val secondaryColor = if (appTheme == AppTheme.CUSTOM && customSecondaryColor != null) customSecondaryColor else if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) customPrimaryColor else LiquidNeonCyan
                 val tertiaryColor = if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) customPrimaryColor else LiquidNeonViolet

                 ColorScheme(
                    primary = primaryColor,
                    onPrimary = Color(0xFFFFFFFF),              // Pure white
                    primaryContainer = primaryColor.copy(alpha = 0.1f),
                    onPrimaryContainer = primaryColor,
                    secondary = secondaryColor,
                    onSecondary = Color(0xFFFFFFFF),            // Pure white
                    secondaryContainer = secondaryColor.copy(alpha = 0.1f),
                    onSecondaryContainer = Color(0xFF003E5C),
                    tertiary = tertiaryColor,
                    onTertiary = Color(0xFFFFFFFF),             // Pure white
                    tertiaryContainer = tertiaryColor.copy(alpha = 0.1f),
                    onTertiaryContainer = Color(0xFF2E003E),
                    background = Color(0xFFF2F2F7),             // iOS Light Gray
                    onBackground = Color(0xFF1A1A1A),           // Professional: 10% black
                    surface = Color.White.copy(alpha = 0.7f),   // Frosty White
                    onSurface = Color(0xFF1A1A1A),              // Professional: 10% black
                    surfaceVariant = Color(0xFFE5E5EA),
                    onSurfaceVariant = Color(0xFF666666),       // Professional: 60% black
                    error = LiquidNeonPink,
                    onError = Color.White,
                    outline = Color(0xFFC7C7CC),
                    outlineVariant = Color(0xFFE5E5EA),
                    scrim = Color.Black,
                    inverseSurface = Color.Black,
                    inverseOnSurface = Color.White,
                    inversePrimary = primaryColor,
                    surfaceTint = primaryColor,
                    surfaceBright = Color.White,
                    surfaceDim = Color(0xFFD1D1D6),
                    surfaceContainer = Color(0xFFF2F2F7),
                    surfaceContainerHigh = Color.White,
                    surfaceContainerHighest = Color.White,
                    surfaceContainerLow = Color(0xFFF2F2F7),
                    surfaceContainerLowest = Color.White,
                    errorContainer = LiquidNeonPink.copy(alpha = 0.1f),
                    onErrorContainer = Color(0xFF8C001A),
                )
            }
        } else if (appTheme == AppTheme.FLUTTER_MUSIC) {
            // Flutter Music Theme
             if (darkTheme) {
                 ColorScheme(
                    primary = FlutterMusicAccent2,
                    onPrimary = Color(0xFFFFFFFF),              // Professional: Pure white
                    primaryContainer = FlutterMusicAccent2.copy(alpha = 0.2f),
                    onPrimaryContainer = Color(0xFFF5F5F5),     // Professional: 96% white
                    secondary = FlutterMusicAccent1,
                    onSecondary = Color(0xFFFFFFFF),            // Professional: Pure white
                    secondaryContainer = FlutterMusicAccent1.copy(alpha = 0.2f),
                    onSecondaryContainer = Color(0xFFF5F5F5),   // Professional: 96% white
                    tertiary = FlutterMusicSuccess,
                    onTertiary = FlutterMusicThemeColor,
                    background = FlutterMusicThemeColor,
                    onBackground = Color(0xFFF5F5F5),           // Professional: 96% white (replaces DAEAf7)
                    surface = FlutterMusicThemeColor,
                    onSurface = Color(0xFFF5F5F5),              // Professional: 96% white (replaces DAEAf7)
                    surfaceVariant = Color(0xFF1A111B),
                    onSurfaceVariant = Color(0xFFB3B3B3),       // Professional: 70% white (replaces F2E7F0)
                    error = FlutterMusicAccent2,
                    onError = Color(0xFFFFFFFF),                // Professional: Pure white
                    outline = Color(0xFFB3B3B3).copy(alpha = 0.3f),
                    outlineVariant = Color(0xFFB3B3B3).copy(alpha = 0.1f),
                    scrim = Color.Black,
                    inverseSurface = Color(0xFFF5F5F5),
                    inverseOnSurface = FlutterMusicThemeColor,
                    inversePrimary = FlutterMusicAccent2,
                    surfaceTint = FlutterMusicAccent2,
                    surfaceBright = FlutterMusicThemeColor,
                    surfaceDim = FlutterMusicThemeColor,
                    surfaceContainer = Color(0xFF1A111B),
                    surfaceContainerHigh = Color(0xFF1A111B),
                    surfaceContainerHighest = Color(0xFF1A111B),
                    surfaceContainerLow = FlutterMusicThemeColor,
                    surfaceContainerLowest = FlutterMusicThemeColor,
                    errorContainer = FlutterMusicAccent2.copy(alpha = 0.2f),
                    onErrorContainer = Color(0xFFF5F5F5),       // Professional: 96% white
                    tertiaryContainer = FlutterMusicSuccess.copy(alpha = 0.2f),
                    onTertiaryContainer = FlutterMusicThemeColor,
                )
            } else {
                // Light Flutter Theme
                 ColorScheme(
                    primary = FlutterMusicAccent2,
                    onPrimary = Color(0xFFFFFFFF),              // Professional: Pure white
                    primaryContainer = FlutterMusicAccent2.copy(alpha = 0.1f),
                    onPrimaryContainer = Color(0xFF2E003E),
                    secondary = FlutterMusicAccent1,
                    onSecondary = Color(0xFFFFFFFF),            // Professional: Pure white
                    secondaryContainer = FlutterMusicAccent1.copy(alpha = 0.1f),
                    onSecondaryContainer = Color(0xFF003E5C),
                    tertiary = FlutterMusicSuccess,
                    onTertiary = Color(0xFF000000),             // Professional: Pure black
                    background = Color(0xFFFAFAFA),
                    onBackground = Color(0xFF1A1A1A),           // Professional: 10% black
                    surface = Color.White,
                    onSurface = Color(0xFF1A1A1A),              // Professional: 10% black
                    surfaceVariant = Color(0xFFF0F0F0),
                    onSurfaceVariant = Color(0xFF666666),       // Professional: 60% black
                    error = Color(0xFFB00020),
                    onError = Color.White,
                    outline = Color(0xFF79747E),
                    outlineVariant = Color(0xFFCAC4D0),
                    scrim = Color.Black,
                    inverseSurface = Color(0xFF313033),
                    inverseOnSurface = Color(0xFFF4EFF4),
                    inversePrimary = FlutterMusicAccent2,
                    surfaceTint = FlutterMusicAccent2,
                    surfaceBright = Color(0xFFF8F9FA),
                    surfaceDim = Color(0xFFE0E2E5),
                    surfaceContainer = Color(0xFFF3F3FA),
                    surfaceContainerHigh = Color(0xFFECECF4),
                    surfaceContainerHighest = Color(0xFFE6E6EE),
                    surfaceContainerLow = Color(0xFFF9F9FC),
                    surfaceContainerLowest = Color.White,
                    errorContainer = Color(0xFF8C001A).copy(alpha = 0.1f),
                    onErrorContainer = Color(0xFF410002),
                    tertiaryContainer = FlutterMusicSuccess.copy(alpha = 0.1f),
                    onTertiaryContainer = Color(0xFF002105),
                )
            }
        } else if (appTheme == AppTheme.CUSTOM && customPrimaryColor != null) {
            // CUSTOM THEME
            val seed = customPrimaryColor
            val generatedScheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), darkTheme, 0.0)
                .toColorScheme()
            
            if (customSecondaryColor != null) {
                generatedScheme.copy(
                    secondary = customSecondaryColor,
                    secondaryContainer = customSecondaryColor.copy(alpha = 0.2f),
                    onSecondaryContainer = if (darkTheme) Color(0xFFF5F5F5) else Color(0xFF1A1A1A),
                    // Ensure professional text colors
                    onSurfaceVariant = if (darkTheme) Color(0xFFB3B3B3) else Color(0xFF666666)
                )
            } else {
                generatedScheme.copy(
                    // Ensure professional text colors for custom theme
                    onSurfaceVariant = if (darkTheme) Color(0xFFB3B3B3) else Color(0xFF666666)
                )
            }
        } else {
             // Default / Dynamic Theme
             val effectiveThemeColor = themeColor
             SchemeTonalSpot(Hct.fromInt(effectiveThemeColor.toArgb()), darkTheme, 0.0)
                .toColorScheme()
        }
        
        baseScheme.pureBlack(pureBlack, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation =
        Palette
            .from(this)
            .maximumColorCount(8)
            .generate()
            .swatches
            .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(darkTheme: Boolean = false): List<Color> {
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

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun DynamicScheme.toColorScheme() =
    ColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
    )

fun ColorScheme.pureBlack(apply: Boolean, isDarkTheme: Boolean) =
    if (apply && isDarkTheme) {
        copy(
            surface = Color.Black,
            background = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceBright = Color.Black,
            surfaceDim = Color.Black,
            // Optimize text colors for pure black OLED - reduces eye strain
            onBackground = Color(0xFFF5F5F5),      // 96% white (softer than pure white)
            onSurface = Color(0xFFF5F5F5),         // 96% white
            onSurfaceVariant = Color(0xFFB3B3B3), // 70% white (professional secondary)
        )
    } else {
        this
    }

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}