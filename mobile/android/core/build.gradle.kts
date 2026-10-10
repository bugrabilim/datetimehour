import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Ortak hesap çekirdeği: namaz vakitleri, mevsimler, Ay. Android'e bağlı değil; telefon, tablet,
// widget ve Wear OS modülleri aynı kodu kullanır. Testler sitedeki hesaplardan üretilen ortak veriyle çalışır.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.json)
}

tasks.test {
    useJUnitPlatform()
    val vectors = rootProject.layout.projectDirectory.dir("../shared/testdata").asFile
    inputs.dir(vectors).withPropertyName("vectors")
    systemProperty("vectors.dir", vectors.canonicalPath)
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
