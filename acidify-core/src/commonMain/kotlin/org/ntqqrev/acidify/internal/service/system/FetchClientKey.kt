package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchClientKeyResp
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.pb.invoke

internal object FetchClientKey : NoInputOidbService<String>(0x102a, 1) {
    // It's actually empty!
    override fun buildOidb(client: LagrangeClient, payload: Unit): ByteArray = ByteArray(0)

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): String =
        FetchClientKeyResp(payload).get { clientKey }
}