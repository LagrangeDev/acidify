package org.ntqqrev.acidify.internal.json.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal open class LightAppPayload(val app: String) {
    companion object {
        val jsonModule = Json {
            ignoreUnknownKeys = true
        }
    }
}