package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class AndroidDeviceReport(
    @ProtoNumber(1) val bootloader: String,
    @ProtoNumber(2) val procVersion: String,
    @ProtoNumber(3) val codeName: String,
    @ProtoNumber(4) val incremental: String? = null,
    @ProtoNumber(5) val fingerprint: String,
    @ProtoNumber(6) val bootId: String,
    @ProtoNumber(7) val androidId: String,
    @ProtoNumber(8) val baseBand: String,
    @ProtoNumber(9) val innerVersion: String,
)