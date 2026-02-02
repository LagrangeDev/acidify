package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetGroupMessageReactionReq(
    @ProtoNumber(2) val groupCode: Long = 0L,
    @ProtoNumber(3) val sequence: Long = 0L,
    @ProtoNumber(4) val code: String = "",
    @ProtoNumber(5) val type: Int = 0,
)
