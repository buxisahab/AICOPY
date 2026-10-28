package com.musifly.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.auth.AuthService
import com.musifly.android.constants.AccountChannelHandleKey
import com.musifly.android.constants.AccountEmailKey
import com.musifly.android.constants.AccountNameKey
import com.musifly.android.constants.ContentCountryKey
import com.musifly.android.constants.ContentLanguageKey
import com.musifly.android.constants.CountryCodeToName
import com.musifly.android.constants.DataSyncIdKey
import com.musifly.android.constants.InnerTubeCookieKey
import com.musifly.android.constants.LanguageCodeToName
import com.musifly.android.constants.MaxImageCacheSizeKey
import com.musifly.android.constants.ProxyEnabledKey
import com.musifly.android.constants.ProxyTypeKey
import com.musifly.android.constants.ProxyUrlKey
import com.musifly.android.constants.SYSTEM_DEFAULT
import com.musifly.android.constants.UseLoginForBrowse
import com.musifly.android.constants.VisitorDataKey
import com.musifly.android.extensions.toEnum
import com.musifly.android.extensions.toInetSocketAddress
import com.musifly.android.sync.SyncManager
import com.musifly.android.sync.SyncManagerEntryPoint
import com.musifly.android.sync.firestore.FirestoreAutoInitService
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.get
import com.musifly.android.utils.reportException
import com.musifly.android.utils.update.UpdateWorkerManager
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.YouTubeLocale
import com.musifly.kugou.KuGou
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import timber.log.Timber
import java.net.Proxy
import java.util.Locale


@HiltAndroidApp
class App : Application(), ImageLoaderFactory {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            initAcra {
                buildConfigClass = BuildConfig::class.java
                reportFormat = StringFormat.KEY_VALUE_LIST
                reportContent = listOf(
                    ReportField.APP_VERSION_CODE,
                    ReportField.APP_VERSION_NAME,
                    ReportField.ANDROID_VERSION,
                    ReportField.PHONE_MODEL,
                    ReportField.BRAND,
                    ReportField.PRODUCT,
                    ReportField.STACK_TRACE,
                    ReportField.USER_COMMENT,
                    ReportField.LOGCAT,
                    ReportField.USER_APP_START_DATE,
                    ReportField.USER_CRASH_DATE,
                    ReportField.DUMPSYS_MEMINFO,
                    ReportField.AVAILABLE_MEM_SIZE,
                    ReportField.TOTAL_MEM_SIZE,
                    ReportField.DISPLAY,
                    ReportField.DEVICE_FEATURES,
                    ReportField.INSTALLATION_ID,
                    ReportField.PACKAGE_NAME
                )

                // Try 'mail' or 'mailSender' - using the most common one for ACRA 5.x
                mailSender {
                    mailTo = "report.musifly@gmail.com"
                    reportAsFile = false
                    subject = "Mussifly Advanced Crash Report"
                    enabled = true
                }

