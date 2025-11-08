package org.ntqqrev.acidify.message

import kotlin.js.JsExport

/**
 * 消息场景
 */
@JsExport
enum class MessageScene {
    /**
     * 好友消息
     */
    FRIEND,

    /**
     * 群聊消息
     */
    GROUP,

    /**
     * 临时会话消息
     */
    TEMP,
}