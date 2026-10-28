package com.musifly.android.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDateTime
import java.time.ZoneOffset

val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val DarkModeKey = stringPreferencesKey("darkMode")
val PureBlackKey = booleanPreferencesKey("pureBlack")
val AppThemeKey = stringPreferencesKey("appTheme")
val ThemeModeKey = stringPreferencesKey("themeMode")
val FullScreenModeKey = booleanPreferencesKey("fullScreenMode")

// Liquid Glass Customization Keys
val LiquidGlassBlurRadiusKey = floatPreferencesKey("liquidGlassBlurRadius")
val LiquidGlassLensHeightKey = floatPreferencesKey("liquidGlassLensHeight")
val LiquidGlassLensAmountKey = floatPreferencesKey("liquidGlassLensAmount")
val LiquidGlassChromaticAberrationKey = booleanPreferencesKey("liquidGlassChromaticAberration")
val LiquidGlassVibrancyKey = booleanPreferencesKey("liquidGlassVibrancy")
val LiquidGlassNavBarHeightKey = floatPreferencesKey("liquidGlassNavBarHeight")
val LiquidGlassNavBarCornerRadiusKey = floatPreferencesKey("liquidGlassNavBarCornerRadius")
val LiquidGlassNavBarWidthKey = floatPreferencesKey("liquidGlassNavBarWidth")
val LiquidGlassNavShowLabelsKey = booleanPreferencesKey("liquidGlassNavShowLabels")
val LiquidGlassMiniPlayerHeightKey = floatPreferencesKey("liquidGlassMiniPlayerHeight")
val LiquidGlassMiniPlayerCornerRadiusKey = floatPreferencesKey("liquidGlassMiniPlayerCornerRadius")

// Standard Floating Docker Customization Keys
val EnableFloatingDockerKey = booleanPreferencesKey("enableFloatingDocker")
val FloatingNavBarWidthPercentKey = floatPreferencesKey("floatingNavBarWidthPercent")
val FloatingNavBarHeightDpKey = floatPreferencesKey("floatingNavBarHeightDp")
val ShowHomeTabsKey = booleanPreferencesKey("showHomeTabs")

val LiquidGlassSourceBarHeightKey = floatPreferencesKey("liquidGlassSourceBarHeight")
val LiquidGlassSourceBarCornerRadiusKey = floatPreferencesKey("liquidGlassSourceBarCornerRadius")
val LiquidGlassSourceBarWidthKey = floatPreferencesKey("liquidGlassSourceBarWidth")

// Liquid Glass Indicator/Bubble Settings
val LiquidGlassIndicatorColorKey = longPreferencesKey("liquidGlassIndicatorColor")
val LiquidGlassIndicatorAlphaKey = floatPreferencesKey("liquidGlassIndicatorAlpha")
val LiquidGlassUseAccentColorKey = booleanPreferencesKey("liquidGlassUseAccentColor")
val LiquidGlassTabBackgroundAlphaKey = floatPreferencesKey("liquidGlassTabBackgroundAlpha")

// Advanced Liquid Glass Settings (from AndroidLiquidGlass)
val LiquidGlassBleedBlurRadiusKey = floatPreferencesKey("liquidGlassBleedBlurRadius")
val LiquidGlassEccentricFactorKey = floatPreferencesKey("liquidGlassEccentricFactor")
val LiquidGlassEnableDispersionKey = booleanPreferencesKey("liquidGlassEnableDispersion")
val LiquidGlassEnableUnsafeValuesKey = booleanPreferencesKey("liquidGlassEnableUnsafeValues")
val LiquidGlassCornerRadiusKey = floatPreferencesKey("liquidGlassCornerRadius")
val LiquidGlassChromaticAberrationAmountKey = floatPreferencesKey("liquidGlassChromaticAberrationAmount")

// Liquid Glass Color Control Keys
val LiquidGlassBrightnessKey = floatPreferencesKey("liquidGlassBrightness")
val LiquidGlassContrastKey = floatPreferencesKey("liquidGlassContrast")
val LiquidGlassSaturationKey = floatPreferencesKey("liquidGlassSaturation")
val LiquidGlassExposureKey = floatPreferencesKey("liquidGlassExposure")
val LiquidGlassGammaKey = floatPreferencesKey("liquidGlassGamma")

