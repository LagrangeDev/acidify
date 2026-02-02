package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked
import org.ntqqrev.acidify.internal.proto.message.Elem

@Serializable
internal class SourceMsg(
    @ProtoNumber(1) @ProtoPacked val origSeqs: List<Long> = emptyList(),
    @ProtoNumber(2) val senderUin: Long = 0L,
    @ProtoNumber(3) val time: Long = 0L,
    @ProtoNumber(4) val flag: Int = 0,
    @ProtoNumber(5) val elems: List<Elem> = emptyList(),
    @ProtoNumber(6) val type: Int = 0,
    @ProtoNumber(7) val richMsg: ByteArray = byteArrayOf(),
    @ProtoNumber(8) val pbReserve: ByteArray = byteArrayOf(),
    @ProtoNumber(9) val srcMsg: ByteArray = byteArrayOf(),
    @ProtoNumber(10) val toUin: Long = 0L,
    @ProtoNumber(11) val troopName: ByteArray = byteArrayOf(),
) {
    @Serializable
    internal class PbReserve(
        @ProtoNumber(6) val senderUid: String = "",
        @ProtoNumber(7) val receiverUid: String = "",
        @ProtoNumber(8) val friendSequence: Long = 0L,
    )
}
