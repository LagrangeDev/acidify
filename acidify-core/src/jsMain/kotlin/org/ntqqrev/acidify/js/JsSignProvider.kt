package org.ntqqrev.acidify.js

import org.ntqqrev.acidify.common.SignResult
import kotlin.js.Promise

@JsExport
@JsName("SignProvider")
interface JsSignProvider {
    fun sign(cmd: String, seq: Int, src: ByteArray): Promise<SignResult>
}