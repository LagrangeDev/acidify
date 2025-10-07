@file:OptIn(ExperimentalTime::class)

package org.ntqqrev.acidify.internal.logic

import co.touchlab.stately.collections.ConcurrentMutableMap
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.service.system.FetchClientKey
import org.ntqqrev.acidify.internal.service.system.FetchPSKey
import org.ntqqrev.acidify.util.createHttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class TicketLogic(client: LagrangeClient) : AbstractLogic(client) {
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

    val currentSKey = KeyWithLifetime.dummy()
    val psKeyCache = ConcurrentMutableMap<String, KeyWithLifetime>()

    private val httpClient = createHttpClient {
        install(HttpCookies)
    }

    suspend fun getSKey(): String {
        if (currentSKey.isValid()) {
            return currentSKey.value
        }
        val clientKey = client.callService(FetchClientKey)
        val jump =
            "https%3A%2F%2Fh5.qzone.qq.com%2Fqqnt%2Fqzoneinpcqq%2Ffriend%3Frefresh%3D0%26clientuin%3D0%26darkMode%3D0&keyindex=19&random=2599"
        val urlString = "https://ssl.ptlogin2.qq.com/jump" +
                "?ptlang=1033" +
                "&clientuin=${client.sessionStore.uin}" +
                "&clientkey=$clientKey" +
                "&u1=$jump"
        httpClient.get(urlString)
        val cookies = httpClient.cookies(urlString)
        cookies.firstOrNull { it.name == "skey" }
            ?.let {
                currentSKey.refreshWith(it.value, 86400L)
            }
            ?: throw RuntimeException("获取 SKey 失败")
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
        psKeyCache[domain]?.let {
            if (it.isValid()) {
                return it.value
            }
        }
        val newKeys = client.callService(FetchPSKey, listOf(domain))
        val newKey = newKeys[domain]
            ?: throw RuntimeException("获取 PSKey 失败")
        psKeyCache[domain] = KeyWithLifetime.create(newKey, 86400L)
        return newKey
    }
}