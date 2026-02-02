package org.ntqqrev.acidify.internal.proto.login

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class TlvQRCodeBodyD1Req(
    @ProtoNumber(1) val system: System = System(),
    @ProtoNumber(4) val typeBuf: ByteArray = byteArrayOf(),
) {
    @Serializable
    class System(
        @ProtoNumber(1) val os: String = "",
        @ProtoNumber(2) val deviceName: String = "",
    )
}

@Serializable
class TlvQRCodeBodyD1Resp(
    @ProtoNumber(2) val qrCodeUrl: String = "",
    @ProtoNumber(3) val qrSig: String = "",
)