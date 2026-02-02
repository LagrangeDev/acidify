package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.CommonMessage

@Serializable
internal class PbMultiMsgTransmit(
    @ProtoNumber(1) val messages: List<CommonMessage> = emptyList(),
    @ProtoNumber(2) val items: List<PbMultiMsgItem> = emptyList(),
)

@Serializable
internal class PbMultiMsgItem(
    @ProtoNumber(1) val fileName: String = "",
    @ProtoNumber(2) val buffer: PbMultiMsgNew? = null,
)

@Serializable
internal class PbMultiMsgNew(
    @ProtoNumber(1) val msg: List<CommonMessage> = emptyList(),
)

@Serializable
internal class LongMsgInterfaceReq(
    @ProtoNumber(1) val recvReq: LongMsgRecvReq? = null,
    @ProtoNumber(2) val sendReq: LongMsgSendReq? = null,
    @ProtoNumber(15) val attr: LongMsgAttr? = null,
)

@Serializable
internal class LongMsgInterfaceResp(
    @ProtoNumber(1) val recvResp: LongMsgRecvResp? = null,
    @ProtoNumber(2) val sendResp: LongMsgSendResp? = null,
    @ProtoNumber(15) val attr: LongMsgAttr? = null,
)

@Serializable
internal class LongMsgAttr(
    @ProtoNumber(1) val subCmd: Int = 0,
    @ProtoNumber(2) val clientType: Int = 0,
    @ProtoNumber(3) val platform: Int = 0,
    @ProtoNumber(4) val proxyType: Int = 0,
)

@Serializable
internal class LongMsgPeerInfo(
    @ProtoNumber(2) val peerUid: String = "",
)

@Serializable
internal class LongMsgRecvReq(
    @ProtoNumber(1) val peerInfo: LongMsgPeerInfo? = null,
    @ProtoNumber(2) val resId: String = "",
    @ProtoNumber(3) val msgType: Int = 0,
)

@Serializable
internal class LongMsgSendReq(
    @ProtoNumber(1) val msgType: Int = 0,
    @ProtoNumber(2) val peerInfo: LongMsgPeerInfo? = null,
    @ProtoNumber(3) val groupUin: Long = 0L,
    @ProtoNumber(4) val payload: ByteArray = byteArrayOf(),
)

@Serializable
internal class LongMsgSendResp(
    @ProtoNumber(3) val resId: String = "",
)

@Serializable
internal class LongMsgRecvResp(
    @ProtoNumber(3) val resId: String = "",
    @ProtoNumber(4) val payload: ByteArray = byteArrayOf(),
)
