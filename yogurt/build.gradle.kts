import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.konfig)
}

version = "0.1.0"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":acidify-core"))
            implementation(project(":yogurt-media-codec"))
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.ktor.server)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.milky.types)
            implementation(libs.qr.matrix)
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
                "-Wl,-Bdynamic"
            )
        }
    }
}

val gitHashProvider = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val buildTimeProvider = providers.provider {
    ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
}

val runNumberProvider = providers.provider {
    System.getenv("GITHUB_RUN_NUMBER") ?: "local"
}

buildkonfig {
    packageName = "org.ntqqrev.yogurt"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "name", "Yogurt")
        buildConfigField(FieldSpec.Type.STRING, "version", "${project.version}-dev.${runNumberProvider.get()}")
        buildConfigField(FieldSpec.Type.STRING, "commitHash", gitHashProvider.get())
        buildConfigField(FieldSpec.Type.STRING, "buildTime", buildTimeProvider.get())
    }
}