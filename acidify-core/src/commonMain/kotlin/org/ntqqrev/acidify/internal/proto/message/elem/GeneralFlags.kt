package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GeneralFlags(
    @ProtoNumber(1) val bubbleDiyTextId: Int = 0,
    @ProtoNumber(2) val groupFlagNew: Int = 0,
    @ProtoNumber(3) val uin: Long = 0L,
    @ProtoNumber(4) val rpId: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val prpFold: Int = 0,
    @ProtoNumber(6) val longTextFlag: Int = 0,
    @ProtoNumber(7) val longTextResId: String = "",
    @ProtoNumber(8) val groupType: Int = 0,
    @ProtoNumber(9) val toUinFlag: Int = 0,
    @ProtoNumber(10) val glamourLevel: Int = 0,
    @ProtoNumber(11) val memberLevel: Int = 0,
    @ProtoNumber(12) val groupRankSeq: Long = 0L,
    @ProtoNumber(13) val olympicTorch: Int = 0,
    @ProtoNumber(14) val babyQGuideMsgCookie: ByteArray = byteArrayOf(),
    @ProtoNumber(15) val uin32ExpertFlag: Int = 0,
    @ProtoNumber(16) val bubbleSubId: Int = 0,
    @ProtoNumber(17) val pendantId: Long = 0L,
    @ProtoNumber(18) val rpIndex: ByteArray = byteArrayOf(),
    @ProtoNumber(19) val pbReserve: ByteArray = byteArrayOf(),
)
