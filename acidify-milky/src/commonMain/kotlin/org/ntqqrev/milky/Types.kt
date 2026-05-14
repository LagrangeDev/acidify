// Generated from Milky 1.3 (1.3.0-rc.1)
@file:OptIn(ExperimentalSerializationApi::class)

package org.ntqqrev.milky

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

const val milkyVersion = "1.3"
const val milkyPackageVersion = "1.3.0-rc.1"

@Target(AnnotationTarget.PROPERTY)
annotation class LiteralDefault(val value: String)

val milkyJsonModule = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal class DropBadElementListSerializer<T>(private val elementSerializer: KSerializer<T>) : KSerializer<List<T>> {
    val listSerializer = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor =
        listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<T>) {
        encoder.encodeSerializableValue(listSerializer, value)
    }

    override fun deserialize(decoder: Decoder): List<T> {
        if (decoder !is JsonDecoder) {
            throw SerializationException("This serializer can be used only with Json format")
        }

        val element = decoder.decodeJsonElement() as? JsonArray
            ?: throw SerializationException("Expected JsonArray for List deserialization")

        val out = ArrayList<T>(element.size)
        for (e in element) {
            try {
                out += decoder.json.decodeFromJsonElement(elementSerializer, e)
            } catch (_: SerializationException) {
                // discard bad element quietly
            }
        }
        return out
    }
}

internal class TransformUnknownSegmentListSerializer(private val elementSerializer: KSerializer<IncomingSegment>) :
    KSerializer<List<IncomingSegment>> {

    val listSerializer = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor =
        listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<IncomingSegment>) {
        encoder.encodeSerializableValue(listSerializer, value)
    }

    override fun deserialize(decoder: Decoder): List<IncomingSegment> {
        if (decoder !is JsonDecoder) {
            throw SerializationException("This serializer can be used only with Json format")
        }

        val element = decoder.decodeJsonElement() as? JsonArray
            ?: throw SerializationException("Expected JsonArray for List deserialization")

        val out = ArrayList<IncomingSegment>(element.size)
        for (e in element) {
            out += try {
                decoder.json.decodeFromJsonElement(elementSerializer, e)
            } catch (_: SerializationException) {
                IncomingSegment.Text(
                    data = IncomingSegment.Text.Data(
                        text = "[${e.jsonObject["type"]!!.jsonPrimitive.content}]"
                    )
                )
            }
        }
        return out
    }
}

// ####################################
// Common Structs
// ####################################

/** 事件 */
@Serializable
@JsonClassDiscriminator("event_type")
sealed class Event {
    /** 事件 Unix 时间戳（秒） */
    abstract val time: Long
    /** 机器人 QQ 号 */
    abstract val selfId: Long

    /** 机器人离线事件 */
    @Serializable
    @SerialName("bot_offline")
    data class BotOffline(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 机器人离线事件 */
        @Serializable
        data class Data(
            /** 下线原因 */
            @SerialName("reason") val reason: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.reason]
         */
        val reason: String get() = data.reason
    }

    /** 消息接收事件 */
    @Serializable
    @SerialName("message_receive")
    data class MessageReceive(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: IncomingMessage
    ) : Event() {
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [IncomingMessage.peerId]
         */
        val peerId: Long get() = data.peerId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [IncomingMessage.messageSeq]
         */
        val messageSeq: Long get() = data.messageSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [IncomingMessage.senderId]
         */
        val senderId: Long get() = data.senderId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [IncomingMessage.time]
         */
        val dataTime: Long get() = data.time
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [IncomingMessage.segments]
         */
        val segments: List<IncomingSegment> get() = data.segments
    }

    /** 消息撤回事件 */
    @Serializable
    @SerialName("message_recall")
    data class MessageRecall(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 消息撤回事件 */
        @Serializable
        data class Data(
            /** 消息场景 */
            @SerialName("message_scene") val messageScene: String,
            /** 好友 QQ 号或群号 */
            @SerialName("peer_id") val peerId: Long,
            /** 消息序列号 */
            @SerialName("message_seq") val messageSeq: Long,
            /** 被撤回的消息的发送者 QQ 号 */
            @SerialName("sender_id") val senderId: Long,
            /** 操作者 QQ 号 */
            @SerialName("operator_id") val operatorId: Long,
            /** 撤回提示的后缀文本 */
            @SerialName("display_suffix") val displaySuffix: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageScene]
         */
        val messageScene: String get() = data.messageScene
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.peerId]
         */
        val peerId: Long get() = data.peerId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageSeq]
         */
        val messageSeq: Long get() = data.messageSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.senderId]
         */
        val senderId: Long get() = data.senderId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displaySuffix]
         */
        val displaySuffix: String get() = data.displaySuffix
    }

    /**
     * 会话置顶变更事件
     * @since 1.2
     */
    @Serializable
    @SerialName("peer_pin_change")
    data class PeerPinChange(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /**
         * 会话置顶变更事件
         * @since 1.2
         */
        @Serializable
        data class Data(
            /** 发生改变的会话的消息场景 */
            @SerialName("message_scene") val messageScene: String,
            /** 发生改变的好友 QQ 号或群号 */
            @SerialName("peer_id") val peerId: Long,
            /** 是否被置顶, `false` 表示取消置顶 */
            @SerialName("is_pinned") val isPinned: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageScene]
         */
        val messageScene: String get() = data.messageScene
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.peerId]
         */
        val peerId: Long get() = data.peerId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isPinned]
         */
        val isPinned: Boolean get() = data.isPinned
    }

    /** 好友请求事件 */
    @Serializable
    @SerialName("friend_request")
    data class FriendRequest(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 好友请求事件 */
        @Serializable
        data class Data(
            /** 申请好友的用户 QQ 号 */
            @SerialName("initiator_id") val initiatorId: Long,
            /** 用户 UID */
            @SerialName("initiator_uid") val initiatorUid: String,
            /** 申请附加信息 */
            @SerialName("comment") val comment: String,
            /** 申请来源 */
            @SerialName("via") val via: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.initiatorId]
         */
        val initiatorId: Long get() = data.initiatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.initiatorUid]
         */
        val initiatorUid: String get() = data.initiatorUid
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.comment]
         */
        val comment: String get() = data.comment
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.via]
         */
        val via: String get() = data.via
    }

    /** 入群请求事件 */
    @Serializable
    @SerialName("group_join_request")
    data class GroupJoinRequest(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 入群请求事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 请求对应的通知序列号 */
            @SerialName("notification_seq") val notificationSeq: Long,
            /** 请求是否被过滤（发起自风险账户） */
            @SerialName("is_filtered") val isFiltered: Boolean,
            /** 申请入群的用户 QQ 号 */
            @SerialName("initiator_id") val initiatorId: Long,
            /** 申请附加信息 */
            @SerialName("comment") val comment: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.notificationSeq]
         */
        val notificationSeq: Long get() = data.notificationSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isFiltered]
         */
        val isFiltered: Boolean get() = data.isFiltered
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.initiatorId]
         */
        val initiatorId: Long get() = data.initiatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.comment]
         */
        val comment: String get() = data.comment
    }

    /** 群成员邀请他人入群请求事件 */
    @Serializable
    @SerialName("group_invited_join_request")
    data class GroupInvitedJoinRequest(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群成员邀请他人入群请求事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 请求对应的通知序列号 */
            @SerialName("notification_seq") val notificationSeq: Long,
            /** 邀请者 QQ 号 */
            @SerialName("initiator_id") val initiatorId: Long,
            /** 被邀请者 QQ 号 */
            @SerialName("target_user_id") val targetUserId: Long,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.notificationSeq]
         */
        val notificationSeq: Long get() = data.notificationSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.initiatorId]
         */
        val initiatorId: Long get() = data.initiatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.targetUserId]
         */
        val targetUserId: Long get() = data.targetUserId
    }

    /** 他人邀请自身入群事件 */
    @Serializable
    @SerialName("group_invitation")
    data class GroupInvitation(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 他人邀请自身入群事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 邀请序列号 */
            @SerialName("invitation_seq") val invitationSeq: Long,
            /** 邀请者 QQ 号 */
            @SerialName("initiator_id") val initiatorId: Long,
            /**
             * 来源群号，如果是通过 QQ 群邀请
             * @since 1.2
             */
            @SerialName("source_group_id") val sourceGroupId: Long? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.invitationSeq]
         */
        val invitationSeq: Long get() = data.invitationSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.initiatorId]
         */
        val initiatorId: Long get() = data.initiatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.sourceGroupId]
         */
        val sourceGroupId: Long? get() = data.sourceGroupId
    }

    /** 好友戳一戳事件 */
    @Serializable
    @SerialName("friend_nudge")
    data class FriendNudge(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 好友戳一戳事件 */
        @Serializable
        data class Data(
            /** 好友 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 是否是自己发送的戳一戳 */
            @SerialName("is_self_send") val isSelfSend: Boolean,
            /** 是否是自己接收的戳一戳 */
            @SerialName("is_self_receive") val isSelfReceive: Boolean,
            /** 戳一戳提示的动作文本 */
            @SerialName("display_action") val displayAction: String,
            /** 戳一戳提示的后缀文本 */
            @SerialName("display_suffix") val displaySuffix: String,
            /** 戳一戳提示的动作图片 URL，用于取代动作提示文本 */
            @SerialName("display_action_img_url") val displayActionImgUrl: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isSelfSend]
         */
        val isSelfSend: Boolean get() = data.isSelfSend
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isSelfReceive]
         */
        val isSelfReceive: Boolean get() = data.isSelfReceive
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displayAction]
         */
        val displayAction: String get() = data.displayAction
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displaySuffix]
         */
        val displaySuffix: String get() = data.displaySuffix
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displayActionImgUrl]
         */
        val displayActionImgUrl: String get() = data.displayActionImgUrl
    }

    /** 好友文件上传事件 */
    @Serializable
    @SerialName("friend_file_upload")
    data class FriendFileUpload(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 好友文件上传事件 */
        @Serializable
        data class Data(
            /** 好友 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 文件 ID */
            @SerialName("file_id") val fileId: String,
            /** 文件名称 */
            @SerialName("file_name") val fileName: String,
            /** 文件大小（字节） */
            @SerialName("file_size") val fileSize: Long,
            /** 文件的 TriSHA1 哈希值 */
            @SerialName("file_hash") val fileHash: String,
            /** 是否是自己发送的文件 */
            @SerialName("is_self") val isSelf: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileId]
         */
        val fileId: String get() = data.fileId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileName]
         */
        val fileName: String get() = data.fileName
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileSize]
         */
        val fileSize: Long get() = data.fileSize
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileHash]
         */
        val fileHash: String get() = data.fileHash
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isSelf]
         */
        val isSelf: Boolean get() = data.isSelf
    }

    /** 群管理员变更事件 */
    @Serializable
    @SerialName("group_admin_change")
    data class GroupAdminChange(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群管理员变更事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发生变更的用户 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /**
             * 操作者 QQ 号
             * @since 1.1
             */
            @SerialName("operator_id") val operatorId: Long,
            /** 是否被设置为管理员，`false` 表示被取消管理员 */
            @SerialName("is_set") val isSet: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isSet]
         */
        val isSet: Boolean get() = data.isSet
    }

    /** 群精华消息变更事件 */
    @Serializable
    @SerialName("group_essence_message_change")
    data class GroupEssenceMessageChange(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群精华消息变更事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发生变更的消息序列号 */
            @SerialName("message_seq") val messageSeq: Long,
            /**
             * 操作者 QQ 号
             * @since 1.1
             */
            @SerialName("operator_id") val operatorId: Long,
            /** 是否被设置为精华，`false` 表示被取消精华 */
            @SerialName("is_set") val isSet: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageSeq]
         */
        val messageSeq: Long get() = data.messageSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isSet]
         */
        val isSet: Boolean get() = data.isSet
    }

    /** 群成员增加事件 */
    @Serializable
    @SerialName("group_member_increase")
    data class GroupMemberIncrease(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群成员增加事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发生变更的用户 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 管理员 QQ 号，如果是管理员同意入群 */
            @SerialName("operator_id") val operatorId: Long? = null,
            /** 邀请者 QQ 号，如果是邀请入群 */
            @SerialName("invitor_id") val invitorId: Long? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long? get() = data.operatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.invitorId]
         */
        val invitorId: Long? get() = data.invitorId
    }

    /** 群成员减少事件 */
    @Serializable
    @SerialName("group_member_decrease")
    data class GroupMemberDecrease(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群成员减少事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发生变更的用户 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 管理员 QQ 号，如果是管理员踢出 */
            @SerialName("operator_id") val operatorId: Long? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long? get() = data.operatorId
    }

    /**
     * 群解散事件
     * @since 1.3
     */
    @Serializable
    @SerialName("group_disband")
    data class GroupDisband(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /**
         * 群解散事件
         * @since 1.3
         */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 操作者 QQ 号 */
            @SerialName("operator_id") val operatorId: Long,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
    }

    /** 群名称变更事件 */
    @Serializable
    @SerialName("group_name_change")
    data class GroupNameChange(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群名称变更事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 新的群名称 */
            @SerialName("new_group_name") val newGroupName: String,
            /** 操作者 QQ 号 */
            @SerialName("operator_id") val operatorId: Long,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.newGroupName]
         */
        val newGroupName: String get() = data.newGroupName
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
    }

    /** 群消息表情回应事件 */
    @Serializable
    @SerialName("group_message_reaction")
    data class GroupMessageReaction(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群消息表情回应事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发送回应者 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 消息序列号 */
            @SerialName("message_seq") val messageSeq: Long,
            /** 表情 ID */
            @SerialName("face_id") val faceId: String,
            /**
             * 收到的回应类型
             * @since 1.2
             */
            @SerialName("reaction_type") val reactionType: String,
            /** 是否为添加，`false` 表示取消回应 */
            @SerialName("is_add") val isAdd: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageSeq]
         */
        val messageSeq: Long get() = data.messageSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.faceId]
         */
        val faceId: String get() = data.faceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.reactionType]
         */
        val reactionType: String get() = data.reactionType
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isAdd]
         */
        val isAdd: Boolean get() = data.isAdd
    }

    /** 群禁言事件 */
    @Serializable
    @SerialName("group_mute")
    data class GroupMute(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群禁言事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发生变更的用户 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 操作者 QQ 号 */
            @SerialName("operator_id") val operatorId: Long,
            /** 禁言时长（秒），为 0 表示取消禁言 */
            @SerialName("duration") val duration: Int,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.duration]
         */
        val duration: Int get() = data.duration
    }

    /** 群全体禁言事件 */
    @Serializable
    @SerialName("group_whole_mute")
    data class GroupWholeMute(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群全体禁言事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 操作者 QQ 号 */
            @SerialName("operator_id") val operatorId: Long,
            /** 是否全员禁言，`false` 表示取消全员禁言 */
            @SerialName("is_mute") val isMute: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.operatorId]
         */
        val operatorId: Long get() = data.operatorId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isMute]
         */
        val isMute: Boolean get() = data.isMute
    }

    /** 群戳一戳事件 */
    @Serializable
    @SerialName("group_nudge")
    data class GroupNudge(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群戳一戳事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发送者 QQ 号 */
            @SerialName("sender_id") val senderId: Long,
            /** 接收者 QQ 号 */
            @SerialName("receiver_id") val receiverId: Long,
            /** 戳一戳提示的动作文本 */
            @SerialName("display_action") val displayAction: String,
            /** 戳一戳提示的后缀文本 */
            @SerialName("display_suffix") val displaySuffix: String,
            /** 戳一戳提示的动作图片 URL，用于取代动作提示文本 */
            @SerialName("display_action_img_url") val displayActionImgUrl: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.senderId]
         */
        val senderId: Long get() = data.senderId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.receiverId]
         */
        val receiverId: Long get() = data.receiverId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displayAction]
         */
        val displayAction: String get() = data.displayAction
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displaySuffix]
         */
        val displaySuffix: String get() = data.displaySuffix
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.displayActionImgUrl]
         */
        val displayActionImgUrl: String get() = data.displayActionImgUrl
    }

    /** 群文件上传事件 */
    @Serializable
    @SerialName("group_file_upload")
    data class GroupFileUpload(
        /** 事件 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 机器人 QQ 号 */
        @SerialName("self_id") override val selfId: Long,
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : Event() {
        /** 群文件上传事件 */
        @Serializable
        data class Data(
            /** 群号 */
            @SerialName("group_id") val groupId: Long,
            /** 发送者 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /** 文件 ID */
            @SerialName("file_id") val fileId: String,
            /** 文件名称 */
            @SerialName("file_name") val fileName: String,
            /** 文件大小（字节） */
            @SerialName("file_size") val fileSize: Long,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.groupId]
         */
        val groupId: Long get() = data.groupId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileId]
         */
        val fileId: String get() = data.fileId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileName]
         */
        val fileName: String get() = data.fileName
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileSize]
         */
        val fileSize: Long get() = data.fileSize
    }
}

