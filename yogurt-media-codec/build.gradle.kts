plugins {
    id("buildsrc.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io)
        }
        jvmMain.dependencies {
            implementation("net.java.dev.jna:jna:5.18.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}