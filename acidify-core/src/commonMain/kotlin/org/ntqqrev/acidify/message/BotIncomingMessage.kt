package org.ntqqrev.acidify.message

import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import kotlin.js.JsExport

/**
 * 接收消息
 * @property scene 消息场景
 * @property peerUin 消息来源的 uin。
 * 对于[好友消息][MessageScene.FRIEND]，为好友的 QQ 号；
 * 对于[群聊消息][MessageScene.GROUP]，为群号。
 * @property peerUid 消息来源的 uid。
 * 对于[好友消息][MessageScene.FRIEND]，为好友的 uid；
 * 对于[群聊消息][MessageScene.GROUP]，为群号经过 [Long.toString] 的结果。
 * @property sequence 消息序列号
 * @property timestamp 消息发送的 Unix 事件戳（秒）
 * @property senderUin 发送者的 QQ 号
 * @property senderUid 发送者的 uid
 * @property random 透传的随机数
 * @property clientSequence 透传的客户端序列号
 * @property messageUid 消息的全局唯一 ID
 * @property segments 消息内容
 * @property extraInfo 群消息的附加信息，可用于刷新群昵称、群头衔等
 */
@JsExport
class BotIncomingMessage internal constructor(
    val scene: MessageScene,
    val peerUin: Long,
    val peerUid: String,
    val sequence: Long,
    val timestamp: Long,
    val senderUin: Long,
    val senderUid: String,
    val clientSequence: Long,
    val random: Int,
    val messageUid: Long,
    internal val raw: CommonMessage,
) {
    lateinit var segments: List<BotIncomingSegment>
        internal set

    var extraInfo: ExtraInfo? = null
        internal set

    /**
     * @property nick 发送者的昵称
     * @property groupCard 发送者的群名片
     * @property specialTitle 发送者的群头衔
     */
    class ExtraInfo internal constructor(
        val nick: String,
        val groupCard: String,
        val specialTitle: String
    )
}
