package com.buxi.music.ui.screens.settings

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeAccentBright
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextTertiary
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeWhite
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buxi.music.innertube.utils.parseCookieString
import com.buxi.music.BuildConfig
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.R
import com.buxi.music.constants.InnerTubeCookieKey
import com.buxi.music.ui.component.AvatarPreferenceManager
import com.buxi.music.ui.component.AvatarSelection
import com.buxi.music.ui.component.ChangelogButton
import com.buxi.music.ui.component.IconButton
import com.buxi.music.ui.utils.backToMain
import com.buxi.music.utils.rememberPreference
import com.buxi.music.ui.component.SettingsCategory
import com.buxi.music.ui.component.SettingsCategoryItem
import com.buxi.music.ui.screens.settings.AboutScreenNew
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL


@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0) // ✅ Solo en Android 13+
            )
        } else {
            @Suppress("DEPRECATION") // Evita advertencias en compilación
            context.packageManager.getPackageInfo(
                context.packageName,
                0 // ✅ Compatible con Android 12 y versiones anteriores
            )
        }
        packageInfo.versionName ?: "Unknown" // Si versionName es null, retorna "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown" // Si ocurre un error, retorna "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VersionCard(uriHandler: UriHandler) {
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }


    Spacer(Modifier.height(25.dp))
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
//            .clip(RoundedCornerShape(38.dp))
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(85.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeDark,

            ),
        shape = RoundedCornerShape(38.dp),
        onClick = { uriHandler.openUri("https://github.com/Arturo254/AiMusic/releases/latest") }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(38.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${stringResource(R.string.Version)} $appVersion",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = ThemeAccentBright,
                modifier = Modifier.align(Alignment.CenterHorizontally),


                )
        }
    }
}



