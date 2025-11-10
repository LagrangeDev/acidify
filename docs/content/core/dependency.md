# 引入依赖

[![Maven Central](https://img.shields.io/maven-central/v/org.ntqqrev/acidify-core?label=Maven%20Central&logo=maven&color=blue)](https://central.sonatype.com/artifact/org.ntqqrev/acidify-core)

Acidify 的核心实现库 `acidify-core` 发布在 Maven Central 上。

对于 JVM 项目，在 `build.gradle.kts` 中添加以下依赖：

```kotlin
dependencies {
    implementation("org.ntqqrev:acidify-core:$version")
}
```

对于 Kotlin Multiplatform 项目：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("org.ntqqrev:acidify-core:$version")
        }
    }
}
```

你需要在项目中添加一个**支持 SSL 的** Ktor Client 引擎依赖。需要特别注意的是，`CIO` 引擎在 Native 平台下不支持 SSL，因此在 Native 下使用时，需要指定 `CIO` 之外的引擎。不同平台下推荐的引擎如下：

| 平台                               | 推荐引擎                  |
|----------------------------------|-----------------------|
| Windows (`mingwX64`)             | `ktor-client-winhttp` |
| macOS (`macosX64`, `macosArm64`) | `ktor-client-darwin`  |
| Linux (`linuxX64`, `linuxArm64`) | `ktor-client-curl`    |
