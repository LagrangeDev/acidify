package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0xE37Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0xE37Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object GetPrivateFileDownloadUrl : OidbService<GetPrivateFileDownloadUrl.Req, String>(0xe37, 1200, true) {
    class Req(
        val receiverUid: String,
        val fileUuid: String,
        val fileHash: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0xE37Req {
            it[subCommand] = 1200
            it[seq] = 1
            it[downloadBody] = Oidb0xE37Req.DownloadBody {
                it[receiverUid] = payload.receiverUid
                it[fileUuid] = payload.fileUuid
                it[type] = 2
                it[fileHash] = payload.fileHash
                it[t2] = 0
            }
            it[field101] = 3
            it[field102] = 103
            it[field200] = 1
            it[field99999] = byteArrayOf(0xc0.toByte(), 0x85.toByte(), 0x2c, 0x01)
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): String {
        val resp = Oidb0xE37Resp(payload).get { downloadBody }.get { result }
        val server = resp.get { server }
        val port = resp.get { port }
        val urlPath = resp.get { url }
        return "http://$server:$port$urlPath&isthumb=0"
    }
}