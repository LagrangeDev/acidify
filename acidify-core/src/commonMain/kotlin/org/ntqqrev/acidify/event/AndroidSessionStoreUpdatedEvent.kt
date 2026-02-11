package org.ntqqrev.acidify.event

import org.ntqqrev.acidify.common.android.AndroidSessionStore

/**
 * Android 会话存储更新事件
 * @property sessionStore 更新后的会话存储
 */
data class AndroidSessionStoreUpdatedEvent(
    val sessionStore: AndroidSessionStore,
) : AcidifyEvent