/** 好友实体 */
@Serializable
data class FriendEntity(
    /** 用户 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 用户昵称 */
    @SerialName("nickname") val nickname: String,
    /** 用户性别 */
    @SerialName("sex") val sex: String,
    /** 用户 QID */
    @SerialName("qid") val qid: String,
    /** 好友备注 */
    @SerialName("remark") val remark: String,
    /** 好友分组 */
    @SerialName("category") val category: FriendCategoryEntity,
)

/** 好友分组实体 */
@Serializable
data class FriendCategoryEntity(
    /** 好友分组 ID */
    @SerialName("category_id") val categoryId: Int,
    /** 好友分组名称 */
    @SerialName("category_name") val categoryName: String,
)

/** 群实体 */
@Serializable
data class GroupEntity(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 群名称 */
    @SerialName("group_name") val groupName: String,
    /** 群成员数量 */
    @SerialName("member_count") val memberCount: Int,
    /** 群容量 */
    @SerialName("max_member_count") val maxMemberCount: Int,
    /**
     * 群备注
     * @since 1.2
     */
    @SerialName("remark") val remark: String,
    /**
     * 群创建时间，Unix 时间戳（秒）
     * @since 1.2
     */
    @SerialName("created_time") val createdTime: Long,
    /**
     * 群简介
     * @since 1.2
     */
    @SerialName("description") val description: String,
    /**
     * 加群验证问题
     * @since 1.2
     */
    @SerialName("question") val question: String,
    /**
     * 群公告预览
     * @since 1.2
     */
    @SerialName("announcement") val announcement: String,
)

/** 群成员实体 */
@Serializable
data class GroupMemberEntity(
    /** 用户 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 用户昵称 */
    @SerialName("nickname") val nickname: String,
    /** 用户性别 */
    @SerialName("sex") val sex: String,
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 成员备注 */
    @SerialName("card") val card: String,
    /** 专属头衔 */
    @SerialName("title") val title: String,
    /** 群等级，注意和 QQ 等级区分 */
    @SerialName("level") val level: Int,
    /** 权限等级 */
    @SerialName("role") val role: String,
    /** 入群时间，Unix 时间戳（秒） */
    @SerialName("join_time") val joinTime: Long,
    /** 最后发言时间，Unix 时间戳（秒） */
    @SerialName("last_sent_time") val lastSentTime: Long,
    /** 禁言结束时间，Unix 时间戳（秒） */
    @SerialName("shut_up_end_time") val shutUpEndTime: Long? = null,
)

/** 群公告实体 */
@Serializable
data class GroupAnnouncementEntity(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 公告 ID */
    @SerialName("announcement_id") val announcementId: String,
    /** 发送者 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** Unix 时间戳（秒） */
    @SerialName("time") val time: Long,
    /** 公告内容 */
    @SerialName("content") val content: String,
    /** 公告图片 URL */
    @SerialName("image_url") val imageUrl: String? = null,
)

/** 群文件实体 */
@Serializable
data class GroupFileEntity(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
    /** 文件名称 */
    @SerialName("file_name") val fileName: String,
    /** 父文件夹 ID */
    @SerialName("parent_folder_id") val parentFolderId: String,
    /** 文件大小（字节） */
    @SerialName("file_size") val fileSize: Long,
    /** 上传时的 Unix 时间戳（秒） */
    @SerialName("uploaded_time") val uploadedTime: Long,
    /** 过期时的 Unix 时间戳（秒） */
    @SerialName("expire_time") val expireTime: Long? = null,
    /** 上传者 QQ 号 */
    @SerialName("uploader_id") val uploaderId: Long,
    /** 下载次数 */
    @SerialName("downloaded_times") val downloadedTimes: Int,
)

/** 群文件夹实体 */
@Serializable
data class GroupFolderEntity(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件夹 ID */
    @SerialName("folder_id") val folderId: String,
    /** 父文件夹 ID */
    @SerialName("parent_folder_id") val parentFolderId: String,
    /** 文件夹名称 */
    @SerialName("folder_name") val folderName: String,
    /** 创建时的 Unix 时间戳（秒） */
    @SerialName("created_time") val createdTime: Long,
    /** 最后修改时的 Unix 时间戳（秒） */
    @SerialName("last_modified_time") val lastModifiedTime: Long,
    /** 创建者 QQ 号 */
    @SerialName("creator_id") val creatorId: Long,
    /** 文件数量 */
    @SerialName("file_count") val fileCount: Int,
)

