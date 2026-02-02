package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchUserInfoByUinReq(
    @ProtoNumber(1) val uin: Long = 0L,
    @ProtoNumber(3) val keys: List<FetchUserInfoReqKey> = emptyList(),
)

@Serializable
internal class FetchUserInfoByUidReq(
    @ProtoNumber(1) val uid: String = "",
    @ProtoNumber(3) val keys: List<FetchUserInfoReqKey> = emptyList(),
)

@Serializable
internal class FetchUserInfoReqKey(
    @ProtoNumber(1) val key: Int = 0,
)

@Serializable
internal class FetchUserInfoResp(
    @ProtoNumber(1) val body: Body = Body(),
) {
    @Serializable
    internal class Body(
        @ProtoNumber(2) val properties: Properties = Properties(),
        @ProtoNumber(3) val uin: Long = 0L,
    ) {
        @Serializable
        internal class Properties(
            @ProtoNumber(1) val numberProps: Map<Int, Int> = emptyMap(),
            @ProtoNumber(2) val stringProps: Map<Int, String> = emptyMap(),
        )
    }
}
