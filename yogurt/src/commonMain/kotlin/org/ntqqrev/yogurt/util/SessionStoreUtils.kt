package org.ntqqrev.yogurt.util

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.event.AndroidSessionStoreUpdatedEvent
import org.ntqqrev.acidify.event.BotOfflineEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.acidify.exception.SessionRenewalException
import org.ntqqrev.acidify.renewSession
import org.ntqqrev.ktfs.withFs
import org.ntqqrev.yogurt.YogurtApp
import org.ntqqrev.yogurt.androidSessionStorePath
import org.ntqqrev.yogurt.sessionStorePath
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

fun Application.configureSessionStoreAutoSave() = launch(start = CoroutineStart.UNDISPATCHED) {
    val bot = dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("SessionStoreUtils")
    if (isPC) {
        bot.eventFlow.filterIsInstance<SessionStoreUpdatedEvent>().collect {
            logger.i { "SessionStore 已更新，正在保存至文件..." }
            withFs { sessionStorePath.writeText(it.sessionStore.toJson()) }
        }
    } else if (isAndroid) {
        bot.eventFlow.filterIsInstance<AndroidSessionStoreUpdatedEvent>().collect {
            logger.i { "Android SessionStore 已更新，正在保存至文件..." }
            withFs { androidSessionStorePath.writeText(it.sessionStore.toJson()) }
        }
    }
}

private val sessionRenewalJobKey = AttributeKey<Job>("SessionRenewalJob")

suspend fun Application.configureSessionAutoRenewal() {
    if (!YogurtApp.config.protocol.autoRenewSession || attributes.contains(sessionRenewalJobKey)) return
    val bot = dependencies.resolve<AbstractBot>() as? Bot ?: return
    val logger = bot.createLogger("SessionRenewal")
    val job = launch renewal@{
        val schedule = SessionRenewalSchedule(Random.nextLong(60, 301))
        val offlineWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
            bot.eventFlow.filterIsInstance<BotOfflineEvent>().first()
            this@renewal.cancel()
        }
        try {
            while (isActive && bot.isLoggedIn) {
                val now = Clock.System.now().epochSeconds
                val wait = schedule.nextDelaySeconds(bot.sessionStore.credentialsUpdatedAtEpochSeconds, now)
                if (wait == null || wait > 0) {
                    // Recheck manual renewals, clock changes and explicit offline at least once a minute.
                    delay((wait ?: 60L).coerceAtMost(60L).seconds)
                    continue
                }
                val previousA2 = bot.sessionStore.a2
                try {
                    logger.i { "开始自动续约登录凭据" }
                    bot.renewSession()
                    logger.i { "登录凭据已续约并重新上线" }
                    schedule.reset()
                } catch (e: CancellationException) {
                    // A timeout inside renewSession is a retryable failure; application cancellation is not.
                    if (!isActive) throw e
                    schedule.retry(Clock.System.now().epochSeconds)
                    logger.w(e) { "自动续约超时，将退避重试" }
                } catch (e: SessionRenewalException) {
                    schedule.pause()
                    logger.e(e) { "自动续约需要人工处理，请重新登录；自动尝试已暂停" }
                } catch (e: Exception) {
                    if (bot.sessionStore.a2 !== previousA2) {
                        schedule.reset()
                        logger.w(e) { "新凭据已获取，连接将继续自动恢复" }
                    } else {
                        schedule.retry(Clock.System.now().epochSeconds)
                        logger.w(e) { "自动续约失败，将退避重试" }
                    }
                }
            }
        } finally {
            offlineWatcher.cancel()
        }
    }
    attributes.put(sessionRenewalJobKey, job)
}

private class SessionRenewalSchedule(private val unknownAgeDelaySeconds: Long) {
    private var initialized = false
    private var observedTimestamp: Long? = null
    private var dueAt = 0L
    private var failures = 0
    private var paused = false

    fun nextDelaySeconds(updatedAt: Long?, now: Long): Long? {
        if (!initialized || observedTimestamp != updatedAt) {
            initialized = true
            observedTimestamp = updatedAt
            dueAt = if (updatedAt == null) now + unknownAgeDelaySeconds else updatedAt + 15 * 86400L
            failures = 0
            paused = false
        }
        return if (paused) null else (dueAt - now).coerceAtLeast(0L)
    }

    fun retry(now: Long) {
        val delays = longArrayOf(60, 300, 900, 3600)
        dueAt = now + delays[failures.coerceAtMost(delays.lastIndex)]
        failures = (failures + 1).coerceAtMost(delays.lastIndex)
    }

    fun pause() {
        paused = true
    }

    fun reset() {
        initialized = false
    }
}