/** 好友请求实体 */
@Serializable
data class FriendRequest(
    /** 请求发起时的 Unix 时间戳（秒） */
    @SerialName("time") val time: Long,
    /** 请求发起者 QQ 号 */
    @SerialName("initiator_id") val initiatorId: Long,
    /** 请求发起者 UID */
    @SerialName("initiator_uid") val initiatorUid: String,
    /** 目标用户 QQ 号 */
    @SerialName("target_user_id") val targetUserId: Long,
    /** 目标用户 UID */
    @SerialName("target_user_uid") val targetUserUid: String,
    /** 请求状态 */
    @SerialName("state") val state: String,
    /** 申请附加信息 */
    @SerialName("comment") val comment: String,
    /** 申请来源 */
    @SerialName("via") val via: String,
    /** 请求是否被过滤（发起自风险账户） */
    @SerialName("is_filtered") val isFiltered: Boolean,
)

/** 群通知实体 */
@Serializable
@JsonClassDiscriminator("type")
sealed class GroupNotification {
    /** 群号 */
    abstract val groupId: Long
    /** 通知序列号 */
    abstract val notificationSeq: Long

    /** 用户入群请求 */
    @Serializable
    @SerialName("join_request")
    data class JoinRequest(
        /** 群号 */
        @SerialName("group_id") override val groupId: Long,
        /** 通知序列号 */
        @SerialName("notification_seq") override val notificationSeq: Long,
        /** 请求是否被过滤（发起自风险账户） */
        @SerialName("is_filtered") val isFiltered: Boolean,
        /** 发起者 QQ 号 */
        @SerialName("initiator_id") val initiatorId: Long,
        /** 请求状态 */
        @SerialName("state") val state: String,
        /** 处理请求的管理员 QQ 号 */
        @SerialName("operator_id") val operatorId: Long? = null,
        /** 入群请求附加信息 */
        @SerialName("comment") val comment: String,
    ) : GroupNotification()

    /** 群管理员变更通知 */
    @Serializable
    @SerialName("admin_change")
    data class AdminChange(
        /** 群号 */
        @SerialName("group_id") override val groupId: Long,
        /** 通知序列号 */
        @SerialName("notification_seq") override val notificationSeq: Long,
        /** 被设置/取消用户 QQ 号 */
        @SerialName("target_user_id") val targetUserId: Long,
        /** 是否被设置为管理员，`false` 表示被取消管理员 */
        @SerialName("is_set") val isSet: Boolean,
        /** 操作者（群主）QQ 号 */
        @SerialName("operator_id") val operatorId: Long,
    ) : GroupNotification()

    /** 群成员被移除通知 */
    @Serializable
    @SerialName("kick")
    data class Kick(
        /** 群号 */
        @SerialName("group_id") override val groupId: Long,
        /** 通知序列号 */
        @SerialName("notification_seq") override val notificationSeq: Long,
        /** 被移除用户 QQ 号 */
        @SerialName("target_user_id") val targetUserId: Long,
        /** 移除用户的管理员 QQ 号 */
        @SerialName("operator_id") val operatorId: Long,
    ) : GroupNotification()

    /** 群成员退群通知 */
    @Serializable
    @SerialName("quit")
    data class Quit(
        /** 群号 */
        @SerialName("group_id") override val groupId: Long,
        /** 通知序列号 */
        @SerialName("notification_seq") override val notificationSeq: Long,
        /** 退群用户 QQ 号 */
        @SerialName("target_user_id") val targetUserId: Long,
    ) : GroupNotification()

    /** 群成员邀请他人入群请求 */
    @Serializable
    @SerialName("invited_join_request")
    data class InvitedJoinRequest(
        /** 群号 */
        @SerialName("group_id") override val groupId: Long,
        /** 通知序列号 */
        @SerialName("notification_seq") override val notificationSeq: Long,
        /** 邀请者 QQ 号 */
        @SerialName("initiator_id") val initiatorId: Long,
        /** 被邀请用户 QQ 号 */
        @SerialName("target_user_id") val targetUserId: Long,
        /** 请求状态 */
        @SerialName("state") val state: String,
        /** 处理请求的管理员 QQ 号 */
        @SerialName("operator_id") val operatorId: Long? = null,
    ) : GroupNotification()
}

/** 接收消息 */
@Serializable
@JsonClassDiscriminator("message_scene")
sealed class IncomingMessage {
    /** 好友 QQ 号或群号 */
    abstract val peerId: Long
    /** 消息序列号 */
    abstract val messageSeq: Long
    /** 发送者 QQ 号 */
    abstract val senderId: Long
    /** 消息 Unix 时间戳（秒） */
    abstract val time: Long
    /** 消息段列表 */
    abstract val segments: List<IncomingSegment>

    /** 好友消息 */
    @Serializable
    @SerialName("friend")
    data class Friend(
        /** 好友 QQ 号或群号 */
        @SerialName("peer_id") override val peerId: Long,
        /** 消息序列号 */
        @SerialName("message_seq") override val messageSeq: Long,
        /** 发送者 QQ 号 */
        @SerialName("sender_id") override val senderId: Long,
        /** 消息 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 消息段列表 */
        @Serializable(with = TransformUnknownSegmentListSerializer::class)
        @SerialName("segments") override val segments: List<IncomingSegment>,
        /** 好友信息 */
        @SerialName("friend") val friend: FriendEntity,
    ) : IncomingMessage()

    /** 群消息 */
    @Serializable
    @SerialName("group")
    data class Group(
        /** 好友 QQ 号或群号 */
        @SerialName("peer_id") override val peerId: Long,
        /** 消息序列号 */
        @SerialName("message_seq") override val messageSeq: Long,
        /** 发送者 QQ 号 */
        @SerialName("sender_id") override val senderId: Long,
        /** 消息 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 消息段列表 */
        @Serializable(with = TransformUnknownSegmentListSerializer::class)
        @SerialName("segments") override val segments: List<IncomingSegment>,
        /** 群信息 */
        @SerialName("group") val group: GroupEntity,
        /** 群成员信息 */
        @SerialName("group_member") val groupMember: GroupMemberEntity,
    ) : IncomingMessage()

    /** 临时会话消息 */
    @Serializable
    @SerialName("temp")
    data class Temp(
        /** 好友 QQ 号或群号 */
        @SerialName("peer_id") override val peerId: Long,
        /** 消息序列号 */
        @SerialName("message_seq") override val messageSeq: Long,
        /** 发送者 QQ 号 */
        @SerialName("sender_id") override val senderId: Long,
        /** 消息 Unix 时间戳（秒） */
        @SerialName("time") override val time: Long,
        /** 消息段列表 */
        @Serializable(with = TransformUnknownSegmentListSerializer::class)
        @SerialName("segments") override val segments: List<IncomingSegment>,
        /** 临时会话发送者的所在的群信息 */
        @SerialName("group") val group: GroupEntity? = null,
    ) : IncomingMessage()
}

/** 接收转发消息 */
@Serializable
data class IncomingForwardedMessage(
    /**
     * 消息序列号
     * @since 1.2
     */
    @SerialName("message_seq") val messageSeq: Long,
    /** 发送者名称 */
    @SerialName("sender_name") val senderName: String,
    /** 发送者头像 URL */
    @SerialName("avatar_url") val avatarUrl: String,
    /** 消息 Unix 时间戳（秒） */
    @SerialName("time") val time: Long,
    /** 消息段列表 */
    @Serializable(with = TransformUnknownSegmentListSerializer::class)
    @SerialName("segments") val segments: List<IncomingSegment>,
)

/** 群精华消息 */
@Serializable
data class GroupEssenceMessage(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
    /** 消息发送时的 Unix 时间戳（秒） */
    @SerialName("message_time") val messageTime: Long,
    /** 发送者 QQ 号 */
    @SerialName("sender_id") val senderId: Long,
    /** 发送者名称 */
    @SerialName("sender_name") val senderName: String,
    /** 设置精华的操作者 QQ 号 */
    @SerialName("operator_id") val operatorId: Long,
    /** 设置精华的操作者名称 */
    @SerialName("operator_name") val operatorName: String,
    /** 消息被设置精华时的 Unix 时间戳（秒） */
    @SerialName("operation_time") val operationTime: Long,
    /** 消息段列表 */
    @Serializable(with = TransformUnknownSegmentListSerializer::class)
    @SerialName("segments") val segments: List<IncomingSegment>,
)

/** 接收消息段 */
@Serializable
@JsonClassDiscriminator("type")
sealed class IncomingSegment {
    /** 文本消息段 */
    @Serializable
    @SerialName("text")
    data class Text(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 文本消息段 */
        @Serializable
        data class Data(
            /** 文本内容 */
            @SerialName("text") val text: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.text]
         */
        val text: String get() = data.text
    }

    /** 提及消息段 */
    @Serializable
    @SerialName("mention")
    data class Mention(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 提及消息段 */
        @Serializable
        data class Data(
            /** 提及的 QQ 号 */
            @SerialName("user_id") val userId: Long,
            /**
             * 去掉 `@` 前缀的提及的名称
             * @since 1.2
             */
            @SerialName("name") val name: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.name]
         */
        val name: String get() = data.name
    }

    /** 提及全体消息段 */
    @Serializable
    @SerialName("mention_all")
    data class MentionAll(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 提及全体消息段 */
        @Serializable
        class Data

    }

