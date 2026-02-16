import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.kotlinx.atomicfu)
}

version = "0.1.0"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":acidify-core"))
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.ktor.server)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.milky.types)
            implementation(libs.acidify.codec)
            implementation(libs.qr.matrix)
            implementation(libs.quickjs.kt)
            implementation(libs.mordant)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.logback.classic)
        }
        mingwMain.dependencies {
            implementation(libs.ktor.client.winhttp)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        linuxMain.dependencies {
            implementation(libs.ktor.client.curl)
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint = "org.ntqqrev.yogurt.main"
            }
        }
    }

    mingwX64 {
        binaries.all {
            linkerOpts(
                "-Wl,-Bstatic",
                "-lstdc++",
                "-lgcc",
                "-lssp",
                "-Wl,-Bdynamic",
            )
        }
    }
}

val gitHashProvider: Provider<String> = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val coreLibGitHashProvider: Provider<String> = providers.exec {
    commandLine("git", "-C", project(":acidify-core").projectDir.absolutePath, "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val buildTimeProvider: Provider<String> = providers.provider {
    ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
}

val runNumberProvider: Provider<String> = providers.provider {
    System.getenv("GITHUB_RUN_NUMBER") ?: "local"
}

buildkonfig {
    packageName = "org.ntqqrev.yogurt"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "name", "Yogurt")
        buildConfigField(FieldSpec.Type.STRING, "version", "${project.version}-dev.${runNumberProvider.get()}")
        buildConfigField(
            FieldSpec.Type.STRING,
            "coreVersion",
            project(":acidify-core").let {
                "${it.name} ${it.version}+${coreLibGitHashProvider.get().substring(0, 7)}"
            }
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "milkyVersion",
            libs.milky.types.get().let {
                "${it.module.name} ${it.version}"
            }
        )
        buildConfigField(FieldSpec.Type.STRING, "commitHash", gitHashProvider.get())
        buildConfigField(FieldSpec.Type.STRING, "buildTime", buildTimeProvider.get())
    }
}