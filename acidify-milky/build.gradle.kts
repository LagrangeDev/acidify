plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.atomicfu)
}

group = "org.ntqqrev"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":acidify-core"))
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.di)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.sse)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}