    /** 表情消息段 */
    @Serializable
    @SerialName("face")
    data class Face(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 表情消息段 */
        @Serializable
        data class Data(
            /** 表情 ID */
            @SerialName("face_id") val faceId: String,
            /**
             * 是否为超级表情
             * @since 1.1
             */
            @SerialName("is_large") val isLarge: Boolean,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.faceId]
         */
        val faceId: String get() = data.faceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isLarge]
         */
        val isLarge: Boolean get() = data.isLarge
    }

    /** 回复消息段 */
    @Serializable
    @SerialName("reply")
    data class Reply(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 回复消息段 */
        @Serializable
        data class Data(
            /** 被引用的消息序列号 */
            @SerialName("message_seq") val messageSeq: Long,
            /**
             * 被引用的消息发送者 QQ 号
             * @since 1.2
             */
            @SerialName("sender_id") val senderId: Long,
            /**
             * 被引用的消息发送者名称，仅在合并转发中能够获取
             * @since 1.2
             */
            @SerialName("sender_name") val senderName: String? = null,
            /**
             * 被引用的消息的 Unix 时间戳（秒）
             * @since 1.2
             */
            @SerialName("time") val time: Long,
            /**
             * 被引用的消息内容
             * @since 1.2
             */
            @Serializable(with = TransformUnknownSegmentListSerializer::class)
            @SerialName("segments") val segments: List<IncomingSegment>,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageSeq]
         */
        val messageSeq: Long get() = data.messageSeq
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.senderId]
         */
        val senderId: Long get() = data.senderId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.senderName]
         */
        val senderName: String? get() = data.senderName
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.time]
         */
        val time: Long get() = data.time
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.segments]
         */
        val segments: List<IncomingSegment> get() = data.segments
    }

    /** 图片消息段 */
    @Serializable
    @SerialName("image")
    data class Image(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 图片消息段 */
        @Serializable
        data class Data(
            /** 资源 ID */
            @SerialName("resource_id") val resourceId: String,
            /** 临时 URL */
            @SerialName("temp_url") val tempUrl: String,
            /** 图片宽度 */
            @SerialName("width") val width: Int,
            /** 图片高度 */
            @SerialName("height") val height: Int,
            /** 图片预览文本 */
            @SerialName("summary") val summary: String,
            /** 图片类型 */
            @SerialName("sub_type") val subType: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.resourceId]
         */
        val resourceId: String get() = data.resourceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.tempUrl]
         */
        val tempUrl: String get() = data.tempUrl
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.width]
         */
        val width: Int get() = data.width
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.height]
         */
        val height: Int get() = data.height
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.summary]
         */
        val summary: String get() = data.summary
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.subType]
         */
        val subType: String get() = data.subType
    }

    /** 语音消息段 */
    @Serializable
    @SerialName("record")
    data class Record(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 语音消息段 */
        @Serializable
        data class Data(
            /** 资源 ID */
            @SerialName("resource_id") val resourceId: String,
            /** 临时 URL */
            @SerialName("temp_url") val tempUrl: String,
            /** 语音时长（秒） */
            @SerialName("duration") val duration: Int,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.resourceId]
         */
        val resourceId: String get() = data.resourceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.tempUrl]
         */
        val tempUrl: String get() = data.tempUrl
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.duration]
         */
        val duration: Int get() = data.duration
    }

    /** 视频消息段 */
    @Serializable
    @SerialName("video")
    data class Video(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 视频消息段 */
        @Serializable
        data class Data(
            /** 资源 ID */
            @SerialName("resource_id") val resourceId: String,
            /** 临时 URL */
            @SerialName("temp_url") val tempUrl: String,
            /** 视频宽度 */
            @SerialName("width") val width: Int,
            /** 视频高度 */
            @SerialName("height") val height: Int,
            /** 视频时长（秒） */
            @SerialName("duration") val duration: Int,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.resourceId]
         */
        val resourceId: String get() = data.resourceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.tempUrl]
         */
        val tempUrl: String get() = data.tempUrl
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.width]
         */
        val width: Int get() = data.width
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.height]
         */
        val height: Int get() = data.height
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.duration]
         */
        val duration: Int get() = data.duration
    }

    /** 文件消息段 */
    @Serializable
    @SerialName("file")
    data class File(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 文件消息段 */
        @Serializable
        data class Data(
            /** 文件 ID */
            @SerialName("file_id") val fileId: String,
            /** 文件名称 */
            @SerialName("file_name") val fileName: String,
            /** 文件大小（字节） */
            @SerialName("file_size") val fileSize: Long,
            /** 文件的 TriSHA1 哈希值，仅在私聊文件中存在 */
            @SerialName("file_hash") val fileHash: String? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileId]
         */
        val fileId: String get() = data.fileId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileName]
         */
        val fileName: String get() = data.fileName
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileSize]
         */
        val fileSize: Long get() = data.fileSize
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.fileHash]
         */
        val fileHash: String? get() = data.fileHash
    }

    /** 合并转发消息段 */
    @Serializable
    @SerialName("forward")
    data class Forward(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 合并转发消息段 */
        @Serializable
        data class Data(
            /** 合并转发 ID */
            @SerialName("forward_id") val forwardId: String,
            /**
             * 合并转发标题
             * @since 1.1
             */
            @SerialName("title") val title: String,
            /**
             * 合并转发预览文本
             * @since 1.1
             */
            @SerialName("preview") val preview: List<String>,
            /**
             * 合并转发摘要
             * @since 1.1
             */
            @SerialName("summary") val summary: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.forwardId]
         */
        val forwardId: String get() = data.forwardId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.title]
         */
        val title: String get() = data.title
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.preview]
         */
        val preview: List<String> get() = data.preview
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.summary]
         */
        val summary: String get() = data.summary
    }

    /** 市场表情消息段 */
    @Serializable
    @SerialName("market_face")
    data class MarketFace(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 市场表情消息段 */
        @Serializable
        data class Data(
            /**
             * 市场表情包 ID
             * @since 1.1
             */
            @SerialName("emoji_package_id") val emojiPackageId: Int,
            /**
             * 市场表情 ID
             * @since 1.1
             */
            @SerialName("emoji_id") val emojiId: String,
            /**
             * 市场表情 Key
             * @since 1.1
             */
            @SerialName("key") val key: String,
            /**
             * 市场表情预览文本
             * @since 1.1
             */
            @SerialName("summary") val summary: String,
            /** 市场表情 URL */
            @SerialName("url") val url: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.emojiPackageId]
         */
        val emojiPackageId: Int get() = data.emojiPackageId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.emojiId]
         */
        val emojiId: String get() = data.emojiId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.key]
         */
        val key: String get() = data.key
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.summary]
         */
        val summary: String get() = data.summary
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.url]
         */
        val url: String get() = data.url
    }

    /** 小程序消息段 */
    @Serializable
    @SerialName("light_app")
    data class LightApp(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** 小程序消息段 */
        @Serializable
        data class Data(
            /** 小程序名称 */
            @SerialName("app_name") val appName: String,
            /** 小程序 JSON 数据 */
            @SerialName("json_payload") val jsonPayload: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.appName]
         */
        val appName: String get() = data.appName
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.jsonPayload]
         */
        val jsonPayload: String get() = data.jsonPayload
    }

    /** XML 消息段 */
    @Serializable
    @SerialName("xml")
    data class Xml(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** XML 消息段 */
        @Serializable
        data class Data(
            /** 服务 ID */
            @SerialName("service_id") val serviceId: Int,
            /** XML 数据 */
            @SerialName("xml_payload") val xmlPayload: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.serviceId]
         */
        val serviceId: Int get() = data.serviceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.xmlPayload]
         */
        val xmlPayload: String get() = data.xmlPayload
    }

    /** Markdown 消息段 */
    @Serializable
    @SerialName("markdown")
    data class Markdown(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : IncomingSegment() {
        /** Markdown 消息段 */
        @Serializable
        data class Data(
            /** Markdown 内容 */
            @SerialName("content") val content: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.content]
         */
        val content: String get() = data.content
    }
}

/** 发送转发消息 */
@Serializable
data class OutgoingForwardedMessage(
    /** 发送者 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 发送者名称 */
    @SerialName("sender_name") val senderName: String,
    /**
     * 消息 Unix 时间戳（秒）
     * @since 1.3
     */
    @SerialName("time") val time: Long? = null,
    /** 消息段列表 */
    @Serializable(with = DropBadElementListSerializer::class)
    @SerialName("segments") val segments: List<OutgoingSegment>,
)

/** 发送消息段 */
@Serializable
@JsonClassDiscriminator("type")
sealed class OutgoingSegment {
    /** 文本消息段 */
    @Serializable
    @SerialName("text")
    data class Text(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 文本消息段 */
        @Serializable
        data class Data(
            /** 文本内容 */
            @SerialName("text") val text: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.text]
         */
        val text: String get() = data.text
    }

    /** 提及消息段 */
    @Serializable
    @SerialName("mention")
    data class Mention(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 提及消息段 */
        @Serializable
        data class Data(
            /** 提及的 QQ 号 */
            @SerialName("user_id") val userId: Long,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.userId]
         */
        val userId: Long get() = data.userId
    }

    /** 提及全体消息段 */
    @Serializable
    @SerialName("mention_all")
    data class MentionAll(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 提及全体消息段 */
        @Serializable
        class Data

    }

    /** 表情消息段 */
    @Serializable
    @SerialName("face")
    data class Face(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 表情消息段 */
        @Serializable
        data class Data(
            /** 表情 ID */
            @SerialName("face_id") val faceId: String,
            /**
             * 是否为超级表情
             * @since 1.1
             */
            @SerialName("is_large") @LiteralDefault("false") val isLarge: Boolean = false,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.faceId]
         */
        val faceId: String get() = data.faceId
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.isLarge]
         */
        val isLarge: Boolean get() = data.isLarge
    }

    /** 回复消息段 */
    @Serializable
    @SerialName("reply")
    data class Reply(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 回复消息段 */
        @Serializable
        data class Data(
            /** 被引用的消息序列号 */
            @SerialName("message_seq") val messageSeq: Long,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messageSeq]
         */
        val messageSeq: Long get() = data.messageSeq
    }

    /** 图片消息段 */
    @Serializable
    @SerialName("image")
    data class Image(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 图片消息段 */
        @Serializable
        data class Data(
            /** 文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
            @SerialName("uri") val uri: String,
            /** 图片类型 */
            @SerialName("sub_type") @LiteralDefault("\"normal\"") val subType: String = "normal",
            /** 图片预览文本 */
            @SerialName("summary") val summary: String? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.uri]
         */
        val uri: String get() = data.uri
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.subType]
         */
        val subType: String get() = data.subType
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.summary]
         */
        val summary: String? get() = data.summary
    }

    /** 语音消息段 */
    @Serializable
    @SerialName("record")
    data class Record(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 语音消息段 */
        @Serializable
        data class Data(
            /** 文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
            @SerialName("uri") val uri: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.uri]
         */
        val uri: String get() = data.uri
    }

    /** 视频消息段 */
    @Serializable
    @SerialName("video")
    data class Video(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 视频消息段 */
        @Serializable
        data class Data(
            /** 文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
            @SerialName("uri") val uri: String,
            /** 封面图片 URI */
            @SerialName("thumb_uri") val thumbUri: String? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.uri]
         */
        val uri: String get() = data.uri
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.thumbUri]
         */
        val thumbUri: String? get() = data.thumbUri
    }

    /** 合并转发消息段 */
    @Serializable
    @SerialName("forward")
    data class Forward(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /** 合并转发消息段 */
        @Serializable
        data class Data(
            /** 合并转发消息内容 */
            @SerialName("messages") val messages: List<OutgoingForwardedMessage>,
            /**
             * 合并转发标题
             * @since 1.2
             */
            @SerialName("title") val title: String? = null,
            /**
             * 合并转发预览文本，若提供，至少 1 条，至多 4 条
             * @since 1.2
             */
            @SerialName("preview") val preview: List<String>? = null,
            /**
             * 合并转发摘要
             * @since 1.2
             */
            @SerialName("summary") val summary: String? = null,
            /**
             * 合并转发的预览外显文本，仅对移动端 QQ 有效
             * @since 1.2
             */
            @SerialName("prompt") val prompt: String? = null,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.messages]
         */
        val messages: List<OutgoingForwardedMessage> get() = data.messages
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.title]
         */
        val title: String? get() = data.title
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.preview]
         */
        val preview: List<String>? get() = data.preview
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.summary]
         */
        val summary: String? get() = data.summary
        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.prompt]
         */
        val prompt: String? get() = data.prompt
    }

    /**
     * 小程序消息段
     * @since 1.2
     */
    @Serializable
    @SerialName("light_app")
    data class LightApp(
        /** 数据字段 */
        @SerialName("data") val data: Data
    ) : OutgoingSegment() {
        /**
         * 小程序消息段
         * @since 1.2
         */
        @Serializable
        data class Data(
            /** 小程序 JSON 数据 */
            @SerialName("json_payload") val jsonPayload: String,
        )

        /**
         * 访问器字段，对应 `data` 中的同名字段
         * @see [Data.jsonPayload]
         */
        val jsonPayload: String get() = data.jsonPayload
    }
}

