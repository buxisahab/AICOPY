package com.buxi.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.R
import com.buxi.music.constants.EnableNewPipeSourceKey
import com.buxi.music.constants.JioSaavnSourceKey
import com.buxi.music.constants.JossRedSourceKey
import com.buxi.music.constants.SearchSource
import com.buxi.music.constants.SearchSourceKey
import com.buxi.music.ui.component.IconButton
import com.buxi.music.ui.component.ListPreference
import com.buxi.music.ui.component.PreferenceGroupTitle
import com.buxi.music.ui.component.SwitchPreference
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.utils.backToMain
import com.buxi.music.utils.rememberEnumPreference
import com.buxi.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSourceSettings(
    navController: NavController,
) {
    val (primarySource, onPrimarySourceChange) = rememberEnumPreference(
        key = SearchSourceKey,
        defaultValue = SearchSource.YOUTUBE
    )
    
    val (newPipeEnabled, onNewPipeEnabledChange) = rememberPreference(
        key = EnableNewPipeSourceKey,
        defaultValue = true
    )
    val (jioSaavnEnabled, onJioSaavnEnabledChange) = rememberPreference(
        key = JioSaavnSourceKey,
        defaultValue = true
    )
    val (jossRedEnabled, onJossRedEnabledChange) = rememberPreference(
        key = JossRedSourceKey,
        defaultValue = true
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.primary_search_source)
        )

        Text(
            text = stringResource(R.string.music_source_preferences_desc),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        ListPreference(
            title = { Text(stringResource(R.string.primary_search_source)) },
            icon = { Icon(painterResource(R.drawable.search), null) },
            selectedValue = primarySource,
            values = listOf(SearchSource.YOUTUBE, SearchSource.JIOSAAVN, SearchSource.LOCAL),
            valueText = {
                when (it) {
                    SearchSource.YOUTUBE -> stringResource(R.string.music_source_youtube)
                    SearchSource.JIOSAAVN -> stringResource(R.string.music_source_jiosaavn)
                    SearchSource.LOCAL -> stringResource(R.string.library)
                }
            },
            onValueSelected = onPrimarySourceChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.music_source_fallback)
        )

        Text(
            text = stringResource(R.string.music_source_fallback_desc),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        SwitchPreference(
            title = { Text(stringResource(R.string.music_source_youtube)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            checked = newPipeEnabled,
            onCheckedChange = onNewPipeEnabledChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.music_source_jiosaavn)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            checked = jioSaavnEnabled,
            onCheckedChange = onJioSaavnEnabledChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.music_source_jossred)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            checked = jossRedEnabled,
            onCheckedChange = onJossRedEnabledChange
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.music_source_preferences)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ThemeDark
        )
    )
}
