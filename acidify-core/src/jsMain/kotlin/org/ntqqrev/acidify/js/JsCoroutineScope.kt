package org.ntqqrev.acidify.js

import kotlinx.coroutines.*

@JsExport
@JsName("CoroutineScope")
class JsCoroutineScope(
    val isSupervised: Boolean = false
) {
    internal val value = CoroutineScope(
        Dispatchers.Default
                + if (isSupervised) SupervisorJob() else Job()
    )

    fun cancel() {
        value.cancel()
    }
}