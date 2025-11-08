package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群精华消息变更事件
 * @property groupUin 群号
 * @property messageSeq 发生变更的消息序列号
 * @property isSet 是否被设置为精华，`false` 表示被取消精华
 */
@JsExport
class GroupEssenceMessageChangeEvent(
    val groupUin: Long,
    val messageSeq: Long,
    val isSet: Boolean
) : AcidifyEvent

