package org.ntqqrev.acidify.common

import kotlin.js.JsExport

@JsExport
class SignResult(
    val sign: ByteArray,
    val token: ByteArray,
    val extra: ByteArray,
)