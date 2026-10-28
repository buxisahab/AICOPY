package com.musifly.android.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.android.viewmodels.OnlineSearchSuggestionViewModel

@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel()
) {
    val navBackStackEntry = navController.currentBackStackEntry
    val initialQuery = navBackStackEntry?.arguments?.getString("query") ?: ""
    
    // We maintain local state for the query to support the TextField interaction
    // inside OnlineSearchScreen. However, the initial value comes from the route.
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { 
        mutableStateOf(TextFieldValue(initialQuery)) 
    }

    val source = navBackStackEntry?.arguments?.getString("source") ?: "ONLINE"

    if (source == "USERS") {
        UserSearchScreen(
            query = query.text,
            navController = navController,
            onDismiss = {}
        )
    } else {
        OnlineSearchScreen(
            query = query.text,
            onQueryChange = { newValue ->
                query = newValue
            },
            navController = navController,
            onSearch = { searchQuery ->
                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                navController.navigate("search/$source/$encodedQuery") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onDismiss = {
                navController.popBackStack()
            },
            viewModel = viewModel
        )
    }
}
