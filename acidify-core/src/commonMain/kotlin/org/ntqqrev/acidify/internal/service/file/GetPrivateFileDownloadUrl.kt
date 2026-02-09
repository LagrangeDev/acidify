package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0xE37Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0xE37Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object GetPrivateFileDownloadUrl : OidbService<GetPrivateFileDownloadUrl.Req, String>(0xe37, 1200, true) {
    class Req(
        val receiverUid: String,
        val fileUuid: String,
        val fileHash: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0xE37Req(
            subCommand = 1200,
            seq = 1,
            downloadBody = Oidb0xE37Req.DownloadBody(
                receiverUid = payload.receiverUid,
                fileUuid = payload.fileUuid,
                type = 2,
                fileHash = payload.fileHash,
                t2 = 0,
            ),
            field101 = 3,
            field102 = 103,
            field200 = 1,
            field99999 = byteArrayOf(0xc0.toByte(), 0x85.toByte(), 0x2c, 0x01),
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): String {
        val resp = payload.pbDecode<Oidb0xE37Resp>().downloadBody.result
        val server = resp.server
        val port = resp.port
        val urlPath = resp.url
        return "http://$server:$port$urlPath&isthumb=0"
    }
}
