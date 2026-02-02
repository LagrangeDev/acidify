package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupAdminChange(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(4) val body: Body? = null,
) {
    @Serializable
    internal class Body(
        @ProtoNumber(1) val unset: TargetInfo? = null,
        @ProtoNumber(2) val set: TargetInfo? = null,
    ) {
        @Serializable
        internal class TargetInfo(
            @ProtoNumber(1) val targetUid: String = "",
        )
    }
}
