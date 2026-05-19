package org.ntqqrev.acidify.milky.event

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.milky.milkyJsonModule

val webhookClient = HttpClient {
    install(ContentNegotiation) {
        json(milkyJsonModule)
    }
}

context(ctx: MilkyContext)
fun Application.eventWebhook() = monitor.subscribe(ApplicationStarted) {
    this.launch {
        val logger = ctx.bot.createLogger("WebhookModule")
        ctx.eventFlow.collect {
            ctx.webhookEndpoints.forEach { webhook ->
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
                    }
                }
            }
        }
    }
}