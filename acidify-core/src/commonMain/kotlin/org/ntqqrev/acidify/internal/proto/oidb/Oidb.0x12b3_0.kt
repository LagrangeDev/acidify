package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchPinsResp(
    @ProtoNumber(1) val friends: List<Friend> = emptyList(),
    @ProtoNumber(3) val groups: List<Group> = emptyList(),
) {
    @Serializable
    internal class Friend(
        @ProtoNumber(1) val uid: String = "",
    )

    @Serializable
    internal class Group(
        @ProtoNumber(1) val uin: Long = 0L,
    )
}
