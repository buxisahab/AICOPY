plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {

    // OkHttp (to JossRedClient)
    implementation(libs.okhttp) // o la versión más reciente

    // If you also need interceptors for logging (optional)
    implementation(libs.logging.interceptor)

}
