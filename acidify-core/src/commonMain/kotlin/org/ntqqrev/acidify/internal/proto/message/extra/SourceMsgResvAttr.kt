package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SourceMsgResvAttr(
    @ProtoNumber(1) val oriMsgType: Int = 0,
    @ProtoNumber(2) val sourceMsgId: Long = 0L,
    @ProtoNumber(3) val senderUid: String = "",
)
