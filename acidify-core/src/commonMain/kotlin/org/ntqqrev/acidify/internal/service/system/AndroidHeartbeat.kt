package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.NoOutputService
import org.ntqqrev.acidify.internal.service.RequestType

internal object AndroidHeartbeat : NoOutputService<Unit>("Heartbeat.Alive") {
    override val ssoRequestType = RequestType.Simple
    override val ssoEncryptType = EncryptType.None

    override fun build(client: org.ntqqrev.acidify.internal.AbstractClient, payload: Unit): ByteArray =
        byteArrayOf(0, 0, 0, 4)
}