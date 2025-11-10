package org.ntqqrev.acidify.logging

import kotlin.js.JsExport
import kotlin.reflect.KClass

internal expect val KClass<*>.loggingTag: String?

/**
 * 缩短包名以用于日志输出，将中间部分缩写为首字母
 * @param tag 完整包名
 */
@JsExport
fun shortenPackageName(tag: String): String {
    val parts = tag.split('.')
    val b = StringBuilder()
    for (i in 0 until parts.size - 1) {
        b.append(parts[i][0])
        b.append('.')
    }
    b.append(parts.last())
    b.padEnd(30)
    return b.toString()
}