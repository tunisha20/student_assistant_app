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
        mavenCentral() // Supabase is here
        maven { url = uri("https://jitpack.io") }
    }}

rootProject.name = "StudentAssistantAppV1"
include(":app")