package org.ntqqrev.yogurt.scripting.stdlib

import com.dokar.quickjs.binding.JsObject
import kotlinx.io.files.Path

internal fun Array<Any?>.requireSize(range: IntRange, signature: String) {
    require(size in range) { "Expected arguments: $signature" }
}

internal fun Array<Any?>.stringArg(index: Int, name: String): String {
    return this[index] as? String
        ?: throw IllegalArgumentException("Expected argument '$name' to be a string")
}

internal fun Array<Any?>.jsObjectArg(index: Int, name: String): JsObject? {
    val value = getOrNull(index) ?: return null
    return value as? JsObject
        ?: throw IllegalArgumentException("Expected argument '$name' to be an object")
}

internal fun Array<Any?>.pathArg(index: Int, name: String): Path {
    return Path(stringArg(index, name))
}

internal fun Array<Any?>.booleanArg(index: Int, name: String, default: Boolean): Boolean {
    val value = getOrNull(index) ?: return default
    return value as? Boolean
        ?: throw IllegalArgumentException("Expected argument '$name' to be a boolean")
}

@OptIn(ExperimentalUnsignedTypes::class)
internal fun Any?.toByteArrayArg(name: String): ByteArray {
    return when (this) {
        is ByteArray -> this
        is UByteArray -> ByteArray(size) { this[it].toByte() }
        else -> throw IllegalArgumentException("Expected argument '$name' to be an Int8Array or Uint8Array")
    }
}

