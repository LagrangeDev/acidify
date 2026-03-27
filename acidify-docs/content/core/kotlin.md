# 在 Kotlin 中使用

## 引入依赖

[![Maven Central](https://img.shields.io/maven-central/v/org.ntqqrev/acidify-core?label=Maven%20Central&logo=maven&color=blue)](https://central.sonatype.com/artifact/org.ntqqrev/acidify-core)

Acidify 的核心实现库 `acidify-core` 发布在 Maven Central 上。`acidify-core` 是一个 Kotlin Multiplatform 库，支持的平台如下：

- Kotlin/JVM
- Kotlin/Native
  - Windows via `mingwX64`
  - macOS via `macosArm64`
  - Linux via `linuxX64` and `linuxArm64`
- Kotlin/JS (Node.js only)

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

| 平台                             | 推荐引擎              |
| -------------------------------- | --------------------- |
| Windows (`mingwX64`)             | `ktor-client-winhttp` |
| macOS (`macosArm64`)             | `ktor-client-darwin`  |
| Linux (`linuxX64`, `linuxArm64`) | `ktor-client-curl`    |

## 初始化 Bot

Bot 可以通过构造器来创建：

```kotlin
// 从文件中加载 SessionStore，如果文件不存在则创建一个空的 SessionStore
val sessionStore = if (File("session-store.json").exists()) {
    val json = File("session-store.json").readText()
    SessionStore.fromJson(json)
} else {
    SessionStore.empty()
}

// 创建一个 UrlSignProvider 实例，参数为签名服务的 URL
// 如果要对接新版 Lagrange V2 Sign API，请使用 LagrangeUrlSignProvider
val signProvider = UrlSignProvider("...")
val scope = CoroutineScope(SupervisorJob())

// 创建 Bot 实例
val bot = Bot(
    appInfo = signProvider.getAppInfo()!!,
    sessionStore = SessionStore.empty(),
    signProvider = signProvider,
    scope = scope,
    minLogLevel = LogLevel.DEBUG,
    logHandler = SimpleLogHandler,
)
```

其中各参数的含义如下：
- `appInfo`：Bot 所要模拟的 QQ 客户端的相关信息，包含客户端版本、操作系统、AppID 等信息，可以通过以下方式提供：
  - [`AppInfo.Bundled`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-app-info/-bundled/index.html) 对象提供了内置的 [`AppInfo`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-app-info/index.html) 实现；
  - [`UrlSignProvider.getAppInfo`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-url-sign-provider/get-app-info.html) 方法可以尝试从提供的 URL 获取 `AppInfo`；
  - [`AppInfo.fromJson`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-app-info/from-json.html) 方法可以从 JSON 字符串中解析 `AppInfo`。
- `sessionStore`：用于存储和加载会话数据的存储对象。可以通过以下方式提供：
  - [`SessionStore.empty`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-session-store/empty.html) 方法创建一个空的 [`SessionStore`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-session-store/index.html)；
  - [`SessionStore.fromJson`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-session-store/from-json.html) 方法可以从 JSON 字符串中解析 `SessionStore`。同样，也可以通过 [`toJson`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-session-store/to-json.html) 方法将 `SessionStore` 序列化为 JSON 字符串以便存储。
- `signProvider`：QQ 要求对部分数据包进行签名，Acidify 并不内置签名算法的实现，而是通过 [`SignProvider`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-sign-provider/index.html) 接口来要求使用者提供签名实现。`acidify-core` 提供了 [`UrlSignProvider`](/kdoc/acidify-core/org.ntqqrev.acidify.common/-url-sign-provider/index.html) 类，可以通过 URL 获取签名。
- `scope`：用于 Bot 运行的 [`CoroutineScope`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/)。
- `minLogLevel`：日志的最低输出级别，低于该级别的日志将不会被输出。
- `logHandler`：日志处理器，用于处理输出的日志信息。你可以自行实现该接口。`acidify-core` 提供了两种简单的内置日志处理器：
  - [`NopLogHandler`](/kdoc/acidify-core/org.ntqqrev.acidify.logging/-nop-log-handler/index.html)：不输出任何日志。
  - [`SimpleLogHandler`](/kdoc/acidify-core/org.ntqqrev.acidify.logging/-simple-log-handler/index.html)：将日志输出到控制台。

> [!tip]
>
> 以下代码均假设你在使用 Kotlin/JVM。如果你使用 Kotlin Multiplatform，可以参考[Yogurt 的源代码](https://github.com/LagrangeDev/acidify/tree/main/yogurt)，其中包含了许多在 Kotlin Multiplatform 使用 `acidify-core` 的最佳实践，下文的很多示例均能在 Yogurt 的源代码中找到类似的实现。此外，不要忘记参考 [`acidify-core` 的 KDoc](/kdoc/index.html)，其中包含了完整的 API 文档。

## 调用 Bot API

### 登录

Bot 的所有功能需要在登录后才能使用。调用 [`Bot.login`](/kdoc/acidify-core/org.ntqqrev.acidify/-bot/login.html) 以登录。

该方法会判断 Session，如果 Session 为空则调用 [`qrCodeLogin`](/kdoc/acidify-core/org.ntqqrev.acidify/-bot/qr-code-login.html) 进行登录；如果 Session 不为空则尝试调用 [`online`](/kdoc/acidify-core/org.ntqqrev.acidify/-bot/online.html)，使用现有的 Session 信息登录，若失败则再调用 `qrCodeLogin` 重新登录。

如果你在初始化时正确配置了 `logHandler`，在首次登录时即可以在控制台看到二维码 URL。稍后将会介绍如何监听在登录过程中产生的事件。

`Bot.login` 接收一个可选的 `preloadContacts` 参数，表示是否在登录完成后预加载好友和群聊等联系人的信息。预加载联系人可以提升部分操作的响应速度，同时修复部分情况下无法解析 uid 的问题，但会显著增加启动时间和内存占用。默认值为 `false`。

### 好友和群聊管理

Bot 提供了丰富的 API 来操作好友和群聊等功能。以下是一些示例：

```kotlin
// 获取好友
val friends = bot.getFriends()
friends.forEach { friend ->
    println("Friend: ${friend.nickname} (${friend.uin})")
}
val friend = bot.getFriend(friends.first().uin)
println("First friend: ${friend.nickname} (${friend.uin})")

// 给好友名片点赞
bot.sendProfileLike(friend.uin, 10)

// 获取群聊
val groups = bot.getGroups()
groups.forEach { group ->
    println("Group: ${group.name} (${group.uin})")
}
val group = bot.getGroup(groups.first().uin)
println("First group: ${group.name} (${group.uin})")

// 设置群名称 (需要群管理员权限)
bot.setGroupName(group.uin, "<新群名称>")

// 获取群成员
val groupMembers = bot.getGroupMembers(group.uin)
groupMembers.forEach { member ->
    println("Member: ${member.nickname} (${member.uin})")
}
val member = bot.getGroupMember(group.uin, groupMembers.first().uin)
println("First member: ${member.nickname} (${member.uin})")

// 设置群成员名片 (需要群管理员权限)
bot.setGroupMemberCard(group.uin, member.uin, "<新名片>")

// 设置群成员专属头衔 (需要群主权限)
bot.setGroupMemberSpecialTitle(group.uin, member.uin, "<新头衔>")
```

### 发送消息

Bot 的一个重要功能是发送消息。`acidify-core` 提供了构建消息的 DSL，可以方便地构建复杂的消息内容。以下是一些发送消息的示例：

```kotlin
// 发送好友消息
bot.sendFriendMessage(friend.uin) {
    text("Hello, Acidify!") // 或直接使用 +"Hello, Acidify!"
}

// 发送群消息
bot.sendGroupMessage(group.uin) {
    text("Hello, $member!")
    mention(member.uin, member.card) // @ 某个群成员
}

// 发送图片消息
bot.sendFriendMessage(friend.uin) {
    image(
        raw = File("path/to/image.png").readBytes(),
        format = ImageFormat.PNG,
        width = 300,
        height = 300,
        subType = ImageSubType.STICKER, // 表示作为表情包发送
        summary = "[Cat]" // 可以自定义图片外显文本
    )
}

// 构造合并转发消息
bot.sendGroupMessage(group.uin) {
    forward {
        node(10001L, "Salt") {
            text("A forwarded message example")
        }
        node(10002L, "Milk") {
            text("Another forwarded message")
        }
        // 可以嵌套多层 forward 节点
        node(10003L, "Acid") {
            forward {
                node(10004L, "Shama") {
                    text("A nested forwarded message")
                }
            }
        }
    }
}
```

> [!note]
>
> `acidify-core` 中提供的消息发送功能较为底层，需要调用者自行提供许多元信息，例如被 @ 的成员名片、图片的宽高等。如果你希望使用更高层次的消息发送功能，请考虑使用 Yogurt 并配合应用端框架使用。

发送消息的 API 会返回一个 [`BotOutgoingMessageResult`](/kdoc/acidify-core/org.ntqqrev.acidify.message/-bot-outgoing-message-result/index.html) 对象，包含了消息的序列号和实际发送的时间。

## 处理事件

Bot 提供了一个 [`eventFlow`](/kdoc/acidify-core/org.ntqqrev.acidify/-bot/event-flow.html) 属性，它是一个 <code><a href="https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/">SharedFlow</a>&lt;<a href="/kdoc/acidify-core/org.ntqqrev.acidify.event/-acidify-event/index.html">AcidifyEvent</a>&gt;</code>，用于监听 Bot 产生的各种事件。你可以通过调用该 Flow 的 [`collect`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect.html) 方法来处理事件。

> [!note]
>
> `collect` 是一个 `suspend fun`，调用它会导致当前协程挂起并持续运行。因此，建议你在一个单独的协程中调用它（使用 [`CoroutineScope.launch`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html) 等方式），以免阻塞主协程。以下的代码示例均假设你已经启动了一个独立的协程来处理事件。

> [!warning]
>
> 任何在 `collect` 块中抛出的异常都会导致该协程终止，从而停止对事件的处理。因此，建议在处理事件时使用 `try-catch` 块来捕获并处理可能的异常，以确保事件处理器能够持续运行。也可以使用 [`runCatching`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/run-catching.html) 来包装事件处理逻辑。以下示例均未展示异常处理逻辑，请根据实际情况添加。

### 接收消息

一个重要的事件类型是 [`MessageReceiveEvent`](/kdoc/acidify-core/org.ntqqrev.acidify.event/-message-receive-event/index.html)，它表示 Bot 收到了一条消息。以下是一个示例，展示了如何监听并处理收到的消息：

```kotlin
bot.eventFlow
    .filterIsInstance<MessageReceiveEvent>()
    .collect { event ->
        val message = event.message
        println("Message from ${message.senderUin}:")
        message.segments.forEach { segment ->
            when (segment) {
                is BotIncomingSegment.Text -> {
                    println("Text: ${segment.text}")
                }
                is BotIncomingSegment.Image -> {
                    println("Image: ${segment.summary} (${segment.width}x${segment.height})")
                }
                else -> {
                    println("Other segment: $segment")
                }
            }
        }
    }
```

从中可以看到一条消息 ([`BotIncomingMessage`](/kdoc/acidify-core/org.ntqqrev.acidify.message/-bot-incoming-message/index.html)) 包含了多个消息段 ([`BotIncomingSegment`](/kdoc/acidify-core/org.ntqqrev.acidify.message/-bot-incoming-segment/index.html))，每个消息段表示消息的一部分内容，例如文本、图片等。你可以根据消息段的类型来处理不同的内容。 

### 监听登录过程

上面提到了初次调用 `login` 进行登录时，会进行二维码登录。二维码登录包含三个阶段：
- 二维码的获取，对应 [`QRCodeGeneratedEvent`](/kdoc/acidify-core/org.ntqqrev.acidify.event/-q-r-code-generated-event/index.html)；
- 二维码的状态查询，对应 [`QRCodeStateQueryEvent`](/kdoc/acidify-core/org.ntqqrev.acidify.event/-q-r-code-state-query-event/index.html)；
- 登录成功，Session 更新，对应 [`SessionStoreUpdatedEvent`](/kdoc/acidify-core/org.ntqqrev.acidify.event/-session-store-updated-event/index.html)。

Bot 的 `qrCodeLogin` 方法会在登录过程中依次触发上述事件。你可以监听这些事件来获取登录状态的更新，并且保存信息以便下次登录使用。以下是一个示例：

```kotlin
bot.eventFlow
    .filterIsInstance<QRCodeGeneratedEvent>()
    .collect { event ->
        println("QR Code URL: ${event.qrCodeUrl}")
        File("qrcode.png").writeBytes(event.qrCodePng)
        println("QR Code saved to qrcode.png")
    }

bot.eventFlow
    .filterIsInstance<SessionStoreUpdatedEvent>()
    .collect { event ->
        println("Session updated, new uin: ${event.sessionStore.uin}")
        val sessionJson = event.sessionStore.toJson()
        File("session-store.json").writeText(sessionJson)
        println("Session saved to session-store.json")
    }
```

> [!note]
>
> `eventFlow` 是一个热流 (Hot Flow)，它会在 Bot 启动时立即开始产生事件。因此，你应该尽早开始监听该 Flow，以免错过重要事件。通常建议在调用 `login` 之前就开始监听 `eventFlow`。

### 处理好友/群聊请求

```kotlin
bot.eventFlow
    .filterIsInstance<FriendRequestEvent>()
    .collect { event ->
        println("Friend req from ${event.requesterUin}")
        if (event.comment.contains("<Password>")) {
            bot.setFriendRequest(
                event.initiatorUid,
                accept = true
            )
        }
    }

bot.eventFlow
    .filterIsInstance<GroupJoinRequestEvent>()
    .collect { event ->
        println("Group join req for group ${event.groupUin} from ${event.requesterUin}")
        if (event.comment.contains("<Reason>")) {
            bot.setGroupJoinRequest(
                event.groupUin,
                event.notificationSeq,
                eventType = 1, // Join request
                accept = true
            )
        }
    }
```

## Android 登录

从版本 `0.10.0` 开始，Acidify 还支持使用 Android 协议登录。要使用 Android 协议，同样需要准备相对应的 `AndroidAppInfo`、`AndroidSessionStore` 和 `AndroidSignProvider`，并用它们创建一个 `AndroidBot` 实例：

```kotlin
val androidAppInfo = AndroidAppInfo.Bundled.AndroidPhone
val androidSessionStore = if (File("session-store-android.json").exists()) {
    val json = File("session-store-android.json").readText()
    AndroidSessionStore.fromJson(json)
} else {
    AndroidSessionStore.empty(uin, password)
}
val androidSignProvider = AndroidUrlSignProvider("...")
val scope = CoroutineScope(SupervisorJob())

val androidBot = AndroidBot(
    appInfo = androidAppInfo,
    sessionStore = androidSessionStore,
    signProvider = androidSignProvider,
    scope = scope,
    minLogLevel = LogLevel.DEBUG,
    logHandler = SimpleLogHandler,
)
```

登录需要调用 [`AndroidBot.login`](/kdoc/acidify-core/org.ntqqrev.acidify/-android-bot/login.html) 方法。`AndroidBot` 的登录 API 与 `Bot` 稍有不同，需要提供额外的两个参数：

```kotlin
suspend fun AndroidBot.login(
    onRequireCaptchaTicket: suspend (captchaUrl: String) -> String,
    onRequireSmsCode: suspend (
        countryCode: String,
        phone: String,
        smsUrl: String
    ) -> String,
    preloadContacts: Boolean = false
)
```

- `onRequireCaptchaTicket` 在登录过程中需要用户完成图形验证码时被调用，`captchaUrl` 是验证码图片的 URL，函数需要返回完成验证码后抓取到的 `ticket`。
- `onRequireSmsCode` 在登录过程中需要用户完成短信验证时被调用，参数 `countryCode` 和 `phone` 是短信发送到的手机号的国家码和号码，函数需要返回用户输入的短信验证码。如果用户没有收到验证码，可以通过 `smsUrl` 参数提供的 URL 来手动验证，这时函数可以返回一个空字符串。

与 `Bot.login` 类似，`AndroidBot.login` 也会先尝试使用现有的 Session 信息登录，如果失败则调用上述的回调函数来完成登录流程。登录成功后，会触发 [`AndroidSessionStoreUpdatedEvent`](/kdoc/acidify-core/org.ntqqrev.acidify.event/-android-session-store-updated-event/index.html)，你可以监听该事件来获取新的 Session 信息并保存以便下次登录使用。
