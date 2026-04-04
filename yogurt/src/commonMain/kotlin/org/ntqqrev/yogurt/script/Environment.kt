package org.ntqqrev.yogurt.script

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.ObjectBindingScope
import com.dokar.quickjs.binding.define
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.acidify.milky.api.MilkyApiHandler
import org.ntqqrev.acidify.milky.api.handler.*
import org.ntqqrev.acidify.milky.mediaSourceScoped
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.yogurt.script.stdlib.defineConsole
import org.ntqqrev.yogurt.script.stdlib.defineHttp
import kotlin.time.DurationUnit
import kotlin.time.measureTime

context(ctx: MilkyContext)
suspend fun Application.createScriptEnvironment() =
    QuickJs.create(jobDispatcher = Dispatchers.Default).apply {
    defineConsole()
    defineHttp()

    evaluate<Any?>(
        """
            const $rootHandle = {
                $apiHandle: {},
                $eventHandle: {},
            };
        """.trimIndent()
    )

    define(internalApiHandle) {
        context(this@apply, this) {
            defineJsApi(GetLoginInfo)
            defineJsApi(GetImplInfo)
            defineJsApi(GetUserProfile)
            defineJsApi(GetFriendList)
            defineJsApi(GetFriendInfo)
            defineJsApi(GetGroupList)
            defineJsApi(GetGroupInfo)
            defineJsApi(GetGroupMemberList)
            defineJsApi(GetGroupMemberInfo)
            defineJsApi(GetPeerPins)
            defineJsApi(SetPeerPin)
            defineJsApi(SetAvatar)
            defineJsApi(SetNickname)
            defineJsApi(SetBio)
            defineJsApi(GetCustomFaceUrlList)
            defineJsApi(GetCookies)
            defineJsApi(GetCsrfToken)

            defineJsApi(SendPrivateMessage)
            defineJsApi(SendGroupMessage)
            defineJsApi(RecallPrivateMessage)
            defineJsApi(RecallGroupMessage)
            defineJsApi(GetMessage)
            defineJsApi(GetHistoryMessages)
            defineJsApi(GetResourceTempUrl)
            defineJsApi(GetForwardedMessages)
            defineJsApi(MarkMessageAsRead)

            defineJsApi(SendFriendNudge)
            defineJsApi(SendProfileLike)
            defineJsApi(DeleteFriend)
            defineJsApi(GetFriendRequests)
            defineJsApi(AcceptFriendRequest)
            defineJsApi(RejectFriendRequest)

            defineJsApi(SetGroupName)
            defineJsApi(SetGroupAvatar)
            defineJsApi(SetGroupMemberCard)
            defineJsApi(SetGroupMemberSpecialTitle)
            defineJsApi(SetGroupMemberAdmin)
            defineJsApi(SetGroupMemberMute)
            defineJsApi(SetGroupWholeMute)
            defineJsApi(KickGroupMember)
            defineJsApi(GetGroupAnnouncements)
            defineJsApi(SendGroupAnnouncement)
            defineJsApi(DeleteGroupAnnouncement)
            defineJsApi(GetGroupEssenceMessages)
            defineJsApi(SetGroupEssenceMessage)
            defineJsApi(QuitGroup)
            defineJsApi(SendGroupMessageReaction)
            defineJsApi(SendGroupNudge)
            defineJsApi(GetGroupNotifications)
            defineJsApi(AcceptGroupRequest)
            defineJsApi(RejectGroupRequest)
            defineJsApi(AcceptGroupInvitation)
            defineJsApi(RejectGroupInvitation)

            defineJsApi(UploadPrivateFile)
            defineJsApi(UploadGroupFile)
            defineJsApi(GetPrivateFileDownloadUrl)
            defineJsApi(GetGroupFileDownloadUrl)
            defineJsApi(GetGroupFiles)
            defineJsApi(MoveGroupFile)
            defineJsApi(RenameGroupFile)
            defineJsApi(DeleteGroupFile)
            defineJsApi(CreateGroupFolder)
            defineJsApi(RenameGroupFolder)
            defineJsApi(DeleteGroupFolder)
        }
    }

    evaluate<Any?>(
        $$"""
            const $$internalEventMapHandle = new Map();
            
            $$rootHandle.$$eventHandle.on = (eventName, listener) => {
                if (!$$internalEventMapHandle.has(eventName)) {
                    $$internalEventMapHandle.set(eventName, []);
                }
                $$internalEventMapHandle.get(eventName).push(listener);
            };
            
            function $$internalEmitHandle(event) {
                const eventName = event.event_type;
                if ($$internalEventMapHandle.has(eventName)) {
                    Promise.all($$internalEventMapHandle.get(eventName).map(async (listener) => {
                        try {
                            await listener(event);
                        } catch (error) {
                            console.error(`Error in event listener for ${eventName}:`, error);
                        }
                    }));
                }
            }
        """.trimIndent()
    )
}

@OptIn(ExperimentalSerializationApi::class)
context(
    qjs: QuickJs,
    scope: ObjectBindingScope,
    ctx: MilkyContext,
)
inline fun <reified T : Any, reified R : Any> Application.defineJsApi(
    handler: MilkyApiHandler<T, R>
) {
    val methodName = handler.path.removePrefix("/")

    runBlocking {
        qjs.evaluate<Any?>(
            """
                $rootHandle.$apiHandle.$methodName = async (payload) => {
                    const payloadStr = payload ? JSON.stringify(payload) : '{}';
                    const respStr = await $internalApiHandle.$methodName(payloadStr);
                    return JSON.parse(respStr);
                };
            """.trimIndent()
        )
    }

    scope.asyncFunction(methodName) { args ->
        require(args.size == 1)
        val payloadString = args[0] as? String
            ?: throw IllegalArgumentException("Expected argument to be a JSON string")
        val bot = dependencies.resolve<AbstractBot>()
        val logger = bot.createLogger("Scripting")
        var resp: R
        try {
            val duration = measureTime {
                resp = mediaSourceScoped(
                    onDisposeFailure = { source, exception ->
                        logger.e(exception) {
                            "释放资源文件 $source 时出现错误"
                        }
                    }
                ) {
                    handler.callHandler(ctx, milkyJsonModule.decodeFromString(payloadString))
                }
            }
            logger.i {
                "脚本调用 API ${handler.path}（成功 ${duration.toString(DurationUnit.MILLISECONDS)}）"
            }
            milkyJsonModule.encodeToString(resp)
        } catch (e: Exception) {
            logger.e(e) { "脚本调用 API ${handler.path}（失败 ${e::class.simpleName}）" }
            throw e
        }
    }
}