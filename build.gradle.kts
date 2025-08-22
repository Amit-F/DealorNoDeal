// --- Type-safe imports for Gradle 9 Kotlin DSL
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    // Keep only non-core plugins here. Core Java plugins live in subprojects.
    id("com.diffplug.spotless") version "6.25.0"
}

allprojects {
    group = "io.github.amitfink"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// Spotless in the ROOT project: format only Gradle Kotlin scripts so we avoid the Java plugin requirement here.
spotless {
    kotlinGradle {
        // format this build file and all subproject *.gradle.kts files
        target("**/*.gradle.kts")
        ktlint() // use ktlint for Gradle Kotlin DSL files
    }
}

subprojects {
    // Apply plugins which should exist on EVERY subproject
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")

    // Spotless Java in SUBPROJECTS ONLY. Explicit target ensures Gradle 9 is happy even if a subproject
    // doesn't apply the Java plugin (though ours do).
    spotless {
        java {
            // Limit to conventional Java sources within each subproject
            target("src/**/*.java")
            googleJavaFormat().aosp().reflowLongStrings()
        }
    }

    // Make all Test tasks use JUnit Platform and finalize with Jacoco report
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    // Configure Jacoco for each subproject
    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    // Configure Jacoco report outputs for each subproject
    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
