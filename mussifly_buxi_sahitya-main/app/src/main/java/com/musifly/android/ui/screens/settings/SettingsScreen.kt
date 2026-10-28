package com.musifly.android.ui.screens.settings

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.musifly.android.secure.SecureString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.musifly.android.BuildConfig
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.db.entities.Playlist
import com.musifly.android.ui.component.BloomeeImportDialog
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.ListDialog
import com.musifly.android.ui.component.PlaylistListItem
import com.musifly.android.ui.component.PreferenceEntry
import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.SpotifyImportDialog
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.PlaylistImportExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import com.musifly.android.utils.update.UpdateData
import com.musifly.android.utils.update.UpdateDialog
import com.musifly.android.utils.update.UpdateChecker
import com.musifly.android.ui.component.ChangelogDialog
import androidx.compose.ui.text.style.TextOverflow
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.ui.player.components.GlassActionBar
import androidx.compose.material.icons.filled.MoreVert


@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        }
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VersionCard(uriHandler: UriHandler) {
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }

    SettingsGroup(
        title = stringResource(R.string.app_info)
    ) {
        PreferenceEntry(
            icon = { Icon(painterResource(R.drawable.info), null) },
            iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.Version),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { uriHandler.openUri("") }
        )
    }
}

@Composable
fun UpdateCard(
    show: Boolean,
    version: String,
    description: String = "",
    onClick: () -> Unit
) {
    if (show) {
        Spacer(Modifier.height(25.dp))
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 170.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(38.dp),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(3.dp))

                val newVersion = stringResource(R.string.NewVersion)
                val tapToUpdate = stringResource(R.string.tap_to_update)
              //  val warn = stringResource(R.string.warn)

                Text(
                    text = "$newVersion: $version",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )

                if (description.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))


                Spacer(Modifier.height(8.dp))

                Text(
                    text = tapToUpdate,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun UpdateDownloadDialog(
    latestVersion: String,
    downloadUrl: String,
    description: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }
    val downloadScope = rememberCoroutineScope()

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls() && downloadedApkUri != null) {
                installApk(context, downloadedApkUri!!)
            }
        }
    }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = {
                if (downloadStatus != DownloadStatus.DOWNLOADING) {
                    onDismiss()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.update_version, latestVersion),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            content = {
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (downloadStatus == DownloadStatus.NOT_STARTED) {
                    Text(stringResource(R.string.download_question))
                } else if (downloadStatus == DownloadStatus.DOWNLOADING) {
                    Text(stringResource(R.string.downloadingup))
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (downloadStatus == DownloadStatus.COMPLETED) {
                    Text(stringResource(R.string.download_completed))
                } else if (downloadStatus == DownloadStatus.ERROR) {
                    Text(stringResource(R.string.download_errorup))
                }
            },
            buttons = {
                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.cancel)
                        )
                        com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                            onClick = {
                                if (downloadUrl.endsWith(".apk", ignoreCase = true)) {
                                    downloadStatus = DownloadStatus.DOWNLOADING
                                    downloadScope.launch {
                                        downloadedApkUri =
                                            downloadApk(context, downloadUrl, latestVersion) { progress ->
                                                downloadProgress = progress
                                                if (progress >= 1f) {
                                                    downloadStatus = DownloadStatus.COMPLETED
                                                }
                                            }
                                        if (downloadedApkUri == null) {
                                            downloadStatus = DownloadStatus.ERROR
                                        }
                                    }
                                } else {
                                    uriHandler.openUri(downloadUrl)
                                    onDismiss()
                                }
                            },
                            text = stringResource(R.string.download),
                            isPrimary = true
                        )
                    }
                    DownloadStatus.DOWNLOADING -> { /* No buttons */ }
                    DownloadStatus.COMPLETED -> {
                        com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.close)
                        )
                        com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                            onClick = {
                                if (downloadedApkUri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (!context.packageManager.canRequestPackageInstalls()) {
                                            val intent =
                                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                    .setData("package:${context.packageName}".toUri())

                                            installPermissionLauncher.launch(intent)
                                        } else {
                                            installApk(context, downloadedApkUri!!)
                                        }
                                    } else {
                                        installApk(context, downloadedApkUri!!)
                                    }
                                }
                            },
                            text = stringResource(R.string.install),
                            isPrimary = true
                        )
                    }
                    DownloadStatus.ERROR -> {
                        com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.close)
                        )
                    }
                }
            }
        )
    } else {
    Dialog(onDismissRequest = {
        if (downloadStatus != DownloadStatus.DOWNLOADING) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.update_version, latestVersion),
                    style = MaterialTheme.typography.titleLarge
                )

                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(stringResource(R.string.download_question))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(onClick = {
                                if (downloadUrl.endsWith(".apk", ignoreCase = true)) {
                                    downloadStatus = DownloadStatus.DOWNLOADING
                                    downloadScope.launch {
                                        downloadedApkUri =
                                            downloadApk(context, downloadUrl, latestVersion) { progress ->
                                                downloadProgress = progress
                                                if (progress >= 1f) {
                                                    downloadStatus = DownloadStatus.COMPLETED
                                                }
                                            }
                                        if (downloadedApkUri == null) {
                                            downloadStatus = DownloadStatus.ERROR
                                        }
                                    }
                                } else {
                                    // Not a direct APK link, open in browser
                                    uriHandler.openUri(downloadUrl)
                                    onDismiss()
                                }
                            }) {
                                Text(stringResource(R.string.download))
                            }
                        }
                    }

                    DownloadStatus.DOWNLOADING -> {
                        Text(stringResource(R.string.downloadingup))
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    DownloadStatus.COMPLETED -> {
                        Text(stringResource(R.string.download_completed))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.close))
                            }
                            Button(onClick = {
                                if (downloadedApkUri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (!context.packageManager.canRequestPackageInstalls()) {
                                            val intent =
                                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                    .setData("package:${context.packageName}".toUri())

                                            installPermissionLauncher.launch(intent)
                                        } else {
                                            installApk(context, downloadedApkUri!!)
                                        }
                                    } else {
                                        installApk(context, downloadedApkUri!!)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.install))
                            }
                        }
                    }

                    DownloadStatus.ERROR -> {
                        Text(stringResource(R.string.download_errorup))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
    }
}

