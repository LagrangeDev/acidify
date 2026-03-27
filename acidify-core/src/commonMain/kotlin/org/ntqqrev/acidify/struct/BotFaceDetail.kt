package org.ntqqrev.acidify.struct

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Bot 表情条目
 * @property qSid 表情 ID
 * @property qDes 表情描述
 */
@JsExport
@Serializable
data class BotFaceDetail internal constructor(
    val qSid: String,
    val qDes: String,
    val emCode: String,
    val qCid: Int,
    val aniStickerType: Int,
    val aniStickerPackId: Int,
    val aniStickerId: Int,
    val baseUrl: String,
    val advUrl: String,
    val emojiNameAlias: List<String>,
    val aniStickerWidth: Int,
    val aniStickerHeight: Int,
)