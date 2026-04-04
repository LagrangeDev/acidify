package org.ntqqrev.yogurt

import com.dokar.quickjs.QuickJs
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
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.acidify.milky.configureMilky
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.milky.milkyVersion
import org.ntqqrev.yogurt.config.loadConfigAndUpdate
import org.ntqqrev.yogurt.debug.configureDebugFaceDetailsApi
import org.ntqqrev.yogurt.fs.withFs
import org.ntqqrev.yogurt.script.createScriptEnvironment
import org.ntqqrev.yogurt.script.loadScripts
import org.ntqqrev.yogurt.util.*
import kotlin.time.Duration.Companion.milliseconds

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
            halt(1)
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
                Listen Address: ${config.milky.http.host}:${config.milky.http.port}${config.milky.http.prefix}
                Data Directory: ${withFs { resolve(Path(".")) }}
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
            delay(10_000.milliseconds)
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


        val ctx = MilkyContext(
            application = this@embeddedServer,
            implName = BuildKonfig.name,
            implVersion = BuildKonfig.version,
            protocolOs = config.protocol.os,
            httpAccessToken = config.milky.http.accessToken,
            webhookEndpoints = config.milky.webhook.endpoints.map { (url, token) ->
                MilkyContext.WebhookEndpoint(url, token)
            },
            reportSelfMessage = config.milky.reportSelfMessage,
            uriResolver = ::resolveUri,
            codec = FFmpegCodec,
        )
        context(ctx) {
            val rawPrefix = config.milky.http.prefix.ifEmpty { "/" }
            val prefix = if (rawPrefix == "/") "/" else rawPrefix.removeSuffix("/")

            routing {
                route(prefix) {
                    configureMilky()

                    route("/debug") {
                        if (config.debug.enableFaceDetailsApi) {
                            configureDebugFaceDetailsApi()
                        }
                    }
                }
            }

            val qjs = createScriptEnvironment()

            dependencies {
                provide<MilkyContext> { ctx }
                provide<QuickJs> { qjs } cleanup { it.close() }
            }
        }

        monitor.subscribe(ApplicationStarted) {
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