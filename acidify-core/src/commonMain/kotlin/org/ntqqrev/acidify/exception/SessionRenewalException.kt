package org.ntqqrev.acidify.exception

import kotlin.js.JsExport

/**
 * A1 session renewal was rejected or requires user verification.
 * Transport failures retain their original exception types.
 */
@JsExport
class SessionRenewalException internal constructor(
    val code: Int,
    val tag: String,
    val msg: String,
    val unusualSig: ByteArray? = null,
) : Exception("Session renewal failed with code=$code ($tag: $msg)")
