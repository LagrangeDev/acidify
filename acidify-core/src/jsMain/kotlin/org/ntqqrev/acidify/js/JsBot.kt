package org.ntqqrev.acidify.js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.event.*
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.message.BotForwardedMessage
import org.ntqqrev.acidify.message.BotHistoryMessages
import org.ntqqrev.acidify.message.BotOutgoingMessageResult
import org.ntqqrev.acidify.struct.*
import kotlin.js.Promise

@JsExport
@JsName("Bot")
class JsBot internal constructor(private val bot: Bot) : CoroutineScope by bot {
    private val jobMap = mutableMapOf<dynamic, Job>()

    val uin: Long get() = bot.uin
    val uid: String get() = bot.uid
    val isLoggedIn: Boolean get() = bot.isLoggedIn

    private inline fun <reified T : AcidifyEvent> subscribeTracking(noinline callback: (T) -> Promise<Unit>) {
        val job = launch {
            bot.eventFlow.filterIsInstance<T>().collect { callback(it) }
        }
        jobMap[callback] = job
    }

    private fun tryUnsubscribe(callback: dynamic): Boolean {
        val job = jobMap[callback] ?: return false
        job.cancel()
        return true
    }

    fun qrCodeLogin(queryInterval: Long = 3000L) = promise {
        bot.qrCodeLogin(queryInterval)
    }

    fun online() = promise { bot.online() }

    fun offline() = promise { bot.offline() }

    fun tryLogin() = promise { bot.tryLogin() }

    fun fetchUserInfoByUin(uin: Long): Promise<BotUserInfo> = promise { bot.fetchUserInfoByUin(uin) }

    fun fetchUserInfoByUid(uid: String): Promise<BotUserInfo> = promise { bot.fetchUserInfoByUid(uid) }

    fun fetchFriends(): Promise<Array<BotFriendData>> = promise { bot.fetchFriends().toTypedArray() }

    fun fetchGroups(): Promise<Array<BotGroupData>> = promise { bot.fetchGroups().toTypedArray() }

    fun fetchGroupMembers(groupUin: Long): Promise<Array<BotGroupMemberData>> = promise {
        bot.fetchGroupMembers(groupUin).toTypedArray()
    }

    fun getFriends(forceUpdate: Boolean = false): Promise<Array<org.ntqqrev.acidify.entity.BotFriend>> = promise {
        bot.getFriends(forceUpdate).toTypedArray()
    }

    fun getFriend(uin: Long, forceUpdate: Boolean = false): Promise<org.ntqqrev.acidify.entity.BotFriend?> = promise {
        bot.getFriend(uin, forceUpdate)
    }

    fun getGroups(forceUpdate: Boolean = false): Promise<Array<org.ntqqrev.acidify.entity.BotGroup>> = promise {
        bot.getGroups(forceUpdate).toTypedArray()
    }

    fun getGroup(uin: Long, forceUpdate: Boolean = false): Promise<org.ntqqrev.acidify.entity.BotGroup?> = promise {
        bot.getGroup(uin, forceUpdate)
    }

    fun getGroupMembers(
        groupUin: Long,
        forceUpdate: Boolean = false
    ): Promise<Array<org.ntqqrev.acidify.entity.BotGroupMember>?> = promise {
        bot.getGroupMembers(groupUin, forceUpdate)?.toTypedArray()
    }

    fun getGroupMember(
        groupUin: Long,
        memberUin: Long,
        forceUpdate: Boolean = false
    ): Promise<org.ntqqrev.acidify.entity.BotGroupMember?> = promise {
        bot.getGroupMember(groupUin, memberUin, forceUpdate)
    }

    fun getUinByUid(uid: String): Promise<Long> = promise { bot.getUinByUid(uid) }

    fun getUidByUin(uin: Long, mayComeFromGroupUin: Long? = null): Promise<String> = promise {
        bot.getUidByUin(uin, mayComeFromGroupUin)
    }

    fun getSKey(): Promise<String> = promise { bot.getSKey() }

    fun getPSKey(domain: String): Promise<String> = promise { bot.getPSKey(domain) }

    fun getCookies(domain: String): Promise<Map<String, String>> = promise { bot.getCookies(domain) }

    fun getCsrfToken(): Promise<Int> = promise { bot.getCsrfToken() }