enum class DownloadStatus {
    NOT_STARTED,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

suspend fun downloadApk(
    context: Context,
    apkUrl: String,
    version: String,
    onProgressUpdate: (Float) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    if (apkUrl.isEmpty()) return@withContext null
    try {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, "mussifly-v$version.apk")

        if (apkFile.exists()) {
            apkFile.delete()
        }

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Downloading mussifly v$version")
            .setDescription("Downloading update...")
            .setDestinationUri(Uri.fromFile(apkFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedColumn =
                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalColumn =
                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                if (statusColumn != -1 && bytesDownloadedColumn != -1 && bytesTotalColumn != -1) {
                    val status = cursor.getInt(statusColumn)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedColumn)
                    val bytesTotal = cursor.getLong(bytesTotalColumn)

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isDownloading = false
                            onProgressUpdate(1f)
                        }

                        DownloadManager.STATUS_FAILED -> {
                            isDownloading = false
                            onProgressUpdate(0f)
                            return@withContext null
                        }

                        else -> {
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                onProgressUpdate(progress)
                            }
                        }
                    }
                }
            }
            cursor.close()
            delay(100)
        }

        return@withContext FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}



fun installApk(context: Context, apkUri: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pm = context.packageManager
        val isAllowed = pm.canRequestPackageInstalls()
        if (!isAllowed) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData("package:${context.packageName}".toUri())
            context.startActivity(intent)
            return
        }
    }

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    context.startActivity(installIntent)
}




