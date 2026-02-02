package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class KickMemberReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
    @ProtoNumber(2) val kickFlag: Int = 0,
    @ProtoNumber(3) val targetUid: String = "",
    @ProtoNumber(4) val rejectAddRequest: Boolean = false,
    @ProtoNumber(5) val reason: String = "",
)
