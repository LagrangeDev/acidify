# 配置

Yogurt 在启动后，会在当前工作目录下生成 `config.json` 文件，用户可以编辑该文件来配置 Yogurt。

```json
{
  "configVersion": 3,
  "protocol": {
    "uin": 0,
    "password": "",
    "os": "Linux",
    "version": "fetched",
    "signApiUrl": "...",
    "pcLagrangeSignToken": "",
    "androidUseLegacySign": false
  },
  "milky": {
    "http": {
      "host": "127.0.0.1",
      "port": 3000,
      "prefix": "",
      "accessToken": "",
      "corsOrigins": []
    },
    "webhook": {
      "endpoints": []
    },
    "reportSelfMessage": true,
    "preloadContacts": false,
    "ffmpegPath": ""
  },
  "logging": {
    "ansiLevel": "ANSI256",
    "coreLogLevel": "DEBUG"
  },
  "security": {
    "skipOnLaunchListenAddressCheck": false
  }
}
```

## 配置项说明

### `configVersion`

配置文件版本号。当前版本为 `3`。

如果读取到没有该字段的旧版配置文件，Yogurt 会将其视为 V1 配置；读取到 `configVersion = 2` 的配置文件时，则会将其视为 V2 配置。两者都会在启动时自动迁移到 V3 格式后重写回 `config.json`。

### `protocol.uin` 和 `protocol.password`

登录凭据，包含 `uin`（QQ 号）和 `password`（密码）。

在使用 `AndroidPhone` 或 `AndroidPad` 协议时，需要同时提供 `uin` 和 `password` 来登录。

在使用 `Windows`、`Mac` 或 `Linux` 协议，并且 `pcLagrangeSignToken` 不为空时，也需要提供 `uin`。`password` 可以留空。

> [!warning]
>
> 由于密码在 `config.json` 中以明文形式保存，请务必妥善保管该文件，避免泄露。此外，也要谨防 `session-store.json` 和 `session-store-android.json` 文件泄露。

### `protocol.os`

Yogurt 使用的协议类型。可选值有：

- `Windows`
- `Mac`
- `Linux`
- `AndroidPhone`
- `AndroidPad`

请注意并非所有协议类型都一定可用，需要与提供的 `protocol.signApiUrl` 对应的签名服务相匹配。

### `protocol.version`

使用的协议版本。支持以下几种配置方式：

- `fetched`：启动时从签名服务获取最新版本。只有在使用 `Windows`、`Mac`、`Linux` 协议时可用。
- 指定具体的协议版本号，例如 `39038`、`9.1.60`。目前仅有如下几种内置的协议版本可用：
  - `Linux` 的：
    - `39038` 版本
    - `46494` 版本
  - `AndroidPhone` 和 `AndroidPad` 的：
    - `9.1.60` 版本
    - `9.1.70` 版本
    - `9.2.0` 版本
    - `9.2.20` 版本
- `custom`：启动时从工作目录下的 `app-info.json` 文件获取版本信息。

> [!tip]
> 
> 如果你认为某个协议版本缺失，或者希望添加对某个新版本的支持，可以提交 Pull Request 或 Issue 以添加。

### `protocol.signApiUrl`

签名服务地址。这是 Yogurt 赖以运行的关键配置项。Yogurt 本身并不处理数据包的签名，而是将这些工作交给一个单独的签名服务来完成。

在 `protocol.androidUseLegacySign` 设置为 `true` 时，可以在 URL 中使用 Basic Auth 进行身份验证，并且添加自定义的 Query Parameter，例如 `https://username:password@example.com/?key=1`。

### `protocol.pcLagrangeSignToken`

默认为空。若填写该字段，则 Yogurt 会在使用 `Windows`、`Mac` 或 `Linux` 协议时，使用新版 Lagrange Sign API 来获取签名。如果你不确定是否使用的是新版 API，请保持该字段为空。

> [!important]
>
> 使用新版 Lagrange Sign API 时，将不能使用 `fetched` 版本，必须指定具体的协议版本号，或者使用 `custom` 版本并提供 `app-info.json` 文件。此外，还需要填写 `uin` 字段，且 `uin` 需要与实际登录的 QQ 号一致。`password` 字段可以留空。

### `protocol.androidUseLegacySign`

是否使用的是旧版的 Android Sign API（需要进行设备 Register 操作，不支持 `/get_tlv553` 端点）。

如果你在使用兼容 ICQQ 的签名服务时遇到登录问题，可以尝试将该选项设置为 `true`。如果你不确定使用的是否为旧版 API，请保持默认值 `false`，出现登录问题时再尝试将其设置为 `true`。

### `milky.http` 和 `milky.webhook.endpoints`

