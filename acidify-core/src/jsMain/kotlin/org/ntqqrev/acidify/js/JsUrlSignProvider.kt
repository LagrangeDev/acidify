package org.ntqqrev.acidify.js

import kotlinx.coroutines.promise
import org.ntqqrev.acidify.common.SignResult
import org.ntqqrev.acidify.common.UrlSignProvider
import kotlin.js.Promise

@JsExport
@JsName("UrlSignProvider")
class JsUrlSignProvider(
    val scope: JsCoroutineScope,
    url: String,
    httpProxy: String? = null
) : JsSignProvider {
    private val urlSignProvider = UrlSignProvider(url, httpProxy)
    override fun sign(
        cmd: String,
        seq: Int,
        src: ByteArray
    ): Promise<SignResult> = scope.value.promise {
        urlSignProvider.sign(cmd, seq, src)
    }
}