package com.buxi.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun LibraryScreen(navController: NavController) {
    // Simplified Profile tab - no filter chips, just the main library content
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LibraryMixScreen(navController, filterContent = null)
    }
}
