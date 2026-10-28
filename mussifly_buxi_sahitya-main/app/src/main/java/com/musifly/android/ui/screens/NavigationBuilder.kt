package com.musifly.android.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.musifly.android.BuildConfig
import com.musifly.android.constants.SearchSource
import com.musifly.android.ui.screens.artist.ArtistItemsScreen
import com.musifly.android.ui.screens.artist.ArtistScreen
import com.musifly.android.ui.screens.artist.ArtistSongsScreen
import com.musifly.android.ui.screens.artist.ArtistSelectionScreen
import com.musifly.android.ui.screens.library.CachePlaylistScreen
import com.musifly.android.ui.screens.library.LibraryScreen
import com.musifly.android.ui.screens.playlist.AutoPlaylistScreen
import com.musifly.android.ui.screens.playlist.LocalPlaylistScreen
import com.musifly.android.ui.screens.playlist.OnlinePlaylistScreen
import com.musifly.android.ui.screens.playlist.TopPlaylistScreen
import com.musifly.android.ui.screens.search.OnlineSearchResult
import com.musifly.android.ui.screens.settings.AboutScreen
import com.musifly.android.ui.screens.settings.AppearanceSettings
import com.musifly.android.ui.screens.settings.AudioEffectsSettings
import com.musifly.android.ui.screens.settings.BackupAndRestore
import com.musifly.android.ui.screens.settings.BeautifulAccountScreen
import com.musifly.android.ui.screens.settings.CommunitySettings
import com.musifly.android.ui.screens.settings.ContentSettings
import com.musifly.android.ui.screens.settings.UserListScreen
import com.musifly.android.ui.screens.settings.DSPSettings

import com.musifly.android.ui.screens.settings.GoogleAccountScreen
import com.musifly.android.ui.screens.settings.PlayerSettings
import com.musifly.android.ui.screens.settings.PrivacySettings
import com.musifly.android.ui.screens.settings.SettingsScreen
import com.musifly.android.ui.screens.settings.StorageSettings
import com.musifly.android.viewmodels.StoryViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    composable(Screens.Explore.route) {
        // Explore now opens search bar directly - no separate screen needed
        // ExploreScreen(navController)
    }
    composable(Screens.Profile.route) {
        BeautifulAccountScreen(navController, showBackButton = false)
    }
    composable(
        route = "profile/{userId}",
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        BeautifulAccountScreen(navController, showBackButton = true, userId = userId)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("downloads") {
        DownloadsScreen(navController = navController)
    }
    // Mood and Genres screen removed as per user request
    // composable("mood_and_genres") {
    //     MoodAndGenresScreen(navController, scrollBehavior)
    // }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("notifications") {
        NotificationScreen(navController, scrollBehavior)
    }






    composable(
        route = "search/{source}/{query}",
        arguments =
            listOf(
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = SearchSource.ONLINE.name
                },
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        OnlineSearchResult(navController)
    }

    // Keep old route for safety or deep links
    composable(
        route = "search/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId")!!
        if (artistId.startsWith("LA")) {
            ArtistSongsScreen(navController, scrollBehavior)
        } else {
            ArtistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }



    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }


    composable("settings") {
        val latestVersion by mutableLongStateOf(BuildConfig.VERSION_CODE.toLong())
        SettingsScreen(latestVersion, navController, scrollBehavior)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/account") {
        BeautifulAccountScreen(navController)
    }
    composable("settings/google_account") {
        GoogleAccountScreen(navController, scrollBehavior)
    }
    composable(
        route = "user_list/{userId}/{isFollowers}",
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("isFollowers") { type = NavType.BoolType }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val isFollowers = backStackEntry.arguments?.getBoolean("isFollowers") ?: true
        UserListScreen(navController, userId, isFollowers)
    }
    composable("liked_songs") {
        LikedSongsScreen(navController, scrollBehavior)
    }
    composable("user_playlists") {
        UserPlaylistListScreen(navController, scrollBehavior)
    }
    composable(
        route = "user_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
    ) { backStackEntry ->
        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
        UserPlaylistDetailsScreen(playlistId, navController, scrollBehavior)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/audio_effects") {
        AudioEffectsSettings(navController, scrollBehavior)
    }
    composable("settings/dsp") {
        DSPSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }

    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
    composable("settings/community") {
        CommunitySettings(navController, scrollBehavior)
    }
    composable("login") {
        LoginScreen(navController)
    }
    composable("permission") {
        PermissionScreen(navController)
    }
    composable("google_login") {
        GoogleLoginScreen(navController)
    }
    composable(Screens.UsernameCreation.route) {
        UsernameCreationScreen(navController)
    }
    composable(Screens.PrivacyPolicy.route) {
        PrivacyPolicyScreen(navController)
    }
    composable(Screens.ArtistSelection.route) {
        ArtistSelectionScreen(navController = navController)
    }
    composable(
        route = "story_view/{userId}",
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val storyViewModel: StoryViewModel = hiltViewModel()
        val stories by storyViewModel.stories.collectAsState()
        val group = stories.find { it.userId == userId }
        
        LaunchedEffect(group) {
            if (group == null || group.stories.isEmpty()) {
                navController.popBackStack()
            }
        }

        if (group == null || group.stories.isEmpty()) {
            return@composable
        }
        
        StoryViewScreen(
            viewModel = storyViewModel,
            group = group,
            navController = navController,
            onDeleteClick = { storyId, mediaUrl ->
                storyViewModel.deleteStory(storyId, mediaUrl)
            },
            onAllStoriesViewed = { navController.popBackStack() },
            onNextUser = {
                val nextIndex = stories.indexOf(group) + 1
                if (nextIndex < stories.size) {
                    val nextUserId = stories[nextIndex].userId
                    navController.popBackStack()
                    navController.navigate("story_view/$nextUserId")
                } else {
                    navController.popBackStack()
                }
            },
            onPrevUser = {
                val prevIndex = stories.indexOf(group) - 1
                if (prevIndex >= 0) {
                    val prevUserId = stories[prevIndex].userId
                    navController.popBackStack()
                    navController.navigate("story_view/$prevUserId")
                } else {
                    navController.popBackStack()
                }
            }
        )
    }
    composable(
        route = "story_editor/{encodedUri}",
        arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
    ) { backStackEntry ->
        val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: return@composable
        val storyViewModel: StoryViewModel = hiltViewModel()
        StoryEditorScreen(
            navController = navController,
            viewModel = storyViewModel,
            encodedUri = encodedUri
        )
    }
}

