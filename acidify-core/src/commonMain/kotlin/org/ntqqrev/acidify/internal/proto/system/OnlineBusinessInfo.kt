package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class OnlineBusinessInfo(
    @ProtoNumber(1) val notifySwitch: Int = 0,
    @ProtoNumber(2) val bindUinNotifySwitch: Int = 0,
)
