package org.ntqqrev.yogurt

import com.github.ajalt.mordant.platform.MultiplatformSystem.exitProcess
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.UrlSignProvider
import org.ntqqrev.acidify.login
import org.ntqqrev.acidify.offline
import org.ntqqrev.milky.Event
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.milky.milkyPackageVersion
import org.ntqqrev.milky.milkyVersion
import org.ntqqrev.yogurt.api.configureMilkyApiAuth
import org.ntqqrev.yogurt.api.configureMilkyApiHttpRoutes
import org.ntqqrev.yogurt.api.configureMilkyApiLoginProtect
import org.ntqqrev.yogurt.event.configureMilkyEventAuth
import org.ntqqrev.yogurt.event.configureMilkyEventSse
import org.ntqqrev.yogurt.event.configureMilkyEventWebSocket
import org.ntqqrev.yogurt.event.configureMilkyEventWebhook
import org.ntqqrev.yogurt.script.Script
import org.ntqqrev.yogurt.script.loadScripts
import org.ntqqrev.yogurt.transform.transformAcidifyEvent
import org.ntqqrev.yogurt.util.*

object YogurtApp {
    val config = YogurtConfig.loadFromFile()
    val t = Terminal(ansiLevel = config.logging.ansiLevel)

    fun createServer() = embeddedServer(
        factory = CIO,
        port = config.httpConfig.port,
        host = config.httpConfig.host
    ) {
        if (config.signApiUrl.isEmpty()) {
            t.println(
                TextColors.brightRed(
                    """
                    错误：你未配置 signApiUrl，这会导致 Yogurt 无法启动。
                    请前往设置中配置一个可用的签名 API 地址。
                """.trimIndent()
                )
            )
            exitProcess(1)
        }

        t.println(
            """
            Starting ${BuildKonfig.name} v${BuildKonfig.version}
            .--------------------------------------.
            |   __  __                       __    |
            |   \ \/ /___  ____ ___  _______/ /_   |
            |    \  / __ \/ __ `/ / / / ___/ __/   |
            |    / / /_/ / /_/ / /_/ / /  / /_     |
            |   /_/\____/\__, /\__,_/_/   \__/     |
            |           /____/   Acidify + Milky   |
            '--------------------------------------'
            Commit Hash:    ${BuildKonfig.commitHash}
            Milky Version:  $milkyVersion+@saltify/milky-types@$milkyPackageVersion
            Build Time:     ${BuildKonfig.buildTime}
            Data Directory: ${SystemFileSystem.resolve(Path("."))}
        """.trimIndent()
        )

        if (
            !config.skipSecurityCheck &&
            config.httpConfig.host == "0.0.0.0" &&
            config.httpConfig.accessToken.isEmpty() &&
            !isDockerEnv
        ) {
            t.println(
                TextColors.brightYellow(
                    """
                    警告：你可能正在将 Yogurt 的 Milky 服务暴露在公网环境下，且未设置 accessToken。
                    这可能导致你的 QQ 账号被他人恶意使用，造成损失。
                    请在设置中配置 accessToken，或将 host 设置为 127.0.0.1 或其他内网 IP 地址。
                    如果你明确知道自己在做什么，可以在配置文件中将 skipSecurityCheck 设置为 true 以跳过此检查。
                    程序将在 10 秒后继续运行...
                """.trimIndent()
                )
            )
            delay(10_000)
        }

        val signProvider = UrlSignProvider(config.signApiUrl)
        val sessionStore: SessionStore = if (SystemFileSystem.exists(sessionStorePath)) {
            SystemFileSystem.source(sessionStorePath).buffered().use {
                SessionStore.fromJson(it.readString())
            }
        } else SessionStore.empty()
        val appInfo: AppInfo = signProvider.getAppInfo() ?: run {
            t.println("获取 AppInfo 失败，使用内置默认值")
            AppInfo.Bundled.Linux
        }
        t.println("使用协议 ${appInfo.os} ${appInfo.currentVersion} (AppId: ${appInfo.subAppId})")
        val bot = Bot.create(
            appInfo = appInfo,
            sessionStore = sessionStore,
            signProvider = signProvider,
            scope = this@embeddedServer, // application is a CoroutineScope
            minLogLevel = config.logging.coreLogLevel,
            logHandler = logHandler
        )

        install(ContentNegotiation) {
            json(milkyJsonModule)
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(milkyJsonModule)
        }
        install(SSE)
        install(CORS) {
            if (config.httpConfig.corsOrigins.isEmpty()) {
                anyHost()
            } else {
                config.httpConfig.corsOrigins.forEach { allowHost(it) }
            }
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
        }

        dependencies {
            provide { bot } cleanup {
                runBlocking { it.offline() }
            }
            provide<SharedFlow<Event>> {
                bot.eventFlow
                    .map(this@embeddedServer::transformAcidifyEvent)
                    .filterNotNull()
                    .shareIn(
                        scope = this@embeddedServer,
                        started = SharingStarted.Lazily,
                    )
            }
        }

        routing {
            route("/api") {
                if (config.httpConfig.accessToken.isNotEmpty()) {
                    configureMilkyApiAuth()
                }
                configureMilkyApiLoginProtect()
                configureMilkyApiHttpRoutes()
            }
            route("/event") {
                if (config.httpConfig.accessToken.isNotEmpty()) {
                    configureMilkyEventAuth()
                }
                configureMilkyEventWebSocket()
                configureMilkyEventSse()
            }
        }

        if (!SystemFileSystem.exists(scriptsPath)) {
            SystemFileSystem.createDirectories(scriptsPath)
        }
        val scripts = SystemFileSystem.list(scriptsPath)
            .filter { it.name.endsWith(".yogurtx.js") }
            .map {
                async {
                    Script(
                        name = it.name,
                        content = SystemFileSystem.source(it).buffered().use { r ->
                            withContext(Dispatchers.IO) {
                                r.readString()
                            }
                        }
                    )
                }
            }
            .awaitAll()

        monitor.subscribe(ApplicationStarted) {
            if (config.webhookConfig.url.isNotEmpty()) {
                configureMilkyEventWebhook()
            }
            configureQRCodeDisplay()
            configureSessionStoreAutoSave()
            configureEventLogging()

            launch {
                bot.login(preloadContacts = config.preloadContacts)
                if (scripts.isNotEmpty()) {
                    loadScripts(scripts)
                }
            }
        }
    }
}