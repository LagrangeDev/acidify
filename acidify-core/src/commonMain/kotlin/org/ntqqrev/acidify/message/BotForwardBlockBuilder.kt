package org.ntqqrev.acidify.message

interface BotForwardBlockBuilder {
    /**
     * 添加一个伪造合并转发消息
     * @param senderUin 该消息的发送者 QQ 号
     * @param senderName 该消息的发送者昵称
     * @param block 构建该消息的内容
     */
    fun node(senderUin: Long, senderName: String, block: suspend BotOutgoingMessageBuilder.() -> Unit)
}