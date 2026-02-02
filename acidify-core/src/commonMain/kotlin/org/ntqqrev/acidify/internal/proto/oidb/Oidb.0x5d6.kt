package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetFriendPinReq(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val info: Info = Info(),
    @ProtoNumber(3) val field3: Int = 0,
) {
    @Serializable
    internal class Info(
        @ProtoNumber(1) val friendUid: String = "",
        @ProtoNumber(400) val field400: Field400 = Field400(),
    ) {
        @Serializable
        internal class Field400(
            @ProtoNumber(1) val field1: Int = 0,
            @ProtoNumber(2) val timestamp: ByteArray = byteArrayOf(),
        )
    }
}

@Serializable
internal class SetGroupPinReq(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val info: Info = Info(),
    @ProtoNumber(3) val field3: Int = 0,
) {
    @Serializable
    internal class Info(
        @ProtoNumber(2) val groupUin: Long = 0L,
        @ProtoNumber(400) val field400: Field400 = Field400(),
    ) {
        @Serializable
        internal class Field400(
            @ProtoNumber(1) val field1: Int = 0,
            @ProtoNumber(2) val timestamp: ByteArray = byteArrayOf(),
        )
    }
}
