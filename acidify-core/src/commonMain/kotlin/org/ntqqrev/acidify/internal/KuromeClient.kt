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
    val loggerFactory: (Any) -> Logger,
    scope: CoroutineScope,
) : IClient, CoroutineScope by scope {
    override val os: String
        get() = "Android"

    override val uin: Long
        get() = sessionStore.uin

    override val uid: String
        get() = sessionStore.uid

    override fun createLogger(forObject: Any): Logger = loggerFactory(forObject)

    override suspend fun doPostOnlineLogic() {
        TODO("Not yet implemented")
    }

    override suspend fun doPreOfflineLogic() {
        TODO("Not yet implemented")
    }

    override suspend fun <T, R> callService(
        service: Service<T, R>,
        payload: T,
        timeout: Long
    ): R {
        TODO("Not yet implemented")
    }
}