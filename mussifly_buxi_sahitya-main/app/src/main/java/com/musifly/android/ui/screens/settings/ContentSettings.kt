package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.NotificationPermissionPreference
import com.musifly.android.R
import com.musifly.android.constants.CountryCodeToName
import com.musifly.android.constants.LanguageCodeToName
import com.musifly.android.constants.PreferredLyricsProvider
import com.musifly.android.constants.QuickPicks
import com.musifly.android.constants.SYSTEM_DEFAULT
import com.musifly.android.ui.component.EditTextPreference
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.LanguagePreference
import com.musifly.android.ui.component.ListPreference
import com.musifly.android.ui.component.LocaleManager
import com.musifly.android.ui.component.PreferenceGroupTitle
import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.DurationSliderPreference
import com.musifly.android.ui.component.SwitchPreference
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import java.net.Proxy

object PreferenceKeys {
    val ContentLanguageKey = stringPreferencesKey("content_language")
    val ContentCountryKey = stringPreferencesKey("content_country")
    val HideExplicitKey = booleanPreferencesKey("hide_explicit")
    val ProxyEnabledKey = booleanPreferencesKey("proxy_enabled")
    val ProxyTypeKey = stringPreferencesKey("proxy_type")
    val ProxyUrlKey = stringPreferencesKey("proxy_url")
    val TopSizeKey = stringPreferencesKey("top_size")
    val HistoryDurationKey = floatPreferencesKey("history_duration")
    val QuickPicksKey = stringPreferencesKey("quick_picks")
    val EnableKugouKey = booleanPreferencesKey("enable_kugou")
    val EnableLrcLibKey = booleanPreferencesKey("enable_lrclib")
    val PreferredLyricsProviderKey = stringPreferencesKey("preferred_lyrics_provider")
    val AppLanguageKey = stringPreferencesKey("app_language")
    // Streaming Engine Preferences
    val EnableJioSaavnKey = booleanPreferencesKey("enable_jiosaavn")
    val EnableYouTubeMusicKey = booleanPreferencesKey("enable_youtube_music")
    val EnableYouTubeVideoKey = booleanPreferencesKey("enable_youtube_video")
    val EnableSpotifyKey = booleanPreferencesKey("enable_spotify")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager.getInstance(context) }


    val (contentLanguage, onContentLanguageChange) = rememberPreference(
        key = PreferenceKeys.ContentLanguageKey,
        defaultValue = "system"
    )
    val (contentCountry, onContentCountryChange) = rememberPreference(
        key = PreferenceKeys.ContentCountryKey,
        defaultValue = "system"
    )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(
        key = PreferenceKeys.HideExplicitKey,
        defaultValue = false
    )
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(
        key = PreferenceKeys.ProxyEnabledKey,
        defaultValue = false
    )
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(
        key = PreferenceKeys.ProxyTypeKey,
        defaultValue = Proxy.Type.HTTP
    )
    val (proxyUrl, onProxyUrlChange) = rememberPreference(
        key = PreferenceKeys.ProxyUrlKey,
        defaultValue = "host:port"
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(
        key = PreferenceKeys.TopSizeKey,
        defaultValue = "50"
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        key = PreferenceKeys.HistoryDurationKey,
        defaultValue = 30f
    )
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(
        key = PreferenceKeys.QuickPicksKey,
        defaultValue = QuickPicks.QUICK_PICKS
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(
        key = PreferenceKeys.EnableKugouKey,
        defaultValue = true
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = PreferenceKeys.EnableLrcLibKey,
        defaultValue = true
    )
    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(
        key = PreferenceKeys.PreferredLyricsProviderKey,
        defaultValue = PreferredLyricsProvider.LRCLIB
    )
    val (selectedLanguage, setSelectedLanguage) = rememberPreference(
        key = PreferenceKeys.AppLanguageKey,
        defaultValue = "en"
    )

    val isLiquidGlass = isLiquidGlassActive()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.content)) },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // General settings
            SettingsGroup(
                title = stringResource(R.string.general)
            ) {
                ListPreference(
                    title = { Text(stringResource(R.string.content_language)) },
                    icon = { Icon(painterResource(R.drawable.language), null) },
                    iconBackgroundColor = Color(0xFF2196F3), // Blue
                    selectedValue = contentLanguage,
                    values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                    valueText = {
                        LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentLanguageChange,
                )
                

                ListPreference(
                    title = { Text(stringResource(R.string.content_country)) },
                    icon = { Icon(painterResource(R.drawable.location_on), null) },
                    iconBackgroundColor = Color(0xFF4CAF50), // Green
                    selectedValue = contentCountry,
                    values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                    valueText = {
                        CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentCountryChange,
                )
                

                // Hide explicit content
                SwitchPreference(
                    title = { Text(stringResource(R.string.hide_explicit)) },
                    icon = { Icon(painterResource(R.drawable.explicit), null) },
                    iconBackgroundColor = Color(0xFFF44336), // Red
                    checked = hideExplicit,
                    onCheckedChange = onHideExplicitChange,
                )


                NotificationPermissionPreference()


                // Application Language - Using consistent PreferenceEntry style
                LanguagePreference(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Proxy settings
            SettingsGroup(
                title = stringResource(R.string.proxy)
            ) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_proxy)) },
                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                    iconBackgroundColor = Color(0xFF673AB7), // Deep Purple
                    checked = proxyEnabled,
                    onCheckedChange = onProxyEnabledChange,
                )
                if (proxyEnabled) {
                        ListPreference(
                        title = { Text(stringResource(R.string.proxy_type)) },
                        selectedValue = proxyType,
                        values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                        valueText = { it.name },
                        onValueSelected = onProxyTypeChange,
                    )
                        EditTextPreference(
                        title = { Text(stringResource(R.string.proxy_url)) },
                        value = proxyUrl,
                        onValueChange = onProxyUrlChange,
                    )
                }
            }

            // Lyrics settings
            SettingsGroup(
                title = stringResource(R.string.lyrics)
            ) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_lrclib)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    iconBackgroundColor = Color(0xFFE91E63), // Pink
                    checked = enableLrclib,
                    onCheckedChange = onEnableLrclibChange,
                )
                

                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_kugou)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    iconBackgroundColor = Color(0xFF9C27B0), // Purple
                    checked = enableKugou,
                    onCheckedChange = onEnableKugouChange,
                )
                

                ListPreference(
                    title = { Text(stringResource(R.string.set_first_lyrics_provider)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                    selectedValue = preferredProvider,
                    values = listOf(PreferredLyricsProvider.KUGOU, PreferredLyricsProvider.LRCLIB),
                    valueText = {
                        it.name.toLowerCase(androidx.compose.ui.text.intl.Locale.current)
                            .capitalize(androidx.compose.ui.text.intl.Locale.current)
                    },
                    onValueSelected = onPreferredProviderChange,
                )
            }

            // Misc settings
             SettingsGroup(
                title = stringResource(R.string.misc)
            ) {
                EditTextPreference(
                    title = { Text(stringResource(R.string.top_length)) },
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    iconBackgroundColor = Color(0xFFFFA000), // Orange
                    value = lengthTop,
                    isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                    onValueChange = onLengthTopChange,
                )
                

                ListPreference(
                    title = { Text(stringResource(R.string.set_quick_picks)) },
                    icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                    iconBackgroundColor = Color(0xFF00BCD4), // Cyan
                    selectedValue = quickPicks,
                    values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
                    valueText = {
                        when (it) {
                            QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                            QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                        }
                    },
                    onValueSelected = onQuickPicksChange,
                )
                

                DurationSliderPreference(
                    title = { Text(stringResource(R.string.history_duration)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    iconBackgroundColor = Color(0xFF3F51B5), // Indigo
                    value = historyDuration,
                    onValueChange = onHistoryDurationChange,
                )
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}
