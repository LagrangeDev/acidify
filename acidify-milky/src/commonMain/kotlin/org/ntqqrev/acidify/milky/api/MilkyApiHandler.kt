package org.ntqqrev.acidify.milky.api

import kotlinx.serialization.json.JsonElement
import org.ntqqrev.acidify.milky.MediaSourceScope
import org.ntqqrev.acidify.milky.MilkyContext

class MilkyApiHandler<T : Any, R : Any>(
    val path: String,
    val transformInput: (JsonElement) -> T,
    val transformOutput: (R) -> JsonElement,
    val callHandler: suspend context(MediaSourceScope) MilkyContext.(payload: T) -> R,
) {
    context(ctx: MilkyContext, _: MediaSourceScope)
    suspend fun handleRaw(input: JsonElement): JsonElement {
        val payload = transformInput(input)
        val result = callHandler(ctx, payload)
        return transformOutput(result)
    }
}