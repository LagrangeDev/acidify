package org.ntqqrev.acidify.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.exception.ServiceException
import org.ntqqrev.acidify.internal.context.HighwayContext
import org.ntqqrev.acidify.internal.context.LoginContext
import org.ntqqrev.acidify.internal.context.PacketContext
import org.ntqqrev.acidify.internal.context.TicketContext
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.logging.Logger

internal class LagrangeClient(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    val signProvider: SignProvider,
    val loggerFactory: (Any) -> Logger,
    scope: CoroutineScope,
) : IClient, CoroutineScope by scope {
    override val os: String
        get() = appInfo.os

    override val uin: Long
        get() = sessionStore.uin

    override val uid: String
        get() = sessionStore.uid

    override val subAppId: Int
        get() = appInfo.subAppId

    override val currentVersion: String
        get() = appInfo.currentVersion

    override val a2: ByteArray
        get() = sessionStore.a2

    override val d2: ByteArray
        get() = sessionStore.d2

    override val d2Key: ByteArray
        get() = sessionStore.d2Key

    override val guid: ByteArray
        get() = sessionStore.guid

    val signRequiredCommand = setOf(
        "MessageSvc.PbSendMsg",
        "wtlogin.trans_emp",
        "wtlogin.login",
        "trpc.login.ecdh.EcdhService.SsoKeyExchange",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginEasyLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLoginNewDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginEasyLoginUnusualDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLoginUnusualDevice",
        "OidbSvcTrpcTcp.0x6d9_4"
    )

    val loginContext = LoginContext(this)
    val packetContext = PacketContext(this)
    val ticketContext = TicketContext(this)
    val highwayContext = HighwayContext(this)
    val pushChannel = Channel<SsoResponse>(capacity = 15, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val contextCollection = listOf(
        loginContext,
        packetContext,
        ticketContext,
        highwayContext,
    )

    override fun createLogger(forObject: Any): Logger = loggerFactory(forObject)

    override suspend fun doPostOnlineLogic() {
        contextCollection.forEach {
            it.postOnline()
        }
    }

    override suspend fun doPreOfflineLogic() {
        contextCollection.forEach {
            it.preOffline()
        }
    }

    override suspend fun <T, R> callService(service: Service<T, R>, payload: T, timeout: Long): R {
        val byteArray = service.build(this, payload)
        val resp = packetContext.sendPacket(
            command = service.cmd,
            payload = byteArray,
            requestType = service.ssoRequestType,
            encryptType = service.ssoEncryptType,
            timeoutMillis = timeout
        ) { seq ->
            if (signRequiredCommand.contains(service.cmd)) {
                signProvider.sign(
                    cmd = service.cmd,
                    seq = seq,
                    src = byteArray,
                ).let {
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
        if (resp.retCode != 0) {
            throw ServiceException(
                service.cmd,
                resp.retCode,
                resp.extra ?: ""
            )
        }
        return service.parse(this, resp.response)
    }

    override suspend fun <R> callService(service: Service<Unit, R>, timeout: Long): R {
        return callService(service, Unit, timeout)
    }
}