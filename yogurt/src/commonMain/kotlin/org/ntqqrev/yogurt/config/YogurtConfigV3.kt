package org.ntqqrev.yogurt.config

import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlinx.serialization.Serializable
import org.ntqqrev.acidify.logging.LogLevel

@Serializable
data class YogurtConfigV3(
    val configVersion: Int = 3,
    val protocol: ProtocolConfig = ProtocolConfig(),
    val milky: MilkyConfig = MilkyConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val security: SecurityConfig = SecurityConfig(),
    val debug: DebugConfig = DebugConfig(),
) {
    @Serializable
    data class ProtocolConfig(
        val uin: Long = 0L,
        val password: String = "",
        val os: String = "Linux",
        val version: String = "fetched",
        val signApiUrl: String = "",
        val pcLagrangeSignToken: String = "",
        val androidUseLegacySign: Boolean = false,
    )

    @Serializable
    data class MilkyConfig(
        val http: HttpConfig = HttpConfig(),
        val webhook: WebhookConfig = WebhookConfig(),
        val reportSelfMessage: Boolean = true,
        val preloadContacts: Boolean = false,
        val ffmpegPath: String = "",
    ) {
        @Serializable
        data class HttpConfig(
            val host: String = "127.0.0.1",
            val port: Int = 3000,
            val prefix: String = "",
            val accessToken: String = "",
            val corsOrigins: List<String> = listOf(),
        )

        @Serializable
        data class WebhookConfig(
            val endpoints: List<Endpoint> = emptyList(),
        ) {
            @Serializable
            data class Endpoint(
                val url: String = "",
                val accessToken: String = "",
            )
        }
    }

    @Serializable
    data class LoggingConfig(
        val ansiLevel: AnsiLevel = AnsiLevel.ANSI256,
        val coreLogLevel: LogLevel = LogLevel.DEBUG,
    )

    @Serializable
    data class SecurityConfig(
        val skipOnLaunchListenAddressCheck: Boolean = false,
    )

    @Serializable
    data class DebugConfig(
        val enableFaceDetailsApi: Boolean = false,
    )
}