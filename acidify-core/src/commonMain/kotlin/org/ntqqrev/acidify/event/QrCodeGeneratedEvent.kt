package org.ntqqrev.acidify.event

/**
 * 二维码生成事件
 * @property url 二维码链接
 * @property png 二维码 PNG 图片数据
 */
class QrCodeGeneratedEvent(val url: String, val png: ByteArray) : AcidifyEvent