// ####################################
// API Input and Output Structs
// ####################################

@Serializable
data class ApiGeneralResponse(
    @SerialName("status") val status: String,
    @SerialName("retcode") val retcode: Int,
    @SerialName("data") val data: JsonElement? = null,
    @SerialName("message") val message: String? = null,
)

@Serializable
class ApiEmptyStruct

// ---- 系统 API ----

/** 获取登录信息 API 请求参数 */
typealias GetLoginInfoInput = ApiEmptyStruct

/** 获取登录信息 API 响应数据 */
@Serializable
data class GetLoginInfoOutput(
    /** 登录 QQ 号 */
    @SerialName("uin") val uin: Long,
    /** 登录昵称 */
    @SerialName("nickname") val nickname: String,
)

/** 获取协议端信息 API 请求参数 */
typealias GetImplInfoInput = ApiEmptyStruct

/** 获取协议端信息 API 响应数据 */
@Serializable
data class GetImplInfoOutput(
    /** 协议端名称 */
    @SerialName("impl_name") val implName: String,
    /** 协议端版本 */
    @SerialName("impl_version") val implVersion: String,
    /** 协议端使用的 QQ 协议版本 */
    @SerialName("qq_protocol_version") val qqProtocolVersion: String,
    /** 协议端使用的 QQ 协议平台 */
    @SerialName("qq_protocol_type") val qqProtocolType: String,
    /** 协议端实现的 Milky 协议版本，目前为 "1.2" */
    @SerialName("milky_version") val milkyVersion: String,
)

/** 获取用户个人信息 API 请求参数 */
@Serializable
data class GetUserProfileInput(
    /** 用户 QQ 号 */
    @SerialName("user_id") val userId: Long,
)

/** 获取用户个人信息 API 响应数据 */
@Serializable
data class GetUserProfileOutput(
    /** 昵称 */
    @SerialName("nickname") val nickname: String,
    /** QID */
    @SerialName("qid") val qid: String,
    /** 年龄 */
    @SerialName("age") val age: Int,
    /** 性别 */
    @SerialName("sex") val sex: String,
    /** 备注 */
    @SerialName("remark") val remark: String,
    /** 个性签名 */
    @SerialName("bio") val bio: String,
    /** QQ 等级 */
    @SerialName("level") val level: Int,
    /** 国家或地区 */
    @SerialName("country") val country: String,
    /** 城市 */
    @SerialName("city") val city: String,
    /** 学校 */
    @SerialName("school") val school: String,
)

/** 获取好友列表 API 请求参数 */
@Serializable
data class GetFriendListInput(
    /** 是否强制不使用缓存 */
    @SerialName("no_cache") @LiteralDefault("false") val noCache: Boolean = false,
)

/** 获取好友列表 API 响应数据 */
@Serializable
data class GetFriendListOutput(
    /** 好友列表 */
    @SerialName("friends") val friends: List<FriendEntity>,
)

/** 获取好友信息 API 请求参数 */
@Serializable
data class GetFriendInfoInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 是否强制不使用缓存 */
    @SerialName("no_cache") @LiteralDefault("false") val noCache: Boolean = false,
)

/** 获取好友信息 API 响应数据 */
@Serializable
data class GetFriendInfoOutput(
    /** 好友信息 */
    @SerialName("friend") val friend: FriendEntity,
)

/** 获取群列表 API 请求参数 */
@Serializable
data class GetGroupListInput(
    /** 是否强制不使用缓存 */
    @SerialName("no_cache") @LiteralDefault("false") val noCache: Boolean = false,
)

/** 获取群列表 API 响应数据 */
@Serializable
data class GetGroupListOutput(
    /** 群列表 */
    @SerialName("groups") val groups: List<GroupEntity>,
)

/** 获取群信息 API 请求参数 */
@Serializable
data class GetGroupInfoInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 是否强制不使用缓存 */
    @SerialName("no_cache") @LiteralDefault("false") val noCache: Boolean = false,
)

/** 获取群信息 API 响应数据 */
@Serializable
data class GetGroupInfoOutput(
    /** 群信息 */
    @SerialName("group") val group: GroupEntity,
)

/** 获取群成员列表 API 请求参数 */
@Serializable
data class GetGroupMemberListInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 是否强制不使用缓存 */
    @SerialName("no_cache") @LiteralDefault("false") val noCache: Boolean = false,
)

/** 获取群成员列表 API 响应数据 */
@Serializable
data class GetGroupMemberListOutput(
    /** 群成员列表 */
    @SerialName("members") val members: List<GroupMemberEntity>,
)

/** 获取群成员信息 API 请求参数 */
@Serializable
data class GetGroupMemberInfoInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 群成员 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 是否强制不使用缓存 */
    @SerialName("no_cache") @LiteralDefault("false") val noCache: Boolean = false,
)

/** 获取群成员信息 API 响应数据 */
@Serializable
data class GetGroupMemberInfoOutput(
    /** 群成员信息 */
    @SerialName("member") val member: GroupMemberEntity,
)

/**
 * 获取置顶的好友和群列表 API 请求参数
 * @since 1.2
 */
typealias GetPeerPinsInput = ApiEmptyStruct

/**
 * 获取置顶的好友和群列表 API 响应数据
 * @since 1.2
 */
@Serializable
data class GetPeerPinsOutput(
    /** 置顶的好友列表 */
    @SerialName("friends") val friends: List<FriendEntity>,
    /** 置顶的群列表 */
    @SerialName("groups") val groups: List<GroupEntity>,
)

/**
 * 设置好友或群的置顶状态 API 请求参数
 * @since 1.2
 */
@Serializable
data class SetPeerPinInput(
    /** 要设置的会话的消息场景 */
    @SerialName("message_scene") val messageScene: String,
    /** 要设置的好友 QQ 号或群号 */
    @SerialName("peer_id") val peerId: Long,
    /** 是否置顶, `false` 表示取消置顶 */
    @SerialName("is_pinned") @LiteralDefault("true") val isPinned: Boolean = true,
)

/**
 * 设置好友或群的置顶状态 API 响应数据
 * @since 1.2
 */
typealias SetPeerPinOutput = ApiEmptyStruct

/**
 * 设置 QQ 账号头像 API 请求参数
 * @since 1.1
 */
@Serializable
data class SetAvatarInput(
    /** 头像文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
    @SerialName("uri") val uri: String,
)

/**
 * 设置 QQ 账号头像 API 响应数据
 * @since 1.1
 */
typealias SetAvatarOutput = ApiEmptyStruct

/**
 * 设置 QQ 账号昵称 API 请求参数
 * @since 1.1
 */
@Serializable
data class SetNicknameInput(
    /** 新昵称 */
    @SerialName("new_nickname") val newNickname: String,
)

/**
 * 设置 QQ 账号昵称 API 响应数据
 * @since 1.1
 */
typealias SetNicknameOutput = ApiEmptyStruct

/**
 * 设置 QQ 账号个性签名 API 请求参数
 * @since 1.1
 */
@Serializable
data class SetBioInput(
    /** 新个性签名 */
    @SerialName("new_bio") val newBio: String,
)

/**
 * 设置 QQ 账号个性签名 API 响应数据
 * @since 1.1
 */
typealias SetBioOutput = ApiEmptyStruct

/**
 * 获取自定义表情 URL 列表 API 请求参数
 * @since 1.1
 */
typealias GetCustomFaceUrlListInput = ApiEmptyStruct

/**
 * 获取自定义表情 URL 列表 API 响应数据
 * @since 1.1
 */
@Serializable
data class GetCustomFaceUrlListOutput(
    /** 自定义表情 URL 列表 */
    @SerialName("urls") val urls: List<String>,
)

/** 获取 Cookies API 请求参数 */
@Serializable
data class GetCookiesInput(
    /** 需要获取 Cookies 的域名 */
    @SerialName("domain") val domain: String,
)

/** 获取 Cookies API 响应数据 */
@Serializable
data class GetCookiesOutput(
    /** 域名对应的 Cookies 字符串 */
    @SerialName("cookies") val cookies: String,
)

/** 获取 CSRF Token API 请求参数 */
typealias GetCsrfTokenInput = ApiEmptyStruct

/** 获取 CSRF Token API 响应数据 */
@Serializable
data class GetCsrfTokenOutput(
    /** CSRF Token */
    @SerialName("csrf_token") val csrfToken: String,
)

// ---- 消息 API ----

/** 发送私聊消息 API 请求参数 */
@Serializable
data class SendPrivateMessageInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 消息内容 */
    @Serializable(with = DropBadElementListSerializer::class)
    @SerialName("message") val message: List<OutgoingSegment>,
)

/** 发送私聊消息 API 响应数据 */
@Serializable
data class SendPrivateMessageOutput(
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
    /** 消息发送时间 */
    @SerialName("time") val time: Long,
)

/** 发送群聊消息 API 请求参数 */
@Serializable
data class SendGroupMessageInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 消息内容 */
    @Serializable(with = DropBadElementListSerializer::class)
    @SerialName("message") val message: List<OutgoingSegment>,
)

/** 发送群聊消息 API 响应数据 */
@Serializable
data class SendGroupMessageOutput(
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
    /** 消息发送时间 */
    @SerialName("time") val time: Long,
)

