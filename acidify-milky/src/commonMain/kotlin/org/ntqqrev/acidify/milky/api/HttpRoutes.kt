package org.ntqqrev.acidify.milky.api

import io.ktor.server.plugins.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.exception.OidbException
import org.ntqqrev.acidify.exception.ServiceException
import org.ntqqrev.acidify.milky.MediaSourceScope
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.acidify.milky.api.handler.*
import org.ntqqrev.acidify.milky.mediaSourceScoped
import org.ntqqrev.milky.ApiEndpoint
import org.ntqqrev.milky.ApiGeneralResponse
import org.ntqqrev.milky.milkyJsonModule
import kotlin.time.DurationUnit
import kotlin.time.measureTime

inline fun <reified T : Any, reified R : Any> ApiEndpoint<T, R>.define(
    noinline handler: suspend context(MediaSourceScope) MilkyContext.(T) -> R
) = MilkyApiHandler(
    path = this.path,
    transformInput = milkyJsonModule::decodeFromJsonElement,
    transformOutput = milkyJsonModule::encodeToJsonElement,
    callHandler = handler,
)

context(ctx: MilkyContext)
private fun <T : Any, R : Any> Route.serve(handler: MilkyApiHandler<T, R>) = post(handler.path) {
    val bot = application.dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("HttpModule")
    try {
        val rawPayload = call.receive<JsonElement>()
        call.respond(
            try {
                var result: JsonElement
                val duration = measureTime {
                    result = mediaSourceScoped(
                        onDisposeFailure = { source, exception ->
                            logger.e(exception) {
                                "释放资源文件 $source 时出现错误"
                            }
                        }
                    ) {
                        handler.handleRaw(rawPayload)
                    }
                }
                logger.i {
                    "${call.request.local.remoteAddress} 调用 API ${handler.path}（成功 ${
                        duration.toString(
                            DurationUnit.MILLISECONDS
                        )
                    }）"
                }
                ApiGeneralResponse(
                    status = "ok",
                    retcode = 0,
                    data = result,
                )
            } catch (e: MilkyApiException) {
                logger.w { "${call.request.local.remoteAddress} 调用 API ${handler.path}（${e.retcode} ${e.message}）" }
                ApiGeneralResponse(
                    status = "failed",
                    retcode = e.retcode,
                    message = e.message
                )
            } catch (e: Exception) {
                logger.e(e) { "${call.request.local.remoteAddress} 调用 API ${handler.path}（失败 ${e::class.simpleName}）" }
                when (e) {
                    is OidbException -> ApiGeneralResponse(
                        status = "failed",
                        retcode = e.oidbResult,
                        message = e.message
                    )

                    is ServiceException -> ApiGeneralResponse(
                        status = "failed",
                        retcode = e.retCode,
                        message = e.message
                    )

                    else -> ApiGeneralResponse(
                        status = "failed",
                        retcode = -500,
                        message = "Internal error: ${e.message}"
                    )
                }
            }
        )
    } catch (e: BadRequestException) {
        logger.w { "${call.request.local.remoteAddress} 调用 API ${handler.path}（Bad Request）" }
        call.respond(
            ApiGeneralResponse(
                status = "failed",
                retcode = -400,
                message = "Bad request: ${e.message}" +
                        (e.cause?.let { " (Caused by ${it::class.simpleName}: ${it.message})" } ?: "")
            )
        )
    } catch (e: Exception) {
        logger.e(e) { "${call.request.local.remoteAddress} 调用 API ${handler.path}（失败 ${e::class.simpleName}）" }
        call.respond(
            ApiGeneralResponse(
                status = "failed",
                retcode = -500,
                message = "Internal error: ${e.message}"
            )
        )
    }
}

val apiHandlers = listOf(
    GetLoginInfo,
    GetImplInfo,
    GetUserProfile,
    GetFriendList,
    GetFriendInfo,
    GetGroupList,
    GetGroupInfo,
    GetGroupMemberList,
    GetGroupMemberInfo,
    GetPeerPins,
    SetPeerPin,
    SetAvatar,
    SetNickname,
    SetBio,
    GetCustomFaceUrlList,
    GetCookies,
    GetCsrfToken,

    SendPrivateMessage,
    SendGroupMessage,
    RecallPrivateMessage,
    RecallGroupMessage,
    GetMessage,
    GetHistoryMessages,
    GetResourceTempUrl,
    GetForwardedMessages,
    MarkMessageAsRead,

    SendFriendNudge,
    SendProfileLike,
    DeleteFriend,
    GetFriendRequests,
    AcceptFriendRequest,
    RejectFriendRequest,

    SetGroupName,
    SetGroupAvatar,
    SetGroupMemberCard,
    SetGroupMemberSpecialTitle,
    SetGroupMemberAdmin,
    SetGroupMemberMute,
    SetGroupWholeMute,
    KickGroupMember,
    GetGroupAnnouncements,
    SendGroupAnnouncement,
    DeleteGroupAnnouncement,
    GetGroupEssenceMessages,
    SetGroupEssenceMessage,
    QuitGroup,
    SendGroupMessageReaction,
    SendGroupNudge,
    GetGroupNotifications,
    AcceptGroupRequest,
    RejectGroupRequest,
    AcceptGroupInvitation,
    RejectGroupInvitation,

    UploadPrivateFile,
    UploadGroupFile,
    GetPrivateFileDownloadUrl,
    GetGroupFileDownloadUrl,
    GetGroupFiles,
    MoveGroupFile,
    RenameGroupFile,
    DeleteGroupFile,
    PersistGroupFile,
    CreateGroupFolder,
    RenameGroupFolder,
    DeleteGroupFolder,
)

context(ctx: MilkyContext)
fun Route.apiRoutes() {
    apiHandlers.forEach {
        serve(it)
    }
}