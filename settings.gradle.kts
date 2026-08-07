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

rootProject.name = "EnglishCoach60"
include(
    ":app",
    ":domain",
    ":data",
    ":core:designsystem",
    ":core:network",
    ":core:database",
    ":core:speech",
)
