package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 机器人离线事件
 * @property reason 下线原因
 */
@JsExport
class BotOfflineEvent(
    val reason: String
) : AcidifyEvent