@Composable
fun UpdateDownloadDialog(
    latestVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }
    val downloadScope = rememberCoroutineScope()

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Cuando vuelve de la pantalla de permisos, verificamos de nuevo si podemos instalar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls() && downloadedApkUri != null) {
                // Ahora que tenemos el permiso, procedemos con la instalación
                installApk(context, downloadedApkUri!!)
            }
        }
    }

    // Dialog para mostrar progreso y opciones
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
                    style = MaterialTheme.typography.headlineSmall
                )

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
                                downloadStatus = DownloadStatus.DOWNLOADING
                                downloadScope.launch {
                                    downloadedApkUri =
                                        downloadApk(context, latestVersion) { progress ->
                                            downloadProgress = progress
                                            if (progress >= 1f) {
                                                downloadStatus = DownloadStatus.COMPLETED
                                            }
                                        }
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
                            // Modifica solo la parte del botón de instalar en el caso COMPLETED
                            Button(onClick = {
                                if (downloadedApkUri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (!context.packageManager.canRequestPackageInstalls()) {
                                            val intent =
                                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                    .setData("package:${context.packageName}".toUri())

                                            installPermissionLauncher.launch(intent)
                                        } else {
                                            // Pasar directamente el objeto Uri, no una cadena
                                            installApk(context, downloadedApkUri!!)
                                        }
                                    } else {
                                        // Pasar directamente el objeto Uri, no una cadena
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


enum class DownloadStatus {
    NOT_STARTED,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

suspend fun downloadApk(
    context: Context,
    version: String,
    onProgressUpdate: (Float) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    try {
        // URL del APK (ajusta esta URL según donde estén alojados tus archivos APK)
        val apkUrl =
            "https://github.com/Arturo254/AiMusic/releases/download/$version/app-release.apk"

        // Crear archivo de destino
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, "app-release-$version.apk")

        // Si ya existe, bórralo
        if (apkFile.exists()) {
            apkFile.delete()
        }

        // Configurar el DownloadManager
        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Descargando Ai Music v$version")
            .setDescription("Descargando actualización...")
            .setDestinationUri(Uri.fromFile(apkFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Monitorear el progreso
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
            delay(100) // Esperar un poco antes de actualizar de nuevo
        }

        // Crear Uri para la instalación con FileProvider
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

// Función corregida para instalar APK usando directamente la URI
fun installApk(context: Context, apkUri: Uri) {
    // Verificar y solicitar permiso para instalar APKs en Android 8+ (API 26+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pm = context.packageManager
        val isAllowed = pm.canRequestPackageInstalls()
        if (!isAllowed) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData("package:${context.packageName}".toUri())
            context.startActivity(intent)
            return // Salimos para que el usuario conceda el permiso antes de continuar
        }
    }

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    context.startActivity(installIntent)
}

// Estas funciones ya las tenías


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {


    val uriHandler = LocalUriHandler.current


//    var isBetaFunEnabled by remember { mutableStateOf(false) }


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
        val context = LocalContext.current
        val avatarManager = remember { AvatarPreferenceManager(context) }
        val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
        val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
        val isLoggedIn = remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn) {
                var imageLoadError by remember { mutableStateOf(false) }
                var isImageLoading by remember { mutableStateOf(false) }

                // Avatar circular para usuario
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ThemeAccent.copy(alpha = 0.1f),
                                    ThemeAccent.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = ThemeAccent.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // Mostrar avatar personalizado si existe y no hay error
                        // Para el código que mostraste anteriormente, aquí está la actualización:

                        currentSelection is AvatarSelection.Custom && !imageLoadError -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                                    .crossfade(true)
                                    .listener(
                                        onStart = { isImageLoading = true },
                                        onSuccess = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = false
                                        },
                                        onError = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = true
                                        }
                                    )
                                    .build(),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            // Overlay de carga
                            if (isImageLoading) {
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(CircleShape)
                                        .background(
                                            ThemeDark.copy(alpha = 0.7f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = ThemeAccent
                                    )
                                }
                            }
                        }

                        currentSelection is AvatarSelection.DiceBear && !imageLoadError -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data((currentSelection as AvatarSelection.DiceBear).url)
                                    .crossfade(true)
                                    .listener(
                                        onStart = { isImageLoading = true },
                                        onSuccess = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = false
                                        },
                                        onError = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = true
                                        }
                                    )
                                    .build(),
                                contentDescription = "Avatar DiceBear",
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            // Overlay de carga
                            if (isImageLoading) {
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(CircleShape)
                                        .background(
                                            ThemeDark.copy(alpha = 0.7f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = ThemeAccent
                                    )
                                }
                            }
                        }

                        // Mostrar iniciales como fallback
                        else -> {
                            val initials = remember {
                                "?"
                            }

                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                ThemeAccent.copy(alpha = 0.8f),
                                                ThemeAccent
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = ThemeWhite,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // Logo para usuario no autenticado
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ThemeAccent.copy(alpha = 0.1f),
                                    ThemeAccent.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.opentune_monochrome),
                        contentDescription = "Logo de Ai Music",
                        modifier = Modifier.fillMaxSize(),
                        tint = ThemeAccent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ai Music",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Extensión para convertir String a Uri de forma segura
        fun String.toUri(): Uri? {
            return try {
                Uri.parse(this)
            } catch (e: Exception) {
                null
            }
        }

        SettingsCategory(
            title = stringResource(R.string.general_settings), // Add this string resource
            items = listOf(
                // Appearance settings hidden - force dark theme always
                // SettingsCategoryItem(
                //     icon = painterResource(R.drawable.palette),
                //     title = { Text(stringResource(R.string.appearance)) },
                //     onClick = { navController.navigate("settings/appearance") }
                // ),
                // Content settings hidden
                // SettingsCategoryItem(
                //     icon = painterResource(R.drawable.language),
                //     title = { Text(stringResource(R.string.content)) },
                //     onClick = { navController.navigate("settings/content") }
                // ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.music_source_preferences)) },
                    onClick = { navController.navigate("settings/music_source") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    onClick = { navController.navigate("settings/player") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.storage)) },
                    onClick = { navController.navigate("settings/storage") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.security),
                    title = { Text(stringResource(R.string.privacy)) },
                    onClick = { navController.navigate("settings/privacy") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text(stringResource(R.string.backup_restore)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                ),
            )
        )

        // About Section
        SettingsCategory(
            title = "About",
            items = listOf(
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text("About Ai Music") },
                    onClick = { navController.navigate("settings/about_new") }
                ),
            )
        )



        TranslatePreference(uriHandler = uriHandler)


    }

    TopAppBar(


        title = { Text(stringResource(R.string.settings)) },
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            )

            {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ThemeDark
        )
    )
}

@Composable
fun TranslatePreference(uriHandler: UriHandler) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsCategoryItem(
        title = { Text(stringResource(R.string.Translate)) },
        icon = painterResource(R.drawable.translate),
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.Redirección)) },
            text = { Text(stringResource(R.string.poeditor_redirect)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        uriHandler.openUri("https://poeditor.com/join/project/208BwCVazA")
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}




