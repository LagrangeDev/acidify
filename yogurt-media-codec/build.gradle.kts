import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
}

val interopDir = file("src/nativeInterop")
fun libraryPath(target: String) = interopDir.resolve("lib/$target")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io)
        }
        jvmMain.dependencies {
            implementation(libs.jna)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    targets.withType<KotlinNativeTarget> {
        val main by compilations.getting
        val nativeInterop by main.cinterops.creating {
            definitionFile.set(project.file("src/nativeInterop/interop.def"))
            extraOpts("-libraryPath", libraryPath(targetName))
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