// Liquid Glass Highlight Detail Keys
val LiquidGlassHighlightIntensityKey = floatPreferencesKey("liquidGlassHighlightIntensity")
val LiquidGlassHighlightAngleKey = floatPreferencesKey("liquidGlassHighlightAngle")
val LiquidGlassHighlightFalloffKey = floatPreferencesKey("liquidGlassHighlightFalloff")

// Liquid Glass Inner Shadow Keys
val LiquidGlassInnerShadowRadiusKey = floatPreferencesKey("liquidGlassInnerShadowRadius")
val LiquidGlassInnerShadowAlphaKey = floatPreferencesKey("liquidGlassInnerShadowAlpha")

// Extra Lens Feature Keys
val LiquidGlassDepthEffectKey = booleanPreferencesKey("liquidGlassDepthEffect")

val DefaultOpenTabKey = stringPreferencesKey("defaultOpenTab")
val SlimNavBarKey = booleanPreferencesKey("slimNavBar")
val GridItemsSizeKey = stringPreferencesKey("gridItemSize")
val SliderStyleKey = stringPreferencesKey("sliderStyle")

enum class SliderStyle {
    DEFAULT,
    SQUIGGLY,
    SLIM,
}

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrclib")
val EnableWebSearchLyricsKey = booleanPreferencesKey("enableWebSearchLyrics")
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
val LastNewReleaseCheckKey = longPreferencesKey("last_new_release_check")
val minPlaybackDurKey = intPreferencesKey("minPlaybackDur")
val ProxyEnabledKey = booleanPreferencesKey("proxyEnabled")
val ProxyUrlKey = stringPreferencesKey("proxyUrl")
val ProxyTypeKey = stringPreferencesKey("proxyType")
val YtmSyncKey = booleanPreferencesKey("ytmSync")

val AudioQualityKey = stringPreferencesKey("audioQuality")

enum class AudioQuality {
    AUTO,
    HIGH,
    LOW,
}

val PersistentQueueKey = booleanPreferencesKey("persistentQueue")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val AudioNormalizationKey = booleanPreferencesKey("audioNormalization")
val AutoLoadMoreKey = booleanPreferencesKey("autoLoadMore")
val SimilarContent = booleanPreferencesKey("similarContent")
val AutoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val StopMusicOnTaskClearKey = booleanPreferencesKey("stopMusicOnTaskClear")
val UseNewPipeStreamingKey = booleanPreferencesKey("useNewPipeStreaming")
// JossRedFallbackKey removed - fake fallback mechanism

// DSP Settings
val BassBoostEnabledKey = booleanPreferencesKey("bassBoostEnabled")
val BassBoostStrengthKey = intPreferencesKey("bassBoostStrength")
val TrebleBoostEnabledKey = booleanPreferencesKey("trebleBoostEnabled")
val TrebleBoostStrengthKey = intPreferencesKey("trebleBoostStrength")
val VolumeBoostEnabledKey = booleanPreferencesKey("volumeBoostEnabled")
val VolumeBoostGainKey = intPreferencesKey("volumeBoostGain")
val EqualizerEnabledKey = booleanPreferencesKey("equalizerEnabled")
val EqualizerPresetKey = stringPreferencesKey("equalizerPreset")
val ReverbEnabledKey = booleanPreferencesKey("reverbEnabled")
val ReverbPresetKey = intPreferencesKey("reverbPreset")
val VirtualizerEnabledKey = booleanPreferencesKey("virtualizerEnabled")
val VirtualizerStrengthKey = intPreferencesKey("virtualizerStrength")
val Surround3dEnabledKey = booleanPreferencesKey("surround3dEnabled")
val Surround3dStrengthKey = intPreferencesKey("surround3dStrength")
val DolbyAtmosEnabledKey = booleanPreferencesKey("dolbyAtmosEnabled")
val DolbyAtmosModeKey = stringPreferencesKey("dolbyAtmosMode")
val AiDspEnabledKey = booleanPreferencesKey("aiDspEnabled")
val ButtonSoundEnabledKey = booleanPreferencesKey("buttonSoundEnabled")
val LiveAlertEnabledKey = booleanPreferencesKey("liveAlertEnabled")
val VerticalSwipeSongChangeKey = booleanPreferencesKey("verticalSwipeSongChange")
val HapticFeedbackEnabledKey = booleanPreferencesKey("hapticFeedbackEnabled")
val HapticFeedbackIntensityKey = stringPreferencesKey("hapticFeedbackIntensity")

