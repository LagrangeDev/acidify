package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchClientKeyResp(
    @ProtoNumber(3) val clientKey: String = "",
    @ProtoNumber(4) val expireTime: Long = 0L,
)
