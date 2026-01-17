package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchClientKeyResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputOidbService

internal object FetchClientKey : NoInputOidbService<String>(0x102a, 1) {
    // It's actually empty!
    override fun buildOidb(client: IClient, payload: Unit): ByteArray = ByteArray(0)

    override fun parseOidb(client: IClient, payload: ByteArray): String =
        FetchClientKeyResp(payload).get { clientKey }
}