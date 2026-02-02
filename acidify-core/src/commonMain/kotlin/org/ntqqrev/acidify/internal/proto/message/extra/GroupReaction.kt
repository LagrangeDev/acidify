package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupReaction(
    @ProtoNumber(1) val data: DataOuter? = null,
) {
    @Serializable
    internal class DataOuter(
        @ProtoNumber(1) val data: DataMiddle? = null,
    ) {
        @Serializable
        internal class DataMiddle(
            @ProtoNumber(2) val target: Target? = null,
            @ProtoNumber(3) val data: DataInner? = null,
        ) {
            @Serializable
            internal class Target(
                @ProtoNumber(1) val sequence: Int = 0,
            )

            @Serializable
            internal class DataInner(
                @ProtoNumber(1) val code: String = "",
                @ProtoNumber(3) val count: Int = 0,
                @ProtoNumber(4) val operatorUid: String = "",
                @ProtoNumber(5) val type: Int = 0,
            )
        }
    }
}
