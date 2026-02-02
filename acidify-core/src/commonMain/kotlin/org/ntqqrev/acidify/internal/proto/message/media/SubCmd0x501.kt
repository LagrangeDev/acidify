package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked
import kotlinx.serialization.protobuf.ProtoType

@Serializable
internal class FetchHighwayInfoReq(
    @ProtoNumber(0x501) val reqBody: Body = Body(),
) {
    @Serializable
    internal class Body(
        @ProtoNumber(1) val uin: Long = 0L,
        @ProtoNumber(2) val idcId: Int = 0,
        @ProtoNumber(3) val appid: Int = 0,
        @ProtoNumber(4) val loginSigType: Int = 0,
        @ProtoNumber(5) val loginSigTicket: ByteArray = byteArrayOf(),
        @ProtoNumber(6) val requestFlag: Int = 0,
        @ProtoNumber(7) @ProtoPacked val serviceTypes: List<Int> = emptyList(),
        @ProtoNumber(8) val bid: Int = 0,
        @ProtoNumber(9) val field9: Int = 0,
        @ProtoNumber(10) val field10: Int = 0,
        @ProtoNumber(11) val field11: Int = 0,
        @ProtoNumber(15) val version: String = "",
    )
}

@Serializable
internal class FetchHighwayInfoResp(
    @ProtoNumber(0x501) val respBody: Body = Body(),
) {
    @Serializable
    internal class Body(
        @ProtoNumber(1) val sigSession: ByteArray = byteArrayOf(),
        @ProtoNumber(2) val sessionKey: ByteArray = byteArrayOf(),
        @ProtoNumber(3) val addrs: List<SrvAddrs> = emptyList(),
    )

    @Serializable
    internal class SrvAddrs(
        @ProtoNumber(1) val serviceType: Int = 0,
        @ProtoNumber(2) val addrs: List<IpAddr> = emptyList(),
    )

    @Serializable
    internal class IpAddr(
        @ProtoNumber(1) val type: Int = 0,
        @ProtoNumber(2) @ProtoType(ProtoIntegerType.FIXED) val ip: Int = 0,
        @ProtoNumber(3) val port: Int = 0,
        @ProtoNumber(4) val area: Int = 0,
    )
}
