package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.*
import org.ntqqrev.acidify.internal.proto.message.action.PbSendMsgReq
import org.ntqqrev.acidify.internal.proto.message.action.PbSendMsgResp
import org.ntqqrev.acidify.internal.proto.message.extra.PrivateFileExtra
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal object BroadcastPrivateFile :
    Service<BroadcastPrivateFile.Req, BroadcastPrivateFile.Resp>("MessageSvc.PbSendMsg") {
    class Req(
        val friendUin: Long,
        val friendUid: String,
        val fileId: String,
        val fileMd510M: ByteArray,
        val fileName: String,
        val fileSize: Long,
        val crcMedia: String
    )

    class Resp(
        val result: Int,
        val errMsg: String,
        val sendTime: Long,
        val sequence: Long
    )

    @OptIn(ExperimentalTime::class)
    override fun build(client: LagrangeClient, payload: Req): ByteArray =
        PbSendMsgReq(
            routingHead = SendRoutingHead(
                trans211 = Trans211(
                    toUin = payload.friendUin,
                    ccCmd = 4,
                    uid = payload.friendUid,
                )
            ),
            contentHead = SendContentHead(
                pkgNum = 1,
            ),
            messageBody = MessageBody(
                msgContent = PrivateFileExtra(
                    notOnlineFile = NotOnlineFile(
                        fileUuid = payload.fileId,
                        fileMd5 = payload.fileMd510M,
                        fileName = payload.fileName,
                        fileSize = payload.fileSize,
                        subCmd = 1,
                        dangerLevel = 0,
                        expireTime = Clock.System.now().epochSeconds + 86400 * 7,
                        fileIdCrcMedia = payload.crcMedia,
                    )
                ).pbEncode()
            ),
            clientSequence = Random.nextLong(),
            random = Random.nextInt(),
        ).pbEncode()

    override fun parse(client: LagrangeClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<PbSendMsgResp>()
        return Resp(
            result = resp.result,
            errMsg = resp.errMsg,
            sendTime = resp.sendTime,
            sequence = resp.clientSequence
        )
    }
}
