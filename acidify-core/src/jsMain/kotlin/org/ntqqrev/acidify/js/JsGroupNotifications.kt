package org.ntqqrev.acidify.js

import org.ntqqrev.acidify.struct.BotGroupNotification

@JsExport
data class JsGroupNotifications(
    val notifications: Array<BotGroupNotification>,
    val nextSequence: Long?
)