    fun sendFriendMessage(
        friendUin: Long,
        build: (JsBotOutgoingMessageBuilder) -> Promise<Unit>
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendFriendMessage(friendUin) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b).await()
        }
    }

    fun sendFriendMessageRich(
        friendUin: Long,
        clientSequence: Long,
        random: Int,
        build: (JsBotOutgoingMessageBuilder) -> Promise<Unit>
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendFriendMessage(friendUin, clientSequence, random) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b).await()
        }
    }

    fun sendGroupMessage(
        groupUin: Long,
        build: (JsBotOutgoingMessageBuilder) -> Promise<Unit>
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendGroupMessage(groupUin) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b).await()
        }
    }

    fun sendGroupMessageRich(
        groupUin: Long,
        clientSequence: Long,
        random: Int,
        build: (JsBotOutgoingMessageBuilder) -> Promise<Unit>
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendGroupMessage(groupUin, clientSequence, random) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b).await()
        }
    }

    fun recallFriendMessage(friendUin: Long, sequence: Long) = promise {
        bot.recallFriendMessage(friendUin, sequence)
    }

    fun recallGroupMessage(groupUin: Long, sequence: Long) = promise {
        bot.recallGroupMessage(groupUin, sequence)
    }

    fun getFriendHistoryMessages(
        friendUin: Long,
        limit: Int,
        startSequence: Long? = null
    ): Promise<BotHistoryMessages> = promise {
        bot.getFriendHistoryMessages(friendUin, limit, startSequence)
    }

    fun getGroupHistoryMessages(
        groupUin: Long,
        limit: Int,
        startSequence: Long? = null
    ): Promise<BotHistoryMessages> = promise {
        bot.getGroupHistoryMessages(groupUin, limit, startSequence)
    }

    fun getDownloadUrl(resourceId: String): Promise<String> = promise {
        bot.getDownloadUrl(resourceId)
    }

    fun getForwardedMessages(resId: String): Promise<Array<BotForwardedMessage>> = promise {
        bot.getForwardedMessages(resId).toTypedArray()
    }

    fun markFriendMessagesAsRead(friendUin: Long, startSequence: Long, startTime: Long) = promise {
        bot.markFriendMessagesAsRead(friendUin, startSequence, startTime)
    }

    fun markGroupMessagesAsRead(groupUin: Long, startSequence: Long) = promise {
        bot.markGroupMessagesAsRead(groupUin, startSequence)
    }

    fun sendFriendNudge(friendUin: Long, isSelf: Boolean = false) = promise {
        bot.sendFriendNudge(friendUin, isSelf)
    }

    fun sendProfileLike(friendUin: Long, count: Int = 1) = promise {
        bot.sendProfileLike(friendUin, count)
    }

    fun getFriendRequests(isFiltered: Boolean = false, limit: Int = 20): Promise<Array<BotFriendRequest>> = promise {
        bot.getFriendRequests(isFiltered, limit).toTypedArray()
    }

    fun setFriendRequest(initiatorUid: String, accept: Boolean, isFiltered: Boolean = false) = promise {
        bot.setFriendRequest(initiatorUid, accept, isFiltered)
    }

    fun setGroupName(groupUin: Long, groupName: String) = promise {
        bot.setGroupName(groupUin, groupName)
    }

    fun setGroupAvatar(groupUin: Long, imageData: ByteArray) = promise {
        bot.setGroupAvatar(groupUin, imageData)
    }

    fun setGroupMemberCard(groupUin: Long, memberUin: Long, card: String) = promise {
        bot.setGroupMemberCard(groupUin, memberUin, card)
    }

    fun setGroupMemberSpecialTitle(groupUin: Long, memberUin: Long, specialTitle: String) = promise {
        bot.setGroupMemberSpecialTitle(groupUin, memberUin, specialTitle)
    }

    fun setGroupMemberAdmin(groupUin: Long, memberUin: Long, isAdmin: Boolean) = promise {
        bot.setGroupMemberAdmin(groupUin, memberUin, isAdmin)
    }

    fun setGroupMemberMute(groupUin: Long, memberUin: Long, duration: Int) = promise {
        bot.setGroupMemberMute(groupUin, memberUin, duration)
    }

    fun setGroupWholeMute(groupUin: Long, isMute: Boolean) = promise {
        bot.setGroupWholeMute(groupUin, isMute)
    }

    fun kickGroupMember(groupUin: Long, memberUin: Long, rejectAddRequest: Boolean = false, reason: String = "") =
        promise {
            bot.kickGroupMember(groupUin, memberUin, rejectAddRequest, reason)
        }

    fun getGroupAnnouncements(groupUin: Long): Promise<Array<BotGroupAnnouncement>> = promise {
        bot.getGroupAnnouncements(groupUin).toTypedArray()
    }

    fun sendGroupAnnouncement(
        groupUin: Long,
        content: String,
        imageUrl: String? = null,
        showEditCard: Boolean = false,
        showTipWindow: Boolean = true,
        confirmRequired: Boolean = true
    ): Promise<String> = promise {
        bot.sendGroupAnnouncement(groupUin, content, imageUrl, showEditCard, showTipWindow, confirmRequired)
    }

    fun deleteGroupAnnouncement(groupUin: Long, announcementId: String) = promise {
        bot.deleteGroupAnnouncement(groupUin, announcementId)
    }

    fun getGroupEssenceMessages(
        groupUin: Long,
        pageIndex: Int,
        pageSize: Int
    ): Promise<org.ntqqrev.acidify.message.BotEssenceMessageResult> = promise {
        bot.getGroupEssenceMessages(groupUin, pageIndex, pageSize)
    }

    fun setGroupEssenceMessage(groupUin: Long, sequence: Long, isSet: Boolean) = promise {
        bot.setGroupEssenceMessage(groupUin, sequence, isSet)
    }

    fun quitGroup(groupUin: Long) = promise { bot.quitGroup(groupUin) }

    fun setGroupMessageReaction(groupUin: Long, sequence: Long, code: String, isAdd: Boolean = true) = promise {
        bot.setGroupMessageReaction(groupUin, sequence, code, isAdd)
    }

    fun sendGroupNudge(groupUin: Long, targetUin: Long) = promise {
        bot.sendGroupNudge(groupUin, targetUin)
    }

    fun getGroupNotifications(
        startSequence: Long? = null,
        isFiltered: Boolean = false,
        count: Int = 20
    ): Promise<JsGroupNotifications> = promise {
        val (list, next) = bot.getGroupNotifications(startSequence, isFiltered, count)
        JsGroupNotifications(list.toTypedArray(), next)
    }

    fun setGroupRequest(
        groupUin: Long,
        sequence: Long,
        eventType: Int,
        accept: Boolean,
        isFiltered: Boolean = false,
        reason: String = ""
    ) = promise {
        bot.setGroupRequest(groupUin, sequence, eventType, accept, isFiltered, reason)
    }

    fun setGroupInvitation(groupUin: Long, invitationSeq: Long, accept: Boolean) = promise {
        bot.setGroupInvitation(groupUin, invitationSeq, accept)
    }

    fun uploadGroupFile(
        groupUin: Long,
        fileName: String,
        fileData: ByteArray,
        parentFolderId: String = "/"
    ): Promise<String> = promise {
        bot.uploadGroupFile(groupUin, fileName, fileData, parentFolderId)
    }

    fun uploadPrivateFile(
        friendUin: Long,
        fileName: String,
        fileData: ByteArray
    ): Promise<String> = promise {
        bot.uploadPrivateFile(friendUin, fileName, fileData)
    }

    fun getPrivateFileDownloadUrl(friendUin: Long, fileId: String, fileHash: String): Promise<String> = promise {
        bot.getPrivateFileDownloadUrl(friendUin, fileId, fileHash)
    }

    fun getGroupFileDownloadUrl(groupUin: Long, fileId: String): Promise<String> = promise {
        bot.getGroupFileDownloadUrl(groupUin, fileId)
    }

    fun getGroupFileList(
        groupUin: Long,
        targetDirectory: String = "/",
        startIndex: Int = 0
    ): Promise<BotGroupFileSystemList> = promise {
        bot.getGroupFileList(groupUin, targetDirectory, startIndex)
    }

    fun renameGroupFile(groupUin: Long, fileId: String, parentFolderId: String, newFileName: String) = promise {
        bot.renameGroupFile(groupUin, fileId, parentFolderId, newFileName)
    }

    fun moveGroupFile(groupUin: Long, fileId: String, parentFolderId: String, targetFolderId: String) = promise {
        bot.moveGroupFile(groupUin, fileId, parentFolderId, targetFolderId)
    }

    fun deleteGroupFile(groupUin: Long, fileId: String) = promise {
        bot.deleteGroupFile(groupUin, fileId)
    }

    fun createGroupFolder(groupUin: Long, folderName: String): Promise<String> = promise {
        bot.createGroupFolder(groupUin, folderName)
    }

    fun renameGroupFolder(groupUin: Long, folderId: String, newFolderName: String) = promise {
        bot.renameGroupFolder(groupUin, folderId, newFolderName)
    }

    fun deleteGroupFolder(groupUin: Long, folderId: String) = promise {
        bot.deleteGroupFolder(groupUin, folderId)
    }

    fun onBotOffline(callback: (BotOfflineEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offBotOffline(callback: (BotOfflineEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onFriendFileUpload(callback: (FriendFileUploadEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offFriendFileUpload(callback: (FriendFileUploadEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onFriendNudge(callback: (FriendNudgeEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offFriendNudge(callback: (FriendNudgeEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onFriendRequest(callback: (FriendRequestEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offFriendRequest(callback: (FriendRequestEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupAdminChange(callback: (GroupAdminChangeEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupAdminChange(callback: (GroupAdminChangeEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupEssenceMessageChange(callback: (GroupEssenceMessageChangeEvent) -> Promise<Unit>) =
        subscribeTracking(callback)

    fun offGroupEssenceMessageChange(callback: (GroupEssenceMessageChangeEvent) -> Promise<Unit>) =
        tryUnsubscribe(callback)

    fun onGroupFileUpload(callback: (GroupFileUploadEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupFileUpload(callback: (GroupFileUploadEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupInvitation(callback: (GroupInvitationEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupInvitation(callback: (GroupInvitationEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupInvitedJoinRequest(callback: (GroupInvitedJoinRequestEvent) -> Promise<Unit>) =
        subscribeTracking(callback)

    fun offGroupInvitedJoinRequest(callback: (GroupInvitedJoinRequestEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupJoinRequest(callback: (GroupJoinRequestEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupJoinRequest(callback: (GroupJoinRequestEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupMemberDecrease(callback: (GroupMemberDecreaseEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupMemberDecrease(callback: (GroupMemberDecreaseEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupMemberIncrease(callback: (GroupMemberIncreaseEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupMemberIncrease(callback: (GroupMemberIncreaseEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupMessageReaction(callback: (GroupMessageReactionEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupMessageReaction(callback: (GroupMessageReactionEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupMute(callback: (GroupMuteEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupMute(callback: (GroupMuteEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupNameChange(callback: (GroupNameChangeEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupNameChange(callback: (GroupNameChangeEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupNudge(callback: (GroupNudgeEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupNudge(callback: (GroupNudgeEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onGroupWholeMute(callback: (GroupWholeMuteEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offGroupWholeMute(callback: (GroupWholeMuteEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onMessageReceive(callback: (MessageReceiveEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offMessageReceive(callback: (MessageReceiveEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onMessageRecall(callback: (MessageRecallEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offMessageRecall(callback: (MessageRecallEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onQRCodeGenerated(callback: (QRCodeGeneratedEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offQRCodeGenerated(callback: (QRCodeGeneratedEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onQRCodeStateQuery(callback: (QRCodeStateQueryEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offQRCodeStateQuery(callback: (QRCodeStateQueryEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    fun onSessionStoreUpdated(callback: (SessionStoreUpdatedEvent) -> Promise<Unit>) = subscribeTracking(callback)

    fun offSessionStoreUpdated(callback: (SessionStoreUpdatedEvent) -> Promise<Unit>) = tryUnsubscribe(callback)

    companion object {
        @JsStatic
        fun create(
            appInfo: AppInfo,
            sessionStore: SessionStore,
            signProvider: JsSignProvider,
            jsScope: JsCoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler
        ): Promise<JsBot> = jsScope.value.promise {
            JsBot(
                Bot.create(
                    appInfo = appInfo,
                    sessionStore = sessionStore,
                    signProvider = { cmd, src, seq ->
                        signProvider.sign(cmd, src, seq).await()
                    },
                    scope = jsScope.value,
                    minLogLevel = minLogLevel,
                    logHandler = logHandler
                )
            )
        }
    }
}
