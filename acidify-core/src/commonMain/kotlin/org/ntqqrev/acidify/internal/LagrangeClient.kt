package org.ntqqrev.acidify.internal

import kotlinx.coroutines.CoroutineScope
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.exception.UrlSignException
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.system.BotOnline
import org.ntqqrev.acidify.logging.Logger

internal class LagrangeClient(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    val signProvider: SignProvider,
    loggerFactory: (Any) -> Logger,
    scope: CoroutineScope,
) : AbstractClient(loggerFactory, scope) {
    override val os: String
        get() = appInfo.os

    override val uin: Long
        get() = sessionStore.uin

    override val uid: String
        get() = sessionStore.uid

    override val appId: Int
        get() = appInfo.appId

    override val subAppId: Int
        get() = appInfo.subAppId

    override val currentVersion: String
        get() = appInfo.currentVersion

    override val appClientVersion: Int
        get() = appInfo.appClientVersion

    override val a2: ByteArray
        get() = sessionStore.a2

    override val d2: ByteArray
        get() = sessionStore.d2

    override val d2Key: ByteArray
        get() = sessionStore.d2Key

    override val guid: ByteArray
        get() = sessionStore.guid

    val signRequiredCommand = setOf(
        "trpc.o3.ecdh_access.EcdhAccess.SsoEstablishShareKey",
        "trpc.o3.ecdh_access.EcdhAccess.SsoSecureAccess",
        "trpc.o3.report.Report.SsoReport",
        "trpc.o3.ecdh_access.EcdhAccess.SsoSecureA2Access",
        "trpc.o3.ecdh_access.EcdhAccess.SsoSecureA2Establish",
        "MessageSvc.PbSendMsg",
        "trpc.group.long_msg_interface.MsgService.SsoRecvLongMsg",
        "trpc.group.long_msg_interface.MsgService.SsoSendLongMsg",
        "trpc.msg.msg_svc.MsgService.SsoReadedReport",
        "trpc.msg.msg_svc.MsgService.SsoC2CRecallMsg",
        "wtlogin.trans_emp",
        "wtlogin.login",
        "trpc.login.ecdh.EcdhService.SsoKeyExchange",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginEasyLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLoginNewDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginEasyLoginUnusualDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLoginUnusualDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginAuthLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginAuthCodeLogin",
        "trpc.qqhb.qqhb_proxy.Handler.sso_handle",
        "QQConnectLogin.auth",
        "QQConnectLogin.pre_auth",
        "OidbSvcTrpcTcp.0x11ec_1",
        "OidbSvcTrpcTcp.0x758_1",
        "OidbSvcTrpcTcp.0x7c2_5",
        "OidbSvcTrpcTcp.0x10db_1",
        "OidbSvcTrpcTcp.0x8a1_7",
        "OidbSvcTrpcTcp.0x89a_0",
        "OidbSvcTrpcTcp.0x89a_15",
        "OidbSvcTrpcTcp.0x88d_0",
        "OidbSvcTrpcTcp.0x88d_14",
        "OidbSvcTrpcTcp.0x112a_1",
        "OidbSvcTrpcTcp.0x112a_2",
        "OidbSvcTrpcTcp.0x587_74",
        "OidbSvcTrpcTcp.0x1100_1",
        "OidbSvcTrpcTcp.0x1102_1",
        "OidbSvcTrpcTcp.0x1103_1",
        "OidbSvcTrpcTcp.0x1107_1",
        "OidbSvcTrpcTcp.0x1105_1",
        "OidbSvcTrpcTcp.0xf88_1",
        "OidbSvcTrpcTcp.0xf89_1",
        "OidbSvcTrpcTcp.0xf57_1",
        "OidbSvcTrpcTcp.0xf57_106",
        "OidbSvcTrpcTcp.0xf57_9",
        "OidbSvcTrpcTcp.0xf55_1",
        "OidbSvcTrpcTcp.0xf67_1",
        "OidbSvcTrpcTcp.0xf67_5",
        "OidbSvcTrpcTcp.0x6d9_4",
        "trpc.login.ecdh.EcdhService.SsoQRLoginGenQr",
        "ConnAuthSvr.fast_qq_login",
        "ConnAuthSvr.sdk_auth_api",
        "ConnAuthSvr.sdk_auth_api_emp",
        "MsgProxy.SendMsg",
        "OidbSvcTrpcTcp.0x101e_1",
        "OidbSvcTrpcTcp.0x101e_2",
        "OidbSvcTrpcTcp.0xf65_1",
        "OidbSvcTrpcTcp.0xf65_10",
        "OidbSvcTrpcTcp.0xf6e_1",
        "OidbSvcTrpcTcp.0xfa5_1",
        "trpc.group_pro.msgproxy.sendmsg",
        "trpc.passwd.manager.PasswdManager.SetPasswd",
        "trpc.passwd.manager.PasswdManager.VerifyPasswd",
        "wtlogin_device.login",
        "wtlogin_device.tran_sim_emp",
        "trpc.login.ecdh.EcdhService.SsoNTLoginTGTExchangeFastLogin",
        "trpc.ecom.api_gateway.ApiGateway.SsoForward",
        "OidbSvcTrpcTcp.0x917b_1",
        "OidbSvcTcp.0x102a",
        "OidbSvcTrpcTcp.0x102a_0",
        "OidbSvcTrpcTcp.0x102a_1",
        "OidbSvc.0xcd5",
        "OidbSvcTrpcTcp.0xcd5",
        "OidbSvcTrpcTcp.0xcd5_0",
        "OidbSvc.0xdc2_34",
        "OidbSvcTrpcTcp.0xdc2_58",
        "OidbSvcTrpcTcp.0xdc2_59",
        "OidbSvcTrpcTcp.0x10c8_2",
        "OidbSvcTrpcTcp.0x93d7_1",
        "OidbSvc.0xb77_9",
        "OidbSvcTrpcTcp.0x112e_1",
        "OidbSvcTrpcTcp.0x10c8_1",
        "OidbSvcTrpcTcp.0x962a_1",
        "OidbSvcTrpcTcp.0x9409_7",
        "OidbSvcTrpcTcp.0x9409_10",
        "OidbSvcTrpcTcp.0x9409_11",
        "OidbSvcTrpcTcp.0x9409_12",
        "OidbSvcTrpcTcp.0x9409_13",
        "OidbSvcTrpcTcp.0x9409_14",
        "OidbSvcTrpcTcp.0x9409_15",
        "OidbSvcTrpcTcp.0x9409_16",
        "OidbSvcTrpcTcp.0x9409_18",
        "OidbSvcTrpcTcp.0x101b_1",
        "trpc.login.ecdh.EcdhService.SsoNTLoginRefreshTicket",
    )

    override suspend fun getSsoSecureInfo(cmd: String, seq: Int, src: ByteArray): SsoSecureInfo? {
        return if (signRequiredCommand.contains(cmd)) {
            signProvider.sign(cmd, seq, src).let {
                SsoSecureInfo(
                    sign = it.sign,
                    token = it.token,
                    extra = it.extra,
                )
            }
        } else {
            null
        }
    }

    override suspend fun sendOnlinePacket() = callService(BotOnline)
}