val DSPModeKey = stringPreferencesKey("dspMode")
val NativeDSPCompressorEnabledKey = booleanPreferencesKey("nativeDSPCompressorEnabled")
val NativeDSPCompressorThresholdKey = floatPreferencesKey("nativeDSPCompressorThreshold")
val NativeDSPCompressorRatioKey = floatPreferencesKey("nativeDSPCompressorRatio")


val MaxImageCacheSizeKey = intPreferencesKey("maxImageCacheSize")
val MaxSongCacheSizeKey = intPreferencesKey("maxSongCacheSize")
val CustomDownloadDirectoryKey = stringPreferencesKey("customDownloadDirectory")

val DisableLoadMoreWhenRepeatAllKey = booleanPreferencesKey("disableLoadMoreWhenRepeatAll")
val ScrobbleDelayPercentKey = floatPreferencesKey("scrobbleDelayPercent")
val ScrobbleMinSongDurationKey = intPreferencesKey("scrobbleMinSongDuration")
val ScrobbleDelaySecondsKey = intPreferencesKey("scrobbleDelaySeconds")
val EnableLastFMScrobblingKey = booleanPreferencesKey("enableLastFMScrobbling")
val LastFMUseNowPlaying = booleanPreferencesKey("lastFMUseNowPlaying")
val AudioOffload = booleanPreferencesKey("audioOffload")

val PlayerTextAlignmentKey = stringPreferencesKey("playerTextAlignment")

val RotateBackgroundKey = booleanPreferencesKey("rotate_background")


val SmallButtonsShapeKey = stringPreferencesKey("small_buttons_shape")
const val DefaultSmallButtonsShape = "Pill"

val PauseListenHistoryKey = booleanPreferencesKey("pauseListenHistory")
val PauseSearchHistoryKey = booleanPreferencesKey("pauseSearchHistory")
val FirstLaunchCompletedKey = booleanPreferencesKey("firstLaunchCompleted")



val ChipSortTypeKey = stringPreferencesKey("chipSortType")
val SongSortTypeKey = stringPreferencesKey("songSortType")
val SongSortDescendingKey = booleanPreferencesKey("songSortDescending")
val PlaylistSongSortTypeKey = stringPreferencesKey("playlistSongSortType")
val PlaylistSongSortDescendingKey = booleanPreferencesKey("playlistSongSortDescending")
val AutoPlaylistSongSortTypeKey = stringPreferencesKey("autoPlaylistSongSortType")
val AutoPlaylistSongSortDescendingKey = booleanPreferencesKey("autoPlaylistSongSortDescending")
val ArtistSortTypeKey = stringPreferencesKey("artistSortType")
val ArtistSortDescendingKey = booleanPreferencesKey("artistSortDescending")
val AlbumSortTypeKey = stringPreferencesKey("albumSortType")
val AlbumSortDescendingKey = booleanPreferencesKey("albumSortDescending")
val PlaylistSortTypeKey = stringPreferencesKey("playlistSortType")
val PlaylistSortDescendingKey = booleanPreferencesKey("playlistSortDescending")
val ArtistSongSortTypeKey = stringPreferencesKey("artistSongSortType")
val ArtistSongSortDescendingKey = booleanPreferencesKey("artistSongSortDescending")
val MixSortTypeKey = stringPreferencesKey("mixSortType")
val MixSortDescendingKey = booleanPreferencesKey("albumSortDescending")

val SongFilterKey = stringPreferencesKey("songFilter")
val ArtistFilterKey = stringPreferencesKey("artistFilter")
val AlbumFilterKey = stringPreferencesKey("albumFilter")


val LyricsScrollKey = booleanPreferencesKey("lyricsScrollKey")




val ArtistViewTypeKey = stringPreferencesKey("artistViewType")
val AlbumViewTypeKey = stringPreferencesKey("albumViewType")
val PlaylistViewTypeKey = stringPreferencesKey("playlistViewType")

