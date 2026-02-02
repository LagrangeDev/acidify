package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SetGroupRequestReq(
    @ProtoNumber(1) val accept: Int = 0,
    @ProtoNumber(2) val body: SetGroupRequestBody = SetGroupRequestBody(),
)

@Serializable
internal class SetGroupRequestBody(
    @ProtoNumber(1) val sequence: Long = 0L,
    @ProtoNumber(2) val eventType: Int = 0,
    @ProtoNumber(3) val groupUin: Long = 0L,
    @ProtoNumber(4) val message: String = "",
)