/** 撤回私聊消息 API 请求参数 */
@Serializable
data class RecallPrivateMessageInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
)

/** 撤回私聊消息 API 响应数据 */
typealias RecallPrivateMessageOutput = ApiEmptyStruct

/** 撤回群聊消息 API 请求参数 */
@Serializable
data class RecallGroupMessageInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
)

/** 撤回群聊消息 API 响应数据 */
typealias RecallGroupMessageOutput = ApiEmptyStruct

/** 获取消息 API 请求参数 */
@Serializable
data class GetMessageInput(
    /** 消息场景 */
    @SerialName("message_scene") val messageScene: String,
    /** 好友 QQ 号或群号 */
    @SerialName("peer_id") val peerId: Long,
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
)

/** 获取消息 API 响应数据 */
@Serializable
data class GetMessageOutput(
    /** 消息内容 */
    @SerialName("message") val message: IncomingMessage,
)

/** 获取历史消息列表 API 请求参数 */
@Serializable
data class GetHistoryMessagesInput(
    /** 消息场景 */
    @SerialName("message_scene") val messageScene: String,
    /** 好友 QQ 号或群号 */
    @SerialName("peer_id") val peerId: Long,
    /** 起始消息序列号，由此开始从新到旧查询，不提供则从最新消息开始 */
    @SerialName("start_message_seq") val startMessageSeq: Long? = null,
    /** 期望获取到的消息数量，最多 30 条 */
    @SerialName("limit") @LiteralDefault("20") val limit: Int = 20,
)

/** 获取历史消息列表 API 响应数据 */
@Serializable
data class GetHistoryMessagesOutput(
    /** 获取到的消息（message_seq 升序排列），部分消息可能不存在，如撤回的消息 */
    @Serializable(with = DropBadElementListSerializer::class)
    @SerialName("messages") val messages: List<IncomingMessage>,
    /** 下一页起始消息序列号 */
    @SerialName("next_message_seq") val nextMessageSeq: Long? = null,
)

/** 获取临时资源链接 API 请求参数 */
@Serializable
data class GetResourceTempUrlInput(
    /** 资源 ID */
    @SerialName("resource_id") val resourceId: String,
)

/** 获取临时资源链接 API 响应数据 */
@Serializable
data class GetResourceTempUrlOutput(
    /** 临时资源链接 */
    @SerialName("url") val url: String,
)

/** 获取合并转发消息内容 API 请求参数 */
@Serializable
data class GetForwardedMessagesInput(
    /** 转发消息 ID */
    @SerialName("forward_id") val forwardId: String,
)

/** 获取合并转发消息内容 API 响应数据 */
@Serializable
data class GetForwardedMessagesOutput(
    /** 转发消息内容 */
    @SerialName("messages") val messages: List<IncomingForwardedMessage>,
)

/** 标记消息为已读 API 请求参数 */
@Serializable
data class MarkMessageAsReadInput(
    /** 消息场景 */
    @SerialName("message_scene") val messageScene: String,
    /** 好友 QQ 号或群号 */
    @SerialName("peer_id") val peerId: Long,
    /** 标为已读的消息序列号，该消息及更早的消息将被标记为已读 */
    @SerialName("message_seq") val messageSeq: Long,
)

/** 标记消息为已读 API 响应数据 */
typealias MarkMessageAsReadOutput = ApiEmptyStruct

// ---- 好友 API ----

/** 发送好友戳一戳 API 请求参数 */
@Serializable
data class SendFriendNudgeInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 是否戳自己 */
    @SerialName("is_self") @LiteralDefault("false") val isSelf: Boolean = false,
)

/** 发送好友戳一戳 API 响应数据 */
typealias SendFriendNudgeOutput = ApiEmptyStruct

/** 发送名片点赞 API 请求参数 */
@Serializable
data class SendProfileLikeInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 点赞数量 */
    @SerialName("count") @LiteralDefault("1") val count: Int = 1,
)

/** 发送名片点赞 API 响应数据 */
typealias SendProfileLikeOutput = ApiEmptyStruct

/**
 * 删除好友 API 请求参数
 * @since 1.1
 */
@Serializable
data class DeleteFriendInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
)

/**
 * 删除好友 API 响应数据
 * @since 1.1
 */
typealias DeleteFriendOutput = ApiEmptyStruct

/** 获取好友请求列表 API 请求参数 */
@Serializable
data class GetFriendRequestsInput(
    /** 获取的最大请求数量 */
    @SerialName("limit") @LiteralDefault("20") val limit: Int = 20,
    /** `true` 表示只获取被过滤（由风险账号发起）的通知，`false` 表示只获取未被过滤的通知 */
    @SerialName("is_filtered") @LiteralDefault("false") val isFiltered: Boolean = false,
)

/** 获取好友请求列表 API 响应数据 */
@Serializable
data class GetFriendRequestsOutput(
    /** 好友请求列表 */
    @SerialName("requests") val requests: List<FriendRequest>,
)

/** 同意好友请求 API 请求参数 */
@Serializable
data class AcceptFriendRequestInput(
    /** 请求发起者 UID */
    @SerialName("initiator_uid") val initiatorUid: String,
    /** 是否是被过滤的请求 */
    @SerialName("is_filtered") @LiteralDefault("false") val isFiltered: Boolean = false,
)

/** 同意好友请求 API 响应数据 */
typealias AcceptFriendRequestOutput = ApiEmptyStruct

/** 拒绝好友请求 API 请求参数 */
@Serializable
data class RejectFriendRequestInput(
    /** 请求发起者 UID */
    @SerialName("initiator_uid") val initiatorUid: String,
    /** 是否是被过滤的请求 */
    @SerialName("is_filtered") @LiteralDefault("false") val isFiltered: Boolean = false,
    /** 拒绝理由 */
    @SerialName("reason") val reason: String? = null,
)

/** 拒绝好友请求 API 响应数据 */
typealias RejectFriendRequestOutput = ApiEmptyStruct

// ---- 群聊 API ----

/** 设置群名称 API 请求参数 */
@Serializable
data class SetGroupNameInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 新群名称 */
    @SerialName("new_group_name") val newGroupName: String,
)

/** 设置群名称 API 响应数据 */
typealias SetGroupNameOutput = ApiEmptyStruct

/** 设置群头像 API 请求参数 */
@Serializable
data class SetGroupAvatarInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 头像文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
    @SerialName("image_uri") val imageUri: String,
)

/** 设置群头像 API 响应数据 */
typealias SetGroupAvatarOutput = ApiEmptyStruct

/** 设置群名片 API 请求参数 */
@Serializable
data class SetGroupMemberCardInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 被设置的群成员 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 新群名片 */
    @SerialName("card") val card: String,
)

/** 设置群名片 API 响应数据 */
typealias SetGroupMemberCardOutput = ApiEmptyStruct

/** 设置群成员专属头衔 API 请求参数 */
@Serializable
data class SetGroupMemberSpecialTitleInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 被设置的群成员 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 新专属头衔 */
    @SerialName("special_title") val specialTitle: String,
)

/** 设置群成员专属头衔 API 响应数据 */
typealias SetGroupMemberSpecialTitleOutput = ApiEmptyStruct

/** 设置群管理员 API 请求参数 */
@Serializable
data class SetGroupMemberAdminInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 被设置的 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 是否设置为管理员，`false` 表示取消管理员 */
    @SerialName("is_set") @LiteralDefault("true") val isSet: Boolean = true,
)

/** 设置群管理员 API 响应数据 */
typealias SetGroupMemberAdminOutput = ApiEmptyStruct

/** 设置群成员禁言 API 请求参数 */
@Serializable
data class SetGroupMemberMuteInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 被设置的 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 禁言持续时间（秒），设为 `0` 为取消禁言 */
    @SerialName("duration") @LiteralDefault("0") val duration: Int = 0,
)

/** 设置群成员禁言 API 响应数据 */
typealias SetGroupMemberMuteOutput = ApiEmptyStruct

/** 设置群全员禁言 API 请求参数 */
@Serializable
data class SetGroupWholeMuteInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 是否开启全员禁言，`false` 表示取消全员禁言 */
    @SerialName("is_mute") @LiteralDefault("true") val isMute: Boolean = true,
)

/** 设置群全员禁言 API 响应数据 */
typealias SetGroupWholeMuteOutput = ApiEmptyStruct

/** 踢出群成员 API 请求参数 */
@Serializable
data class KickGroupMemberInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 被踢的 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 是否拒绝加群申请，`false` 表示不拒绝 */
    @SerialName("reject_add_request") @LiteralDefault("false") val rejectAddRequest: Boolean = false,
)

/** 踢出群成员 API 响应数据 */
typealias KickGroupMemberOutput = ApiEmptyStruct

/** 获取群公告列表 API 请求参数 */
@Serializable
data class GetGroupAnnouncementsInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
)

/** 获取群公告列表 API 响应数据 */
@Serializable
data class GetGroupAnnouncementsOutput(
    /** 群公告列表 */
    @SerialName("announcements") val announcements: List<GroupAnnouncementEntity>,
)

/** 发送群公告 API 请求参数 */
@Serializable
data class SendGroupAnnouncementInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 公告内容 */
    @SerialName("content") val content: String,
    /** 公告附带图像文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
    @SerialName("image_uri") val imageUri: String? = null,
)

/** 发送群公告 API 响应数据 */
typealias SendGroupAnnouncementOutput = ApiEmptyStruct

/** 删除群公告 API 请求参数 */
@Serializable
data class DeleteGroupAnnouncementInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 公告 ID */
    @SerialName("announcement_id") val announcementId: String,
)

/** 删除群公告 API 响应数据 */
typealias DeleteGroupAnnouncementOutput = ApiEmptyStruct

/** 获取群精华消息列表 API 请求参数 */
@Serializable
data class GetGroupEssenceMessagesInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 页码索引，从 0 开始 */
    @SerialName("page_index") val pageIndex: Int,
    /** 每页包含的精华消息数量 */
    @SerialName("page_size") val pageSize: Int,
)

/** 获取群精华消息列表 API 响应数据 */
@Serializable
data class GetGroupEssenceMessagesOutput(
    /** 精华消息列表 */
    @SerialName("messages") val messages: List<GroupEssenceMessage>,
    /** 是否已到最后一页 */
    @SerialName("is_end") val isEnd: Boolean,
)

/** 设置群精华消息 API 请求参数 */
@Serializable
data class SetGroupEssenceMessageInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
    /** 是否设置为精华消息，`false` 表示取消精华 */
    @SerialName("is_set") @LiteralDefault("true") val isSet: Boolean = true,
)

