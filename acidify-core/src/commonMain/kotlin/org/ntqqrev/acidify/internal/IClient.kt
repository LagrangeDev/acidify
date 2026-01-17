package org.ntqqrev.acidify.internal

import org.ntqqrev.acidify.internal.service.Service

internal interface IClient {
    val os: String
    val uin: Long
    val uid: String

    suspend fun doPostOnlineLogic()
    suspend fun doPreOfflineLogic()
    suspend fun <T, R> callService(service: Service<T, R>, payload: T, timeout: Long = 10_000L): R
    suspend fun <R> callService(service: Service<Unit, R>, timeout: Long = 10_000L): R =
        callService(service, Unit, timeout)
}