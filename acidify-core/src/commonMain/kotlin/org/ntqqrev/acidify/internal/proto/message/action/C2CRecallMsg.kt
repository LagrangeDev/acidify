package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class C2CRecallMsg(
    @ProtoNumber(1) val type: Int = 0,
    @ProtoNumber(3) val targetUid: String = "",
    @ProtoNumber(4) val info: Info? = null,
    @ProtoNumber(5) val settings: Settings? = null,
    @ProtoNumber(6) val field6: Boolean = false,
) {
    @Serializable
    internal class Info(
        @ProtoNumber(1) val clientSequence: Long = 0L,
        @ProtoNumber(2) val random: Int = 0,
        @ProtoNumber(3) val messageId: Long = 0L,
        @ProtoNumber(4) val timestamp: Long = 0L,
        @ProtoNumber(5) val field5: Int = 0,
        @ProtoNumber(6) val messageSequence: Long = 0L,
    )

    @Serializable
    internal class Settings(
        @ProtoNumber(1) val field1: Boolean = false,
        @ProtoNumber(2) val field2: Boolean = false,
    )
}
