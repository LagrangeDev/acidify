package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SsoGetPeerSeqReq(
    @ProtoNumber(1) val peerUid: String = "",
)

@Serializable
internal class SsoGetPeerSeqResp(
    @ProtoNumber(3) val seq1: Long = 0L,
    @ProtoNumber(4) val seq2: Long = 0L,
    @ProtoNumber(5) val latestMsgTime: Long = 0L,
)
