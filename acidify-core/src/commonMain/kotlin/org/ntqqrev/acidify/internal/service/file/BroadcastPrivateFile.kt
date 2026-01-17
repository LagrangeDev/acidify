package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.*
import org.ntqqrev.acidify.internal.packet.message.action.PbSendMsgReq
import org.ntqqrev.acidify.internal.packet.message.action.PbSendMsgResp
import org.ntqqrev.acidify.internal.packet.message.extra.PrivateFileExtra
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.Service
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
    override fun build(client: IClient, payload: Req): ByteArray =
        PbSendMsgReq {
            it[routingHead] = SendRoutingHead {
                it[trans211] = Trans211 {
                    it[toUin] = payload.friendUin
                    it[ccCmd] = 4
                    it[uid] = payload.friendUid
                }
            }
            it[contentHead] = SendContentHead {
                it[pkgNum] = 1
            }
            it[messageBody] = MessageBody {
                it[msgContent] = PrivateFileExtra {
                    it[notOnlineFile] = NotOnlineFile {
                        it[fileUuid] = payload.fileId
                        it[fileMd5] = payload.fileMd510M
                        it[fileName] = payload.fileName
                        it[fileSize] = payload.fileSize
                        it[subCmd] = 1
                        it[dangerLevel] = 0
                        it[expireTime] = Clock.System.now().epochSeconds + 86400 * 7
                        it[fileIdCrcMedia] = payload.crcMedia
                    }
                }.toByteArray()
            }
            it[clientSequence] = Random.nextLong()
            it[random] = Random.nextInt()
        }.toByteArray()

    override fun parse(client: IClient, payload: ByteArray): Resp {
        val resp = PbSendMsgResp(payload)
        return Resp(
            result = resp.get { result },
            errMsg = resp.get { errMsg },
            sendTime = resp.get { sendTime },
            sequence = resp.get { clientSequence }
        )
    }
}