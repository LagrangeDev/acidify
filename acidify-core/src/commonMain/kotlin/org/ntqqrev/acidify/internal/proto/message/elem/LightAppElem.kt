package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class LightAppElem(
    @ProtoNumber(1) val bytesData: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val bytesMsgResid: ByteArray = byteArrayOf(),
)
