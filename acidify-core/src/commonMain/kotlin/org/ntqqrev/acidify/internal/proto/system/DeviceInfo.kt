package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class DeviceInfo(
    @ProtoNumber(1) val devName: String = "",
    @ProtoNumber(2) val devType: String = "",
    @ProtoNumber(3) val osVer: String = "",
    @ProtoNumber(4) val brand: String? = null,
    @ProtoNumber(5) val vendorOsName: String = "",
)
