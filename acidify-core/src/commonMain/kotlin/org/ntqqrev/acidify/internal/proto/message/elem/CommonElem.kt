package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class CommonElem(
    @ProtoNumber(1) val serviceType: Int = 0,
    @ProtoNumber(2) val pbElem: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val businessType: Int = 0,
)
