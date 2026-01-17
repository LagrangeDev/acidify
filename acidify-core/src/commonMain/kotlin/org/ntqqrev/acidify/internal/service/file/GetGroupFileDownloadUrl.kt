package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object GetGroupFileDownloadUrl : OidbService<GetGroupFileDownloadUrl.Req, String>(0x6d6, 2, true) {
    class Req(
        val groupUin: Long,
        val fileId: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0x6D6Req {
            it[downloadFile] = Oidb0x6D6Req.DownloadFile {
                it[groupUin] = payload.groupUin
                it[appId] = 7
                it[busId] = 102
                it[fileId] = payload.fileId
            }
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): String {
        val resp = Oidb0x6D6Resp(payload).get { downloadFile }
        val dns = resp.get { downloadDns }
        val url = resp.get { downloadUrl }.toHexString()
        return "https://$dns/ftn_handler/$url/?fname="
    }
}