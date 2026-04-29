package org.ntqqrev.acidify.internal.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 200.milliseconds,
    val maxDelay: Duration = 5.seconds,
    val multiplier: Double = 2.0,
    val jitterRatio: Double = 0.2,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(initialDelay >= Duration.ZERO) { "initialDelay must be >= 0" }
        require(maxDelay >= initialDelay) { "maxDelay must be >= initialDelay" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
        require(jitterRatio in 0.0..1.0) { "jitterRatio must be in 0.0..1.0" }
    }
}

internal suspend inline fun <T> withRetry(
    policy: RetryPolicy = RetryPolicy(),
    shouldRetry: (Throwable) -> Boolean = { true },
    onRetry: (
        attempt: Int,
        cause: Throwable,
        nextDelay: Duration,
    ) -> Unit = { _, _, _ -> },
    block: (attempt: Int) -> T,
): T {
    var attempt = 1
    var nextDelay = policy.initialDelay

    while (true) {
        try {
            return block(attempt)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            val canRetry = attempt < policy.maxAttempts && shouldRetry(e)
            if (!canRetry) throw e

            val delayWithJitter = applyJitter(nextDelay, policy.jitterRatio)

            onRetry(attempt, e, delayWithJitter)

            delay(delayWithJitter)

            attempt++
            nextDelay = (nextDelay * policy.multiplier).coerceAtMost(policy.maxDelay)
        }
    }
}

internal fun applyJitter(
    baseDelay: Duration,
    jitterRatio: Double,
): Duration {
    if (baseDelay == Duration.ZERO || jitterRatio == 0.0) {
        return baseDelay
    }
    val factor = 1.0 - jitterRatio + Random.nextDouble() * jitterRatio * 2.0
    return (baseDelay * factor).coerceAtLeast(Duration.ZERO)
}