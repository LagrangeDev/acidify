package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class ExtraInfo(
    @ProtoNumber(1) val nick: String = "",
    @ProtoNumber(2) val groupCard: String = "",
    @ProtoNumber(3) val level: Int = 0,
    @ProtoNumber(4) val flags: Int = 0,
    @ProtoNumber(5) val groupMask: Int = 0,
    @ProtoNumber(6) val msgTailId: Int = 0,
    @ProtoNumber(7) val senderTitle: String = "",
    @ProtoNumber(8) val apnsTips: String = "",
    @ProtoNumber(9) val uin: Long = 0L,
    @ProtoNumber(10) val msgStateFlag: Int = 0,
    @ProtoNumber(11) val apnsSoundType: Int = 0,
    @ProtoNumber(12) val newGroupFlag: Int = 0,
)
