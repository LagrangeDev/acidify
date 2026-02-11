package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class AndroidThirdPartyLoginResponse(
    @ProtoNumber(1) val seq: Long = 0L,
    @ProtoNumber(9) val commonInfo: RespCommonInfo = RespCommonInfo(),
) {
    @Serializable
    class RespCommonInfo(
        @ProtoNumber(10) val needVerifyScenes: UInt = 0u,
        @ProtoNumber(11) val rspNT: RspNT = RspNT(),
        @ProtoNumber(12) val a1Seq: UInt = 0u,
    ) {
        @Serializable
        class RspNT(
            @ProtoNumber(1) val uid: String = "",
            @ProtoNumber(2) val ua2: ByteArray = byteArrayOf(),
        )
    }
}