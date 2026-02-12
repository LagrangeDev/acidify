package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class AndroidFetchClientKeyReq(
    @ProtoNumber(2) val appId: Int = 0,
    @ProtoNumber(3) val field3: String = "",
    @ProtoNumber(4) val field4: Int = 0,
    @ProtoNumber(8) val guid: String = "",
    @ProtoNumber(12) val subAppId: Int = 0,
)

@Serializable
internal class AndroidFetchClientKeyResp(
    @ProtoNumber(10) val clientKey: ByteArray,
)