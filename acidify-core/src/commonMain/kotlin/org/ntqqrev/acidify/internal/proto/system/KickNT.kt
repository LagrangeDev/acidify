package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class KickNT(
    @ProtoNumber(3) val tip: String = "",
    @ProtoNumber(4) val title: String = "",
)
