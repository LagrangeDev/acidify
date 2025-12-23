package org.ntqqrev.yogurt.music

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * 音乐平台类型
 */
enum class MusicType(val value: String) {
    QQ("qq"),
    NETEASE("163"),
    KUGOU("kugou"),
    MIGU("migu"),
    KUWO("kuwo"),
    CUSTOM("custom");

    companion object {
        fun fromString(value: String): MusicType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown music type: $value")
        }
    }
}

/**
 * 音乐消息段 - Milky 协议扩展
 * 
 * 用于发送音乐卡片消息，支持 QQ音乐、网易云、酷狗等平台
 */
@Serializable
data class MusicSegment(
    /** 音乐类型: qq, 163, kugou, migu, kuwo, custom */
    @SerialName("type") val type: String,
    /** 音乐段数据 */
    @SerialName("data") val data: MusicData
)

@Serializable
data class MusicData(
    /** 音乐类型 */
    @SerialName("music_type") val musicType: String,
    /** 音乐 ID（平台音乐必填） */
    @SerialName("id") val id: String? = null,
    /** 跳转 URL（自定义音乐必填） */
    @SerialName("url") val url: String? = null,
    /** 音频 URL（自定义音乐必填） */
    @SerialName("audio") val audio: String? = null,
    /** 标题（自定义音乐必填） */
    @SerialName("title") val title: String? = null,
    /** 封面图片 URL（自定义音乐可选） */
    @SerialName("image") val image: String? = null,
    /** 歌手名称（自定义音乐可选） */
    @SerialName("singer") val singer: String? = null,
)

/**
 * 音乐签名请求
 */
@Serializable
data class MusicSignRequest(
    /** 音乐类型 */
    @SerialName("type") val type: String,
    /** 音乐 ID */
    @SerialName("id") val id: String? = null,
    /** 跳转 URL */
    @SerialName("url") val url: String? = null,
    /** 音频 URL */
    @SerialName("audio") val audio: String? = null,
    /** 标题 */
    @SerialName("title") val title: String? = null,
    /** 封面图片 URL */
    @SerialName("image") val image: String? = null,
    /** 歌手名称 */
    @SerialName("singer") val singer: String? = null,
)

/**
 * 音乐签名响应
 */
@Serializable
data class MusicSignResponse(
    /** 签名后的 JSON Ark Payload */
    @SerialName("data") val data: String? = null,
    /** 错误信息 */
    @SerialName("error") val error: String? = null,
    /** 状态码 */
    @SerialName("code") val code: Int = 0,
)

/**
 * 音乐签名异常
 */
class MusicSignException(message: String) : Exception(message)

/**
 * 音乐签名器 - 调用外部签名服务获取 Ark JSON
 * 
 * 签名服务地址: https://ss.xingzhige.com/music_card/card
 * 该服务直接返回原始 Ark JSON (不是包装的响应对象)
 */
class MusicSigner(
    private val signUrl: String = "https://ss.xingzhige.com/music_card/card"
) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 签名音乐请求，返回 Ark JSON 字符串
     * 
     * @param request 音乐签名请求
     * @return Ark JSON 字符串
     * @throws MusicSignException 签名失败时抛出
     */
    suspend fun sign(request: MusicSignRequest): String {
        try {
            // 构建请求体
            val requestBody = buildJsonObject {
                put("type", request.type)
                request.id?.let { put("id", it) }
                request.url?.let { put("url", it) }
                request.audio?.let { put("audio", it) }
                request.title?.let { put("title", it) }
                request.image?.let { put("image", it) }
                request.singer?.let { put("singer", it) }
            }
            
            val response = client.post(signUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            if (!response.status.isSuccess()) {
                throw MusicSignException("Sign service returned ${response.status}")
            }
            
            // 签名服务直接返回 Ark JSON
            val responseText = response.bodyAsText()
            
            // 验证响应是否为有效的 Ark JSON (应该包含 "app" 字段)
            try {
                val jsonElement = json.parseToJsonElement(responseText)
                if (jsonElement is JsonObject && jsonElement.containsKey("app")) {
                    return responseText
                } else {
                    throw MusicSignException("Invalid response: not an Ark message")
                }
            } catch (e: Exception) {
                if (e is MusicSignException) throw e
                throw MusicSignException("Failed to parse response: ${e.message}")
            }
        } catch (e: Exception) {
            if (e is MusicSignException) throw e
            throw MusicSignException("Sign request failed: ${e.message}")
        }
    }
    
    fun close() {
        client.close()
    }
}
