package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupInvitation(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(5) val invitorUid: String = "",
)
