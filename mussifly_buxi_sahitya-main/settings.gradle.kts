@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.clojars.org") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.10.0")
}


rootProject.name = "Musifly"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")

include(":material-color-utilities")

