<div align="center">

![acidify](https://socialify.git.ci/LagrangeDev/acidify/image?custom_description=&description=1&font=Inter&forks=1&issues=1&language=1&logo=https%3A%2F%2Fstatic.live.moe%2Flagrange.jpg&name=1&owner=1&pulls=1&stargazers=1&theme=Light)

[![QQ 群](https://img.shields.io/badge/QQ_Group-570335215-green?logo=qq)](https://qm.qq.com/q/C04kPQzayk)
[![Telegram](https://img.shields.io/badge/Telegram-WeavingStar-orange?logo=telegram)](https://t.me/WeavingStar)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/LagrangeDev/acidify)

</div>

## 使用

参见[项目主页](https://acidify.ntqqrev.org/)。

## 模块一览

- `acidify-core` - PC & Android NTQQ 协议的核心实现 [![Maven Central](https://img.shields.io/maven-central/v/org.ntqqrev/acidify-core?label=Maven%20Central&logo=maven&color=blue)](https://central.sonatype.com/artifact/org.ntqqrev/acidify-core)
  - `@acidify/core` - `acidify-core` 的 Kotlin/JS 导出版本 [![npm](https://img.shields.io/npm/v/%40acidify%2Fcore)](https://www.npmjs.com/package/@acidify/core)
- `yogurt` - 基于 `acidify-core` 的 QQ 协议端 [![GitHub Release](https://img.shields.io/github/v/release/SaltifyDev/yogurt-releases?label=GitHub%20release)](https://github.com/SaltifyDev/yogurt-releases)
  - `@acidify/yogurt` - Yogurt 的预编译二进制包 [![npm](https://img.shields.io/npm/v/%40acidify%2Fyogurt)](https://www.npmjs.com/package/@acidify/yogurt)
  - `yogurt-jvm` - Yogurt 的 JVM 平台适配 (Workaround for Ktor plugin's incompatibility issue)

## 支持平台

- Kotlin/JVM
- Kotlin/Native
  - Windows via `mingwX64`
  - macOS via `macosArm64`
  - Linux via `linuxX64` and `linuxArm64`
- Kotlin/JS (for `acidify-core`, Node.js only)

## See Also

- [Milky](https://milky.ntqqrev.org/) - 基于 HTTP / WebSocket 通信的新时代 QQ 机器人应用接口标准
- [Saltify](https://saltify.ntqqrev.org/) - 跨平台、可扩展的 QQ Bot 框架 & Milky SDK
- [yogurt-docker](https://github.com/shoucandanghehe/yogurt-docker) - 社区提供的 Yogurt Docker 部署方案

## Special Thanks

- [Lagrange.Core](https://github.com/LagrangeDev/Lagrange.Core) 提供项目的基础架构和绝大多数协议包定义
- [Konata.Core](https://github.com/KonataDev/Konata.Core) 最初的 PC NTQQ 协议实现
- [lagrange-kotlin](https://github.com/LagrangeDev/lagrange-kotlin) 提供 TEA & 登录认证的实现
- [qrcode-kotlin](https://github.com/g0dkar/qrcode-kotlin/) 提供二维码矩阵生成的实现
- [LagrangeCodec](https://github.com/LagrangeDev/LagrangeCodec) 提供多媒体编解码的实现
- [@Linwenxuan04](https://github.com/Linwenxuan04) 编写 crypto 和 math（原 multiprecision） 部分
- ... and all the contributors along the way!

## Contributors

### Directly to this repository

![Contributors of Acidify](https://contributors-img.web.app/image?repo=LagrangeDev/Acidify)

### Lagrange.Core

![Contributors of Lagrange.Core](https://contributors-img.web.app/image?repo=LagrangeDev/Lagrange.Core)

### LagrangeV2

![Contributors of LagrangeV2](https://contributors-img.web.app/image?repo=LagrangeDev/LagrangeV2)

### Konata.Core

![Contributors of Konata.Core](https://contributors-img.web.app/image?repo=KonataDev/Konata.Core)
