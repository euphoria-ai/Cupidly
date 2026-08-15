import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load keystore credentials from local.properties if present so that a keystore
// is NOT required to build/sync/assembleDebug. Release stays unsigned when absent.
//
// Add these keys to local.properties (kept out of git) to enable release signing:
//   RELEASE_STORE_FILE=hook-upload.jks   (relative to this repo's app/ dir, or absolute)
//   RELEASE_STORE_PASSWORD=your-store-password
//   RELEASE_KEY_ALIAS=your-key-alias
//   RELEASE_KEY_PASSWORD=your-key-password
val keystoreProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

// java.util.Properties keeps quotes verbatim, so RELEASE_STORE_PASSWORD="hunter2"
// would be read as the 9-character string including the quotes and the keystore
// would refuse to open. Trim whitespace and one matching pair of surrounding
// quotes so both quoted and unquoted values in local.properties behave the same.
fun secret(name: String): String {
    val raw = keystoreProperties.getProperty(name)?.trim().orEmpty()
    val unquoted = if (raw.length >= 2 &&
        ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'")))
    ) {
        raw.substring(1, raw.length - 1)
    } else {
        raw
    }
    return unquoted
}

val hasReleaseSigning = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD",
).all { secret(it).isNotBlank() }

// Client secrets come from the same (gitignored) local.properties file and are
// exposed as BuildConfig constants. Both default to "" so a fresh clone still
// builds and runs — the app degrades gracefully instead of crashing.
//
// Add these keys to local.properties (one per line, no quotes, no spaces
// around the "=") to wire the app up for real:
//   REVENUECAT_PUBLIC_SDK_KEY=test_XXXXXXXXXXXXXXXXXXXXXXXXXXX
//   APP_API_KEY=the-shared-key-the-Hook-server-expects
//
// The key prefix decides which store the SDK talks to, and the SDK works it out
// on its own — `goog_` drives real Google Play Billing, `test_` drives
// RevenueCat's Test Store (sandbox purchases, no Play account needed). The app
// code never inspects the prefix, so either kind of key just works.
//
// The RevenueCat *public* SDK key is designed to ship inside the app. Never put
// the RevenueCat secret key (or any Supabase key) here — those are server-only.
fun secretLiteral(name: String): String {
    val escaped = secret(name).replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

// Every Play upload needs a versionCode higher than the last one that reached
// the console. Override at build time instead of editing this file:
//   ./gradlew bundleRelease -PhookVersionCode=2 -PhookVersionName=1.0.1
val hookVersionCode = (findProperty("hookVersionCode") as String?)?.toInt() ?: 1
val hookVersionName = (findProperty("hookVersionName") as String?) ?: "1.0"

android {
    namespace = "com.tomfricks.hook"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tom7.hook"
        minSdk = 29
        targetSdk = 35
        versionCode = hookVersionCode
        versionName = hookVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "REVENUECAT_PUBLIC_SDK_KEY", secretLiteral("REVENUECAT_PUBLIC_SDK_KEY"))
        buildConfigField("String", "APP_API_KEY", secretLiteral("APP_API_KEY"))
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                // Resolved against the root project so a bare file name such as
                // RELEASE_STORE_FILE=hook-upload.jks points at the keystore next
                // to local.properties rather than inside app/.
                storeFile = rootProject.file(secret("RELEASE_STORE_FILE"))
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only attach the release signing config when the keystore
            // credentials are provided; otherwise leave the build unsigned
            // so configuration/assembleRelease degrades gracefully.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Without this Kotlin targets whatever JVM the Gradle daemon runs on, which
    // fails the JVM-target consistency check against the Java 11 above.
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.revenuecat.purchases)
    // RevenueCat's dashboard-configured Paywalls + Customer Center (Compose).
    implementation(libs.revenuecat.purchases.ui)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}