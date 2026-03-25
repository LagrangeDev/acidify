package org.ntqqrev.yogurt.config

import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlinx.serialization.Serializable
import org.ntqqrev.acidify.logging.LogLevel

@Serializable
class YogurtConfig(
    val signApiUrl: String = "",
    val protocol: Protocol = Protocol(),
    val androidCredentials: AndroidCredentials = AndroidCredentials(),
    val androidUseLegacySign: Boolean = false,
    val reportSelfMessage: Boolean = true,
    val preloadContacts: Boolean = false,
    val transformIncomingMFaceToImage: Boolean = false,
    val httpConfig: MilkyHttpConfig = MilkyHttpConfig(),
    val webhookConfig: MilkyWebhookConfig = MilkyWebhookConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val skipSecurityCheck: Boolean = false,
) {
    @Serializable
    class Protocol(
        val os: String = "Linux",
        val version: String = "fetched",
    )

    @Serializable
    class AndroidCredentials(
        val uin: Long = 0L,
        val password: String = "",
    )

    @Serializable
    class LoggingConfig(
        val ansiLevel: AnsiLevel = AnsiLevel.ANSI256,
        val coreLogLevel: LogLevel = LogLevel.DEBUG,
    )

    @Serializable
    class MilkyHttpConfig(
        val host: String = "127.0.0.1",
        val port: Int = 3000,
        val accessToken: String = "",
        val corsOrigins: List<String> = listOf(),
    )

    @Serializable
    class MilkyWebhookConfig(
        val url: List<String> = emptyList(),
        val accessToken: String = "",
    )
}