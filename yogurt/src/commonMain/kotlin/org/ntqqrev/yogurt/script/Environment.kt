package org.ntqqrev.yogurt.script

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.ntqqrev.yogurt.api.handler.*
import org.ntqqrev.yogurt.script.stdlib.defineConsole
import org.ntqqrev.yogurt.script.stdlib.defineHttp
import org.ntqqrev.yogurt.util.defineJsApi

suspend fun Application.createScriptEnvironment() = QuickJs.create(jobDispatcher = Dispatchers.Default).apply {
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
        defineJsApi(this@apply, this, GetLoginInfo)
        defineJsApi(this@apply, this, GetImplInfo)
        defineJsApi(this@apply, this, GetUserProfile)
        defineJsApi(this@apply, this, GetFriendList)
        defineJsApi(this@apply, this, GetFriendInfo)
        defineJsApi(this@apply, this, GetGroupList)
        defineJsApi(this@apply, this, GetGroupInfo)
        defineJsApi(this@apply, this, GetGroupMemberList)
        defineJsApi(this@apply, this, GetGroupMemberInfo)
        defineJsApi(this@apply, this, SetAvatar)
        defineJsApi(this@apply, this, SetNickname)
        defineJsApi(this@apply, this, SetBio)
        defineJsApi(this@apply, this, GetCustomFaceUrlList)
        defineJsApi(this@apply, this, GetCookies)
        defineJsApi(this@apply, this, GetCsrfToken)

        defineJsApi(this@apply, this, SendPrivateMessage)
        defineJsApi(this@apply, this, SendGroupMessage)
        defineJsApi(this@apply, this, RecallPrivateMessage)
        defineJsApi(this@apply, this, RecallGroupMessage)
        defineJsApi(this@apply, this, GetMessage)
        defineJsApi(this@apply, this, GetHistoryMessages)
        defineJsApi(this@apply, this, GetResourceTempUrl)
        defineJsApi(this@apply, this, GetForwardedMessages)
        defineJsApi(this@apply, this, MarkMessageAsRead)

        defineJsApi(this@apply, this, SendFriendNudge)
        defineJsApi(this@apply, this, SendProfileLike)
        defineJsApi(this@apply, this, DeleteFriend)
        defineJsApi(this@apply, this, GetFriendRequests)
        defineJsApi(this@apply, this, AcceptFriendRequest)
        defineJsApi(this@apply, this, RejectFriendRequest)

        defineJsApi(this@apply, this, SetGroupName)
        defineJsApi(this@apply, this, SetGroupAvatar)
        defineJsApi(this@apply, this, SetGroupMemberCard)
        defineJsApi(this@apply, this, SetGroupMemberSpecialTitle)
        defineJsApi(this@apply, this, SetGroupMemberAdmin)
        defineJsApi(this@apply, this, SetGroupMemberMute)
        defineJsApi(this@apply, this, SetGroupWholeMute)
        defineJsApi(this@apply, this, KickGroupMember)
        defineJsApi(this@apply, this, GetGroupAnnouncements)
        defineJsApi(this@apply, this, SendGroupAnnouncement)
        defineJsApi(this@apply, this, DeleteGroupAnnouncement)
        defineJsApi(this@apply, this, GetGroupEssenceMessages)
        defineJsApi(this@apply, this, SetGroupEssenceMessage)
        defineJsApi(this@apply, this, QuitGroup)
        defineJsApi(this@apply, this, SendGroupMessageReaction)
        defineJsApi(this@apply, this, SendGroupNudge)
        defineJsApi(this@apply, this, GetGroupNotifications)
        defineJsApi(this@apply, this, AcceptGroupRequest)
        defineJsApi(this@apply, this, RejectGroupRequest)
        defineJsApi(this@apply, this, AcceptGroupInvitation)
        defineJsApi(this@apply, this, RejectGroupInvitation)

        defineJsApi(this@apply, this, UploadPrivateFile)
        defineJsApi(this@apply, this, UploadGroupFile)
        defineJsApi(this@apply, this, GetPrivateFileDownloadUrl)
        defineJsApi(this@apply, this, GetGroupFileDownloadUrl)
        defineJsApi(this@apply, this, GetGroupFiles)
        defineJsApi(this@apply, this, MoveGroupFile)
        defineJsApi(this@apply, this, RenameGroupFile)
        defineJsApi(this@apply, this, DeleteGroupFile)
        defineJsApi(this@apply, this, CreateGroupFolder)
        defineJsApi(this@apply, this, RenameGroupFolder)
        defineJsApi(this@apply, this, DeleteGroupFolder)
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
                    for (const listener of $$internalEventMapHandle.get(eventName)) {
                        try {
                            listener(event);
                        } catch (error) {
                            console.error(`Error in event listener for ${eventName}:`, error);
                        }
                    }
                }
            }
        """.trimIndent()
    )
}