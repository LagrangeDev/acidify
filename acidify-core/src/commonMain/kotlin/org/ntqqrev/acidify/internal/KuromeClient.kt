package org.ntqqrev.acidify.internal

import kotlinx.coroutines.CoroutineScope
import org.ntqqrev.acidify.common.android.AndroidAppInfo
import org.ntqqrev.acidify.common.android.AndroidSessionStore
import org.ntqqrev.acidify.common.android.AndroidSignProvider
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.logging.Logger

internal class KuromeClient(
    val appInfo: AndroidAppInfo,
    val sessionStore: AndroidSessionStore,
    val signProvider: AndroidSignProvider,
    loggerFactory: (Any) -> Logger,
    scope: CoroutineScope,
) : AbstractClient(loggerFactory, scope) {
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
        get() = sessionStore.wloginSigs.a2

    override val d2: ByteArray
        get() = sessionStore.wloginSigs.d2

    override val d2Key: ByteArray
        get() = sessionStore.wloginSigs.d2Key

    override val guid: ByteArray
        get() = sessionStore.guid

    override suspend fun <T, R> callService(
        service: Service<T, R>,
        payload: T,
        timeout: Long
    ): R {
        TODO("Not yet implemented")
    }

    override suspend fun sendHeartbeat() {
        TODO("Not yet implemented")
    }

    override suspend fun sendOnlinePacket() {
        TODO("Not yet implemented")
    }
}