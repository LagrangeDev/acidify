package org.ntqqrev.acidify.common

import co.touchlab.stately.concurrency.AtomicBoolean
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.entity.AbstractEntity

/**
 * Bot 缓存服务
 *
 * 用于管理实体的缓存，支持自动更新和线程安全的访问。
 *
 * @param K 缓存键的类型
 * @param V 实体类型，必须继承自 [AbstractEntity]
 * @property bot Bot 实例
 * @property updateCache 更新缓存的函数，返回键到数据绑定的映射
 * @property entityFactory 实体工厂函数，用于创建新的实体实例
 */
class CacheUtility<K, V : AbstractEntity<D>, D>(
    val bot: Bot,
    private val updateCache: suspend (bot: Bot) -> Map<K, D>,
    private val entityFactory: (bot: Bot, data: D) -> V,
) {
    private val mutex = Mutex()
    private var map = mutableMapOf<K, V>()
    private val updating = AtomicBoolean(false)
    private val logger = bot.createLogger(this)

    /**
     * 获取指定键的实体
     *
     * @param key 实体的键
     * @param forceUpdate 是否强制更新缓存
     * @return 实体对象，如果不存在则返回 null
     */
    suspend fun get(key: K, forceUpdate: Boolean = false): V? {
        if (!map.containsKey(key) || forceUpdate) {
            logger.v { "缓存中 $key 不存在，请求刷新" }
            update()
        }
        return map[key]
    }

    /**
     * 获取所有实体
     *
     * @param forceUpdate 是否强制更新缓存
     * @return 所有实体的列表
     */
    suspend fun getAll(forceUpdate: Boolean = false): List<V> {
        if (forceUpdate || map.isEmpty()) {
            update()
        }
        return map.values.toList()
    }

    /**
     * 更新缓存
     */
    suspend fun update() {
        if (updating.value) {
            logger.v { "重复的刷新请求，已忽略" }
            mutex.withLock { } // 等待正在进行的更新完成
        } else {
            updating.value = true
            mutex.withLock {
                try {
                    val data = updateCache(bot)
                    acceptData(data)
                } catch (e: Exception) {
                    logger.w(e) { "缓存刷新失败" }
                } finally {
                    updating.value = false
                }
            }
        }
    }

    /**
     * 接受新的数据并更新缓存
     *
     * @param data 新的数据映射
     */
    @Suppress("UNCHECKED_CAST")
    fun acceptData(data: Map<K, D>) {
        val newMap = mutableMapOf<K, V>()
        for ((key, value) in data.entries) {
            val entity = map[key]
            if (entity != null) {
                entity.updateBinding(value)
                newMap[key] = entity
            } else {
                newMap[key] = entityFactory(bot, value)
            }
        }
        map = newMap
    }
}