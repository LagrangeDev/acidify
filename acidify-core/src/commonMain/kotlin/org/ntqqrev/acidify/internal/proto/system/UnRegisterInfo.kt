package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class UnRegisterInfo(
    @ProtoNumber(2) val device: DeviceInfo = DeviceInfo(),
)
