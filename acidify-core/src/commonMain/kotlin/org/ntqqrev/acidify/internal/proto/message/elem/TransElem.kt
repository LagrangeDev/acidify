package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class TransElem(
    @ProtoNumber(1) val elemType: Int = 0,
    @ProtoNumber(2) val elemValue: ByteArray = byteArrayOf(),
)
