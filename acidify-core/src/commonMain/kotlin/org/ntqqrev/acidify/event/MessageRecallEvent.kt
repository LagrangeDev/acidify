package org.ntqqrev.acidify.event

/**
 * 消息撤回事件
 * @property messageScene 消息场景
 * @property peerId 好友 QQ 号或群号
 * @property messageSeq 消息序列号
 * @property senderUin 被撤回的消息的发送者 QQ 号
 * @property senderUid 被撤回的消息的发送者 uid
 * @property operatorUin 操作者 QQ 号
 * @property operatorUid 操作者 uid
 * @property displaySuffix 撤回提示的后缀文本
 */
class MessageRecallEvent(
    val messageScene: String,
    val peerId: Long,
    val messageSeq: Long,
    val senderUin: Long,
    val senderUid: String,
    val operatorUin: Long,
    val operatorUid: String,
    val displaySuffix: String
) : AcidifyEvent