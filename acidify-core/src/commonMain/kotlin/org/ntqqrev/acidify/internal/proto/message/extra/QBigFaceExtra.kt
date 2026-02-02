package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class QBigFaceExtra(
    @ProtoNumber(1) val aniStickerPackId: String = "",
    @ProtoNumber(2) val aniStickerId: String = "",
    @ProtoNumber(3) val faceId: Int = 0,
    @ProtoNumber(4) val field4: Int = 0,
    @ProtoNumber(5) val aniStickerType: Int = 0,
    @ProtoNumber(6) val field6: String = "",
    @ProtoNumber(7) val preview: String = "",
    @ProtoNumber(9) val field9: Int = 0,
)
