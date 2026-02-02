package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SendRoutingHead(
    @ProtoNumber(1) val c2c: C2C = C2C(),
    @ProtoNumber(2) val group: Grp = Grp(),
    @ProtoNumber(15) val trans211: Trans211 = Trans211(),
) {
    @Serializable
    internal class C2C(
        @ProtoNumber(1) val peerUin: Long = 0L,
        @ProtoNumber(2) val peerUid: String = "",
    )

    @Serializable
    internal class Grp(
        @ProtoNumber(1) val groupUin: Long = 0L,
    )
}
