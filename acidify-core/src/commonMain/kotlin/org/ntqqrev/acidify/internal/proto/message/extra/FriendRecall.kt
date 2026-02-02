package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FriendRecall(
    @ProtoNumber(1) val body: Body? = null,
) {
    @Serializable
    internal class Body(
        @ProtoNumber(1) val fromUid: String = "",
        @ProtoNumber(2) val toUid: String = "",
        @ProtoNumber(3) val clientSequence: Int = 0,
        @ProtoNumber(13) val tipInfo: TipInfo? = null,
        @ProtoNumber(20) val sequence: Int = 0,
    ) {
        @Serializable
        internal class TipInfo(
            @ProtoNumber(2) val tip: String = "",
        )
    }
}
