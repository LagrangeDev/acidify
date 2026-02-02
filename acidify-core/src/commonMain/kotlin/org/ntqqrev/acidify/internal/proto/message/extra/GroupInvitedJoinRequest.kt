package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupInvitedJoinRequest(
    @ProtoNumber(1) val command: Int = 0,
    @ProtoNumber(2) val info: Info? = null,
) {
    @Serializable
    internal class Info(
        @ProtoNumber(1) val inner: Inner? = null,
    ) {
        @Serializable
        internal class Inner(
            @ProtoNumber(1) val groupUin: Long = 0L,
            @ProtoNumber(5) val targetUid: String = "",
            @ProtoNumber(6) val invitorUid: String = "",
        )
    }
}
