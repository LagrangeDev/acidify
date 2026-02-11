package org.ntqqrev.acidify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.common.*
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.RequestType
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import kotlin.js.JsStatic

/**
 * Acidify Bot 实例
 */
class Bot internal constructor(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    signProvider: SignProvider,
    scope: CoroutineScope,
) : AbstractBot(scope) {
    override val client = LagrangeClient(appInfo, sessionStore, signProvider, this::createLogger, scope)

    override val uin: Long
        get() = sessionStore.uin.takeIf { it != 0L }
            ?: throw IllegalStateException("用户尚未登录")

    override val uid: String
        get() = sessionStore.uid.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("用户尚未登录")

    /**
     * 发送自定义 SSO 数据包。
     * **Ensure that you know what you are doing!**
     * @param cmd 命令字符串
     * @param payload 原始数据
     * @param timeoutMillis 超时时间，默认 10000 毫秒
     */
    @UnsafeAcidifyApi
    suspend fun sendPacket(cmd: String, payload: ByteArray, timeoutMillis: Long = 10000L): SsoResponse {
        val sequence = client.ssoSequence++
        return client.packetContext.sendPacket(
            command = cmd,
            sequence = sequence,
            payload = payload,
            requestType = RequestType.D2Auth,
            encryptType = EncryptType.WithD2Key,
            timeoutMillis = timeoutMillis,
            ssoSecureInfo = if (client.signRequiredCommand.contains(cmd)) {
                client.signProvider.sign(
                    cmd = cmd,
                    seq = sequence,
                    src = payload,
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
        )
    }

    companion object {
        @JsStatic
        suspend fun create(
            appInfo: AppInfo,
            sessionStore: SessionStore,
            signProvider: SignProvider,
            scope: CoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler,
        ): Bot = Bot(
            appInfo = appInfo,
            sessionStore = sessionStore,
            signProvider = signProvider,
            scope = scope,
        ).apply {
            launch {
                sharedLogFlow
                    .filter { it.level >= minLogLevel }
                    .collect {
                        logHandler.handleLog(
                            it.level,
                            it.tag,
                            it.messageSupplier(),
                            it.throwable
                        )
                    }
            }
            client.packetContext.startConnectLoop()
        }
    }
}
