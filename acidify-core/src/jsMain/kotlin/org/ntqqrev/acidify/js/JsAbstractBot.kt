package org.ntqqrev.acidify.js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.common.UnsafeAcidifyApi
import org.ntqqrev.acidify.event.*
import org.ntqqrev.acidify.message.*
import org.ntqqrev.acidify.struct.*
import kotlin.js.Promise

@JsExport
@JsName("AbstractBot")
@AcidifyJsWrapper
abstract class JsAbstractBot internal constructor(
    protected open val bot: AbstractBot
) : CoroutineScope by bot {
    private val jobMap = mutableMapOf<dynamic, Job>()

    val uin: Long get() = bot.uin
    val uid: String get() = bot.uid
    val isLoggedIn: Boolean get() = bot.isLoggedIn

    private inline fun <reified T : AcidifyEvent> subscribeTracking(noinline callback: (T) -> Unit): ((T) -> Unit) {
        val job = launch {
            bot.eventFlow.filterIsInstance<T>().collect { callback(it) }
        }
        jobMap[callback] = job
        return callback
    }

    private fun tryUnsubscribe(callback: dynamic): Boolean {
        val job = jobMap[callback] ?: return false
        job.cancel()
        return true
    }

    @UnsafeAcidifyApi
    fun unsafeSendPacket(cmd: String, payload: ByteArray, timeoutMillis: Long = 10000L) = promise {
        bot.sendPacket(cmd, payload, timeoutMillis)
    }

    fun online(preloadContacts: Boolean = false) = promise { bot.online(preloadContacts) }

    fun offline() = promise { bot.offline() }

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

    fun getCustomFaceUrl(): Promise<Array<String>> = promise { bot.getCustomFaceUrl().toTypedArray() }

    fun getPins(): Promise<BotPinnedChats> = promise { bot.getPins() }

    fun setFriendPin(friendUin: Long, isPinned: Boolean) = promise {
        bot.setFriendPin(friendUin, isPinned)
    }

    fun setGroupPin(groupUin: Long, isPinned: Boolean) = promise {
        bot.setGroupPin(groupUin, isPinned)
    }

    fun setAvatar(imageData: ByteArray) = promise { bot.setAvatar(imageData) }

    fun setNickname(nickname: String) = promise { bot.setNickname(nickname) }

    fun setBio(bio: String) = promise { bot.setBio(bio) }

    fun getSKey(): Promise<String> = promise { bot.getSKey() }

    fun getPSKey(domain: String): Promise<String> = promise { bot.getPSKey(domain) }

    fun getCookies(domain: String): Promise<Map<String, String>> = promise { bot.getCookies(domain) }

    fun getCsrfToken(): Promise<Int> = promise { bot.getCsrfToken() }

    fun sendFriendMessage(
        friendUin: Long,
        build: (JsBotOutgoingMessageBuilder) -> Unit
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendFriendMessage(friendUin) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b)
        }
    }

    fun sendFriendMessageRich(
        friendUin: Long,
        clientSequence: Long,
        random: Int,
        build: (JsBotOutgoingMessageBuilder) -> Unit
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendFriendMessage(friendUin, clientSequence, random) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b)
        }
    }

    fun sendFriendMessageBySegments(
        friendUin: Long,
        clientSequence: Long,
        random: Int,
        segments: Array<BotOutgoingSegment>
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendFriendMessage(friendUin, clientSequence, random, segments.toList())
    }

    fun sendGroupMessage(
        groupUin: Long,
        build: (JsBotOutgoingMessageBuilder) -> Unit
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendGroupMessage(groupUin) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b)
        }
    }

    fun sendGroupMessageRich(
        groupUin: Long,
        clientSequence: Long,
        random: Int,
        build: (JsBotOutgoingMessageBuilder) -> Unit
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendGroupMessage(groupUin, clientSequence, random) {
            val b = JsBotOutgoingMessageBuilder(this)
            build(b)
        }
    }

    fun sendGroupMessageBySegments(
        groupUin: Long,
        clientSequence: Long,
        random: Int,
        segments: Array<BotOutgoingSegment>
    ): Promise<BotOutgoingMessageResult> = promise {
        bot.sendGroupMessage(groupUin, clientSequence, random, segments.toList())
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

    fun deleteFriend(friendUin: Long, block: Boolean = false) = promise {
        bot.deleteFriend(friendUin, block)
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
        imageData: ByteArray? = null,
        imageFormat: ImageFormat? = null,
        showEditCard: Boolean = false,
        showTipWindow: Boolean = true,
        confirmRequired: Boolean = true,
        isPinned: Boolean = false,
    ): Promise<String> = promise {
        bot.sendGroupAnnouncement(
            groupUin,
            content,
            imageData,
            imageFormat,
            showEditCard,
            showTipWindow,
            confirmRequired,
            isPinned
        )
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

    fun setGroupMessageReaction(
        groupUin: Long,
        sequence: Long,
        code: String,
        type: Int = 1,
        isAdd: Boolean = true
    ) = promise {
        bot.setGroupMessageReaction(groupUin, sequence, code, type, isAdd)
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

    fun persistGroupFile(groupUin: Long, fileId: String) = promise {
        bot.persistGroupFile(groupUin, fileId)
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

    fun onAndroidSessionStoreUpdated(callback: (AndroidSessionStoreUpdatedEvent) -> Unit) = subscribeTracking(callback)
    fun offAndroidSessionStoreUpdated(callback: (AndroidSessionStoreUpdatedEvent) -> Unit) = tryUnsubscribe(callback)

    fun onBotOffline(callback: (BotOfflineEvent) -> Unit) = subscribeTracking(callback)
    fun offBotOffline(callback: (BotOfflineEvent) -> Unit) = tryUnsubscribe(callback)

    fun onFriendFileUpload(callback: (FriendFileUploadEvent) -> Unit) = subscribeTracking(callback)
    fun offFriendFileUpload(callback: (FriendFileUploadEvent) -> Unit) = tryUnsubscribe(callback)

    fun onFriendNudge(callback: (FriendNudgeEvent) -> Unit) = subscribeTracking(callback)
    fun offFriendNudge(callback: (FriendNudgeEvent) -> Unit) = tryUnsubscribe(callback)

    fun onFriendRequest(callback: (FriendRequestEvent) -> Unit) = subscribeTracking(callback)
    fun offFriendRequest(callback: (FriendRequestEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupAdminChange(callback: (GroupAdminChangeEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupAdminChange(callback: (GroupAdminChangeEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupEssenceMessageChange(callback: (GroupEssenceMessageChangeEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupEssenceMessageChange(callback: (GroupEssenceMessageChangeEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupFileUpload(callback: (GroupFileUploadEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupFileUpload(callback: (GroupFileUploadEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupInvitation(callback: (GroupInvitationEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupInvitation(callback: (GroupInvitationEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupInvitedJoinRequest(callback: (GroupInvitedJoinRequestEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupInvitedJoinRequest(callback: (GroupInvitedJoinRequestEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupJoinRequest(callback: (GroupJoinRequestEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupJoinRequest(callback: (GroupJoinRequestEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupMemberDecrease(callback: (GroupMemberDecreaseEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupMemberDecrease(callback: (GroupMemberDecreaseEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupMemberIncrease(callback: (GroupMemberIncreaseEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupMemberIncrease(callback: (GroupMemberIncreaseEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupMessageReaction(callback: (GroupMessageReactionEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupMessageReaction(callback: (GroupMessageReactionEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupMute(callback: (GroupMuteEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupMute(callback: (GroupMuteEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupNameChange(callback: (GroupNameChangeEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupNameChange(callback: (GroupNameChangeEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupNudge(callback: (GroupNudgeEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupNudge(callback: (GroupNudgeEvent) -> Unit) = tryUnsubscribe(callback)

    fun onGroupWholeMute(callback: (GroupWholeMuteEvent) -> Unit) = subscribeTracking(callback)
    fun offGroupWholeMute(callback: (GroupWholeMuteEvent) -> Unit) = tryUnsubscribe(callback)

    fun onPinChanged(callback: (PinChangedEvent) -> Unit) = subscribeTracking(callback)
    fun offPinChanged(callback: (PinChangedEvent) -> Unit) = tryUnsubscribe(callback)

    fun onMessageReceive(callback: (MessageReceiveEvent) -> Unit) = subscribeTracking(callback)
    fun offMessageReceive(callback: (MessageReceiveEvent) -> Unit) = tryUnsubscribe(callback)

    fun onMessageRecall(callback: (MessageRecallEvent) -> Unit) = subscribeTracking(callback)
    fun offMessageRecall(callback: (MessageRecallEvent) -> Unit) = tryUnsubscribe(callback)

    fun onQRCodeGenerated(callback: (QRCodeGeneratedEvent) -> Unit) = subscribeTracking(callback)
    fun offQRCodeGenerated(callback: (QRCodeGeneratedEvent) -> Unit) = tryUnsubscribe(callback)

    fun onQRCodeStateQuery(callback: (QRCodeStateQueryEvent) -> Unit) = subscribeTracking(callback)
    fun offQRCodeStateQuery(callback: (QRCodeStateQueryEvent) -> Unit) = tryUnsubscribe(callback)

    fun onSessionStoreUpdated(callback: (SessionStoreUpdatedEvent) -> Unit) = subscribeTracking(callback)
    fun offSessionStoreUpdated(callback: (SessionStoreUpdatedEvent) -> Unit) = tryUnsubscribe(callback)
}
