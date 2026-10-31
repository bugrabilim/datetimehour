// Saat Tarih Wear OS saat kadranı (rakip analizi listesi #78): Watch Face Format ile kodsuz kadran paketi
// (res/raw/watchface.xml + görseller). Wear OS 4+ (API 33). Paket adı ayrı: kadranlar Play'de saat uygulamasından
// ayrı bir paket olarak yayımlanır. Kadran XML'i Google'ın WFF 1 şemasıyla (github.com/google/watchface) doğrulandı.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "tr.bumba.time.watchface"
    compileSdk = 37

    defaultConfig {
        applicationId = "tr.bumba.time.watchface"
        minSdk = 33
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
