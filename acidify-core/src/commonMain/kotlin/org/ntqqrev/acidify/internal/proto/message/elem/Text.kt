package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Text(
    @ProtoNumber(1) val textMsg: String = "",
    @ProtoNumber(2) val link: String = "",
    @ProtoNumber(3) val attr6Buf: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val attr7Buf: ByteArray = byteArrayOf(),
    @ProtoNumber(11) val buf: ByteArray = byteArrayOf(),
    @ProtoNumber(12) val pbReserve: ByteArray = byteArrayOf(),
)
