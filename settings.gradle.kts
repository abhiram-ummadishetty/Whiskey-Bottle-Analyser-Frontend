pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Read credentials from local.properties (ignored by git)
        val localProps = java.util.Properties().apply {
            val file = File(rootDir, "local.properties")
            if (file.exists()) load(file.inputStream())
        }

        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                username = localProps.getProperty("gpr.user") ?: System.getenv("GPR_USER")
                password = localProps.getProperty("gpr.key") ?: System.getenv("GPR_KEY")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EdgeAI"
include(":app")
