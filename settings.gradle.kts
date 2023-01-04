rootProject.name = "ktlint-intellij-plugin"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// Include subprojects
fileTree(rootProject.projectDir) {
    include("**/build.gradle.kts")
    exclude("build.gradle.kts") // Exclude root build.gradle.kts
    exclude("**/buildSrc") // Exclude build sources
    exclude(".*") // Exclude hidden sources
    exclude("**/build", "**/out") // Exclude build directories
}
    .asSequence()
    .map(File::getParent) // Resolve directory of found build.gradle.kts file
    .map(::relativePath).map { relativePath -> relativePath.replace(File.separator, ":") }
    .asIterable().let(::include)
