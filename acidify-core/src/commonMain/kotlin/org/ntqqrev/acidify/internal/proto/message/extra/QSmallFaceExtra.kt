package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class QSmallFaceExtra(
    @ProtoNumber(1) val faceId: Int = 0,
    @ProtoNumber(2) val text: String = "",
    @ProtoNumber(3) val compatText: String = "",
)
