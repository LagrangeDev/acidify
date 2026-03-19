@file:OptIn(ExperimentalSerializationApi::class)

package org.ntqqrev.yogurt.config

import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.json.io.encodeToSink
import org.ntqqrev.yogurt.configPath

fun YogurtConfig.toV2() = YogurtConfigV2(
    signApiUrl = signApiUrl,
    protocol = protocol,
    androidCredentials = androidCredentials,
    androidUseLegacySign = androidUseLegacySign,
    reportSelfMessage = reportSelfMessage,
    preloadContacts = preloadContacts,
    transformIncomingMFaceToImage = transformIncomingMFaceToImage,
    httpConfig = httpConfig,
    webhookConfig = webhookConfig.url.map { webhookUrl ->
        YogurtConfigV2.WebhookEndpointConfig(
            url = webhookUrl,
            accessToken = webhookConfig.accessToken,
        )
    },
    logging = logging,
    skipSecurityCheck = skipSecurityCheck,
)

fun YogurtConfigV2.toV3() = YogurtConfigV3(
    protocol = YogurtConfigV3.ProtocolConfig(
        uin = androidCredentials.uin,
        password = androidCredentials.password,
        os = protocol.os,
        version = protocol.version,
        signApiUrl = signApiUrl,
        androidUseLegacySign = androidUseLegacySign,
    ),
    milky = YogurtConfigV3.MilkyConfig(
        http = YogurtConfigV3.MilkyConfig.HttpConfig(
            host = httpConfig.host,
            port = httpConfig.port,
            accessToken = httpConfig.accessToken,
            corsOrigins = httpConfig.corsOrigins,
        ),
        webhook = YogurtConfigV3.MilkyConfig.WebhookConfig(
            endpoints = webhookConfig.map {
                YogurtConfigV3.MilkyConfig.WebhookConfig.Endpoint(
                    url = it.url,
                    accessToken = it.accessToken,
                )
            }
        ),
        reportSelfMessage = reportSelfMessage,
        preloadContacts = preloadContacts,
    ),
    logging = YogurtConfigV3.LoggingConfig(
        coreLogLevel = logging.coreLogLevel,
        ansiLevel = logging.ansiLevel,
    ),
    security = YogurtConfigV3.SecurityConfig(
        skipOnLaunchListenAddressCheck = skipSecurityCheck,
    ),
)

fun detectConfigVersion(configJson: JsonElement): Int {
    return configJson.jsonObject["configVersion"]?.jsonPrimitive?.intOrNull ?: 1
}

val jsonModule = Json {
    prettyPrint = true
    encodeDefaults = true
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = true
}

fun loadConfigAndUpdate(): YogurtConfigV3 {
    if (!SystemFileSystem.exists(configPath)) {
        val defaultConfig = YogurtConfigV3()
        SystemFileSystem.sink(configPath).buffered().use {
            jsonModule.encodeToSink(defaultConfig, it)
        }
        println("配置文件已生成于 ${SystemFileSystem.resolve(configPath)}")
        println("请根据需要进行修改，修改完成后按 Enter 键继续...")
        readln()
    }

    val configJson = jsonModule.parseToJsonElement(
        SystemFileSystem.source(configPath).buffered().use {
            it.readString()
        }
    )
    val config = when (val configVersion = detectConfigVersion(configJson)) {
        1 -> jsonModule.decodeFromJsonElement<YogurtConfig>(configJson).toV2().toV3()
        2 -> jsonModule.decodeFromJsonElement<YogurtConfigV2>(configJson).toV3()
        3 -> jsonModule.decodeFromJsonElement<YogurtConfigV3>(configJson)
        else -> error("不支持的配置版本 $configVersion")
    }

    SystemFileSystem.sink(configPath).buffered().use { sink ->
        jsonModule.encodeToSink(config, sink)
        // rewrite it to format, and add new fields if any
    }

    return config
}