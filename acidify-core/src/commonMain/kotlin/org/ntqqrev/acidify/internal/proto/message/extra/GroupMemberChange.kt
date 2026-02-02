package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupMemberChange(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(3) val memberUid: String = "",
    @ProtoNumber(4) val type: Int = 0,
    @ProtoNumber(5) val operatorInfo: ByteArray? = null,
) {
    @Serializable
    internal class OperatorInfo(
        @ProtoNumber(1) val body: Body? = null,
    ) {
        @Serializable
        internal class Body(
            @ProtoNumber(1) val uid: String = "",
        )
    }
}
