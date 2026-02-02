package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class QuitGroupReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
)
