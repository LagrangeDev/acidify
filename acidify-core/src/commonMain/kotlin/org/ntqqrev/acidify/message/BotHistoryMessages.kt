package org.ntqqrev.acidify.message

/**
 * 获取历史消息的结果
 * @param messages 消息列表
 * @param nextStartSequence 下一次获取历史消息时的起始序列号
 */
class BotHistoryMessages(
    val messages: List<BotIncomingMessage>,
    val nextStartSequence: Long? = null
)