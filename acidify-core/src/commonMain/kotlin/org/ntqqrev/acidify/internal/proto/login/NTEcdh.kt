package org.ntqqrev.acidify.internal.proto.login

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class KeyExchangeRequest(
    @ProtoNumber(1) val publicKey: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val type: UInt = 0u,
    @ProtoNumber(3) val secret: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val timestamp: Long = 0L,
    @ProtoNumber(5) val verifyHash: ByteArray = byteArrayOf(),
)

@Serializable
internal class KeyExchangeResponse(
    @ProtoNumber(1) val secret: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val field2: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val publicKey: ByteArray = byteArrayOf(),
)

@Serializable
internal class KeyExchangeRequestBuf(
    @ProtoNumber(1) val uin: String = "",
    @ProtoNumber(2) val guid: ByteArray = byteArrayOf(),
)

@Serializable
internal class KeyExchangeResponseSecret(
    @ProtoNumber(1) val sessionKey: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val sessionTicket: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val expiration: UInt = 0u,
)

@Serializable
internal class NTLoginForwardRequest(
    @ProtoNumber(1) val sessionTicket: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val buffer: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val type: UInt = 0u,
    @ProtoNumber(5) val secBuffer: ByteArray = byteArrayOf(),
)
