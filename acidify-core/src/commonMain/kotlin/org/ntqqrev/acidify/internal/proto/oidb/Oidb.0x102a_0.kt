package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchPsKeyReq(
    @ProtoNumber(1) val domains: List<String> = emptyList(),
)

@Serializable
internal class FetchPsKeyResp(
    @ProtoNumber(1) val psKeyEntries: Map<String, String> = emptyMap(),
)