                dialog {
                    title = getString(R.string.crash_dialog_title)
                    text = getString(R.string.crash_dialog_text)
                    commentPrompt = getString(R.string.crash_dialog_comment_prompt)
                    positiveButtonText = getString(R.string.crash_dialog_ok)
                    negativeButtonText = getString(R.string.crash_dialog_cancel)
                    enabled = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("App", "Failed to initialize ACRA", e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this;
        // Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this)) // Replaced by ACRA
        Timber.plant(Timber.DebugTree())
        
        // CRITICAL: Check if we're in crash_handler process FIRST
        // crash_handler process must NOT initialize ANYTHING (Firebase, Firestore, Media, Audio, etc.)
        val pid = android.os.Process.myPid()
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val processName = manager.runningAppProcesses?.find { it.pid == pid }?.processName
        
        // NEVER initialize anything in crash_handler or acra process
        if (processName?.contains("crash_handler") == true || processName?.contains("acra") == true) {
            Timber.tag("App").w("Crash or ACRA process detected - skipping ALL initialization")
            // Only set crash handler (already handled by ACRA), nothing else
            return
        }
        
        // Check if we're in the main process
        // Firebase/Firestore should only be initialized in the main process to avoid database lock conflicts
        val isMainProcess = isMainProcess()
        
        // Initialize Firebase only in main process
        if (isMainProcess) {
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    // Initialize manually with obfuscated configuration from SecureString
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.FIREBASE_APP_ID))
                        .setApiKey(com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.FIREBASE_API_KEY))
                        .setDatabaseUrl(com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.FIREBASE_URL))
                        .setStorageBucket(com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.FIREBASE_STORAGE_BUCKET))
                        .setProjectId(com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.FIREBASE_PROJECT_ID))
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Timber.tag("App").d("Firebase initialized with obfuscated options in main process")
                }
                
                // Initialize AuthService with Web Client ID
                // Note: This should be configured in strings.xml (default_web_client_id)
                // Or if google-services.json is present, you can extract it from there
                try {
                    val webClientId = com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.FIREBASE_WEB_CLIENT_ID)
                    if (webClientId.isNotEmpty()) {
                        AuthService.getInstance(this).initialize(webClientId)
                        Timber.tag("App").d("AuthService initialized with obfuscated Web Client ID")
                    } else {
                        Timber.tag("App").w("Firebase Web Client ID not found in SecureString. Google Sign-In will not work.")
                    }
                } catch (e: Exception) {
                    Timber.tag("App").w("Could not initialize AuthService: ${e.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize Firebase")
                reportException(e)
            }
        } else {
            // Log process name for debugging
            val pid = android.os.Process.myPid()
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val processName = manager.runningAppProcesses?.find { it.pid == pid }?.processName ?: "unknown"
            Timber.tag("App").d("Skipping Firebase initialization in non-main process: $processName")
        }

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] != false) {
            YouTube.useLoginForBrowse = true
        }
        
        // Initialize update system (start periodic update checker)
        // Start periodic update check (non-blocking, errors won't crash app)
        try {
            UpdateWorkerManager.startPeriodicCheck(this)
        } catch (e: Exception) {
            Timber.tag("App").w(e, "Failed to start periodic update check (non-critical)")
            // Don't crash the app if update check fails
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to get visitorData.", LENGTH_SHORT)
                                    .show()
                            }
                            reportException(it)
                        }.getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        /*
                         * Workaround to avoid breaking older installations that have a dataSyncId
                         * that contains "||" in it.
                         * If the dataSyncId ends with "||" and contains only one id, then keep the
                         * id before the "||".
                         * If the dataSyncId contains "||" and is not at the end, then keep the
                         * second id.
                         * This is needed to keep using the same account as before.
                         */
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        // we now allow user input now, here be the demons. This serves as a last ditch effort to avoid a crash loop
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@App)
                    }
                }
        }
        
        // Initialize Firebase Auth state listener for sync on app start
        // If user is already logged in when app starts, sync their data
        // Only in main process to avoid Firestore database lock conflicts
        if (isMainProcess) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
            var lastSyncTime = 0L
            val syncCooldownMs = 30_000L // 30 seconds cooldown to prevent multiple syncs
            
            firebaseAuth.addAuthStateListener { auth ->
                val user = auth.currentUser
                val now = System.currentTimeMillis()
                
                if (user != null && (now - lastSyncTime > syncCooldownMs)) {
                    lastSyncTime = now
                    // User is logged in, trigger sync
                    // Use SupervisorJob + CoroutineExceptionHandler to prevent crashes
                    val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
                        Timber.tag("App").e(throwable, "Uncaught exception in sync coroutine - preventing crash")
                    }
                    GlobalScope.launch(Dispatchers.IO + kotlinx.coroutines.SupervisorJob() + exceptionHandler) {
                        try {
                            // Small delay to ensure Firebase is fully initialized
                            kotlinx.coroutines.delay(1000)
                            
                            val entryPoint = try {
                                EntryPointAccessors.fromApplication(
                                    this@App,
                                    SyncManagerEntryPoint::class.java
                                )
                            } catch (e: Exception) {
                                Timber.tag("App").e(e, "Failed to get SyncManagerEntryPoint")
                                return@launch
                            }
                            
                            val syncManager = try {
                                SyncManager(
                                    this@App,
                                    entryPoint.likesRepository(),
                                    entryPoint.playlistRepository(),
                                    entryPoint.cachedDownloadsRepository(),
                                    entryPoint.bookmarksRepository(),
                                    entryPoint.migrationService()
                                )
                            } catch (e: Exception) {
                                Timber.tag("App").e(e, "Failed to create SyncManager")
                                return@launch
                            }
                            
                            try {
                                syncManager.syncAllUserData(GlobalScope) { result ->
                                    result.onSuccess {
                                        Timber.tag("App").d("Auto-sync on app start: ${it.likedSongsCount} likes, ${it.playlistsCount} playlists")
                                    }.onFailure { e ->
                                        Timber.tag("App").e(e, "Auto-sync on app start failed")
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.tag("App").e(e, "Failed to trigger syncAllUserData")
                            }
                        } catch (e: Exception) {
                            Timber.tag("App").e(e, "Failed to trigger auto-sync on app start")
                            // Don't crash the app if sync fails - this is non-critical
                        }
                    }
                }
            }
                Timber.tag("App").d("Firebase Auth state listener initialized for auto-sync")
            } catch (e: Exception) {
                Timber.tag("App").w(e, "Could not initialize Firebase Auth state listener")
            }
        }
        
        // Track app open/foreground events for User Activity Stats
        // Moved from MainActivity.onResume to prevent duplicate updates
        if (isMainProcess) {
            try {
                ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        try {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                GlobalScope.launch(Dispatchers.IO) {
                                    FirestoreAutoInitService.updateUserActivity(currentUser.uid)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag("App").e(e, "Failed to track user activity")
                        }
                    }
                })
            } catch (e: Exception) {
                Timber.tag("App").e(e, "Failed to setup ProcessLifecycleOwner")
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]

        // will crash app if you set to 0 after cache starts being used
        if (cacheSize == 0) {
            return ImageLoader.Builder(this)
                .crossfade(true)
                .respectCacheHeaders(false)
                .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }

        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            )
            .build()
    }

    /**
     * Check if the current process is the main process
     * Firebase/Firestore should only be initialized in the main process
     */
    private fun isMainProcess(): Boolean {
        val pid = android.os.Process.myPid()
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == pid) {
                // Main process name equals package name
                return processInfo.processName == packageName
            }
        }
        // Default to true if we can't determine (shouldn't happen)
        return true
    }

    companion object {
        lateinit var instance: App
            private set

        fun forgetAccount(context: Context) {
            runBlocking {
                context.dataStore.edit { settings ->
                    settings.remove(InnerTubeCookieKey)
                    settings.remove(VisitorDataKey)
                    settings.remove(DataSyncIdKey)
                    settings.remove(AccountNameKey)
                    settings.remove(AccountEmailKey)
                    settings.remove(AccountChannelHandleKey)
                }
            }
        }
    }
}