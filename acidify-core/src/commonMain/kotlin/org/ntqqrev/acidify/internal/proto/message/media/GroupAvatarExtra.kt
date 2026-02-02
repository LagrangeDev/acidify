package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupAvatarExtra(
    @ProtoNumber(1) val type: Int = 0,
    @ProtoNumber(2) val groupUin: Long = 0L,
    @ProtoNumber(3) val field3: GroupAvatarExtraField3 = GroupAvatarExtraField3(),
    @ProtoNumber(5) val field5: Int = 0,
    @ProtoNumber(6) val field6: Int = 0,
)

@Serializable
internal class GroupAvatarExtraField3(
    @ProtoNumber(1) val field1: Int = 0,
)
