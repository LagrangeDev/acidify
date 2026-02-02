package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class RichMsg(
    @ProtoNumber(1) val bytesTemplate1: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val serviceId: Int = 0,
    @ProtoNumber(3) val bytesMsgResid: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val rand: Int = 0,
    @ProtoNumber(5) val seq: Int = 0,
    @ProtoNumber(6) val flags: Int = 0,
)
