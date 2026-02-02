package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Attr(
    @ProtoNumber(1) val codePage: Int = 0,
    @ProtoNumber(2) val time: Int = 0,
    @ProtoNumber(3) val playModeRandom: Int = 0,
    @ProtoNumber(4) val color: Int = 0,
    @ProtoNumber(5) val size: Int = 0,
    @ProtoNumber(6) val tabEffect: Int = 0,
    @ProtoNumber(7) val charSet: Int = 0,
    @ProtoNumber(8) val pitchAndFamily: Int = 0,
    @ProtoNumber(9) val fontName: String = "",
    @ProtoNumber(10) val reserveData: ByteArray = byteArrayOf(),
)
