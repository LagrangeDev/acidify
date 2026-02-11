package org.ntqqrev.acidify.entity

import org.ntqqrev.acidify.AbstractBot

/**
 * Bot 实体基类
 *
 * 实体类是对数据的视图封装，背后绑定的数据是可以更改的，而实体本身只是一个视图。
 *
 * @param T 数据绑定的类型
 * @property bot Bot 实例
 * @property data 绑定的数据
 */
abstract class BotEntity<T>(
    val bot: AbstractBot,
    internal var data: T,
) {
    /**
     * 更新绑定的数据
     */
    internal fun updateBinding(data: T) {
        this.data = data
    }
}