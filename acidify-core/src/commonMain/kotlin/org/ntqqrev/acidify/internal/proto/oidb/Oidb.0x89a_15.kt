package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetGroupNameReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
    @ProtoNumber(2) val targetName: String = "",
)
