package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class PushMsg(
    @ProtoNumber(1) val message: CommonMessage = CommonMessage(),
)
