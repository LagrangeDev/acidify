package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class NTV2RichMediaHighwayExt(
    @ProtoNumber(1) val fileUuid: String = "",
    @ProtoNumber(2) val uKey: String = "",
    @ProtoNumber(5) val network: NTHighwayNetwork = NTHighwayNetwork(),
    @ProtoNumber(6) val msgInfoBody: List<MsgInfoBody> = emptyList(),
    @ProtoNumber(10) val blockSize: Int = 0,
    @ProtoNumber(11) val hash: NTHighwayHash = NTHighwayHash(),
)

@Serializable
internal class NTHighwayHash(
    @ProtoNumber(1) val fileSha1: List<ByteArray> = emptyList(),
)

@Serializable
internal class NTHighwayNetwork(
    @ProtoNumber(1) val iPv4s: List<NTHighwayIPv4> = emptyList(),
)

@Serializable
internal class NTHighwayIPv4(
    @ProtoNumber(1) val domain: NTHighwayDomain = NTHighwayDomain(),
    @ProtoNumber(2) val port: Int = 0,
)

@Serializable
internal class NTHighwayDomain(
    @ProtoNumber(1) val isEnable: Boolean = false,
    @ProtoNumber(2) val iP: String = "",
)
