package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class PokeReq(
    @ProtoNumber(1) val targetUin: Long = 0L,
    @ProtoNumber(2) val groupUin: Long = 0L,
    @ProtoNumber(5) val friendUin: Long = 0L,
    @ProtoNumber(6) val ext: Int = 0,
)
