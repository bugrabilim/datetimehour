import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Saat Tarih Wear OS saati: büyük saat, sıradaki namaz vakti, bugünün vakitleri, hicri tarih; kutucuk (tile) ve
// komplikasyonlar. Hesap :core'da (telefonla aynı); il listesi, hicri ay başları ve metinler gen-strings.mjs ile
// üretilir. Paket adı telefon uygulamasıyla aynı (Play'de Wear OS form faktörü olarak ayrı yayımlanır).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "tr.bumba.time.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "tr.bumba.time"
        minSdk = 30
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Ekran görüntüleri Robolectric ile (WearScreenshotTest): ./gradlew :wear:testDebugUnitTest → wear/build/screens
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
    implementation(libs.activity.compose)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.complications)
    implementation(libs.concurrent.futures)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
