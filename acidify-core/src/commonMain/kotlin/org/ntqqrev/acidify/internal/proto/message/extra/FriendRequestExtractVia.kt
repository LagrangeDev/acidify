package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FriendRequestExtractVia(
    @ProtoNumber(1) val body: Body? = null,
) {
    @Serializable
    internal class Body(
        @ProtoNumber(5) val via: String = "",
    )
}
