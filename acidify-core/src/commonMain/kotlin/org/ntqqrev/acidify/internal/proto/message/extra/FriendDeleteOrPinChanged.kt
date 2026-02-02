package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FriendDeleteOrPinChanged(
    @ProtoNumber(1) val body: Body = Body(),
) {
    @Serializable
    internal class Body(
        @ProtoNumber(2) val type: Int = 0,
        @ProtoNumber(20) val pinChanged: PinChanged? = null,
    )

    @Serializable
    internal class PinChanged(
        @ProtoNumber(1) val body: PinChangedBody = PinChangedBody(),
    ) {
        @Serializable
        internal class PinChangedBody(
            @ProtoNumber(1) val uid: String = "",
            @ProtoNumber(2) val groupUin: Long? = null,
            @ProtoNumber(400) val info: PinChangedInfo = PinChangedInfo(),
        )
    }

    @Serializable
    internal class PinChangedInfo(
        @ProtoNumber(2) val timestamp: ByteArray = byteArrayOf(),
    )
}
