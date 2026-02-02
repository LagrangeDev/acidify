package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Oidb(
    @ProtoNumber(1) val command: Int = 0,
    @ProtoNumber(2) val service: Int = 0,
    @ProtoNumber(3) val result: Int = 0,
    @ProtoNumber(4) val body: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val message: String = "",
    @ProtoNumber(12) val reserved: Boolean = false,
)
