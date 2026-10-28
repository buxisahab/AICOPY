@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.musifly.android"
    //noinspection GradleDependency
    compileSdk = 35

    defaultConfig {
        applicationId = "com.musifly.android"
        minSdk = 25
        targetSdk = 35
        versionCode = 19
        versionName = "4.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        multiDexEnabled = true
        
        // Resource configuration - keep only used languages to reduce APK size
        resourceConfigurations += listOf("en", "es", "hi", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko", "ar", "tr", "pl", "nl", "cs", "sv", "da", "no", "ro", "uk", "vi", "id", "he", "ca", "hu", "el", "bg", "hr", "sk", "sl", "et", "lv", "lt", "mt", "ga", "cy", "af", "be", "bn", "fa", "iw", "ne", "or", "pa", "sr")

    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true  // Enable PNG crunching to reduce size
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            // Enable aggressive optimization
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
            // Keep ProGuard rules even in debug to ensure protobuf classes are kept
            proguardFiles("proguard-rules.pro")
        }
    }

    signingConfigs {
        getByName("debug") {
            if (System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD") != null) {
                storeFile = file(System.getenv("MUSIC_DEBUG_KEYSTORE_FILE"))
                storePassword = System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD")
                keyAlias = "debug"
                keyPassword = System.getenv("MUSIC_DEBUG_SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Align with Java 17 (installed JDK version)
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    lint {
        disable += "MissingTranslation"
    }
    
    // Suppress duplicate class warnings for protobuf classes
    // These are expected and handled by pickFirsts in packaging.resources
    packaging {
        jniLibs {
            // Disable legacy packaging to allow 16 KB page size alignment
            // The build system will automatically align native libraries
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            // Firestore removed - no protobuf conflicts
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    // Ensure native libraries are extracted and can be processed
    }


// Register task to generate native secrets
tasks.register<Exec>("generateSecrets") {
    group = "build"
    description = "Generates native-lib.cpp from secret_strings.json"
    commandLine("python", "generate_secrets.py")
    workingDir(projectDir)
}

// Ensure secrets are generated before preBuild
tasks.named("preBuild") {
    dependsOn("generateSecrets")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)
    
    implementation("androidx.multidex:multidex:2.0.1")

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose Core
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)
    implementation(libs.ui.geometry)
    implementation(libs.runtime.livedata)
    implementation(libs.ui)
    implementation(libs.play.services.basement)
    // implementation(libs.androidx.material3) // Removed to resolve version conflict and internal API usage issues
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.remote.creation.core)
    implementation(libs.androidx.material3)
    implementation(libs.foundation.layout)
    // Firestore is added below with proper exclusions to avoid protobuf conflicts

    // Compose tooling only for debug builds
    debugImplementation(libs.compose.ui.tooling)

    // ViewModel
    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    // Material Design
    implementation(libs.material3)
    implementation(libs.palette)
    implementation(projects.materialColorUtilities)
    implementation(libs.material.ripple)
    implementation(libs.material.icons.extended)

    // Image Loading & UI
    implementation(libs.coil)
    implementation(libs.shimmer)
    implementation(libs.lottie.compose)

    // Media Playback
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)
    implementation(libs.androidx.media) // For MediaStyle notifications and Live Alert support
    implementation(libs.squigglyslider)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Widgets & Work
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.graphics.shapes)
    implementation(libs.work.runtime.ktx)

    // Lifecycle & AppCompat
    implementation(libs.appcompat)
    implementation(libs.lifecycle.process)



    implementation(libs.hilt)
    implementation("org.jsoup:jsoup:1.22.1")
    ksp(libs.hilt.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)
    implementation(projects.lrclib)


    implementation(libs.ktor.client.core)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)
    
    // Tubular (NewPipe Extractor fork) for video/audio downloads - ESSENTIAL
    implementation(libs.tubular)
    
    // implementation("com.arthenica:smart-exception-java:0.2.1") // Unused/Transitive?
    
    // Firebase Authentication and Google Sign-In
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // Firestore - DO NOT exclude protolite-well-known-types as it contains required classes
    // Instead, we'll handle conflicts via resolution strategy below
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Ensure protobuf-javalite is available (Firestore uses it internally)
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-base:18.5.0") // For ConnectionResult and CommonStatusCodes
    
    // Removed: Retrofit/Gson for Piped API - Not actively used (NewPipe handles everything)
    // If Piped API needed in future, add back:
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp) // Use OkHttp engine for Ktor

    // ACRA - Advanced Crash Reports for Android
    implementation(libs.acra.core)
    implementation(libs.acra.mail)
    implementation(libs.acra.dialog)
}

