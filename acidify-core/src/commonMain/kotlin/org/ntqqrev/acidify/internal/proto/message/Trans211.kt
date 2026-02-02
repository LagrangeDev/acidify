package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Trans211(
    @ProtoNumber(1) val toUin: Long = 0L,
    @ProtoNumber(2) val ccCmd: Int = 0,
    @ProtoNumber(8) val uid: String = "",
)
