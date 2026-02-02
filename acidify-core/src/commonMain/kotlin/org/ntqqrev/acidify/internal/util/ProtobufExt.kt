package org.ntqqrev.acidify.internal.util

import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

val protobufModule = ProtoBuf {
    // Configuration can be added here if needed
}

inline fun <reified T> ByteArray.pbDecode(): T =
    protobufModule.decodeFromByteArray(this)

inline fun <reified T> T.pbEncode(): ByteArray =
    protobufModule.encodeToByteArray(this)