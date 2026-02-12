package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SsoInfoSyncReq(
    @ProtoNumber(1) val syncFlag: Int = 0,
    @ProtoNumber(2) val reqRandom: Int = 0,
    @ProtoNumber(4) val curActiveStatus: Int = 0,
    @ProtoNumber(5) val groupLastMsgTime: Long = 0,
    @ProtoNumber(6) val c2cSyncInfo: C2CSyncInfo = C2CSyncInfo(),
    @ProtoNumber(8) val normalConfig: NormalConfig = NormalConfig(),
    @ProtoNumber(9) val registerInfo: RegisterInfo = RegisterInfo(),
    @ProtoNumber(10) val unknown: Map<Int, Int> = emptyMap(),
    @ProtoNumber(11) val appState: CurAppState = CurAppState(),
) {
    @Serializable
    class C2CSyncInfo(
        @ProtoNumber(1) val c2cMsgCookie: MsgCookie = MsgCookie(),
        @ProtoNumber(2) val c2cLastMsgTime: Long = 0,
        @ProtoNumber(3) val lastC2CMsgCookie: MsgCookie = MsgCookie(),
    ) {
        @Serializable
        class MsgCookie(
            @ProtoNumber(1) val c2cLastMsgTime: Long = 0,
        )
    }

    @Serializable
    class NormalConfig(
        @ProtoNumber(1) val intCfg: Map<Int, Int> = emptyMap(),
    )

    @Serializable
    class CurAppState(
        @ProtoNumber(1) val isDelayRequest: Int = 0,
        @ProtoNumber(2) val appStatus: Int = 0,
        @ProtoNumber(3) val silenceStatus: Int = 0,
    )
}