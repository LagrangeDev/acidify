package buildsrc.convention

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    val actionsTarget = System.getenv("GITHUB_ACTIONS_BUILD_TARGET")
    when (actionsTarget) {
        // is run from GitHub Actions - build Yogurt, improving dependency pulling time
        "jvm" -> {} // already added
        "mingwX64" -> mingwX64()
        "macosX64" -> macosX64()
        "macosArm64" -> macosArm64()
        "linuxX64" -> linuxX64()
        "linuxArm64" -> linuxArm64()

        // is run locally
        else -> {
            jvm()
            mingwX64()
            macosX64()
            macosArm64()
            linuxX64()
            linuxArm64()
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
        .matching { it.name == "linuxArm64" }
        .all {
            binaries.configureEach {
                val libgccPath = providers.gradleProperty("linuxArm64LibgccPath").orNull
                    ?: System.getenv("LINUX_ARM64_LIBGCC")
                    ?: "/usr/lib/gcc-cross/aarch64-linux-gnu/11/libgcc.a"
                linkerOpts(libgccPath)
            }
        }

    jvmToolchain(25)
}
