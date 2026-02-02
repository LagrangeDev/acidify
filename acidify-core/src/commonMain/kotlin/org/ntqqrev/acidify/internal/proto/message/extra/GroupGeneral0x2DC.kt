package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.readUInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.writeBytes

internal class GroupGeneral0x2DC {
    val groupUin: Long
    val body: Body

    constructor(byteArray: ByteArray) {
        val buffer = Buffer()
        buffer.writeBytes(byteArray)
        this.groupUin = buffer.readUInt().toLong()
        buffer.readByte()
        buffer.readShort()
        body = buffer.readByteArray().pbDecode()
    }

    @Serializable
    internal class Body(
        @ProtoNumber(1) val type: Int = 0,
        @ProtoNumber(4) val groupUin: Long = 0L,
        @ProtoNumber(5) val eventParam: ByteArray? = null,
        @ProtoNumber(11) val recall: GroupRecall? = null,
        @ProtoNumber(13) val field13: Int? = null,
        @ProtoNumber(21) val operatorUid: String? = null,
        @ProtoNumber(26) val generalGrayTip: GeneralGrayTip? = null,
        @ProtoNumber(33) val essenceMessageChange: GroupEssenceMessageChange? = null,
        @ProtoNumber(37) val msgSequence: Int = 0,
        @ProtoNumber(44) val reaction: GroupReaction? = null,
    )
}