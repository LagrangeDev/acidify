package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetFilteredFriendRequestReq(
    @ProtoNumber(1) val selfUid: String = "",
    @ProtoNumber(2) val requestUid: String = "",
)