/** 设置群精华消息 API 响应数据 */
typealias SetGroupEssenceMessageOutput = ApiEmptyStruct

/** 退出群 API 请求参数 */
@Serializable
data class QuitGroupInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
)

/** 退出群 API 响应数据 */
typealias QuitGroupOutput = ApiEmptyStruct

/** 发送群消息表情回应 API 请求参数 */
@Serializable
data class SendGroupMessageReactionInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 要回应的消息序列号 */
    @SerialName("message_seq") val messageSeq: Long,
    /** 发送的回应的表情 ID */
    @SerialName("reaction") val reaction: String,
    /**
     * 发送的回应类型
     * @since 1.2
     */
    @SerialName("reaction_type") @LiteralDefault("\"face\"") val reactionType: String = "face",
    /** 是否添加表情，`false` 表示取消 */
    @SerialName("is_add") @LiteralDefault("true") val isAdd: Boolean = true,
)

/** 发送群消息表情回应 API 响应数据 */
typealias SendGroupMessageReactionOutput = ApiEmptyStruct

/** 发送群戳一戳 API 请求参数 */
@Serializable
data class SendGroupNudgeInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 被戳的群成员 QQ 号 */
    @SerialName("user_id") val userId: Long,
)

/** 发送群戳一戳 API 响应数据 */
typealias SendGroupNudgeOutput = ApiEmptyStruct

/** 获取群通知列表 API 请求参数 */
@Serializable
data class GetGroupNotificationsInput(
    /** 起始通知序列号 */
    @SerialName("start_notification_seq") val startNotificationSeq: Long? = null,
    /** `true` 表示只获取被过滤（由风险账号发起）的通知，`false` 表示只获取未被过滤的通知 */
    @SerialName("is_filtered") @LiteralDefault("false") val isFiltered: Boolean = false,
    /** 获取的最大通知数量 */
    @SerialName("limit") @LiteralDefault("20") val limit: Int = 20,
)

/** 获取群通知列表 API 响应数据 */
@Serializable
data class GetGroupNotificationsOutput(
    /** 获取到的群通知（notification_seq 降序排列），序列号不一定连续 */
    @Serializable(with = DropBadElementListSerializer::class)
    @SerialName("notifications") val notifications: List<GroupNotification>,
    /** 下一页起始通知序列号 */
    @SerialName("next_notification_seq") val nextNotificationSeq: Long? = null,
)

/** 同意入群/邀请他人入群请求 API 请求参数 */
@Serializable
data class AcceptGroupRequestInput(
    /** 请求对应的通知序列号 */
    @SerialName("notification_seq") val notificationSeq: Long,
    /** 请求对应的通知类型 */
    @SerialName("notification_type") val notificationType: String,
    /** 请求所在的群号 */
    @SerialName("group_id") val groupId: Long,
    /** 是否是被过滤的请求 */
    @SerialName("is_filtered") @LiteralDefault("false") val isFiltered: Boolean = false,
)

/** 同意入群/邀请他人入群请求 API 响应数据 */
typealias AcceptGroupRequestOutput = ApiEmptyStruct

/** 拒绝入群/邀请他人入群请求 API 请求参数 */
@Serializable
data class RejectGroupRequestInput(
    /** 请求对应的通知序列号 */
    @SerialName("notification_seq") val notificationSeq: Long,
    /** 请求对应的通知类型 */
    @SerialName("notification_type") val notificationType: String,
    /** 请求所在的群号 */
    @SerialName("group_id") val groupId: Long,
    /** 是否是被过滤的请求 */
    @SerialName("is_filtered") @LiteralDefault("false") val isFiltered: Boolean = false,
    /** 拒绝理由 */
    @SerialName("reason") val reason: String? = null,
)

/** 拒绝入群/邀请他人入群请求 API 响应数据 */
typealias RejectGroupRequestOutput = ApiEmptyStruct

/** 同意他人邀请自身入群 API 请求参数 */
@Serializable
data class AcceptGroupInvitationInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 邀请序列号 */
    @SerialName("invitation_seq") val invitationSeq: Long,
)

/** 同意他人邀请自身入群 API 响应数据 */
typealias AcceptGroupInvitationOutput = ApiEmptyStruct

/** 拒绝他人邀请自身入群 API 请求参数 */
@Serializable
data class RejectGroupInvitationInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 邀请序列号 */
    @SerialName("invitation_seq") val invitationSeq: Long,
)

/** 拒绝他人邀请自身入群 API 响应数据 */
typealias RejectGroupInvitationOutput = ApiEmptyStruct

// ---- 文件 API ----

/** 上传私聊文件 API 请求参数 */
@Serializable
data class UploadPrivateFileInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
    @SerialName("file_uri") val fileUri: String,
    /** 文件名称 */
    @SerialName("file_name") val fileName: String,
)

/** 上传私聊文件 API 响应数据 */
@Serializable
data class UploadPrivateFileOutput(
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
)

/** 上传群文件 API 请求参数 */
@Serializable
data class UploadGroupFileInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 目标文件夹 ID */
    @SerialName("parent_folder_id") @LiteralDefault("\"/\"") val parentFolderId: String = "/",
    /** 文件 URI，支持 `file://` `http(s)://` `base64://` 三种格式 */
    @SerialName("file_uri") val fileUri: String,
    /** 文件名称 */
    @SerialName("file_name") val fileName: String,
)

/** 上传群文件 API 响应数据 */
@Serializable
data class UploadGroupFileOutput(
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
)

/** 获取私聊文件下载链接 API 请求参数 */
@Serializable
data class GetPrivateFileDownloadUrlInput(
    /** 好友 QQ 号 */
    @SerialName("user_id") val userId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
    /** 文件的 TriSHA1 哈希值 */
    @SerialName("file_hash") val fileHash: String,
)

/** 获取私聊文件下载链接 API 响应数据 */
@Serializable
data class GetPrivateFileDownloadUrlOutput(
    /** 文件下载链接 */
    @SerialName("download_url") val downloadUrl: String,
)

/** 获取群文件下载链接 API 请求参数 */
@Serializable
data class GetGroupFileDownloadUrlInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
)

/** 获取群文件下载链接 API 响应数据 */
@Serializable
data class GetGroupFileDownloadUrlOutput(
    /** 文件下载链接 */
    @SerialName("download_url") val downloadUrl: String,
)

/** 获取群文件列表 API 请求参数 */
@Serializable
data class GetGroupFilesInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 父文件夹 ID */
    @SerialName("parent_folder_id") @LiteralDefault("\"/\"") val parentFolderId: String = "/",
)

/** 获取群文件列表 API 响应数据 */
@Serializable
data class GetGroupFilesOutput(
    /** 文件列表 */
    @SerialName("files") val files: List<GroupFileEntity>,
    /** 文件夹列表 */
    @SerialName("folders") val folders: List<GroupFolderEntity>,
)

/** 移动群文件 API 请求参数 */
@Serializable
data class MoveGroupFileInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
    /** 文件所在的文件夹 ID */
    @SerialName("parent_folder_id") @LiteralDefault("\"/\"") val parentFolderId: String = "/",
    /** 目标文件夹 ID */
    @SerialName("target_folder_id") @LiteralDefault("\"/\"") val targetFolderId: String = "/",
)

/** 移动群文件 API 响应数据 */
typealias MoveGroupFileOutput = ApiEmptyStruct

/** 重命名群文件 API 请求参数 */
@Serializable
data class RenameGroupFileInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
    /** 文件所在的文件夹 ID */
    @SerialName("parent_folder_id") @LiteralDefault("\"/\"") val parentFolderId: String = "/",
    /** 新文件名称 */
    @SerialName("new_file_name") val newFileName: String,
)

/** 重命名群文件 API 响应数据 */
typealias RenameGroupFileOutput = ApiEmptyStruct

/** 删除群文件 API 请求参数 */
@Serializable
data class DeleteGroupFileInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
)

/** 删除群文件 API 响应数据 */
typealias DeleteGroupFileOutput = ApiEmptyStruct

/**
 * 转存群文件为永久文件 API 请求参数
 * @since 1.3
 */
@Serializable
data class PersistGroupFileInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件 ID */
    @SerialName("file_id") val fileId: String,
)

/**
 * 转存群文件为永久文件 API 响应数据
 * @since 1.3
 */
typealias PersistGroupFileOutput = ApiEmptyStruct

/** 创建群文件夹 API 请求参数 */
@Serializable
data class CreateGroupFolderInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件夹名称 */
    @SerialName("folder_name") val folderName: String,
)

/** 创建群文件夹 API 响应数据 */
@Serializable
data class CreateGroupFolderOutput(
    /** 文件夹 ID */
    @SerialName("folder_id") val folderId: String,
)

/** 重命名群文件夹 API 请求参数 */
@Serializable
data class RenameGroupFolderInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件夹 ID */
    @SerialName("folder_id") val folderId: String,
    /** 新文件夹名 */
    @SerialName("new_folder_name") val newFolderName: String,
)

/** 重命名群文件夹 API 响应数据 */
typealias RenameGroupFolderOutput = ApiEmptyStruct

/** 删除群文件夹 API 请求参数 */
@Serializable
data class DeleteGroupFolderInput(
    /** 群号 */
    @SerialName("group_id") val groupId: Long,
    /** 文件夹 ID */
    @SerialName("folder_id") val folderId: String,
)

/** 删除群文件夹 API 响应数据 */
typealias DeleteGroupFolderOutput = ApiEmptyStruct

// ####################################
// API Endpoint Constants
// ####################################

