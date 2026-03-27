package org.ntqqrev.acidify.internal.json.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
internal class ForwardJsonPayload(
    val app: String = "com.tencent.multimsg",
    val config: JsonObject,
    val meta: Meta,
    val desc: String,
    val extra: JsonElement,
    val prompt: String,
    val ver: String,
    val view: String,
) {
    @Serializable
    class Meta(
        val detail: Detail
    ) {
        @Serializable
        class Detail(
            val news: List<NewsItem>,
            val resid: String,
            val source: String,
            val summary: String,
            val uniseq: String,
        ) {
            @Serializable
            class NewsItem(
                val text: String
            )
        }
    }
}