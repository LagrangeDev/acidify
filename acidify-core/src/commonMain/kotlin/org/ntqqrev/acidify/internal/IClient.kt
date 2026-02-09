package org.ntqqrev.acidify.internal

import kotlinx.coroutines.CoroutineScope
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.logging.Logger

internal sealed interface IClient : CoroutineScope {
    val os: String
    val uin: Long
    val uid: String

    val subAppId: Int
    val currentVersion: String

    val d2: ByteArray
    val d2Key: ByteArray
    val a2: ByteArray
    val guid: ByteArray

    fun createLogger(forObject: Any): Logger
    suspend fun doPostOnlineLogic()
    suspend fun doPreOfflineLogic()
    suspend fun <T, R> callService(service: Service<T, R>, payload: T, timeout: Long = 10_000L): R
    suspend fun <R> callService(service: Service<Unit, R>, timeout: Long = 10_000L): R =
        callService(service, Unit, timeout)
}