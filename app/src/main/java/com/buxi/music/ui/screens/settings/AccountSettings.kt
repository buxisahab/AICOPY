package com.buxi.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.utils.parseCookieString
import com.buxi.music.App.Companion.forgetAccount
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.R
import com.buxi.music.constants.AccountChannelHandleKey
import com.buxi.music.constants.AccountEmailKey
import com.buxi.music.constants.AccountNameKey
import com.buxi.music.constants.DataSyncIdKey
import com.buxi.music.constants.InnerTubeCookieKey
import com.buxi.music.constants.UseLoginForBrowse
import com.buxi.music.constants.VisitorDataKey
import com.buxi.music.constants.YtmSyncKey
import com.buxi.music.ui.component.IconButton
import com.buxi.music.ui.component.InfoLabel
import com.buxi.music.ui.component.PreferenceEntry
import com.buxi.music.ui.component.PreferenceGroupTitle
import com.buxi.music.ui.component.SwitchPreference
import com.buxi.music.ui.component.TextFieldDialog
import com.buxi.music.ui.utils.backToMain
import com.buxi.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (accountName, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(
        AccountChannelHandleKey,
        ""
    )
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

    // Función para obtener el nombre de cuenta de manera segura
    val getAccountDisplayName =
        remember(accountName, accountEmail, accountChannelHandle, isLoggedIn) {
            when {
                !isLoggedIn -> ""
                accountName.isNotBlank() -> accountName
                accountEmail.isNotBlank() -> accountEmail.substringBefore("@")
                accountChannelHandle.isNotBlank() -> accountChannelHandle
                else -> "Usuario sin nombre" // Fallback para evitar crashes
            }
        }

    // Función para obtener la descripción de la cuenta de manera segura
    val getAccountDescription = remember(accountEmail, accountChannelHandle, isLoggedIn) {
        when {
            !isLoggedIn -> null
            accountEmail.isNotBlank() -> accountEmail
            accountChannelHandle.isNotBlank() -> accountChannelHandle
            else -> null
        }
    }

    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, defaultValue = true)

    var showToken: Boolean by remember {
        mutableStateOf(false)
    }
    var showTokenEditor by remember {
        mutableStateOf(false)
    }
    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    stringResource(R.string.logout),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Are you sure you want to log out of your account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onInnerTubeCookieChange("")
                        onAccountNameChange("")
                        onAccountEmailChange("")
                        onAccountChannelHandleChange("")
                        onVisitorDataChange("")
                        onDataSyncIdChange("")
                        forgetAccount(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4D67),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Color.White.copy(0.6f))
                }
            },
            containerColor = Color(0xFF1E1E28),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account)) },
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .verticalScroll(rememberScrollState())
        ) {

            PreferenceGroupTitle(
                title = stringResource(R.string.google),
            )

            PreferenceEntry(
                title = {
                    Text(
                        if (isLoggedIn) {
                            getAccountDisplayName.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.login)
                        } else {
                            stringResource(R.string.login)
                        }
                    )
                },
                description = if (isLoggedIn) getAccountDescription else null,
                icon = { Icon(painterResource(R.drawable.login), null) },
                trailingContent = {
                    if (isLoggedIn) {
                        OutlinedButton(onClick = {
                            showLogoutDialog = true
                        }) {
                            Text(stringResource(R.string.logout))
                        }
                    }
                },
                onClick = { if (!isLoggedIn) navController.navigate("login") }
            )

            if (showTokenEditor) {
                val text =
                    "***INNERTUBE COOKIE*** =${innerTubeCookie}\n\n***VISITOR DATA*** =${visitorData}\n\n***DATASYNC ID*** =${dataSyncId}\n\n***ACCOUNT NAME*** =${accountName}\n\n***ACCOUNT EMAIL*** =${accountEmail}\n\n***ACCOUNT CHANNEL HANDLE*** =${accountChannelHandle}"
                TextFieldDialog(
                    modifier = Modifier,
                    initialTextFieldValue = TextFieldValue(text),
                    onDone = { data ->
                        data.split("\n").forEach {
                            when {
                                it.startsWith("***INNERTUBE COOKIE*** =") -> {
                                    val cookie =
                                        it.substringAfter("***INNERTUBE COOKIE*** =").trim()
                                    onInnerTubeCookieChange(cookie)
                                }

                                it.startsWith("***VISITOR DATA*** =") -> {
                                    val visitorDataValue =
                                        it.substringAfter("***VISITOR DATA*** =").trim()
                                    onVisitorDataChange(visitorDataValue)
                                }

                                it.startsWith("***DATASYNC ID*** =") -> {
                                    val dataSyncIdValue =
                                        it.substringAfter("***DATASYNC ID*** =").trim()
                                    onDataSyncIdChange(dataSyncIdValue)
                                }

                                it.startsWith("***ACCOUNT NAME*** =") -> {
                                    val name = it.substringAfter("***ACCOUNT NAME*** =").trim()
                                    onAccountNameChange(name)
                                }

                                it.startsWith("***ACCOUNT EMAIL*** =") -> {
                                    val email = it.substringAfter("***ACCOUNT EMAIL*** =").trim()
                                    onAccountEmailChange(email)
                                }

                                it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> {
                                    val handle =
                                        it.substringAfter("***ACCOUNT CHANNEL HANDLE*** =").trim()
                                    onAccountChannelHandleChange(handle)
                                }
                            }
                        }
                    },
                    onDismiss = { showTokenEditor = false },
                    singleLine = false,
                    maxLines = 20,
                    isInputValid = { input ->
                        input.isNotEmpty() &&
                                try {
                                    val cookieLine = input.lines()
                                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                                    if (cookieLine != null) {
                                        val cookie =
                                            cookieLine.substringAfter("***INNERTUBE COOKIE*** =")
                                                .trim()
                                        cookie.isEmpty() || "SAPISID" in parseCookieString(cookie)
                                    } else {
                                        false
                                    }
                                } catch (e: Exception) {
                                    false
                                }
                    },
                    extraContent = {
                        InfoLabel(text = stringResource(R.string.token_adv_login_description))
                    }
                )
            }

            PreferenceEntry(
                title = {
                    if (!isLoggedIn) {
                        Text(stringResource(R.string.advanced_login))
                    } else {
                        if (showToken) {
                            Text(stringResource(R.string.token_shown))
                        } else {
                            Text(stringResource(R.string.token_hidden))
                        }
                    }
                },
                icon = { Icon(painterResource(R.drawable.token), null) },
                onClick = {
                    if (!isLoggedIn) {
                        showTokenEditor = true
                    } else {
                        if (!showToken) {
                            showToken = true
                        } else {
                            showTokenEditor = true
                        }
                    }
                },
            )

            if (isLoggedIn) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.use_login_for_browse)) },
                    description = stringResource(R.string.use_login_for_browse_desc),
                    icon = { Icon(painterResource(R.drawable.person), null) },
                    checked = useLoginForBrowse,
                    onCheckedChange = {
                        YouTube.useLoginForBrowse = it
                        onUseLoginForBrowseChange(it)
                    }
                )
            }

            if (isLoggedIn) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.ytm_sync)) },
                    icon = { Icon(painterResource(R.drawable.cached), null) },
                    checked = ytmSync,
                    onCheckedChange = onYtmSyncChange,
                    isEnabled = isLoggedIn
                )
            }

            PreferenceGroupTitle(
                title = stringResource(R.string.discord),
            )

            PreferenceEntry(
                title = { Text(stringResource(R.string.discord_integration)) },
                icon = { Icon(painterResource(R.drawable.discord), null) },
                onClick = { navController.navigate("settings/discord") }
            )
        }
    }
}
