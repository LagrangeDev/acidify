package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetGroupEssenceMessageReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
    @ProtoNumber(2) val sequence: Long = 0L,
    @ProtoNumber(3) val random: Int = 0,
)