val PlaylistEditLockKey = booleanPreferencesKey("playlistEditLock")
val QuickPicksKey = stringPreferencesKey("discover")
val PreferredLyricsProviderKey = stringPreferencesKey("lyricsProvider")
val QueueEditLockKey = booleanPreferencesKey("queueEditLock")

val LyricFontSizeKey = intPreferencesKey("lyricFontSize")
val fullScreenLyricsKey = booleanPreferencesKey("fullScreenLyrics")

enum class LibraryViewType {
    LIST,
    GRID,
    ;

    fun toggle() =
        when (this) {
            LIST -> GRID
            GRID -> LIST
        }
}

enum class SongFilter {
    LIBRARY,
    LIKED,
    DOWNLOADED
}

enum class ArtistFilter {
    LIBRARY,
    LIKED
}

enum class AlbumFilter {
    LIBRARY,
    LIKED
}

enum class SongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class PlaylistSongSortType {
    CUSTOM,
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class AutoPlaylistSongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class ArtistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    PLAY_TIME,
}

enum class ArtistSongSortType {
    CREATE_DATE,
    NAME,
    PLAY_TIME,
}

enum class AlbumSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    YEAR,
    SONG_COUNT,
    LENGTH,
    PLAY_TIME,
}

enum class PlaylistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    LAST_UPDATED,
}

enum class MixSortType {
    CREATE_DATE,
    NAME,
    LAST_UPDATED,
}

enum class GridItemSize {
    SMALL,
    BIG,
}

enum class MyTopFilter {
    ALL_TIME,
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ;

