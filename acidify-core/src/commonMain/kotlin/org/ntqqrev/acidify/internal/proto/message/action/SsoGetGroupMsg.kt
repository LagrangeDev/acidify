package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.CommonMessage

@Serializable
internal class SsoGetGroupMsgReq(
    @ProtoNumber(1) val groupInfo: GroupInfo = GroupInfo(),
    @ProtoNumber(2) val filter: Int = 0,
) {
    @Serializable
    class GroupInfo(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val startSequence: Long = 0L,
        @ProtoNumber(3) val endSequence: Long = 0L,
    )
}

@Serializable
internal class SsoGetGroupMsgResp(
    @ProtoNumber(1) val retcode: Int = 0,
    @ProtoNumber(2) val errorMsg: String = "",
    @ProtoNumber(3) val body: Body = Body(),
) {
    @Serializable
    class Body(
        @ProtoNumber(1) val retcode: Int = 0,
        @ProtoNumber(2) val errorMsg: String = "",
        @ProtoNumber(3) val groupUin: Int = 0,
        @ProtoNumber(4) val startSequence: Int = 0,
        @ProtoNumber(5) val endSequence: Int = 0,
        @ProtoNumber(6) val messages: List<CommonMessage> = emptyList(),
    )
}
