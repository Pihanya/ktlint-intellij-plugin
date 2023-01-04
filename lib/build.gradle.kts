import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Delete after https://github.com/gradle/gradle/issues/22797
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

dependencies {
    api(libs.ktlint.core) {
        exclude(group = "org.slf4j").because("Duplicated in IDE environment")
    }
    implementation(libs.ktlint.ruleset.standard) {
        exclude(group = "org.slf4j").because("Duplicated in IDE environment")
    }
    implementation(libs.ktlint.ruleset.experimental) {
        exclude(group = "org.slf4j").because("Duplicated in IDE environment")
    }
}

tasks {
    val javaVersion = libs.versions.java.get()

    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion
    }

    withType<ShadowJar> {
        configurations = listOf(
            project.configurations.api.get(),
            project.configurations.implementation.get(),
        ).onEach { cfg -> cfg.isCanBeResolved = true }

        mergeServiceFiles() // Expose all ruleset implementations

        relocate("org.jetbrains.kotlin.psi.KtPsiFactory", "shadow.org.jetbrains.kotlin.psi.KtPsiFactory")
        relocate("org.jetbrains.kotlin.psi.psiUtil", "shadow.org.jetbrains.kotlin.psi.psiUtil")
    }
}
