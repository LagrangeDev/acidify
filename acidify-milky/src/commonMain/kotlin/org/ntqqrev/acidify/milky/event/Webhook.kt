package org.ntqqrev.acidify.milky.event

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.milky.milkyJsonModule

private const val WEBHOOK_CONNECT_TIMEOUT_MILLIS = 5_000L
private const val WEBHOOK_REQUEST_TIMEOUT_MILLIS = 15_000L
private const val WEBHOOK_MAX_IN_FLIGHT_PER_ENDPOINT = 16

val webhookClient = HttpClient {
    install(ContentNegotiation) {
        json(milkyJsonModule)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = WEBHOOK_CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = WEBHOOK_REQUEST_TIMEOUT_MILLIS
    }
}

context(ctx: MilkyContext)
fun Application.eventWebhook() = monitor.subscribe(ApplicationStarted) {
    this.launch {
        val bot = dependencies.resolve<AbstractBot>()
        val logger = bot.createLogger("WebhookModule")
        // One collector per endpoint so a stalled endpoint cannot hold back the others.
        // Once its in-flight limit is reached, the collector suspends and eventFlow drops the oldest events.
        ctx.webhookEndpoints.forEach { webhook ->
            launch {
                val inFlight = Semaphore(WEBHOOK_MAX_IN_FLIGHT_PER_ENDPOINT)
                ctx.eventFlow.collect {
                    inFlight.acquire()
                    launch {
                        try {
                            webhookClient.post(webhook.url) {
                                if (webhook.accessToken.isNotEmpty()) {
                                    bearerAuth(webhook.accessToken)
                                }
                                contentType(ContentType.Application.Json)
                                setBody(it)
                            }
                        } catch (e: Exception) {
                            logger.w(e) { "发送事件到 Webhook URL ${webhook.url} 失败" }
                        } finally {
                            inFlight.release()
                        }
                    }
                }
            }
        }
    }
}
