package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetGroupWholeMuteReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
    @ProtoNumber(2) val state: State = State(),
) {
    @Serializable
    internal class State(
        @ProtoNumber(17) val isMute: Int = 0,
    )
}
