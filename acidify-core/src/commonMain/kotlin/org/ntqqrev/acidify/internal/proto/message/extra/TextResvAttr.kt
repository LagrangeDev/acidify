package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class TextResvAttr(
    @ProtoNumber(3) val atType: Int = 0,
    @ProtoNumber(4) val atMemberUin: Long = 0L,
    @ProtoNumber(5) val atMemberTinyid: Long = 0L,
    @ProtoNumber(9) val atMemberUid: String = "",
)