// Disable duplicate class check for protobuf classes
// These duplicates are expected and handled by pickFirsts in packaging.resources
// Configure after tasks are created by Android plugin
afterEvaluate {
    tasks.findByName("checkDebugDuplicateClasses")?.enabled = false
    tasks.findByName("checkReleaseDuplicateClasses")?.enabled = false
    
    // Verify that protobuf dependencies are present (protolite-well-known-types is now required)
    tasks.register("verifyProtobufDependencies") {
        group = "verification"
        description = "Verify that required protobuf dependencies are present"
        doLast {
            val configurations = project.configurations
            var foundProtolite = false
            var foundProtobufJavalite = false
            configurations.forEach { config ->
                try {
                    config.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                        val id = artifact.moduleVersion.id
                        if (id.group == "com.google.firebase" && id.name.contains("protolite-well-known-types")) {
                            foundProtolite = true
                            println("✅ Found protolite-well-known-types: $id (required for com.google.type.LatLng)")
                        }
                        if (id.group == "com.google.protobuf" && id.name == "protobuf-javalite") {
                            foundProtobufJavalite = true
                            println("✅ Found protobuf-javalite: $id")
                        }
                    }
                } catch (e: Exception) {
                    // Configuration might not be resolved yet, skip
                }
            }
            if (foundProtolite && foundProtobufJavalite) {
                println("✅ SUCCESS: All required protobuf dependencies found")
            } else {
                if (!foundProtolite) {
                    println("⚠️  WARNING: protolite-well-known-types not found (required for Firestore)")
                }
                if (!foundProtobufJavalite) {
                    println("⚠️  WARNING: protobuf-javalite not found")
                }
            }
        }
    }
    
    // Task to clean protobuf-related cache (if needed for troubleshooting)
    tasks.register("cleanProtobufCache") {
        group = "cleanup"
        description = "Clean Gradle cache for protobuf-related dependencies (use if experiencing conflicts)"
        doLast {
            println("🧹 Cleaning protobuf cache...")
            val gradleCache = file("${gradle.gradleUserHomeDir}/caches")
            
            // Clean all protobuf-related cache (use with caution)
            val protobufPatterns = listOf(
                "modules-2/files-2.1/com.google.protobuf",
                "modules-2/files-2.1/com.google.firebase/protolite",
                "transforms-*/**/protobuf*",
                "transforms-*/**/protolite*"
            )
            
            protobufPatterns.forEach { pattern ->
                gradleCache.listFiles()?.forEach { cacheDir ->
                    try {
                        fileTree(cacheDir).matching {
                            include(pattern)
                        }.forEach { file ->
                            println("  Deleting: ${file.absolutePath}")
                            file.deleteRecursively()
                        }
                    } catch (e: Exception) {
                        // Ignore errors for missing files
                    }
                }
            }
            
            println("✅ Protobuf cache cleaned.")
            println("⚠️  IMPORTANT: Now run './gradlew clean --refresh-dependencies' to rebuild")
        }
    }
}

// Force protobuf version and handle conflicts
// This must be outside dependencies block
configurations.all {
    resolutionStrategy {
        preferProjectModules()
        // FORCE ONLY ONE VERSION OF PROTOBUF ACROSS THE WHOLE APP
        // This ensures Firebase Firestore, TensorFlow Lite, ML Kit, and grpc/protolite
        // all use the same protobuf version → no duplicate classes ever again
        force("com.google.protobuf:protobuf-javalite:3.21.12")
    }
    
    // DO NOT exclude protolite-well-known-types - it contains com.google.type.LatLng
    // Instead, we handle conflicts via resolution strategy above
}
