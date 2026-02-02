package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupRecall(
    @ProtoNumber(1) val operatorUid: String = "",
    @ProtoNumber(3) val recallMessages: List<RecallMessage> = emptyList(),
    @ProtoNumber(5) val userDef: ByteArray? = null,
    @ProtoNumber(6) val groupType: Int = 0,
    @ProtoNumber(7) val opType: Int = 0,
    @ProtoNumber(9) val tipInfo: TipInfo? = null,
) {
    @Serializable
    internal class RecallMessage(
        @ProtoNumber(1) val sequence: Int = 0,
        @ProtoNumber(2) val time: Int = 0,
        @ProtoNumber(3) val random: Int = 0,
        @ProtoNumber(4) val type: Int = 0,
        @ProtoNumber(5) val flag: Int = 0,
        @ProtoNumber(6) val authorUid: String = "",
    )

    @Serializable
    internal class TipInfo(
        @ProtoNumber(2) val tip: String? = null,
    )
}
