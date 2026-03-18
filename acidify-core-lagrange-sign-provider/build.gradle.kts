plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kmpgrpc)
}

version = "0.1.0"

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":acidify-core"))
        }
    }
}

kmpGrpc {
    common()
    jvm()
    js()
    native()

    internalVisibility = true
    protoSourceFolders = project.files("./proto")
}