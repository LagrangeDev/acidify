package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupFileExtra(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val fileName: String = "",
    @ProtoNumber(3) val display: String = "",
    @ProtoNumber(7) val inner: Inner = Inner(),
) {
    @Serializable
    internal class Inner(
        @ProtoNumber(2) val info: Info = Info(),
    ) {
        @Serializable
        internal class Info(
            @ProtoNumber(1) val busId: Int = 0,
            @ProtoNumber(2) val fileId: String = "",
            @ProtoNumber(3) val fileSize: Long = 0L,
            @ProtoNumber(4) val fileName: String = "",
            @ProtoNumber(5) val field5: Int = 0,
            @ProtoNumber(7) val field7: String = "",
            @ProtoNumber(8) val fileMd5: String = "",
        )
    }
}
