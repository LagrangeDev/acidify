package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.media.*
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.message.MessageScene

internal abstract class RichMediaDownload(
    oidbCommand: Int,
    oidbService: Int,
    val requestType: Int,
    val businessType: Int,
    val scene: MessageScene,
) : OidbService<PbObject<IndexNode>, String>(oidbCommand, oidbService) {
    override fun buildOidb(client: IClient, payload: PbObject<IndexNode>): ByteArray = NTV2RichMediaReq {
        it[reqHead] = MultiMediaReqHead {
            it[common] = CommonHead {
                it[requestId] = 1
                it[command] = oidbService
            }
            it[scene] = SceneInfo {
                it[requestType] = this@RichMediaDownload.requestType
                it[businessType] = this@RichMediaDownload.businessType
                when (this@RichMediaDownload.scene) {
                    MessageScene.FRIEND -> {
                        it[sceneType] = 1
                        it[c2C] = C2CUserInfo {
                            it[targetUid] = client.uid
                            it[accountType] = 2
                        }
                    }

                    MessageScene.GROUP -> {
                        it[sceneType] = 2
                    }

                    else -> {}
                }
            }
            it[this.client] = ClientMeta {
                it[agentType] = 2
            }
        }
        it[download] = DownloadReq {
            it[node] = payload
        }
    }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): String =
        NTV2RichMediaResp(payload).let {
            val download = it.get { download }
            val downloadInfo = download.get { info }
            "https://" + downloadInfo.get { domain } + downloadInfo.get { urlPath } + download.get { rKeyParam }
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