@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showSpotifyImportDialog by remember { mutableStateOf(false) }
    var showBloomeeImportDialog by remember { mutableStateOf(false) }
    var showExportPlaylistDialog by remember { mutableStateOf(false) }
    var showPlaylistImportExportDialog by remember { mutableStateOf(false) }

    // Update States
    var showUpdateCard by remember { mutableStateOf(false) }
    var currentUpdateData by remember { mutableStateOf<UpdateData?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isCheckingManual by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    if (showChangelogDialog) {
        ChangelogDialog(
            updateData = currentUpdateData,
            onDismiss = { showChangelogDialog = false }
        )
        
        // Fetch if null, try cache first
        if (currentUpdateData == null) {
            LaunchedEffect(Unit) {
                // Try to load cached JSON first for instant show
                val cachedJson = com.musifly.android.utils.update.UpdatePrefs.getCachedJson(context)
                if (cachedJson != null) {
                    currentUpdateData = com.musifly.android.utils.update.UpdateData.fromJson(cachedJson)
                }
                
                // Then fetch fresh data
                val freshData = UpdateChecker.checkForUpdate(context)
                if (freshData != null) {
                    currentUpdateData = freshData
                }
            }
        }
    }

    if (showDownloadDialog && currentUpdateData != null) {
        UpdateDialog(
            updateData = currentUpdateData!!,
            onDismiss = { showDownloadDialog = false },
            onUpdateClick = {
                showDownloadDialog = false
            }
        )
    }

    LaunchedEffect(Unit) {
        val updateData = UpdateChecker.checkForUpdate(context)
        if (updateData != null) {
            showUpdateCard = true
            currentUpdateData = updateData
        }
    }

    val onCheckUpdates = {
        if (!isCheckingManual) {
            coroutineScope.launch {
                try {
                    isCheckingManual = true
                    Toast.makeText(context, context.resources.getString(R.string.checking_for_updates), Toast.LENGTH_SHORT).show()

                    val updateData = UpdateChecker.checkForUpdate(context, forceShow = true)
                    isCheckingManual = false

                    if (updateData != null) {
                        currentUpdateData = updateData
                        showUpdateCard = true
                        showDownloadDialog = true
                    } else {
                        Toast.makeText(context, context.resources.getString(R.string.no_updates_found), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isCheckingManual = false
                    Toast.makeText(context, "Update check failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val onOpenWebsite = {
        SecureString.openLink(context, SecureString.LINK_OFFICIAL_WEBSITE)
    }

    // Spotify Import Dialog
    if (showSpotifyImportDialog) {
        SpotifyImportDialog(
            onDismiss = { showSpotifyImportDialog = false },
            onImportComplete = { playlist ->
                // Playlist created successfully
            }
        )
    }

    // Bloomee Import Dialog
    if (showBloomeeImportDialog) {
        BloomeeImportDialog(
            onDismiss = { showBloomeeImportDialog = false },
            onImportComplete = {
                // Playlist imported successfully
            }
        )
    }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    // Import/Export Playlist Options Dialog
    if (showPlaylistImportExportDialog) {
        val dialogTitle: @Composable () -> Unit = {
            Text(
                text = "Playlist Import / Export",
                style = MaterialTheme.typography.titleLarge
            )
        }

        val dialogContent: @Composable ColumnScope.() -> Unit = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Import Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPlaylistImportExportDialog = false
                            showBloomeeImportDialog = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_import),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    Column {
                        Text(
                            text = "Import mussifly Playlist",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Import playlist from mussifly JSON file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Export Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPlaylistImportExportDialog = false
                            showExportPlaylistDialog = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF2196F3)
                        )
                    }
                    Column {
                        Text(
                            text = "Export Playlist",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Export playlist to mussifly JSON format",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val buttonsContent: @Composable RowScope.() -> Unit = {
            if (isLiquidGlass) {
                com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                     onClick = { showPlaylistImportExportDialog = false },
                     text = "Cancel"
                )
            } else {
                TextButton(onClick = { showPlaylistImportExportDialog = false }) {
                    Text("Cancel")
                }
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = { showPlaylistImportExportDialog = false },
                title = dialogTitle,
                content = dialogContent,
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                onDismissRequest = { showPlaylistImportExportDialog = false },
                title = dialogTitle,
                text = { Column { dialogContent() } },
                confirmButton = { Row { buttonsContent() } }
            )
        }
    }

    // Export folder selection
    var selectedExportFolderUri by remember { mutableStateOf<Uri?>(null) }
    var showExportFolderPicker by remember { mutableStateOf(false) }
    var pendingPlaylistId by remember { mutableStateOf<String?>(null) }

    val exportFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedExportFolderUri = uri
            // Take persistable URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Failed to take URI permission: ${e.message}", e)
            }

            // Now export the playlist
            pendingPlaylistId?.let { playlistId ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val result = PlaylistImportExportService.exportPlaylist(
                            context = context,
                            database = database,
                            playlistId = playlistId,
                            exportFolderUri = selectedExportFolderUri
                        )
                        withContext(Dispatchers.Main) {
                            result.onSuccess { uri ->
                                snackbarHostState.showSnackbar(
                                    message = "Playlist saved successfully",
                                    duration = SnackbarDuration.Short
                                )

                                kotlinx.coroutines.delay(500)

                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, "Exported playlist")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                            }.onFailure { exception ->
                                android.util.Log.e("SettingsScreen", "Export failed: ${exception.message}", exception)
                                snackbarHostState.showSnackbar("Export failed: ${exception.message}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsScreen", "Error exporting playlist: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                }
            }
            pendingPlaylistId = null
        }
        showExportFolderPicker = false
    }

    // Export Playlist Selection Dialog
    var exportPlaylists by remember { mutableStateOf(emptyList<Playlist>()) }
    LaunchedEffect(showExportPlaylistDialog) {
        if (showExportPlaylistDialog) {
            withContext(Dispatchers.IO) {
                try {
                    // Use editablePlaylistsByCreateDateAsc() which already filters for editable playlists
                    val allPlaylists = database.editablePlaylistsByCreateDateAsc().first()

                    // Filter out auto-playlists (liked, downloaded, top, cached, history, etc.)
                    val autoPlaylistIds = setOf(
                        "liked",
                        "LP_LIKED",
                        "downloaded",
                        "LP_DOWNLOADED",
                        "top",
                        "cached",
                        "history"
                    )

                    exportPlaylists = allPlaylists.filter { playlist: com.musifly.android.db.entities.Playlist ->
                        playlist.id !in autoPlaylistIds &&
                        playlist.playlist.isEditable &&
                        playlist.playlist.bookmarkedAt != null // Only show bookmarked playlists
                    }

                    android.util.Log.d("SettingsScreen", "Found ${exportPlaylists.size} playlists to export (from ${allPlaylists.size} total editable playlists)")
                    if (exportPlaylists.isEmpty() && allPlaylists.isNotEmpty()) {
                        android.util.Log.w("SettingsScreen", "No playlists to export after filtering. Auto-playlist IDs found: ${allPlaylists.map { it.id }}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsScreen", "Error fetching playlists for export: ${e.message}", e)
                    exportPlaylists = emptyList()
                }
            }
        }
    }

    if (showExportPlaylistDialog) {
        ListDialog(
            onDismiss = { showExportPlaylistDialog = false }
        ) {
            if (exportPlaylists.isEmpty()) {
                item {
                    Text(
                        text = "No playlists available to export",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(exportPlaylists) { playlist ->
                    PlaylistListItem(
                        playlist = playlist,
                        modifier = Modifier.clickable {
                            showExportPlaylistDialog = false
                            // Show folder picker dialog
                            pendingPlaylistId = playlist.id
                            showExportFolderPicker = true
                            exportFolderPickerLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }

    // val isLiquidGlass = isLiquidGlassActive() // removed redundant

    if (isLiquidGlass) {
        // Use LiquidGlassBackdropScaffold for liquid glass theme
        com.musifly.android.ui.component.liquidglass.LiquidGlassBackdropScaffold {
            backdrop ->
            Scaffold(
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                actions = {
                    GlassActionBar {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = { /* TODO: Implement search */ },
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Search),
                            contentDescription = "Search",
                            tint = Color.White
                        )
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = { /* TODO: Implement menu */ },
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MoreVert),
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
                contentWindowInsets = LocalPlayerAwareWindowInsets.current
            ) { innerPadding ->
                Box(
                    modifier = Modifier.padding(innerPadding)
                ) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState())
                    ) {
                        UpdateCard(
                            show = showUpdateCard,
                            version = currentUpdateData?.latestVersionName ?: "",
                            description = currentUpdateData?.updateDescription ?: "",
                            onClick = {
                                currentUpdateData?.downloadUrl?.let { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to open link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(12.dp))




                        // General Settings Group
                        SettingsGroup(
                            title = stringResource(R.string.general_settings)
                        ) {
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.palette), null) },
                                iconBackgroundColor = Color(0xFFFFA000), // Orange
                                title = { Text(stringResource(R.string.appearance)) },
                                description = "Themes, Night Mode, Icons, Interface",
                                onClick = { navController.navigate("settings/appearance") }
                            )
                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.language), null) },
                                iconBackgroundColor = Color(0xFF9C27B0), // Purple
                                title = { Text(stringResource(R.string.content)) },
                                description = "Language, Region, Content Filters",
                                onClick = { navController.navigate("settings/content") }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.play_circle), null) },
                                iconBackgroundColor = Color(0xFF4CAF50), // Green
                                title = { Text(stringResource(R.string.player_and_audio)) },
                                description = "Normalize volume, Proxy, Audio Quality",
                                onClick = { navController.navigate("settings/player") }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.equalizer), null) },
                                iconBackgroundColor = Color(0xFFE91E63), // Pink
                                title = { Text("AI sound effects") },
                                description = "Bass Boost, Loudness, Spatial Audio",
                                onClick = { navController.navigate("settings/audio_effects") }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.storage), null) },
                                iconBackgroundColor = Color(0xFFFFC107), // Amber
                                title = { Text(stringResource(R.string.storage)) },
                                description = "Cache, Downloads, Space Management",
                                onClick = { navController.navigate("settings/storage") }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.lock), null) },
                                iconBackgroundColor = Color(0xFF2196F3), // Blue
                                title = { Text(stringResource(R.string.privacy)) },
                                description = "History, Analytics, Personal Data",
                                onClick = { navController.navigate("settings/privacy") }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.restore), null) },
                                iconBackgroundColor = Color(0xFF00BCD4), // Cyan
                                title = { Text(stringResource(R.string.backup_restore)) },
                                description = "Export data, Import backups",
                                onClick = { navController.navigate("settings/backup_restore") }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.playlist_import), null) },
                                iconBackgroundColor = Color(0xFF1DB954), // Spotify Green
                                title = { Text(stringResource(R.string.import_from_spotify)) },
                                description = "Transfer your Spotify Playlists",
                                onClick = { showSpotifyImportDialog = true }
                            )
                            

                            
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.import_export), null) },
                                iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                                title = { Text("Import / Export Playlist") },
                                description = "Share your playlists with others",
                                onClick = { showPlaylistImportExportDialog = true }
                            )
                            

                        }
                        


                        // Updates Group
                        SettingsGroup(title = stringResource(R.string.updates)) {
                            PreferenceEntry(
                                icon = {
                                    if (isCheckingManual) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(painterResource(R.drawable.update), null)
                                    }
                                },
                                iconBackgroundColor = Color(0xFF2196F3), // Blue
                                title = { Text(stringResource(R.string.check_for_updates)) },
                                description = "Version ${com.musifly.android.BuildConfig.VERSION_NAME}",
                                onClick = onCheckUpdates
                            )

                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.history), null) },
                                iconBackgroundColor = Color(0xFF4CAF50), // Green
                                title = { Text("What's new") },
                                description = "See the latest changes and features",
                                onClick = { showChangelogDialog = true }
                            )

                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.language), null) },
                                iconBackgroundColor = Color(0xFFFFA000), // Orange
                                title = { Text(stringResource(R.string.official_website)) },
                                description = "musi-fly.com",
                                onClick = onOpenWebsite
                            )
                        }

                        // Community & About Group
                        SettingsGroup(title = "Community & About") {
                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.group), null) },
                                iconBackgroundColor = Color(0xFFE91E63), // Pink
                                title = { Text(stringResource(R.string.community)) },
                                description = "Join our community and follow us",
                                onClick = { navController.navigate("settings/community") }
                            )

                            PreferenceEntry(
                                icon = { Icon(painterResource(R.drawable.info), null) },
                                iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                                title = { Text(stringResource(R.string.about)) },
                                description = "App version, licenses, and more",
                                onClick = { navController.navigate("settings/about") }
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(androidx.compose.foundation.layout.WindowInsets.ime))
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    } else {
    // Diálogo de traducción
    if (showTranslateDialog) {
        AlertDialog(
            onDismissRequest = { showTranslateDialog = false },
            title = { Text(stringResource(R.string.Redirección)) },
            text = { Text(stringResource(R.string.poeditor_redirect)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTranslateDialog = false
                        SecureString.openLink(context, SecureString.LINK_TELEGRAM)
                    }
                ) {
                    Text("OK")
                }
            }
        )
        }

        // Standard Scaffold for non-liquid glass theme
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            stringResource(R.string.settings),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 32.sp 
                            )
                        ) 
                    },
                    navigationIcon = {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain,
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    },
                    actions = {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = { /* TODO: Implement search */ },
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Search),
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = { /* TODO: Implement menu */ },
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MoreVert),
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            contentWindowInsets = LocalPlayerAwareWindowInsets.current
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                ) {
                    UpdateCard(
                        show = showUpdateCard,
                        version = currentUpdateData?.latestVersionName ?: "",
                        description = currentUpdateData?.updateDescription ?: "",
                        onClick = {
                            currentUpdateData?.downloadUrl?.let { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))



                    Spacer(Modifier.height(12.dp))

                    // General Settings Group - All main options in one card (like liquid glass theme)
                    SettingsGroup(
                        title = stringResource(R.string.general_settings)
                    ) {
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.palette), null) },
                            iconBackgroundColor = Color(0xFFFFA000), // Orange
                            title = { Text(stringResource(R.string.appearance)) },
                            description = "Themes, Night Mode, Icons, Interface",
                            onClick = { navController.navigate("settings/appearance") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.language), null) },
                            iconBackgroundColor = Color(0xFF9C27B0), // Purple
                            title = { Text(stringResource(R.string.content)) },
                            description = "Language, Region, Content Filters",
                            onClick = { navController.navigate("settings/content") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.play_circle), null) },
                            iconBackgroundColor = Color(0xFF4CAF50), // Green
                            title = { Text(stringResource(R.string.player_and_audio)) },
                            description = "Normalize volume, Proxy, Audio Quality",
                            onClick = { navController.navigate("settings/player") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.equalizer), null) },
                            iconBackgroundColor = Color(0xFFE91E63), // Pink
                            title = { Text("AI sound effects") },
                            description = "Bass Boost, Loudness, Spatial Audio",
                            onClick = { navController.navigate("settings/audio_effects") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.storage), null) },
                            iconBackgroundColor = Color(0xFFFFC107), // Amber
                            title = { Text(stringResource(R.string.storage)) },
                            description = "Cache, Downloads, Space Management",
                            onClick = { navController.navigate("settings/storage") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.lock), null) },
                            iconBackgroundColor = Color(0xFF2196F3), // Blue
                            title = { Text(stringResource(R.string.privacy)) },
                            description = "History, Analytics, Personal Data",
                            onClick = { navController.navigate("settings/privacy") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.restore), null) },
                            iconBackgroundColor = Color(0xFF00BCD4), // Cyan
                            title = { Text(stringResource(R.string.backup_restore)) },
                            description = "Export data, Import backups",
                            onClick = { navController.navigate("settings/backup_restore") }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.playlist_import), null) },
                            iconBackgroundColor = Color(0xFF1DB954), // Spotify Green
                            title = { Text(stringResource(R.string.import_from_spotify)) },
                            description = "Transfer your Spotify Playlists",
                            onClick = { showSpotifyImportDialog = true }
                        )
                        

                        
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.import_export), null) },
                            iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                            title = { Text("Import / Export Playlist") },
                            description = "Share your playlists with others",
                            onClick = { showPlaylistImportExportDialog = true }
                        )
                    }

                    Spacer(Modifier.height(12.dp))



                    // Updates Group
                    SettingsGroup(title = stringResource(R.string.updates)) {
                        PreferenceEntry(
                            icon = {
                                if (isCheckingManual) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(painterResource(R.drawable.update), null)
                                }
                            },
                            iconBackgroundColor = Color(0xFF2196F3), // Blue
                            title = { Text(stringResource(R.string.check_for_updates)) },
                            description = "Version ${com.musifly.android.BuildConfig.VERSION_NAME}",
                            onClick = onCheckUpdates
                        )

                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.history), null) },
                            iconBackgroundColor = Color(0xFF4CAF50), // Green
                            title = { Text("What's new") },
                            description = "See the latest changes and features",
                            onClick = { showChangelogDialog = true }
                        )

                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.language), null) },
                            iconBackgroundColor = Color(0xFFFFA000), // Orange
                            title = { Text(stringResource(R.string.official_website)) },
                            description = "musi-fly.com",
                            onClick = onOpenWebsite
                        )
                    }

                    // Community & About Group
                    SettingsGroup(title = "Community & About") {
                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.group), null) },
                            iconBackgroundColor = Color(0xFFE91E63), // Pink
                            title = { Text(stringResource(R.string.community)) },
                            description = "Join our community and follow us",
                            onClick = { navController.navigate("settings/community") }
                        )

                        PreferenceEntry(
                            icon = { Icon(painterResource(R.drawable.info), null) },
                            iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                            title = { Text(stringResource(R.string.about)) },
                            description = "App version, licenses, and more",
                            onClick = { navController.navigate("settings/about") }
                        )
                    }

                    // Add dynamic bottom padding to account for the navigation bar
                    val playerAwareInsets: WindowInsets = LocalPlayerAwareWindowInsets.current
                    val density = LocalDensity.current
                    val bottomPadding = with(density) { 
                        playerAwareInsets.getBottom(this).toDp() 
                    }
                    Spacer(Modifier.height(bottomPadding + 16.dp))
                }
            }
        }
    }
}