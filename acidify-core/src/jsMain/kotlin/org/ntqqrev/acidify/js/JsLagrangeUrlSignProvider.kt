package org.ntqqrev.acidify.js

import kotlinx.coroutines.promise
import org.ntqqrev.acidify.common.LagrangeUrlSignProvider
import org.ntqqrev.acidify.common.SignResult
import kotlin.js.Promise

@JsExport
@JsName("LagrangeUrlSignProvider")
@AcidifyJsWrapper
class JsLagrangeUrlSignProvider(
    val scope: JsCoroutineScope,
    url: String,
    token: String,
    uin: Long,
    guid: String,
    qua: String,
    httpProxy: String? = null
) : JsSignProvider {
    private val urlSignProvider = LagrangeUrlSignProvider(url, token, uin, guid, qua, httpProxy)

    override fun sign(
        cmd: String,
        seq: Int,
        src: ByteArray
    ): Promise<SignResult?> = scope.value.promise {
        urlSignProvider.sign(cmd, seq, src)
    }
}
