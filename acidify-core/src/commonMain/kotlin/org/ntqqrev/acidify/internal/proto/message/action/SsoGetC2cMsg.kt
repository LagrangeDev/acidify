package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.CommonMessage

@Serializable
internal class SsoGetC2cMsgReq(
    @ProtoNumber(2) val peerUid: String = "",
    @ProtoNumber(3) val startSequence: Long = 0L,
    @ProtoNumber(4) val endSequence: Long = 0L,
)

@Serializable
internal class SsoGetC2cMsgResp(
    @ProtoNumber(1) val retcode: Int = 0,
    @ProtoNumber(2) val errorMsg: String = "",
    @ProtoNumber(7) val messages: List<CommonMessage> = emptyList(),
)
