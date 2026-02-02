package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class ContentHead(
    @ProtoNumber(1) val type: Int = 0,
    @ProtoNumber(2) val subType: Int = 0,
    @ProtoNumber(3) val c2CCommand: Int = 0,
    @ProtoNumber(4) val random: Int = 0,
    @ProtoNumber(5) val sequence: Long = 0L,
    @ProtoNumber(6) val time: Long = 0L,
    @ProtoNumber(7) val pkgNum: Int = 0,
    @ProtoNumber(8) val pkgIndex: Int = 0,
    @ProtoNumber(9) val divSeq: Int = 0,
    @ProtoNumber(10) val autoReply: Int = 0,
    @ProtoNumber(11) val clientSequence: Long = 0L,
    @ProtoNumber(12) val msgUid: Long = 0L,
    @ProtoNumber(15) val forwardExt: Forward = Forward(),
) {
    @Serializable
    internal class Forward(
        @ProtoNumber(1) val field1: Int = 0,
        @ProtoNumber(2) val field2: Int = 0,
        @ProtoNumber(3) val field3: Int = 0,
        @ProtoNumber(4) val unknownBase64: String = "",
        @ProtoNumber(5) val avatar: String = "",
    )
}
