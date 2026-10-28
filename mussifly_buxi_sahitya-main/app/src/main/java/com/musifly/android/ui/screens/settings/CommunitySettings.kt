package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.SettingsCategory
import com.musifly.android.ui.component.SettingsCategoryItem
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.secure.SecureString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community)) },
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {
            // Community buttons
            SettingsCategory(
                title = stringResource(R.string.community),
                items = listOf(
                    SettingsCategoryItem(
                        icon = painterResource(R.drawable.instagram),
                        title = { Text(stringResource(R.string.instagram)) },
                        iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFFE1306C), // Instagram Pink
                        onClick = { SecureString.openLink(context, SecureString.LINK_INSTA_WEB) }
                    ),
                    SettingsCategoryItem(
                        icon = painterResource(R.drawable.paypal),
                        title = { Text(stringResource(R.string.Donate)) },
                        isHighlighted = true,
                        iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF003087), // PayPal Blue
                        onClick = { SecureString.openLink(context, SecureString.LINK_PAYPAL_COM) }
                    ),
                    SettingsCategoryItem(
                        icon = painterResource(R.drawable.telegram),
                        title = { Text(stringResource(R.string.Telegramchanel)) },
                        iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF0088CC), // Telegram Blue
                        onClick = { SecureString.openLink(context, SecureString.LINK_TELEGRAM) }
                    ),
                    SettingsCategoryItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text(stringResource(R.string.about)) },
                        iconBackgroundColor = androidx.compose.ui.graphics.Color(0xFF607D8B), // Blue Grey
                        onClick = { navController.navigate("settings/about") }
                    )
                )
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
