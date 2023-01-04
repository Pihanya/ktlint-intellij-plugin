
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.*

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Delete after https://github.com/gradle/gradle/issues/22797
plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.intellij)
    alias(libs.plugins.jetbrains.changelog)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    compileOnly(projects.lib) { because("Required for IDE") }

    /**
     * TODO: Dependency to `shadow` configuration of `:lib` project could be replaced
     *   with defining configurations dependencies using typesafe project accessor:
     *   ```kt
     *   configurations {
     *       implementation.extendsFrom(
     *           projects.lib // typesafe project accessor
     *               .dependencyProject
     *               .configurations.named("shadow")
     *       )
     *   }
     *   ```
     *   ... but for some reason it will not work - compilation error will appear.
     *   Probably we should raise an issue in the Gradle issue tracker.
     */
    implementation(project(":lib", "shadow"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.rollbarJava) {
        exclude(group = "org.slf4j").because("Duplicated in IDE environment")
    }
    implementation(libs.reflections) {
        exclude(group = "org.slf4j").because("Duplicated in IDE environment")
    }
    implementation(libs.kotlin.reflect)

    testImplementation(projects.lib)
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.kotest.runnerJunit5)
}

val pluginProperties: Properties = rootProject.file("plugin.properties").takeIf(File::exists)
    ?.let { file -> Properties().apply { load(FileInputStream(file)) } }
    ?: throw GradleException("plugin.properties not found.")

val javaVersion: JavaVersion = libs.versions.java.get().toInt().let(JavaVersion::toVersion)
check(
    value = JavaVersion.current().isCompatibleWith(javaVersion),
    lazyMessage = { "The current JVM [${JavaVersion.current()}] is incompatible with [$javaVersion]" },
)

java {
    toolchain {
        @Suppress("UnstableApiUsage")
        vendor.set(JvmVendorSpec.ADOPTIUM)
        languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
    }
    withJavadocJar()
    withSourcesJar()
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(libs.ktlintIJ.pluginDescriptor.get().name)
    version.set(pluginProperties.getProperty("platformVersion"))
    type.set(pluginProperties.getProperty("platformType"))
    downloadSources.set(pluginProperties.getProperty("platformDownloadSources").toBoolean())

    // Plugin Dependencies:
    // https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_dependencies.html
    plugins.set(listOf("Kotlin"))
}

// Configure BuildConfig generation
buildConfig {
    packageName(project.group as String)

    buildConfigField("String", "PLUGIN_ID", "\"${project.group}\"")
    buildConfigField("String", "PLUGIN_VERSION", "\"${project.version}\"")
    buildConfigField("String", "BUNDLED_KTLINT_VERSION", "\"${libs.versions.ktlint.get()}\"")
    buildConfigField("String", "REPOSITORY_URL", "\"https://github.com/Pihanya/ktlint-intellij-plugin\"")
    buildConfigField("String", "DONATION_URL", "\"https://buymeacoffee.com/pihanya\"")
    buildConfigField("String?", "ROLLBAR_ACCESS_TOKEN", "\"${project.property("rollbarAccessToken")}\"")
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(project.version as String)
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            freeCompilerArgs += "-opt-in=kotlin.ExperimentalStdlibApi"
        }
    }

    withType<Detekt>().configureEach {
        jvmTarget = javaVersion.toString()
    }

    named<Test>("test").configure {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showExceptions = true
            showCauses = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

tasks {
    patchPluginXml.configure {
        pluginId.set(project.group as String)
        version.set(project.version as String)
        // TODO: sinceBuild.set(pluginProperties.getProperty("pluginSinceBuild"))
        // TODO: untilBuild.set(pluginProperties.getProperty("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            rootProject.file("README.md")
                .readText().lines()
                .let { lns ->
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    val hasDescriptionSection = lns.containsAll(listOf(start, end))
                    if (!hasDescriptionSection) {
                        throw GradleException(
                            """
                                Plugin description section not found in README.md file:
                                $start
                                <...>
                                $end
                            """.trimIndent(),
                        )
                    }

                    lns.subList(
                        fromIndex = lns.indexOf(start) + 1,
                        toIndex = lns.indexOf(end),
                    )
                }
                .joinToString("\n").let(::markdownToHTML),
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            provider {
                changelog.renderItem(
                    item = changelog.getLatest(),
                    outputType = Changelog.OutputType.HTML,
                )
            },
        )
    }

    runPluginVerifier.configure {
        ideVersions.set(
            pluginProperties.getProperty("pluginVerifierIdeVersions")
                .split(',')
                .map(String::trim).filter(String::isNotBlank),
        )
        failureLevel.set(listOf(DEPRECATED_API_USAGES, INVALID_PLUGIN))
    }

    publishPlugin.configure {
        dependsOn("patchChangelog")

        token.set(
            findProperty("intellijPublishToken")
                ?.let { it as? String }.orEmpty(),
        )

        val versionChannel = (project.version as String)
            .split('-').getOrElse(index = 1) { "default" }
            .split('.').first()
        channels.set(listOf(versionChannel))
    }
}
