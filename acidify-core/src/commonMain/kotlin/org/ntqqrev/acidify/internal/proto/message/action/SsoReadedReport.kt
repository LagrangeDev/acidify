package org.ntqqrev.acidify.internal.proto.message.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SsoReadedReportReq(
    @ProtoNumber(1) val group: SsoReadedReportGroup? = null,
    @ProtoNumber(2) val c2c: SsoReadedReportC2C? = null,
)

@Serializable
internal class SsoReadedReportC2C(
    @ProtoNumber(2) val targetUid: String = "",
    @ProtoNumber(3) val time: Long = 0L,
    @ProtoNumber(4) val startSequence: Long = 0L,
)

@Serializable
internal class SsoReadedReportGroup(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(2) val startSequence: Long = 0L,
)
