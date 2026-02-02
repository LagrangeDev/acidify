package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class DeleteFriendReq(
    @ProtoNumber(1) val body: Body = Body(),
) {
    @Serializable
    class Body(
        @ProtoNumber(1) val targetUid: String = "",
        @ProtoNumber(2) val field2: Field2 = Field2(),
        @ProtoNumber(3) val block: Boolean = false,
        @ProtoNumber(4) val field4: Boolean = false,
    ) {
        @Serializable
        class Field2(
            @ProtoNumber(1) val field1: Int = 0,
            @ProtoNumber(2) val field2: Int = 0,
            @ProtoNumber(3) val field3: Field3 = Field3(),
        ) {
            @Serializable
            class Field3(
                @ProtoNumber(1) val field1: Int = 0,
                @ProtoNumber(2) val field2: Int = 0,
                @ProtoNumber(3) val field3: Int = 0,
            )
        }
    }
}
