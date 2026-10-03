import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Sandbox credentials from gitignored local.properties or the environment.
 * Blank is a valid build: Link stays closed. Values are escaped and never printed.
 * Release builds do not embed them.
 */
fun plaidBuildConfig(name: String): String {
    val local = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { local.load(it) }
    val raw = (local.getProperty(name) ?: System.getenv(name) ?: "")
        .replace("\r", "")
        .replace("\n", "")
        .trim()
    val escaped = buildString {
        for (ch in raw) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\${'$'}")
                else -> append(ch)
            }
        }
    }
    return "\"$escaped\""
}

android {
    namespace = "com.aicfo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aicfo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "PLAID_CLIENT_ID", plaidBuildConfig("PLAID_CLIENT_ID"))
            buildConfigField("String", "PLAID_SECRET", plaidBuildConfig("PLAID_SECRET"))
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Scaffold only — replace with an upload keystore before Play Console.
            signingConfig = signingConfigs.getByName("debug")
            // Sandbox secrets stay out of release binaries.
            buildConfigField("String", "PLAID_CLIENT_ID", "\"\"")
            buildConfigField("String", "PLAID_SECRET", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.window)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.biometric)
    implementation(libs.fragment)
    implementation(libs.plaid.link)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(platform(libs.compose.bom))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(platform(libs.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
}
