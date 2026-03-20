# 开始使用

Yogurt 是基于 `acidify-core` 实现的 QQ 协议端，实现了 [Milky 协议](https://milky.ntqqrev.org/)，支持 PC 和 Android 两种登录方式，提供 HTTP 和 WebSocket 接口供应用端调用。

> **Acid**ify + **Milk**y = Yogurt!

> [!important]
>
> Yogurt 需要一个可用的签名 API URL 才能运行，具体协议标准见[签名 API 标准](./signing.md)。Yogurt **不提供**内置签名 API，请自行寻找或实现相关 API。如果没有可用的签名 API，可以考虑使用 [Yogurt-PMHQ](https://github.com/LLOneBot/yogurt-pmhq) 等替代方案。

## 启动

Yogurt 原生支持在 Windows / macOS / Linux 平台上运行，同时通过 JVM 也可以在其他平台上运行。

### 通过 npm 安装和运行（推荐）

[![npm](https://img.shields.io/npm/v/%40acidify%2Fyogurt)](https://www.npmjs.com/package/@acidify/yogurt)

Yogurt 的预编译二进制包发布在 npm 的 `@acidify/yogurt` 包中。先安装 [Node.js](https://nodejs.org/zh-cn/download)（通常会包含一个 `npm`），然后运行以下命令安装 Yogurt：

```
npm install -g @acidify/yogurt
```

安装完成后，可以直接通过 `yogurt` 命令启动 Yogurt。

支持的平台如下：

| OS      | Arch       |
|---------|------------|
| Windows | x64        |
| macOS   | arm64      |
| Linux   | x64, arm64 |

### 从 Releases 下载和运行

Yogurt 的构建产物发布在 [SaltifyDev/yogurt-releases](https://github.com/SaltifyDev/yogurt-releases/releases) 仓库的 Releases 下。下载对应平台的压缩包，解压后运行 `yogurt.(k)exe` 即可。支持的平台如下：

| OS      | Arch       |
|---------|------------|
| Windows | x64        |
| macOS   | arm64      |
| Linux   | x64, arm64 |

Yogurt 的构建产物中还包含可在 JVM 上运行的 fat-jar。配置 Java 25+ 运行时，然后在 Releases 中下载 `yogurt-jvm-all.jar`，运行：

```
java -jar yogurt-jvm-all.jar
```

注意：Yogurt 的 JVM 版本理论上可以在任何支持 Java 25+ 的平台上运行，但由于 Yogurt 依赖 [LagrangeCodec](https://github.com/LagrangeDev/LagrangeCodec) 的预编译构建，因此只支持在以下平台**发送语音和视频消息**：

| OS      | Arch       |
|---------|------------|
| Windows | x86, x64   |
| Linux   | x64, arm64 |
| macOS   | x64, arm64 |

> [!important]
>
> 构建产物发布在 SaltifyDev/yogurt-releases，并非原仓库 LagrangeDev/acidify；后者用于记录 `acidify-core` 的版本迭代，并不包含 Yogurt 的构建产物。

### 通过 Docker 部署（第三方社区实现）

> [!warning]
>
> 以下 Docker 部署方案由第三方社区项目维护，并非 Acidify / Yogurt 官方提供或支持的发行方式。使用前请自行审查镜像、Dockerfile 和部署配置。

> [!tip]
>
> 项目地址：[`shoucandanghehe/yogurt-docker`](https://github.com/shoucandanghehe/yogurt-docker)

#### 使用 Docker Compose 部署

```yaml
services:
  yogurt:
    image: ghcr.io/shoucandanghehe/yogurt-docker:latest
    container_name: yogurt
    restart: unless-stopped
    ports:
      - "3000:3000"
    volumes:
      - ./config.json:/app/config.json
      - data:/app/data

volumes:
  data:
```

首次运行前先创建空配置文件，避免 Docker 将挂载目标创建为目录：

```bash
touch config.json
docker compose up
# 看到 Yogurt 生成默认配置后按 Ctrl+C 停止
```

随后编辑 `config.json`，补全 QQ 号、签名 API 地址等必要配置，再重新启动容器：

```bash
docker compose up -d
```

#### 使用 Docker 部署

```bash
docker run -d \
  --name yogurt \
  --restart unless-stopped \
  -v $(pwd)/config.json:/app/config.json \
  -v $(pwd)/data:/app/data \
  -p 3000:3000 \
  ghcr.io/shoucandanghehe/yogurt-docker:latest
```

详细使用方法、镜像细节和最新说明请查看 [README](https://github.com/shoucandanghehe/yogurt-docker)。
