package org.ntqqrev.acidify.internal.json.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal class OutgoingForwardPayload(
    val config: JsonObject,
    val desc: String,
    val extra: String,
    val meta: JsonObject,
    val prompt: String,
    val ver: String,
    val view: String,
) : LightAppPayload("com.tencent.multimsg")