package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchPsKeyReq(
    @ProtoNumber(1) val domains: List<String> = emptyList(),
)

@Serializable
internal class FetchPsKeyResp(
    @ProtoNumber(1) val psKeyEntries: List<PsKeyEntry> = emptyList(),
) {
    @Serializable
    internal class PsKeyEntry(
        @ProtoNumber(1) val domain: String = "",
        @ProtoNumber(2) val key: String = "",
    )
}
