package org.ntqqrev.acidify.message

import kotlin.js.JsExport

/**
 * 图像格式枚举
 */
@JsExport
enum class ImageFormat(val ext: String, val underlying: Int) {
    PNG("png", 1001),
    GIF("gif", 2000),
    JPEG("jpg", 1000),
    BMP("bmp", 1005),
    WEBP("webp", 1002),
    TIFF("tiff", 1006);

    companion object {
        fun fromExtension(ext: String): ImageFormat? {
            return entries.find { it.ext.equals(ext, ignoreCase = true) }
        }

        fun fromUnderlying(value: Int): ImageFormat? {
            return entries.find { it.underlying == value }
        }
    }
}