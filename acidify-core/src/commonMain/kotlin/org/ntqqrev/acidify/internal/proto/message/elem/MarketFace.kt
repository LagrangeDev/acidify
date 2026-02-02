package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class MarketFace(
    @ProtoNumber(1) val summary: String = "",
    @ProtoNumber(2) val itemType: Int = 0,
    @ProtoNumber(3) val info: Int = 0,
    @ProtoNumber(4) val faceId: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val tabId: Int = 0,
    @ProtoNumber(6) val subType: Int = 0,
    @ProtoNumber(7) val key: String = "",
    @ProtoNumber(10) val width: Int = 0,
    @ProtoNumber(11) val height: Int = 0,
    @ProtoNumber(13) val pbReserve: PbReserve = PbReserve(),
) {
    @Serializable
    internal class PbReserve(
        @ProtoNumber(8) val field8: Int = 0,
    )
}
