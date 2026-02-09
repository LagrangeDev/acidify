package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.message.media.*
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.message.MessageScene

internal abstract class RichMediaDownload(
    oidbCommand: Int,
    oidbService: Int,
    val requestType: Int,
    val businessType: Int,
    val scene: MessageScene,
) : OidbService<IndexNode, String>(oidbCommand, oidbService) {
    override fun buildOidb(client: AbstractClient, payload: IndexNode): ByteArray = NTV2RichMediaReq(
        reqHead = MultiMediaReqHead(
            common = CommonHead(
                requestId = 1,
                command = oidbService,
            ),
            scene = SceneInfo(
                requestType = requestType,
                businessType = businessType,
                sceneType = when (scene) {
                    MessageScene.FRIEND -> 1
                    MessageScene.GROUP -> 2
                    else -> 0
                },
                c2C = if (scene == MessageScene.FRIEND) {
                    C2CUserInfo(
                        targetUid = client.uid,
                        accountType = 2,
                    )
                } else {
                    C2CUserInfo()
                }
            ),
            client = ClientMeta(agentType = 2),
        ),
        download = DownloadReq(node = payload),
    ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): String =
        payload.pbDecode<NTV2RichMediaResp>().let {
            val download = it.download
            val downloadInfo = download.info
            "https://" + downloadInfo.domain + downloadInfo.urlPath + download.rKeyParam
        }

    object GroupImage : RichMediaDownload(
        oidbCommand = 0x11c4,
        oidbService = 200,
        requestType = 2,
        businessType = 1,
        scene = MessageScene.GROUP
    )

    object GroupRecord : RichMediaDownload(
        oidbCommand = 0x126e,
        oidbService = 200,
        requestType = 1,
        businessType = 3,
        scene = MessageScene.GROUP
    )

    object GroupVideo : RichMediaDownload(
        oidbCommand = 0x11ea,
        oidbService = 200,
        requestType = 2,
        businessType = 2,
        scene = MessageScene.GROUP
    )

    object PrivateImage : RichMediaDownload(
        oidbCommand = 0x11c5,
        oidbService = 200,
        requestType = 2,
        businessType = 1,
        scene = MessageScene.FRIEND
    )

    object PrivateRecord : RichMediaDownload(
        oidbCommand = 0x126d,
        oidbService = 200,
        requestType = 1,
        businessType = 3,
        scene = MessageScene.FRIEND
    )

    object PrivateVideo : RichMediaDownload(
        oidbCommand = 0x11e9,
        oidbService = 200,
        requestType = 2,
        businessType = 2,
        scene = MessageScene.FRIEND
    )
}
