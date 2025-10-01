package org.ntqqrev.acidify.message

/**
 * 图片子类型枚举
 */
enum class ImageSubType(val underlying: Int) {
    /**
     * 普通图片
     */
    NORMAL(0),

    /**
     * 表情包（一般展示为 `动画表情`）
     */
    STICKER(1),
}