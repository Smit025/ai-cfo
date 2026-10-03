plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val mailApiKey = "FINWISE_RESEND_API_KEY"
val mailFrom = "FINWISE_RESEND_FROM"

fun mailValue(name: String): String {
    val fromEnv = System.getenv(name)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv
    val file = rootProject.file("email.local.properties")
    if (!file.isFile) return ""
    for (raw in file.readLines()) {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) continue
        val eq = line.indexOf('=')
        if (eq <= 0) continue
        if (line.substring(0, eq).trim() == name) return line.substring(eq + 1).trim()
    }
    return ""
}

fun quoteJava(value: String): String {
    val escaped = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n', '\r' -> Unit
                else -> append(ch)
            }
        }
    }
    return "\"" + escaped + "\""
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
        // Debug stays empty so a local key is not copied into the debug APK.
        buildConfigField("String", "RESEND_API_KEY", "\"\"")
        buildConfigField("String", "RESEND_FROM", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Scaffold only — replace with an upload keystore before Play Console.
            signingConfig = signingConfigs.getByName("debug")
            // Values come from the environment or gitignored email.local.properties.
            // Do not print them. An empty value keeps release sign-in fail-closed.
            buildConfigField("String", "RESEND_API_KEY", quoteJava(mailValue(mailApiKey)))
            buildConfigField("String", "RESEND_FROM", quoteJava(mailValue(mailFrom)))
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
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(platform(libs.compose.bom))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(platform(libs.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
}
