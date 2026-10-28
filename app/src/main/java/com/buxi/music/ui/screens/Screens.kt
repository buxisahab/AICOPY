package com.buxi.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.buxi.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    data object ForYou : Screens(
        titleId = R.string.for_you,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    data object Explore : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.explore_outlined,
        iconIdActive = R.drawable.explore_filled,
        route = "explore"
    )

    data object Profile : Screens(
        titleId = R.string.profile,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(ForYou, Explore, Profile)
    }
}
