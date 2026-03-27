package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.NoOutputService
import org.ntqqrev.acidify.internal.service.RequestType

internal object Alive : NoOutputService<Unit>("Heartbeat.Alive") {
    override val ssoRequestType = RequestType.Simple
    override val ssoEncryptType = EncryptType.None

    override fun build(client: AbstractClient, payload: Unit): ByteArray =
        byteArrayOf(0, 0, 0, 4)
}