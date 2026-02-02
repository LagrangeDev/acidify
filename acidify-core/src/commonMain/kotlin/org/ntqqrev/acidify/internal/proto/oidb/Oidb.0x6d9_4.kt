package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class BroadcastGroupFileReq(
    @ProtoNumber(5) val body: Body = Body(),
) {
    @Serializable
    class Body(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val type: Int = 0,
        @ProtoNumber(3) val info: Info = Info(),
    ) {
        @Serializable
        internal class Info(
            @ProtoNumber(1) val busiType: Int = 0,
            @ProtoNumber(2) val fileId: String = "",
            @ProtoNumber(3) val field3: Int = 0,
            @ProtoNumber(4) val field4: String = "",
            @ProtoNumber(5) val field5: Boolean = false,
        )
    }
}
