plugins {
    id("buildsrc.convention.kotlin-multiplatform")
}

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io)
        }

        val wrappedMain by creating {
            dependsOn(commonMain.get())
        }

        jvmMain.get().dependsOn(wrappedMain)
        macosArm64Main.get().dependsOn(wrappedMain)
        linuxX64Main.get().dependsOn(wrappedMain)
        linuxArm64Main.get().dependsOn(wrappedMain)

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
