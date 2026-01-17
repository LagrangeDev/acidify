package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.action.SsoGetPeerSeqReq
import org.ntqqrev.acidify.internal.packet.message.action.SsoGetPeerSeqResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.Service
import kotlin.math.max

internal object GetFriendLatestSequence : Service<String, Long>("trpc.msg.msg_svc.MsgService.SsoGetPeerSeq") {
    override fun build(client: IClient, payload: String): ByteArray {
        return SsoGetPeerSeqReq {
            it[peerUid] = payload
        }.toByteArray()
    }

    override fun parse(client: IClient, payload: ByteArray): Long {
        val resp = SsoGetPeerSeqResp(payload)
        val seq1 = resp.get { seq1 }
        val seq2 = resp.get { seq2 }
        return max(seq1, seq2)
    }
}

