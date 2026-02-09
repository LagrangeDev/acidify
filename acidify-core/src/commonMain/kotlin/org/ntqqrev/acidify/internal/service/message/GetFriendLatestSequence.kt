package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.message.action.SsoGetPeerSeqReq
import org.ntqqrev.acidify.internal.proto.message.action.SsoGetPeerSeqResp
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.math.max

internal object GetFriendLatestSequence : Service<String, Long>("trpc.msg.msg_svc.MsgService.SsoGetPeerSeq") {
    override fun build(client: AbstractClient, payload: String): ByteArray {
        return SsoGetPeerSeqReq(
            peerUid = payload
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray): Long {
        val resp = payload.pbDecode<SsoGetPeerSeqResp>()
        val seq1 = resp.seq1
        val seq2 = resp.seq2
        return max(seq1, seq2)
    }
}
