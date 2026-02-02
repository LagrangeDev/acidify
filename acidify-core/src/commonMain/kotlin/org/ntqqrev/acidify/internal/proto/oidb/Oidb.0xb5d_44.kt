package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetFriendRequestReq(
    @ProtoNumber(1) val accept: Int = 0,
    @ProtoNumber(2) val targetUid: String = "",
)
