import io.gitlab.arturbosch.detekt.Detekt

group = libs.ktlintIJ.pluginDescriptor.get().group
version = libs.versions.ktlintIJ.get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenLocal()
        mavenCentral()
    }

    buildDir = run {
        val globalBuildDir: File = rootProject.projectDir.resolve("build")
        val relativeProjectPath = projectDir.relativeTo(rootProject.projectDir)
        globalBuildDir.resolve(relativeProjectPath)
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Delete after https://github.com/gradle/gradle/issues/22797
plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlintGradle)
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true
}

tasks {
    withType<Detekt> {
        reports {
            html.required.set(false)
            xml.required.set(false)
            txt.required.set(false)
        }
    }
}
