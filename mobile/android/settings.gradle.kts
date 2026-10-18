// Saat Tarih Android projesi: :core (ortak hesap çekirdeği, saf Kotlin/JVM) ve :app (telefon/tablet uygulaması,
// Jetpack Compose). Widget'lar ve Wear OS modülleri sonraki adımlarda eklenir.
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
