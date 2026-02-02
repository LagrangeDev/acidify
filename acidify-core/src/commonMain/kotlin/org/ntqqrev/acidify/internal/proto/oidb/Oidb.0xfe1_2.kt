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
            @ProtoNumber(1) val numberProps: List<NumberProp> = emptyList(),
            @ProtoNumber(2) val stringProps: List<StringProp> = emptyList(),
        ) {
            @Serializable
            internal class NumberProp(
                @ProtoNumber(1) val key: Int = 0,
                @ProtoNumber(2) val value: Int = 0,
            )

            @Serializable
            internal class StringProp(
                @ProtoNumber(1) val key: Int = 0,
                @ProtoNumber(2) val value: String = "",
            )
        }
    }
}
