import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
}

version = "0.1.0"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":acidify-core"))
            implementation(libs.mordant)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
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
                entryPoint = "org.ntqqrev.acidify.runner.main"
            }
        }
    }

    tasks.withType<JavaExec> {
        standardInput = System.`in`
    }
}