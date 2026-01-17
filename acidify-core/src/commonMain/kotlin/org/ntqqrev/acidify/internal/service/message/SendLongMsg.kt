package org.ntqqrev.acidify.internal.service.message

import korlibs.io.compression.compress
import korlibs.io.compression.deflate.GZIP
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.CommonMessage
import org.ntqqrev.acidify.internal.packet.message.action.*
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.message.MessageScene

internal object SendLongMsg :
    Service<SendLongMsg.Req, String>("trpc.group.long_msg_interface.MsgService.SsoSendLongMsg") {
    class Req(
        val scene: MessageScene,
        val peerUin: Long,
        val peerUid: String,
        val messages: List<PbObject<CommonMessage>>,
        val nestedForwardTrace: Map<String, List<PbObject<CommonMessage>>>
    )

    override fun build(client: IClient, payload: Req): ByteArray {
        val content = PbMultiMsgTransmit {
            it[items] = buildList {
                this.add(PbMultiMsgItem {
                    it[fileName] = "MultiMsg"
                    it[buffer] = PbMultiMsgNew {
                        it[msg] = payload.messages
                    }
                })
                this.addAll(payload.nestedForwardTrace.map { (key, value) ->
                    PbMultiMsgItem {
                        it[fileName] = key
                        it[buffer] = PbMultiMsgNew {
                            it[msg] = value
                        }
                    }
                })
            }
        }

        val compressedContent = GZIP.compress(content.toByteArray())

        val longMsg = LongMsgInterfaceReq {
            it[sendReq] = LongMsgSendReq {
                it[msgType] = if (payload.scene == MessageScene.FRIEND) 1 else 3
                it[peerInfo] = LongMsgPeerInfo {
                    it[peerUid] = payload.peerUid
                }
                it[groupUin] = if (payload.scene == MessageScene.GROUP) payload.peerUin else 0L
                it[this.payload] = compressedContent
            }
            it[attr] = LongMsgAttr {
                it[subCmd] = 4
                it[clientType] = 1
                it[platform] = when (client.os) {
                    "Windows" -> 3
                    "Linux" -> 6
                    "Mac" -> 7
                    else -> 0
                }
                it[proxyType] = 0
            }
        }

        return longMsg.toByteArray()
    }

    override fun parse(client: IClient, payload: ByteArray): String {
        val rsp = PbObject(LongMsgInterfaceResp, payload)
        return rsp.get { sendResp }?.get { this.resId }
            ?: throw IllegalStateException("No resId in LongMsgInterfaceResp")
    }
}

