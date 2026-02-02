package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.MessageBody
import org.ntqqrev.acidify.internal.proto.message.SendContentHead
import org.ntqqrev.acidify.internal.proto.message.SendRoutingHead

@Serializable
internal class PbSendMsgReq(
    @ProtoNumber(1) val routingHead: SendRoutingHead = SendRoutingHead(),
    @ProtoNumber(2) val contentHead: SendContentHead = SendContentHead(),
    @ProtoNumber(3) val messageBody: MessageBody = MessageBody(),
    @ProtoNumber(4) val clientSequence: Long = 0L,
    @ProtoNumber(5) val random: Int = 0,
)

@Serializable
internal class PbSendMsgResp(
    @ProtoNumber(1) val result: Int = 0,
    @ProtoNumber(2) val errMsg: String = "",
    @ProtoNumber(3) val sendTime: Long = 0L,
    @ProtoNumber(10) val msgInfoFlag: Int = 0,
    @ProtoNumber(11) val sequence: Long = 0L,
    @ProtoNumber(14) val clientSequence: Long = 0L,
)
