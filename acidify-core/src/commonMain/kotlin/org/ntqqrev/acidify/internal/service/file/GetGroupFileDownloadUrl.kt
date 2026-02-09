package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object GetGroupFileDownloadUrl : OidbService<GetGroupFileDownloadUrl.Req, String>(0x6d6, 2, true) {
    class Req(
        val groupUin: Long,
        val fileId: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x6D6Req(
            downloadFile = Oidb0x6D6Req.DownloadFile(
                groupUin = payload.groupUin,
                appId = 7,
                busId = 102,
                fileId = payload.fileId,
            )
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): String {
        val resp = payload.pbDecode<Oidb0x6D6Resp>().downloadFile
        val dns = resp.downloadDns
        val url = resp.downloadUrl.toHexString()
        return "https://$dns/ftn_handler/$url/?fname="
    }
}
