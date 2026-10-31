import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Saat Tarih telefon uygulaması (Jetpack Compose). Hesaplar :core'da; metinler ve veriler sitedeki tek kaynaktan
// `node mobile/shared/gen-strings.mjs` ile üretilir (res/values*/strings.xml, assets/*.json).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "tr.bumba.time"
    // Compose 1.12 kütüphaneleri 37 ister; hedef API (targetSdk) Google Play kuralına göre 36
    compileSdk = 37

    defaultConfig {
        applicationId = "tr.bumba.time"
        minSdk = 26
        targetSdk = 36
        // CI yapı numarasını verir (Google Play her yüklemede artmasını ister)
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Metinler çalışma anında adla okunduğu için kaynak küçültme kapalı (bkz. Strings.kt)
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Ekran görüntüleri emülatörsüz, Robolectric ile (ScreenshotTest): ./gradlew :app:testDebugUnitTest
    // → app/build/screens/*.png (CI'da artifact)
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("screens.dir", layout.buildDirectory.dir("screens").get().asFile.path)
                it.maxHeapSize = "2g"
            }
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.code.scanner)
    implementation(libs.play.location)
    implementation(libs.health.connect)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
