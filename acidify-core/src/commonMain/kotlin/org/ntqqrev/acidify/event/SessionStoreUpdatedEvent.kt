package org.ntqqrev.acidify.event

import org.ntqqrev.acidify.common.SessionStore
import kotlin.js.JsExport

/**
 * 会话存储更新事件
 * @property sessionStore 更新后的会话存储
 */
@JsExport
class SessionStoreUpdatedEvent(val sessionStore: SessionStore) : AcidifyEvent