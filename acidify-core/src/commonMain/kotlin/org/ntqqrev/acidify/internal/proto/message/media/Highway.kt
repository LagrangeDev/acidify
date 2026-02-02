package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class ReqDataHighwayHead(
    @ProtoNumber(1) val msgBaseHead: DataHighwayHead = DataHighwayHead(),
    @ProtoNumber(2) val msgSegHead: SegHead = SegHead(),
    @ProtoNumber(3) val bytesReqExtendInfo: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val timestamp: Long = 0L,
    @ProtoNumber(5) val msgLoginSigHead: LoginSigHead = LoginSigHead(),
)

@Serializable
internal class RespDataHighwayHead(
    @ProtoNumber(1) val msgBaseHead: DataHighwayHead = DataHighwayHead(),
    @ProtoNumber(2) val msgSegHead: SegHead = SegHead(),
    @ProtoNumber(3) val errorCode: Int = 0,
    @ProtoNumber(4) val allowRetry: Int = 0,
    @ProtoNumber(5) val cacheCost: Int = 0,
    @ProtoNumber(6) val htCost: Int = 0,
    @ProtoNumber(7) val bytesRspExtendInfo: ByteArray = byteArrayOf(),
    @ProtoNumber(8) val timestamp: Long = 0L,
    @ProtoNumber(9) val range: Long = 0L,
    @ProtoNumber(10) val isReset: Int = 0,
)

@Serializable
internal class DataHighwayHead(
    @ProtoNumber(1) val version: Int = 0,
    @ProtoNumber(2) val uin: String = "",
    @ProtoNumber(3) val command: String = "",
    @ProtoNumber(4) val seq: Int = 0,
    @ProtoNumber(5) val retryTimes: Int = 0,
    @ProtoNumber(6) val appId: Int = 0,
    @ProtoNumber(7) val dataFlag: Int = 0,
    @ProtoNumber(8) val commandId: Int = 0,
    @ProtoNumber(9) val buildVer: ByteArray = byteArrayOf(),
)

@Serializable
internal class LoginSigHead(
    @ProtoNumber(1) val uint32LoginSigType: Int = 0,
    @ProtoNumber(2) val bytesLoginSig: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val appId: Int = 0,
)

@Serializable
internal class SegHead(
    @ProtoNumber(1) val serviceId: Int = 0,
    @ProtoNumber(2) val filesize: Long = 0L,
    @ProtoNumber(3) val dataOffset: Long = 0L,
    @ProtoNumber(4) val dataLength: Int = 0,
    @ProtoNumber(5) val retCode: Int = 0,
    @ProtoNumber(6) val serviceTicket: ByteArray = byteArrayOf(),
    @ProtoNumber(8) val md5: ByteArray = byteArrayOf(),
    @ProtoNumber(9) val fileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(10) val cacheAddr: Int = 0,
    @ProtoNumber(13) val cachePort: Int = 0,
)
