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
            val file = settingsDir.resolve("local.properties")
            if (file.exists()) {
                file.inputStream().use { load(it) }
            }
        }

        val gprUser: String? = localProps.getProperty("gpr.user") ?: System.getenv("GPR_USER")
        val gprKey: String? = localProps.getProperty("gpr.key") ?: System.getenv("GPR_KEY")

        if (gprUser != null && gprKey != null) {
            maven {
                url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
                credentials {
                    username = gprUser
                    password = gprKey
                }
            }
        }

        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EdgeAI"
include(":app")
