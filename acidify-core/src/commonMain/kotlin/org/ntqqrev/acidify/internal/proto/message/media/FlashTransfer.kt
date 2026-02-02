package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FlashTransferUploadReq(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val appId: Int = 0,
    @ProtoNumber(3) val field3: Int = 0,
    @ProtoNumber(107) val body: FlashTransferUploadBody = FlashTransferUploadBody(),
)

@Serializable
internal class FlashTransferUploadBody(
    @ProtoNumber(1) val field1: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val uKey: String = "",
    @ProtoNumber(3) val start: Int = 0,
    @ProtoNumber(4) val end: Int = 0,
    @ProtoNumber(5) val sha1: ByteArray = byteArrayOf(),
    @ProtoNumber(6) val sha1StateV: FlashTransferSha1StateV = FlashTransferSha1StateV(),
    @ProtoNumber(7) val body: ByteArray = byteArrayOf(),
)

@Serializable
internal class FlashTransferSha1StateV(
    @ProtoNumber(1) val state: List<ByteArray> = emptyList(),
)

@Serializable
internal class FlashTransferUploadResp(
    @ProtoNumber(5) val status: String = "",
)
