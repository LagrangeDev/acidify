package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchGroupExtraInfoReq(
    @ProtoNumber(1) val random: Int = 0,
    @ProtoNumber(2) val config: Config = Config(),
) {
    @Serializable
    class Config(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val flags: Flags = Flags(),
    ) {
        @Serializable
        class Flags(
            @ProtoNumber(22) val latestMessageSeq: Boolean = false,
        )
    }
}

@Serializable
internal class FetchGroupExtraInfoResp(
    @ProtoNumber(1) val info: Info = Info(),
) {
    @Serializable
    class Info(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(3) val results: Results = Results(),
    ) {
        @Serializable
        class Results(
            @ProtoNumber(22) val latestMessageSeq: Long = 0L,
        )
    }
}
