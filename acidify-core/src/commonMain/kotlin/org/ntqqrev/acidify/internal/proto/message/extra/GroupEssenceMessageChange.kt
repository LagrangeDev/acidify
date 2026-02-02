package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupEssenceMessageChange(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(2) val msgSequence: Int = 0,
    @ProtoNumber(3) val random: Int = 0,
    @ProtoNumber(4) val setFlag: Int = 0,
    @ProtoNumber(5) val memberUin: Long = 0L,
    @ProtoNumber(6) val operatorUin: Long = 0L,
)
