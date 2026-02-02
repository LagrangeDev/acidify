package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GeneralGrayTip(
    @ProtoNumber(1) val bizType: Int = 0,
    @ProtoNumber(7) val templateParams: Map<String, String> = emptyMap(),
)
