// Saat Tarih Android projesi. Şimdilik yalnız :core (ortak hesap çekirdeği, saf Kotlin/JVM);
// telefon/tablet uygulaması, widget'lar ve Wear OS modülleri sonraki adımlarda eklenir.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
