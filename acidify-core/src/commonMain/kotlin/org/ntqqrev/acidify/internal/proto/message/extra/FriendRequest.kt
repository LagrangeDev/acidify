package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FriendRequest(
    @ProtoNumber(1) val body: Body? = null,
) {
    @Serializable
    internal class Body(
        @ProtoNumber(2) val fromUid: String = "",
        @ProtoNumber(10) val message: String = "",
        @ProtoNumber(11) val via: String? = null,
    )
}
