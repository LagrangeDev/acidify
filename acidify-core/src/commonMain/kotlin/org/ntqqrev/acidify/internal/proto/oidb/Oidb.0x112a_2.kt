package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetUserProfileReq(
    @ProtoNumber(1) val uin: Long = 0L,
    @ProtoNumber(2) val stringProps: List<StringProp> = emptyList(),
    @ProtoNumber(3) val numberProps: List<NumberProp> = emptyList(),
) {
    @Serializable
    internal class StringProp(
        @ProtoNumber(1) val key: Int = 0,
        @ProtoNumber(2) val value: String = "",
    )

    @Serializable
    internal class NumberProp(
        @ProtoNumber(1) val key: Int = 0,
        @ProtoNumber(2) val value: Int = 0,
    )
}
