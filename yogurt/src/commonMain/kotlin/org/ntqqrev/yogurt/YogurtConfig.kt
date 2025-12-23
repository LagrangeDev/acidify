@file:OptIn(ExperimentalSerializationApi::class)

package org.ntqqrev.yogurt

import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import org.ntqqrev.acidify.logging.LogLevel

@Serializable
class YogurtConfig(
    val signApiUrl: String = "",
    /** 音乐卡片签名服务器 URL，留空使用默认值 (https://ss.xingzhige.com/music_card/card) */
    val musicSignUrl: String = "",
    val reportSelfMessage: Boolean = true,
    val preloadContacts: Boolean = false,
    val transformIncomingMFaceToImage: Boolean = false,
    val httpConfig: MilkyHttpConfig = MilkyHttpConfig(),
    val webhookConfig: MilkyWebhookConfig = MilkyWebhookConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val skipSecurityCheck: Boolean = false,
) {
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

    companion object {
        val path = Path("config.json")
        val jsonModule = Json {
            prettyPrint = true
            encodeDefaults = true
            allowComments = true
            allowTrailingComma = true
            ignoreUnknownKeys = true
        }

        fun loadFromFile(): YogurtConfig {
            if (!SystemFileSystem.exists(path)) {
                val defaultConfig = YogurtConfig()
                SystemFileSystem.sink(path).buffered().use {
                    jsonModule.encodeToSink(defaultConfig, it)
                }
                println("配置文件已生成于 ${SystemFileSystem.resolve(path)}")
                println("请根据需要进行修改，修改完成后按 Enter 键继续...")
                readln()
            }
            return SystemFileSystem.source(path).buffered().use {
                jsonModule.decodeFromSource<YogurtConfig>(it)
            }.also {
                SystemFileSystem.sink(path).buffered().use { sink ->
                    jsonModule.encodeToSink(it, sink)
                    // rewrite it to format, and add new fields if any
                }
            }
        }
    }
}