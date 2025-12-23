package org.ntqqrev.yogurt.api.message

import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ntqqrev.acidify.Bot
import org.ntqqrev.yogurt.music.MusicSignException
import org.ntqqrev.yogurt.music.MusicSignRequest
import org.ntqqrev.yogurt.music.MusicSigner
import org.ntqqrev.yogurt.music.MusicType

/**
 * 发送群聊音乐卡片 API
 * 
 * 端点: POST /send_group_music
 */
fun Route.sendGroupMusicRoute() {
    post("/send_group_music") {
        val bot = application.dependencies.resolve<Bot>()
        val input = call.receive<SendGroupMusicInput>()
        
        // 检查群聊是否存在
        bot.getGroup(input.groupId)
            ?: return@post call.respond(
                HttpStatusCode.OK,
                MusicErrorResponse(
                    retcode = -404,
                    message = "Group not found"
                )
            )
        
        try {
            // 验证输入
            val musicType = try {
                MusicType.fromString(input.musicType)
            } catch (e: IllegalArgumentException) {
                return@post call.respond(
                    HttpStatusCode.OK,
                    MusicErrorResponse(
                        retcode = -400,
                        message = "Invalid music type: ${input.musicType}"
                    )
                )
            }
            
            // 对于自定义音乐，验证必填字段
            if (musicType == MusicType.CUSTOM) {
                if (input.url.isNullOrBlank() || input.audio.isNullOrBlank() || input.title.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.OK,
                        MusicErrorResponse(
                            retcode = -400,
                            message = "Custom music requires url, audio, and title fields"
                        )
                    )
                }
            } else {
                // 对于平台音乐，验证 ID
                if (input.id.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.OK,
                        MusicErrorResponse(
                            retcode = -400,
                            message = "Platform music requires id field"
                        )
                    )
                }
            }
            
            // 签名音乐卡片
            val signer = MusicSigner()
            val signRequest = MusicSignRequest(
                type = input.musicType,
                id = input.id,
                url = input.url,
                audio = input.audio,
                title = input.title,
                image = input.image,
                singer = input.singer
            )
            
            val arkPayload = try {
                signer.sign(signRequest)
            } catch (e: MusicSignException) {
                return@post call.respond(
                    HttpStatusCode.OK,
                    MusicErrorResponse(
                        retcode = -500,
                        message = "Music sign failed: ${e.message}"
                    )
                )
            } finally {
                signer.close()
            }
            
            // 发送消息
            val result = bot.sendGroupMessage(input.groupId) {
                lightApp(arkPayload)
            }
            
            call.respond(
                HttpStatusCode.OK,
                MusicSuccessResponse(
                    data = SendMusicOutput(
                        messageSeq = result.sequence,
                        time = result.sendTime
                    )
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.OK,
                MusicErrorResponse(
                    retcode = -500,
                    message = "Internal error: ${e.message}"
                )
            )
        }
    }
}

/**
 * 发送私聊音乐卡片 API
 * 
 * 端点: POST /send_private_music
 */
fun Route.sendPrivateMusicRoute() {
    post("/send_private_music") {
        val bot = application.dependencies.resolve<Bot>()
        val input = call.receive<SendPrivateMusicInput>()
        
        // 检查好友是否存在
        bot.getFriend(input.userId)
            ?: return@post call.respond(
                HttpStatusCode.OK,
                MusicErrorResponse(
                    retcode = -404,
                    message = "Friend not found"
                )
            )
        
        try {
            // 验证输入
            val musicType = try {
                MusicType.fromString(input.musicType)
            } catch (e: IllegalArgumentException) {
                return@post call.respond(
                    HttpStatusCode.OK,
                    MusicErrorResponse(
                        retcode = -400,
                        message = "Invalid music type: ${input.musicType}"
                    )
                )
            }
            
            // 对于自定义音乐，验证必填字段
            if (musicType == MusicType.CUSTOM) {
                if (input.url.isNullOrBlank() || input.audio.isNullOrBlank() || input.title.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.OK,
                        MusicErrorResponse(
                            retcode = -400,
                            message = "Custom music requires url, audio, and title fields"
                        )
                    )
                }
            } else {
                // 对于平台音乐，验证 ID
                if (input.id.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.OK,
                        MusicErrorResponse(
                            retcode = -400,
                            message = "Platform music requires id field"
                        )
                    )
                }
            }
            
            // 签名音乐卡片
            val signer = MusicSigner()
            val signRequest = MusicSignRequest(
                type = input.musicType,
                id = input.id,
                url = input.url,
                audio = input.audio,
                title = input.title,
                image = input.image,
                singer = input.singer
            )
            
            val arkPayload = try {
                signer.sign(signRequest)
            } catch (e: MusicSignException) {
                return@post call.respond(
                    HttpStatusCode.OK,
                    MusicErrorResponse(
                        retcode = -500,
                        message = "Music sign failed: ${e.message}"
                    )
                )
            } finally {
                signer.close()
            }
            
            // 发送消息
            val result = bot.sendFriendMessage(input.userId) {
                lightApp(arkPayload)
            }
            
            call.respond(
                HttpStatusCode.OK,
                MusicSuccessResponse(
                    data = SendMusicOutput(
                        messageSeq = result.sequence,
                        time = result.sendTime
                    )
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.OK,
                MusicErrorResponse(
                    retcode = -500,
                    message = "Internal error: ${e.message}"
                )
            )
        }
    }
}

@Serializable
data class SendGroupMusicInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 音乐类型: qq, 163, kugou, migu, kuwo, custom */
    @SerialName("music_type") val musicType: String,
    /** 音乐 ID（平台音乐必填） */
    @SerialName("id") val id: String? = null,
    /** 跳转 URL（自定义音乐必填） */
    @SerialName("url") val url: String? = null,
    /** 音频 URL（自定义音乐必填） */
    @SerialName("audio") val audio: String? = null,
    /** 标题（自定义音乐必填） */
    @SerialName("title") val title: String? = null,
    /** 封面图片 URL（可选） */
    @SerialName("image") val image: String? = null,
    /** 歌手名称（可选） */
    @SerialName("singer") val singer: String? = null,
)

@Serializable
data class SendPrivateMusicInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 音乐类型: qq, 163, kugou, migu, kuwo, custom */
    @SerialName("music_type") val musicType: String,
    /** 音乐 ID（平台音乐必填） */
    @SerialName("id") val id: String? = null,
    /** 跳转 URL（自定义音乐必填） */
    @SerialName("url") val url: String? = null,
    /** 音频 URL（自定义音乐必填） */
    @SerialName("audio") val audio: String? = null,
    /** 标题（自定义音乐必填） */
    @SerialName("title") val title: String? = null,
    /** 封面图片 URL（可选） */
    @SerialName("image") val image: String? = null,
    /** 歌手名称（可选） */
    @SerialName("singer") val singer: String? = null,
)

@Serializable
data class SendMusicOutput(
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
    /** 消息发送时间 */
    @SerialName("time") val time: Long,
)

/** 错误响应（无 data 字段） */
@Serializable
data class MusicErrorResponse(
    val status: String = "failed",
    val retcode: Int,
    val message: String,
)

/** 成功响应（带 data 字段） */
@Serializable
data class MusicSuccessResponse(
    val status: String = "ok",
    val retcode: Int = 0,
    val data: SendMusicOutput,
)
