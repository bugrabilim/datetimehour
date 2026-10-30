// Saat Tarih Android projesi: :core (ortak hesap çekirdeği, saf Kotlin/JVM), :app (telefon/tablet uygulaması,
// Jetpack Compose) ve :wear (Wear OS saati: uygulama, kutucuk, komplikasyonlar).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SaatTarih"
include(":core")
include(":app")
include(":wear")
