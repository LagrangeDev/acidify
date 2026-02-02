package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetMemberMuteReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
    @ProtoNumber(2) val type: Int = 0,
    @ProtoNumber(3) val body: Body = Body(),
) {
    @Serializable
    internal class Body(
        @ProtoNumber(1) val targetUid: String = "",
        @ProtoNumber(2) val duration: Int = 0,
    )
}
