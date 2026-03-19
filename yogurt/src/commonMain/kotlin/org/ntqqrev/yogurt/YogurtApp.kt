package org.ntqqrev.yogurt

import com.dokar.quickjs.QuickJs
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.milky.milkyVersion
import org.ntqqrev.yogurt.api.configureMilkyApiAuth
import org.ntqqrev.yogurt.api.configureMilkyApiHttpRoutes
import org.ntqqrev.yogurt.api.configureMilkyApiLoginProtect
import org.ntqqrev.yogurt.config.loadConfigAndUpdate
import org.ntqqrev.yogurt.event.configureMilkyEventAuth
import org.ntqqrev.yogurt.event.configureMilkyEventSse
import org.ntqqrev.yogurt.event.configureMilkyEventWebSocket
import org.ntqqrev.yogurt.event.configureMilkyEventWebhook
import org.ntqqrev.yogurt.script.createScriptEnvironment
import org.ntqqrev.yogurt.script.loadScripts
import org.ntqqrev.yogurt.util.*

object YogurtApp {
    val config = loadConfigAndUpdate()
    val t = Terminal(ansiLevel = config.logging.ansiLevel)

    fun createServer() = embeddedServer(
        factory = CIO,
        port = config.milky.http.port,
        host = config.milky.http.host
    ) {
        if (config.protocol.signApiUrl.isEmpty()) {
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
                .--------------------------------------.
                |   __  __                       __    |
                |   \ \/ /___  ____ ___  _______/ /_   |
                |    \  / __ \/ __ `/ / / / ___/ __/   |
                |    / / /_/ / /_/ / /_/ / /  / /_     |
                |   /_/\____/\__, /\__,_/_/   \__/     |
                |           /____/   Acidify + Milky   |
                '--------------------------------------'
                ${BuildKonfig.name} v${BuildKonfig.version}
                
                Commit Hash:    ${BuildKonfig.commitHash}
                Core Version:   ${BuildKonfig.coreVersion}
                Milky Version:  ${BuildKonfig.milkyVersion} ($milkyVersion)
                Build Time:     ${BuildKonfig.buildTime}
                Data Directory: ${SystemFileSystem.resolve(Path("."))}
            """.trimIndent()
        )

        if (
            !config.security.skipOnLaunchListenAddressCheck &&
            config.milky.http.host == "0.0.0.0" &&
            config.milky.http.accessToken.isEmpty() &&
            !isDockerEnv
        ) {
            t.println(
                TextColors.brightYellow(
                    """
                        警告：你可能正在将 Yogurt 的 Milky 服务暴露在公网环境下，且未设置 accessToken。
                        这可能导致你的 QQ 账号被他人恶意使用，造成损失。
                        请在设置中配置 accessToken，或将 host 设置为 127.0.0.1 或其他内网 IP 地址。
                        如果你明确知道自己在做什么，可以在配置文件中将 skipOnLaunchListenAddressCheck 设置为 true 以跳过此检查。
                        程序将在 10 秒后继续运行...
                    """.trimIndent()
                )
            )
            delay(10_000)
        }

        when {
            isPC -> initializePC()
            isAndroid -> initializeAndroid()
            else -> throw IllegalStateException(
                "不支持的协议 ${config.protocol.os}，当前仅支持 Windows、Mac、Linux、AndroidPhone、AndroidPad"
            )
        }

        install(ContentNegotiation) {
            json(milkyJsonModule)
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(milkyJsonModule)
        }
        install(SSE)
        install(CORS) {
            if (config.milky.http.corsOrigins.isEmpty()) {
                anyHost()
            } else {
                config.milky.http.corsOrigins.forEach { allowHost(it) }
            }
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
        }

        val qjs = createScriptEnvironment()
        dependencies {
            provide<QuickJs> { qjs } cleanup { it.close() }
        }

        routing {
            route("/api") {
                if (config.milky.http.accessToken.isNotEmpty()) {
                    configureMilkyApiAuth()
                }
                configureMilkyApiLoginProtect()
                configureMilkyApiHttpRoutes()
            }
            route("/event") {
                if (config.milky.http.accessToken.isNotEmpty()) {
                    configureMilkyEventAuth()
                }
                configureMilkyEventWebSocket()
                configureMilkyEventSse()
            }
        }

        if (!SystemFileSystem.exists(scriptsPath)) {
            SystemFileSystem.createDirectories(scriptsPath)
        }

        monitor.subscribe(ApplicationStarted) {
            if (config.milky.webhook.endpoints.isNotEmpty()) {
                configureMilkyEventWebhook()
            }
            configureQRCodeDisplay()
            configureSessionStoreAutoSave()
            configureEventLogging()

            launch {
                botLogin()
                loadScripts()
            }
        }
    }
}