sealed class ApiEndpoint<T : Any, R : Any>(val path: String) {
    /** 获取登录信息 */
    object GetLoginInfo : ApiEndpoint<GetLoginInfoInput, GetLoginInfoOutput>("/get_login_info")
    /** 获取协议端信息 */
    object GetImplInfo : ApiEndpoint<GetImplInfoInput, GetImplInfoOutput>("/get_impl_info")
    /** 获取用户个人信息 */
    object GetUserProfile : ApiEndpoint<GetUserProfileInput, GetUserProfileOutput>("/get_user_profile")
    /** 获取好友列表 */
    object GetFriendList : ApiEndpoint<GetFriendListInput, GetFriendListOutput>("/get_friend_list")
    /** 获取好友信息 */
    object GetFriendInfo : ApiEndpoint<GetFriendInfoInput, GetFriendInfoOutput>("/get_friend_info")
    /** 获取群列表 */
    object GetGroupList : ApiEndpoint<GetGroupListInput, GetGroupListOutput>("/get_group_list")
    /** 获取群信息 */
    object GetGroupInfo : ApiEndpoint<GetGroupInfoInput, GetGroupInfoOutput>("/get_group_info")
    /** 获取群成员列表 */
    object GetGroupMemberList : ApiEndpoint<GetGroupMemberListInput, GetGroupMemberListOutput>("/get_group_member_list")
    /** 获取群成员信息 */
    object GetGroupMemberInfo : ApiEndpoint<GetGroupMemberInfoInput, GetGroupMemberInfoOutput>("/get_group_member_info")
    /**
     * 获取置顶的好友和群列表
     * @since 1.2
     */
    object GetPeerPins : ApiEndpoint<GetPeerPinsInput, GetPeerPinsOutput>("/get_peer_pins")
    /**
     * 设置好友或群的置顶状态
     * @since 1.2
     */
    object SetPeerPin : ApiEndpoint<SetPeerPinInput, SetPeerPinOutput>("/set_peer_pin")
    /**
     * 设置 QQ 账号头像
     * @since 1.1
     */
    object SetAvatar : ApiEndpoint<SetAvatarInput, SetAvatarOutput>("/set_avatar")
    /**
     * 设置 QQ 账号昵称
     * @since 1.1
     */
    object SetNickname : ApiEndpoint<SetNicknameInput, SetNicknameOutput>("/set_nickname")
    /**
     * 设置 QQ 账号个性签名
     * @since 1.1
     */
    object SetBio : ApiEndpoint<SetBioInput, SetBioOutput>("/set_bio")
    /**
     * 获取自定义表情 URL 列表
     * @since 1.1
     */
    object GetCustomFaceUrlList : ApiEndpoint<GetCustomFaceUrlListInput, GetCustomFaceUrlListOutput>("/get_custom_face_url_list")
    /** 获取 Cookies */
    object GetCookies : ApiEndpoint<GetCookiesInput, GetCookiesOutput>("/get_cookies")
    /** 获取 CSRF Token */
    object GetCsrfToken : ApiEndpoint<GetCsrfTokenInput, GetCsrfTokenOutput>("/get_csrf_token")
    /** 发送私聊消息 */
    object SendPrivateMessage : ApiEndpoint<SendPrivateMessageInput, SendPrivateMessageOutput>("/send_private_message")
    /** 发送群聊消息 */
    object SendGroupMessage : ApiEndpoint<SendGroupMessageInput, SendGroupMessageOutput>("/send_group_message")
    /** 撤回私聊消息 */
    object RecallPrivateMessage : ApiEndpoint<RecallPrivateMessageInput, RecallPrivateMessageOutput>("/recall_private_message")
    /** 撤回群聊消息 */
    object RecallGroupMessage : ApiEndpoint<RecallGroupMessageInput, RecallGroupMessageOutput>("/recall_group_message")
    /** 获取消息 */
    object GetMessage : ApiEndpoint<GetMessageInput, GetMessageOutput>("/get_message")
    /** 获取历史消息列表 */
    object GetHistoryMessages : ApiEndpoint<GetHistoryMessagesInput, GetHistoryMessagesOutput>("/get_history_messages")
    /** 获取临时资源链接 */
    object GetResourceTempUrl : ApiEndpoint<GetResourceTempUrlInput, GetResourceTempUrlOutput>("/get_resource_temp_url")
    /** 获取合并转发消息内容 */
    object GetForwardedMessages : ApiEndpoint<GetForwardedMessagesInput, GetForwardedMessagesOutput>("/get_forwarded_messages")
    /** 标记消息为已读 */
    object MarkMessageAsRead : ApiEndpoint<MarkMessageAsReadInput, MarkMessageAsReadOutput>("/mark_message_as_read")
    /** 发送好友戳一戳 */
    object SendFriendNudge : ApiEndpoint<SendFriendNudgeInput, SendFriendNudgeOutput>("/send_friend_nudge")
    /** 发送名片点赞 */
    object SendProfileLike : ApiEndpoint<SendProfileLikeInput, SendProfileLikeOutput>("/send_profile_like")
    /**
     * 删除好友
     * @since 1.1
     */
    object DeleteFriend : ApiEndpoint<DeleteFriendInput, DeleteFriendOutput>("/delete_friend")
    /** 获取好友请求列表 */
    object GetFriendRequests : ApiEndpoint<GetFriendRequestsInput, GetFriendRequestsOutput>("/get_friend_requests")
    /** 同意好友请求 */
    object AcceptFriendRequest : ApiEndpoint<AcceptFriendRequestInput, AcceptFriendRequestOutput>("/accept_friend_request")
    /** 拒绝好友请求 */
    object RejectFriendRequest : ApiEndpoint<RejectFriendRequestInput, RejectFriendRequestOutput>("/reject_friend_request")
    /** 设置群名称 */
    object SetGroupName : ApiEndpoint<SetGroupNameInput, SetGroupNameOutput>("/set_group_name")
    /** 设置群头像 */
    object SetGroupAvatar : ApiEndpoint<SetGroupAvatarInput, SetGroupAvatarOutput>("/set_group_avatar")
    /** 设置群名片 */
    object SetGroupMemberCard : ApiEndpoint<SetGroupMemberCardInput, SetGroupMemberCardOutput>("/set_group_member_card")
    /** 设置群成员专属头衔 */
    object SetGroupMemberSpecialTitle : ApiEndpoint<SetGroupMemberSpecialTitleInput, SetGroupMemberSpecialTitleOutput>("/set_group_member_special_title")
    /** 设置群管理员 */
    object SetGroupMemberAdmin : ApiEndpoint<SetGroupMemberAdminInput, SetGroupMemberAdminOutput>("/set_group_member_admin")
    /** 设置群成员禁言 */
    object SetGroupMemberMute : ApiEndpoint<SetGroupMemberMuteInput, SetGroupMemberMuteOutput>("/set_group_member_mute")
    /** 设置群全员禁言 */
    object SetGroupWholeMute : ApiEndpoint<SetGroupWholeMuteInput, SetGroupWholeMuteOutput>("/set_group_whole_mute")
    /** 踢出群成员 */
    object KickGroupMember : ApiEndpoint<KickGroupMemberInput, KickGroupMemberOutput>("/kick_group_member")
    /** 获取群公告列表 */
    object GetGroupAnnouncements : ApiEndpoint<GetGroupAnnouncementsInput, GetGroupAnnouncementsOutput>("/get_group_announcements")
    /** 发送群公告 */
    object SendGroupAnnouncement : ApiEndpoint<SendGroupAnnouncementInput, SendGroupAnnouncementOutput>("/send_group_announcement")
    /** 删除群公告 */
    object DeleteGroupAnnouncement : ApiEndpoint<DeleteGroupAnnouncementInput, DeleteGroupAnnouncementOutput>("/delete_group_announcement")
    /** 获取群精华消息列表 */
    object GetGroupEssenceMessages : ApiEndpoint<GetGroupEssenceMessagesInput, GetGroupEssenceMessagesOutput>("/get_group_essence_messages")
    /** 设置群精华消息 */
    object SetGroupEssenceMessage : ApiEndpoint<SetGroupEssenceMessageInput, SetGroupEssenceMessageOutput>("/set_group_essence_message")
    /** 退出群 */
    object QuitGroup : ApiEndpoint<QuitGroupInput, QuitGroupOutput>("/quit_group")
    /** 发送群消息表情回应 */
    object SendGroupMessageReaction : ApiEndpoint<SendGroupMessageReactionInput, SendGroupMessageReactionOutput>("/send_group_message_reaction")
    /** 发送群戳一戳 */
    object SendGroupNudge : ApiEndpoint<SendGroupNudgeInput, SendGroupNudgeOutput>("/send_group_nudge")
    /** 获取群通知列表 */
    object GetGroupNotifications : ApiEndpoint<GetGroupNotificationsInput, GetGroupNotificationsOutput>("/get_group_notifications")
    /** 同意入群/邀请他人入群请求 */
    object AcceptGroupRequest : ApiEndpoint<AcceptGroupRequestInput, AcceptGroupRequestOutput>("/accept_group_request")
    /** 拒绝入群/邀请他人入群请求 */
    object RejectGroupRequest : ApiEndpoint<RejectGroupRequestInput, RejectGroupRequestOutput>("/reject_group_request")
    /** 同意他人邀请自身入群 */
    object AcceptGroupInvitation : ApiEndpoint<AcceptGroupInvitationInput, AcceptGroupInvitationOutput>("/accept_group_invitation")
    /** 拒绝他人邀请自身入群 */
    object RejectGroupInvitation : ApiEndpoint<RejectGroupInvitationInput, RejectGroupInvitationOutput>("/reject_group_invitation")
    /** 上传私聊文件 */
    object UploadPrivateFile : ApiEndpoint<UploadPrivateFileInput, UploadPrivateFileOutput>("/upload_private_file")
    /** 上传群文件 */
    object UploadGroupFile : ApiEndpoint<UploadGroupFileInput, UploadGroupFileOutput>("/upload_group_file")
    /** 获取私聊文件下载链接 */
    object GetPrivateFileDownloadUrl : ApiEndpoint<GetPrivateFileDownloadUrlInput, GetPrivateFileDownloadUrlOutput>("/get_private_file_download_url")
    /** 获取群文件下载链接 */
    object GetGroupFileDownloadUrl : ApiEndpoint<GetGroupFileDownloadUrlInput, GetGroupFileDownloadUrlOutput>("/get_group_file_download_url")
    /** 获取群文件列表 */
    object GetGroupFiles : ApiEndpoint<GetGroupFilesInput, GetGroupFilesOutput>("/get_group_files")
    /** 移动群文件 */
    object MoveGroupFile : ApiEndpoint<MoveGroupFileInput, MoveGroupFileOutput>("/move_group_file")
    /** 重命名群文件 */
    object RenameGroupFile : ApiEndpoint<RenameGroupFileInput, RenameGroupFileOutput>("/rename_group_file")
    /** 删除群文件 */
    object DeleteGroupFile : ApiEndpoint<DeleteGroupFileInput, DeleteGroupFileOutput>("/delete_group_file")
    /**
     * 转存群文件为永久文件
     * @since 1.3
     */
    object PersistGroupFile : ApiEndpoint<PersistGroupFileInput, PersistGroupFileOutput>("/persist_group_file")
    /** 创建群文件夹 */
    object CreateGroupFolder : ApiEndpoint<CreateGroupFolderInput, CreateGroupFolderOutput>("/create_group_folder")
    /** 重命名群文件夹 */
    object RenameGroupFolder : ApiEndpoint<RenameGroupFolderInput, RenameGroupFolderOutput>("/rename_group_folder")
    /** 删除群文件夹 */
    object DeleteGroupFolder : ApiEndpoint<DeleteGroupFolderInput, DeleteGroupFolderOutput>("/delete_group_folder")
}