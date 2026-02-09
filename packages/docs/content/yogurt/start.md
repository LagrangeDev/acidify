# 开始使用

Yogurt 是基于 Acidify 的 [Milky](https://milky.ntqqrev.org/) 实现，支持 Kotlin/JVM 和 Kotlin/Native 平台。

> **Acid**ify + **Milk**y = Yogurt!

## 启动

Yogurt 支持的平台有 Kotlin/Native 和 Kotlin/JVM 两种。

### 通过包管理器安装并启动 (Kotlin/Native)

[![npm](https://img.shields.io/npm/v/%40acidify%2Fyogurt)](https://www.npmjs.com/package/@acidify/yogurt)

Yogurt 的预编译二进制包发布在 npm 的 `@acidify/yogurt` 包中。先安装 [Node.js](https://nodejs.org/zh-cn/download)（通常会包含一个 `npm`），然后运行以下命令安装并启动 Yogurt：

```
npm install -g @acidify/yogurt
yogurt
```

支持的平台如下：

| OS      | Arch       |
|---------|------------|
| Windows | x64        |
| Linux   | x64, arm64 |
| macOS   | x64, arm64 |

### 通过 Java 运行时启动 (Kotlin/JVM)

配置 Java 25+ 运行时，然后在 [Actions](https://github.com/LagrangeDev/acidify/actions/workflows/build-yogurt.yml) 中下载 JAR 文件，运行：

```
java -jar yogurt-jvm-all.jar
```

注意：Yogurt 的 JVM 版本理论上可以在任何支持 Java 25+ 的平台上运行，但由于 Yogurt 依赖 [LagrangeCodec](https://github.com/LagrangeDev/LagrangeCodec) 的预编译构建，因此只支持在以下平台**发送语音和视频消息**：

| OS      | Arch       |
|---------|------------|
| Windows | x86, x64   |
| Linux   | x64, arm64 |
| macOS   | x64, arm64 |

> [!note]
> 
> 下载 Actions 的构建产物必须登录 GitHub，并且构建产物有保存期限，请及时下载。对于一般的使用需求，推荐使用 npm 包的方式安装和运行 Yogurt。
