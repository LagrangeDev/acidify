package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class RegisterInfo(
    @ProtoNumber(1) val guid: String = "",
    @ProtoNumber(2) val kickPc: Boolean? = null,
    @ProtoNumber(3) val currentVersion: String = "",
    @ProtoNumber(4) val isFirstRegisterProxyOnline: Boolean? = null,
    @ProtoNumber(5) val localeId: Int? = null,
    @ProtoNumber(6) val device: DeviceInfo = DeviceInfo(),
    @ProtoNumber(7) val setMute: Int? = null,
    @ProtoNumber(8) val registerVendorType: Int? = null,
    @ProtoNumber(9) val regType: Int? = null,
    @ProtoNumber(10) val businessInfo: OnlineBusinessInfo = OnlineBusinessInfo(),
    @ProtoNumber(11) val batteryStatus: Int? = null,
)