Milky 协议服务的有关配置，参考 [Milky 文档的“通信”部分](https://milky.ntqqrev.org/guide/communication)。

### `milky.http.prefix`

Milky 协议服务所在路由的路径前缀。默认为空字符串。

如果设置了该项，例如设置为 `/mybot`，那么所有的 API 都会被挂载到 `/mybot/api` 下，所有的事件流都会被挂载到 `/mybot/event` 下。

### `milky.http.corsOrigins`

允许跨域请求的来源列表。若为空数组，则允许所有来源。

在允许所有来源时，依然可以通过 Authorization 头携带访问令牌，因为 `Access-Control-Allow-Headers` 头会包含 `Authorization`。

### `milky.reportSelfMessage`

是否上报自己发送的消息。

### `milky.preloadContacts`

是否在启动时预加载联系人列表。预加载联系人列表可以提升部分操作的响应速度，同时修复部分情况下无法解析 uid 的问题，但会显著增加启动时间和内存占用。

### `milky.ffmpegPath`

FFmpeg 可执行文件的路径。默认为空字符串，表示不使用 FFmpeg，而使用内置的媒体处理功能。若使用内置媒体处理功能时发生崩溃，可以尝试安装 FFmpeg 并将该项设置为 FFmpeg 可执行文件的路径来解决问题。

### `logging.ansiLevel`

Yogurt 日志中 ANSI 颜色的输出级别。可选值有 `NONE`, `ANSI16`, `ANSI256` 和 `TRUECOLOR`。如果不设置该配置项，则默认使用 `ANSI256`。如果你的终端不支持 ANSI 颜色，可以将该配置项设置为 `NONE` 来禁用颜色输出，或降级到 `ANSI16`。更详细的说明请参考 [Mordant 文档中的 AnsiLevel](https://ajalt.github.io/mordant/api/mordant/com.github.ajalt.mordant.rendering/-ansi-level/index.html)。

### `security.skipOnLaunchListenAddressCheck`

是否跳过检测是否在非 Docker 环境下将 HTTP 服务绑定到 `0.0.0.0` 并且未设置访问令牌。

设为 `false` 时，如果符合上述条件，Yogurt 会在启动时打印一条警告信息并延迟 10 秒，以提醒用户注意安全风险。为 `true` 时，则会跳过检查和警告。

## 日志配置

Yogurt 的日志分为两类：由 Yogurt 自身产生的日志和 Ktor 产生的日志。在不同平台下，日志的配置方式有很大不同。

### Kotlin/Native 平台

Kotlin/Native 平台的 Yogurt 使用 `println` 输出日志。可以想象有以下的闸门：

```mermaid
flowchart TD
    subgraph inputs[日志源]
        direction LR
        A[Yogurt 日志]
        D[Ktor 日志]
    end

    A --> B[闸门 1<br/>由 <code>coreLogLevel</code> 控制]
    B --> C[标准输出]

    D --> E[闸门 2<br/>由 <code>KTOR_LOG_LEVEL</code> 环境变量控制]
    E --> C
```

要控制 Yogurt 日志的输出级别，可以在 `config.json` 中配置 `logging.coreLogLevel`，可选值有 `VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`。如果不设置该配置项，则默认输出 `DEBUG` 以上级别的日志。

要控制 Ktor 日志的输出级别，可以设置环境变量 `KTOR_LOG_LEVEL`，可选值有 `DEBUG`, `INFO`, `WARN`, `ERROR`。如果不设置该环境变量，则 Ktor 默认输出 `INFO` 级别及以上的日志。

### Kotlin/JVM 平台

Kotlin/JVM 平台的 Yogurt 使用 [Logback](https://logback.qos.ch/) 进行日志管理。可以想象有以下的闸门：

```mermaid
flowchart TD
    subgraph inputs[日志源]
        direction LR
        A[Yogurt 日志]
        E[Ktor 日志]
    end

    A --> B[闸门 1<br/>由 <code>coreLogLevel</code> 控制]
    B --> C[闸门 2<br/>由 <code>logback.xml</code> 控制]
    C --> D[标准输出 / 文件]
    E --> C
```

要控制 Yogurt 日志的输出级别，可以在 `config.json` 中配置 `logging.coreLogLevel`，设置方式和 Kotlin/Native 平台相同。

Yogurt/JVM 的日志最终由 Logback 处理，因此可以通过配置 Logback 来控制日志的输出方式和格式。JAR 文件中已经包含了一个默认的 `logback.xml`，默认向控制台输出带有颜色的、最低等级为 `DEBUG` 的日志，内容如下：

```xml
<configuration>
    <statusListener class="ch.qos.logback.core.status.NopStatusListener"/>
    <appender name="CONSOLE" class="org.ntqqrev.yogurt.util.YogurtConsoleAppender"/>
    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

如果需要自定义日志配置，可以在运行时通过 `-Dlogback.configurationFile=path/to/logback.xml` 指定自定义的配置文件。你可以基于上述配置进行自定义。
