import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

group = "org.ntqqrev"
version = "0.2.2"

kotlin {
    js(IR) {
        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            freeCompilerArgs.add("-Xes-long-as-bigint")
        }
        compilations["main"].packageJson {
            name = "@acidify/core"
            customField("description", "Kotlin NTQQ protocol implementation, ported to JS")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("reflect"))
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.io)
            implementation(libs.bundles.ktor.client)
            implementation(libs.ktor.network)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.korlibs.compression)
            implementation(libs.bundles.xmlutil)
            implementation(libs.stately.concurrent.collections)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(kotlin("test"))
        }
        val nativeMain by creating {
            listOf(
                "linuxX64",
                "linuxArm64",
                "macosX64",
                "macosArm64",
                "mingwX64"
            ).forEach { platformName ->
                getByName(platformName + "Main").dependsOn(this)
            }
        }
        val nonJsMain by creating {
            dependsOn(getByName("commonMain"))
            getByName("jvmMain").dependsOn(this)
            nativeMain.dependsOn(this)
        }
        all {
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
            languageSettings.optIn("kotlin.js.ExperimentalJsStatic")
            languageSettings.optIn("kotlin.js.ExperimentalWasmJsInterop")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString()
    )

    pom {
        name = project.name
        description = "Kotlin NTQQ protocol implementation"
        url = "https://github.com/LagrangeDev/acidify"
        inceptionYear = "2025"
        licenses {
            license {
                name = "GNU General Public License v3.0"
                url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
            }
        }
        developers {
            developer {
                id = "Wesley-Young"
                name = "Wesley F. Young"
                email = "wesley.f.young@outlook.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/LagrangeDev/acidify.git"
            developerConnection = "scm:git:ssh://github.com/LagrangeDev/acidify.git"
            url = "https://github.com/LagrangeDev/acidify"
        }
    }
}

val currentYearProvider = providers.provider {
    ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        .format(DateTimeFormatter.ofPattern("yyyy"))
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("../../docs/public/kdoc"))
    }
    dokkaSourceSets.commonMain {
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl("https://github.com/LagrangeDev/acidify/tree/main/acidify-core/src")
            remoteLineSuffix.set("#L")
        }
    }
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("kotlinx-coroutines") {
            url("https://kotlinlang.org/api/kotlinx.coroutines/")
        }
    }
    pluginsConfiguration.html {
        homepageLink = "https://acidify.ntqqrev.org"
        footerMessage = "© ${currentYearProvider.get()} LagrangeDev. Licensed under GNU GPLv3."
    }
}