package org.ntqqrev.yogurt.event

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.milky.Event
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.yogurt.YogurtApp.config

val webhookClient = HttpClient {
    install(ContentNegotiation) {
        json(milkyJsonModule)
    }
}

fun Application.configureMilkyEventWebhook() = launch {
    val bot = dependencies.resolve<AbstractBot>()
    val flow = dependencies.resolve<SharedFlow<Event>>()
    val logger = bot.createLogger("WebhookModule")
    flow.collect {
        config.milky.webhook.endpoints.forEach { webhook ->
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