    fun toTimeMillis(): Long =
        when (this) {
            DAY ->
                LocalDateTime
                    .now()
                    .minusDays(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            WEEK ->
                LocalDateTime
                    .now()
                    .minusWeeks(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            MONTH ->
                LocalDateTime
                    .now()
                    .minusMonths(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            YEAR ->
                LocalDateTime
                    .now()
                    .minusMonths(12)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            ALL_TIME -> 0
        }
}

enum class QuickPicks {
    QUICK_PICKS,
    LAST_LISTEN,
}

enum class PreferredLyricsProvider {
    LRCLIB,
    KUGOU,
}

enum class PlayerBackgroundStyle {
    DEFAULT,
    GRADIENT,
    BLUR,
}

val CustomPrimaryColorKey = intPreferencesKey("customPrimaryColor")
val CustomSecondaryColorKey = intPreferencesKey("customSecondaryColor")

// App Theme Styles - Different color themes for the app
enum class AppTheme {
    DEFAULT,        // Default blue theme (0xFF4285F4)
    FLUTTER_MUSIC,  // Flutter Music theme (dark purple/pink with blue accents)
    CUSTOM,         // User defined colors
}

// Theme Mode - Overall theme style (Light, Dark)
enum class ThemeMode {
    SYSTEM,         // Follow system settings
    LIGHT,          // Light theme with soft whites and pastel gradients
    DARK,           // Dark theme optimized for OLED (true black)
    LIQUID_GLASS,   // Liquid Glass theme
}

// Player Layout Styles - Different UI layouts for the music player
enum class PlayerLayoutStyle {
    INSTAGRAM,  // Instagram Reels-style fullscreen player (DEFAULT)
    SPOTIFY,    // Spotify-style dark theme player

    BLOOME,     // Bloome-style player with tabs and up-next panel
    MUSSIFLY_PREMIUM // Mussifly Premium Player (Default)
}

val PlayerLayoutStyleKey = stringPreferencesKey("playerLayoutStyle_v2")

// Thumbnail Display Modes - Different thumbnail rendering styles for the player
enum class ThumbnailDisplayMode {
    BLUR_HD,        // Blur Background + HD Thumbnail (Instagram/Reels/Resso Style)
    FIT_CENTER,     // FitCenter Mode (No Crop, No Blur)
    CENTER_CROP,    // CenterCrop Mode (Immersive Fullscreen)
}

val ThumbnailDisplayModeKey = stringPreferencesKey("thumbnailDisplayMode")

enum class PlayerButtonsStyle {
    DEFAULT,
    SECONDARY,
}

enum class EqualizerPreset {
    NORMAL,
    POP,
    ROCK,
    JAZZ,
    CLASSICAL,
    ELECTRONIC,
    HIP_HOP,
    ACOUSTIC,
    BASS_BOOST,
    TREBLE_BOOST,
    VOCAL,
    DANCE,
    METAL,
    COUNTRY,
    FLAT,
}

enum class DolbyAtmosMode {
    OFF,
    MUSIC,
    MOVIE,
    GAME,
    VOICE,
}

enum class HapticFeedbackIntensity {
    OFF,
    LIGHT,
    MEDIUM,
    STRONG,
}

val TopSize = stringPreferencesKey("topSize")
val HistoryDuration = floatPreferencesKey("historyDuration")

val PlayerBackgroundStyleKey = stringPreferencesKey("playerBackgroundStyle")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val LyricsTextPositionKey = stringPreferencesKey("lyricsTextPosition")
val LyricsClickKey = booleanPreferencesKey("lyricsClick")
val TranslateLyricsKey = booleanPreferencesKey("translateLyrics")

val PlayerVolumeKey = floatPreferencesKey("playerVolume")
val RepeatModeKey = intPreferencesKey("repeatMode")
val PlayerButtonsStyleKey = stringPreferencesKey("player_buttons_style")

val SearchSourceKey = stringPreferencesKey("searchSource")
val SwipeThumbnailKey = booleanPreferencesKey("swipeThumbnail")
val WelcomeJoinedKey = booleanPreferencesKey("welcome_joined")

enum class SearchSource {
    LOCAL,
    ONLINE,
    SAAVN,
    USERS,
    ;

    fun toggle() =
        when (this) {
            LOCAL -> ONLINE
            ONLINE -> SAAVN
            SAAVN -> USERS
            USERS -> LOCAL
        }
}

val VisitorDataKey = stringPreferencesKey("visitorData")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val AccountPhotoUrlKey = stringPreferencesKey("account_photo_url")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val AccountNameKey = stringPreferencesKey("accountName")
val AccountEmailKey = stringPreferencesKey("accountEmail")
val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")

// Liquid Glass Keys removed

val LanguageCodeToName =
    mapOf(
        "af" to "Afrikaans",
        "az" to "Azərbaycan",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Malaysia",
        "ca" to "Català",
        "cs" to "Čeština",
        "da" to "Dansk",
        "de" to "Deutsch",
        "et" to "Eesti",
        "en-GB" to "English (UK)",
        "en" to "English (US)",
        "es" to "Español (España)",
        "es-419" to "Español (Latinoamérica)",
        "eu" to "Euskara",
        "fil" to "Filipino",
        "fr" to "Français",
        "fr-CA" to "Français (Canada)",
        "gl" to "Galego",
        "hr" to "Hrvatski",
        "zu" to "IsiZulu",
        "is" to "Íslenska",
        "it" to "Italiano",
        "sw" to "Kiswahili",
        "lt" to "Lietuvių",
        "hu" to "Magyar",
        "nl" to "Nederlands",
        "no" to "Norsk",
        "or" to "Odia",
        "uz" to "O‘zbe",
        "pl" to "Polski",
        "pt-PT" to "Português",
        "pt" to "Português (Brasil)",
        "ro" to "Română",
        "sq" to "Shqip",
        "sk" to "Slovenčina",
        "sl" to "Slovenščina",
        "fi" to "Suomi",
        "sv" to "Svenska",
        "bo" to "Tibetan བོད་སྐད།",
        "vi" to "Tiếng Việt",
        "tr" to "Türkçe",
        "bg" to "Български",
        "ky" to "Кыргызча",
        "kk" to "Қазақ Тілі",
        "mk" to "Македонски",
        "mn" to "Монгол",
        "ru" to "Русский",
        "sr" to "Српски",
        "uk" to "Українська",
        "el" to "Ελληνικά",
        "hy" to "Հայերեն",
        "iw" to "עברית",
        "ur" to "اردو",
        "ar" to "العربية",
        "fa" to "فارسی",
        "ne" to "नेपाली",
        "mr" to "मराठी",
        "hi" to "हिन्दी",
        "bn" to "বাংলা",
        "pa" to "ਪੰਜਾਬੀ",
        "gu" to "ગુજરાતી",
        "ta" to "தமிழ்",
        "te" to "తెలుగు",
        "kn" to "ಕನ್ನಡ",
        "ml" to "മലയാളം",
        "si" to "සිංහල",
        "th" to "ภาษาไทย",
        "lo" to "ລາວ",
        "my" to "ဗမာ",
        "ka" to "ქართული",
        "am" to "አማርኛ",
        "km" to "ខ្មែរ",
        "zh-CN" to "中文 (简体)",
        "zh-TW" to "中文 (繁體)",
        "zh-HK" to "中文 (香港)",
        "ja" to "日本語",
        "ko" to "한국어",
    )

val CountryCodeToName =
    mapOf(
        "DZ" to "Algeria",
        "AR" to "Argentina",
        "AU" to "Australia",
        "AT" to "Austria",
        "AZ" to "Azerbaijan",
        "BH" to "Bahrain",
        "BD" to "Bangladesh",
        "BY" to "Belarus",
        "BE" to "Belgium",
        "BO" to "Bolivia",
        "BA" to "Bosnia and Herzegovina",
        "BR" to "Brazil",
        "BG" to "Bulgaria",
        "KH" to "Cambodia",
        "CA" to "Canada",
        "CL" to "Chile",
        "HK" to "Hong Kong",
        "CO" to "Colombia",
        "CR" to "Costa Rica",
        "HR" to "Croatia",
        "CY" to "Cyprus",
        "CZ" to "Czech Republic",
        "DK" to "Denmark",
        "DO" to "Dominican Republic",
        "EC" to "Ecuador",
        "EG" to "Egypt",
        "SV" to "El Salvador",
        "EE" to "Estonia",
        "FI" to "Finland",
        "FR" to "France",
        "GE" to "Georgia",
        "DE" to "Germany",
        "GH" to "Ghana",
        "GR" to "Greece",
        "GT" to "Guatemala",
        "HN" to "Honduras",
        "HU" to "Hungary",
        "IS" to "Iceland",
        "IN" to "India",
        "ID" to "Indonesia",
        "IQ" to "Iraq",
        "IE" to "Ireland",
        "IL" to "Israel",
        "IT" to "Italy",
        "JM" to "Jamaica",
        "JP" to "Japan",
        "JO" to "Jordan",
        "KZ" to "Kazakhstan",
        "KE" to "Kenya",
        "KR" to "South Korea",
        "KW" to "Kuwait",
        "LA" to "Lao",
        "LV" to "Latvia",
        "LB" to "Lebanon",
        "LY" to "Libya",
        "LI" to "Liechtenstein",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "MK" to "Macedonia",
        "MY" to "Malaysia",
        "MT" to "Malta",
        "MX" to "Mexico",
        "ME" to "Montenegro",
        "MA" to "Morocco",
        "NP" to "Nepal",
        "NL" to "Netherlands",
        "NZ" to "New Zealand",
        "NI" to "Nicaragua",
        "NG" to "Nigeria",
        "NO" to "Norway",
        "OM" to "Oman",
        "PK" to "Pakistan",
        "PA" to "Panama",
        "PG" to "Papua New Guinea",
        "PY" to "Paraguay",
        "PE" to "Peru",
        "PH" to "Philippines",
        "PL" to "Poland",
        "PT" to "Portugal",
        "PR" to "Puerto Rico",
        "QA" to "Qatar",
        "RO" to "Romania",
        "RU" to "Russian Federation",
        "SA" to "Saudi Arabia",
        "SN" to "Senegal",
        "RS" to "Serbia",
        "SG" to "Singapore",
        "SK" to "Slovakia",
        "SI" to "Slovenia",
        "ZA" to "South Africa",
        "ES" to "Spain",
        "LK" to "Sri Lanka",
        "SE" to "Sweden",
        "CH" to "Switzerland",
        "TW" to "Taiwan",
        "TZ" to "Tanzania",
        "TH" to "Thailand",
        "TN" to "Tunisia",
        "TR" to "Turkey",
        "UG" to "Uganda",
        "UA" to "Ukraine",
        "AE" to "United Arab Emirates",
        "GB" to "United Kingdom",
        "US" to "United States",
        "UY" to "Uruguay",
        "VE" to "Venezuela (Bolivarian Republic)",
        "VN" to "Vietnam",
        "YE" to "Yemen",
        "ZW" to "Zimbabwe",
    )
