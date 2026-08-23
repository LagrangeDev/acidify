package org.ntqqrev.acidify.internal.proto.login

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class NTLoginCommon(
    @ProtoNumber(1) val head: NTLoginHead = NTLoginHead(),
    @ProtoNumber(2) val body: ByteArray = byteArrayOf(),
)

@Serializable
internal class NTLoginHead(
    @ProtoNumber(1) val userInfo: NTLoginUserInfo = NTLoginUserInfo(),
    @ProtoNumber(2) val clientInfo: NTLoginClientInfo = NTLoginClientInfo(),
    @ProtoNumber(3) val appInfo: NTLoginAppInfo = NTLoginAppInfo(),
    @ProtoNumber(4) val errorInfo: NTLoginErrorInfo? = null,
    @ProtoNumber(5) val cookie: NTLoginCookie = NTLoginCookie(),
    @ProtoNumber(7) val sdkInfo: NTLoginSdkInfo = NTLoginSdkInfo(),
)

@Serializable
internal class NTLoginUserInfo(
    @ProtoNumber(1) val account: String = "",
    @ProtoNumber(2) val countryCode: UInt = 0u,
)

@Serializable
internal class NTLoginClientInfo(
    @ProtoNumber(1) val deviceType: String = "",
    @ProtoNumber(2) val deviceName: String = "",
    @ProtoNumber(3) val platform: Int = 0,
    @ProtoNumber(4) val guid: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val pubno: UInt = 0u,
    @ProtoNumber(6) val clientVer: UInt = 0u,
    @ProtoNumber(7) val clientType: UInt = 0u,
    @ProtoNumber(8) val ssoVer: UInt = 0u,
)

@Serializable
internal class NTLoginAppInfo(
    @ProtoNumber(1) val version: String = "",
    @ProtoNumber(2) val appId: Int = 0,
    @ProtoNumber(3) val appName: String = "",
    @ProtoNumber(4) val clientA1Version: UInt = 0u,
    @ProtoNumber(5) val qua: String = "",
)

@Serializable
internal class NTLoginSdkInfo(
    @ProtoNumber(1) val version: UInt = 0u,
)

@Serializable
internal class NTLoginCookie(
    @ProtoNumber(1) val cookieContent: String = "",
)

@Serializable
internal class NTLoginErrorInfo(
    @ProtoNumber(1) val errCode: ULong = 0uL,
    @ProtoNumber(2) val strTipsTitle: String = "",
    @ProtoNumber(3) val strTipsContent: String = "",
)

@Serializable
internal class NTLoginTickets(
    @ProtoNumber(3) val a1: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val a2: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val d2: ByteArray = byteArrayOf(),
    @ProtoNumber(6) val d2Key: ByteArray = byteArrayOf(),
)

@Serializable
internal class NTLoginSecCheck(
    @ProtoNumber(3) val iframeUrl: String = "",
)

@Serializable
internal class NTLoginSecProtect(
    @ProtoNumber(1) val newDeviceCheckSig: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val unusualDeviceCheckSig: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val unusualDeviceQrSig: String = "",
    @ProtoNumber(4) val uinToken: String = "",
)

@Serializable
internal class NTLoginEasyLoginReqBody(
    @ProtoNumber(1) val a1: ByteArray = byteArrayOf(),
)

@Serializable
internal class NTLoginEasyLoginRspBody(
    @ProtoNumber(1) val tickets: NTLoginTickets = NTLoginTickets(),
    @ProtoNumber(2) val secCheck: NTLoginSecCheck = NTLoginSecCheck(),
    @ProtoNumber(3) val secProtect: NTLoginSecProtect = NTLoginSecProtect(),
)

@Serializable
internal class NTLoginEasyLoginUnusualDeviceReqBody(
    @ProtoNumber(1) val a1: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val unusualDeviceCheckSucceedSig: ByteArray = byteArrayOf(),
)

@Serializable
internal class NTLoginEasyLoginUnusualDeviceRspBody(
    @ProtoNumber(1) val tickets: NTLoginTickets = NTLoginTickets(),
)
