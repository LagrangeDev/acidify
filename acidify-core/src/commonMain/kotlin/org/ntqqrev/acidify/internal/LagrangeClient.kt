package org.ntqqrev.acidify.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.event.AcidifyEvent
import org.ntqqrev.acidify.exception.UrlSignException
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.system.BotOnline
import org.ntqqrev.acidify.logging.Logger

internal class LagrangeClient(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    val signProvider: SignProvider,
    loggerFactory: (Any) -> Logger,
    eventFlow: MutableSharedFlow<AcidifyEvent>,
    scope: CoroutineScope,
) : AbstractClient(loggerFactory, eventFlow, scope) {
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

    override val deviceName: String
        get() = sessionStore.deviceName

    override val packageName: String
        get() = appInfo.packageName

    override val qua: String
        get() = appInfo.qua

    override val a2: ByteArray
        get() = sessionStore.a2

    override val d2: ByteArray
        get() = sessionStore.d2

    override val d2Key: ByteArray
        get() = sessionStore.d2Key

    override val guid: ByteArray
        get() = sessionStore.guid

    override val ntLoginDeviceType: String
        get() = os

    override val ntLoginPlatform: Int
        get() = appInfo.platform

    override val ntLoginVersion: String
        get() = appInfo.kernel

    override val ntLoginSessionTicket: ByteArray?
        get() = sessionStore.keySig

    override val ntLoginSessionKey: ByteArray?
        get() = sessionStore.exchangeKey

    override var ntLoginCookie: String?
        get() = sessionStore.unusualCookies
        set(value) {
            sessionStore.unusualCookies = value
        }

    override suspend fun getSsoSecureInfo(cmd: String, seq: Int, src: ByteArray): SsoSecureInfo? {
        return signProvider.sign(cmd, seq, src)?.let {
            SsoSecureInfo(
                sign = it.sign,
                token = it.token,
                extra = it.extra,
            )
        }
    }

    override suspend fun sendOnlinePacket() = callService(BotOnline)
}

private val AppInfo.platform: Int
    get() = when (os.lowercase()) {
        "windows" -> 4
        "mac", "macos" -> 5
        "linux" -> 7
        else -> 0
    }

private val AppInfo.qua: String
    get() {
        val platform = when (os.lowercase()) {
            "windows" -> "WIN"
            "mac", "macos" -> "MAC"
            "linux" -> "LNX"
            else -> error("Unsupported NTLogin platform: $os")
        }
        return "V1_${platform}_NQ_${currentVersion.replace('-', '_')}_GW_B"
    }