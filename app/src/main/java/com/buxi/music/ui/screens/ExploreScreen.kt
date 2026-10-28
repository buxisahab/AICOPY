package com.buxi.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.buxi.music.ui.theme.ThemeTextTertiary
import com.buxi.music.ui.theme.ThemeTextPrimary
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.LocalPlayerConnection
import com.buxi.music.R
import com.buxi.music.ui.component.LocalMenuState
import com.buxi.music.ui.component.NavigationTitle
import com.buxi.music.ui.component.TopSearch
import com.buxi.music.ui.component.shimmer.ShimmerHost
import com.buxi.music.ui.component.shimmer.TextPlaceholder
import com.buxi.music.viewmodels.ExploreViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val explorePage by viewModel.explorePage.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()



    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {

            explorePage?.moodAndGenres?.let { moodAndGenres ->
                NavigationTitle(
                    title = stringResource(R.string.mood_and_genres),
                    onClick = {
                        navController.navigate("mood_and_genres")
                    },
                )

                LazyHorizontalGrid(
                    rows = GridCells.Fixed(4),
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier.height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp),
                ) {
                    items(moodAndGenres) {
                        MoodAndGenresButton(
                            title = it.title,
                            onClick = {
                                navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                            },
                            modifier =
                                Modifier
                                    .padding(6.dp)
                                    .width(180.dp),
                        )
                    }
                }
                Spacer(
                    Modifier.height(
                        LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            .calculateBottomPadding()
                    )
                )
            }

            if (explorePage == null) {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier =
                            Modifier
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .width(250.dp),
                    )
                    repeat(4) {
                        Row {
                            repeat(2) {
                                TextPlaceholder(
                                    height = MoodAndGenresButtonHeight,
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 6.dp)
                                            .width(200.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
