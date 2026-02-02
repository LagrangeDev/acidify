package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupMute(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(4) val operatorUid: String = "",
    @ProtoNumber(5) val info: Info? = null,
) {
    @Serializable
    internal class Info(
        @ProtoNumber(1) val timestamp: Int = 0,
        @ProtoNumber(3) val state: State? = null,
    ) {
        @Serializable
        internal class State(
            @ProtoNumber(1) val targetUid: String? = null,
            @ProtoNumber(2) val duration: Int = 0,
        )
    }
}
