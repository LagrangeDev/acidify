package org.ntqqrev.acidify.internal.context

import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ntqqrev.acidify.exception.WebApiException
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.service.system.AndroidFetchClientKey
import org.ntqqrev.acidify.internal.service.system.FetchClientKey
import org.ntqqrev.acidify.internal.service.system.FetchPSKey
import org.ntqqrev.acidify.internal.util.RetryPolicy
import org.ntqqrev.acidify.internal.util.withRetry
import kotlin.time.Clock

internal class TicketContext(client: AbstractClient) : AbstractContext(client) {
    internal class KeyWithLifetime(var value: String, var expireTime: Long) {
        companion object {
            fun dummy() = KeyWithLifetime("", 0L)
            fun create(key: String, lifetimeSeconds: Long): KeyWithLifetime {
                val expireTime = Clock.System.now().epochSeconds + lifetimeSeconds
                return KeyWithLifetime(key, expireTime)
            }
        }

        fun isValid(): Boolean {
            return Clock.System.now().epochSeconds < expireTime
        }

        fun refreshWith(newKey: String, lifetimeSeconds: Long) {
            value = newKey
            expireTime = Clock.System.now().epochSeconds + lifetimeSeconds
        }
    }

    private val currentSKey = KeyWithLifetime.dummy()
    private val psKeyCache = mutableMapOf<String, KeyWithLifetime>()
    private val psKeyQueryMutex = Mutex()
    private val httpClient = HttpClient {
        install(HttpCookies)
        followRedirects = false
    }

    override suspend fun postOnline() {
        getSKey()
    }

    suspend fun getSKey(): String {
        if (currentSKey.isValid()) {
            return currentSKey.value
        }
        val clientKey = when (client) {
            is LagrangeClient -> client.callService(FetchClientKey)
            is KuromeClient -> client.callService(AndroidFetchClientKey)
        }
        val jump =
            "https%3A%2F%2Fh5.qzone.qq.com%2Fqqnt%2Fqzoneinpcqq%2Ffriend%3Frefresh%3D0%26clientuin%3D0%26darkMode%3D0&keyindex=19&random=2599"
        val urlString = "https://ssl.ptlogin2.qq.com/jump" +
                "?ptlang=1033" +
                "&clientuin=${client.uin}" +
                "&clientkey=$clientKey" +
                "&u1=$jump"
        try {
            withRetry(
                policy = RetryPolicy(maxAttempts = 5),
                onRetry = { attempt, cause, nextDelay ->
                    logger.w(cause) { "获取 SKey 失败（第 $attempt 次尝试），将在 ${nextDelay.inWholeMilliseconds}ms 后重试" }
                }
            ) {
                val resp = httpClient.get(urlString)
                val cookies = httpClient.cookies(urlString)
                cookies.firstOrNull { it.name == "skey" }
                    ?.let { currentSKey.refreshWith(it.value, 86400L) }
                    ?: throw WebApiException("获取 SKey 失败", resp.status.value)
            }
        } catch (e: WebApiException) {
            when (client) {
                is LagrangeClient -> throw e // 给你机会你不中用啊
                is KuromeClient -> {
                    logger.w { "通过 URL 刷新 SKey 失败，使用 SessionStore 中的 SKey（可能已经过期）" }
                    currentSKey.refreshWith(client.sessionStore.wloginSigs.sKey.toHexString(), 86400L)
                }
            }
        }
        return currentSKey.value
    }

    suspend fun getCsrfToken(): Int {
        val skey = getSKey()
        var hash = 5381
        for (element in skey) {
            hash += (hash shl 5) + element.code
        }
        return hash and 0x7fffffff
    }

    suspend fun getPSKey(domain: String): String {
        psKeyQueryMutex.withLock {
            psKeyCache[domain]?.let {
                if (it.isValid()) {
                    return it.value
                }
            }
        }
        val newKeys = client.callService(FetchPSKey, listOf(domain))
        val newKey = newKeys[domain]
            ?: throw RuntimeException("获取 PSKey 失败")
        psKeyQueryMutex.withLock {
            psKeyCache[domain] = KeyWithLifetime.create(newKey, 86400L)
        }
        return newKey
    }
}