package com.musifly.android.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.musifly.android.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    data object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    data object Explore : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "explore"
    )

    data object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.queue_music,
        iconIdActive = R.drawable.queue_music,
        route = "library"
    )

    data object Settings : Screens(
        titleId = R.string.settings,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "settings"
    )

    data object Profile : Screens(
        titleId = R.string.profile,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "profile"
    )

    data object UsernameCreation : Screens(
        titleId = R.string.profile,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "username_creation"
    )

    data object PrivacyPolicy : Screens(
        titleId = R.string.privacy,
        iconIdInactive = R.drawable.info,
        iconIdActive = R.drawable.info,
        route = "privacy_policy"
    )

    data object ArtistSelection : Screens(
        titleId = R.string.artist,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "artist_selection"
    )

    companion object {
        val MainScreens = listOf(Home, Explore, Library, Profile)
    }
}
