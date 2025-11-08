package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * Bot 表情条目
 * @property qSid 表情 ID
 * @property qDes 表情描述
 */
@JsExport
data class BotFaceDetail(
    val qSid: String,
    val qDes: String,
    val emCode: String,
    val qCid: Int,
    val aniStickerType: Int,
    val aniStickerPackId: Int,
    val aniStickerId: Int,
    val url: String,
    val emojiNameAlias: List<String>,
    val aniStickerWidth: Int,
    val aniStickerHeight: Int,
)