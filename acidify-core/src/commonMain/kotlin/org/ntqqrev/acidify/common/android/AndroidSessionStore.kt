package org.ntqqrev.acidify.common.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * 存储 Bot 登录会话相关信息，如密钥等
 */
@Serializable
class AndroidSessionStore(
    var uin: Long,
    var password: String,
    var uid: String,
    var state: State,
    var wloginSigs: WLoginSigs,
    var guid: ByteArray,
    var androidId: String,
    var qimei: String,
    var deviceName: String,
) {
    @Serializable
    class WLoginSigs(
        var a2: ByteArray,
        var a2Key: ByteArray,
        var d2: ByteArray,
        var d2Key: ByteArray,
        var a1: ByteArray,
        var a1Key: ByteArray,
        var noPicSig: ByteArray,
        var tgtgtKey: ByteArray,
        var ksid: ByteArray,
        var superKey: ByteArray,
        var stKey: ByteArray,
        var stWeb: ByteArray,
        var st: ByteArray,
        var wtSessionTicket: ByteArray,
        var wtSessionTicketKey: ByteArray,
        var randomKey: ByteArray,
        var sKey: ByteArray,
        var psKey: Map<String, String>,
    ) {
        fun clear() {
            a2 = ByteArray(0)
            d2 = ByteArray(0)
            d2Key = ByteArray(16)
            a1 = ByteArray(0)
            tgtgtKey = Random.nextBytes(16)
            randomKey = ByteArray(16)
        }
    }

    @Serializable
    class State(
        var tlv104: ByteArray? = null,
        var tlv547: ByteArray? = null,
        var tlv174: ByteArray? = null,
        var keyExchangeSession: KeyExchangeSession? = null,
        var cookie: String? = null,
        var qrSig: ByteArray? = null,
    ) {
        @Serializable
        class KeyExchangeSession(
            var sessionTicket: ByteArray,
            var sessionKey: ByteArray,
        )
    }

    companion object {
        fun empty(uin: Long, password: String): AndroidSessionStore {
            return AndroidSessionStore(
                uin = uin,
                password = password,
                uid = "",
                state = State(),
                wloginSigs = WLoginSigs(
                    a2 = ByteArray(0),
                    a2Key = ByteArray(16),
                    d2 = ByteArray(0),
                    d2Key = ByteArray(16),
                    a1 = ByteArray(0),
                    a1Key = ByteArray(16),
                    noPicSig = ByteArray(0),
                    tgtgtKey = Random.nextBytes(16),
                    ksid = ByteArray(0),
                    superKey = ByteArray(0),
                    stKey = ByteArray(0),
                    stWeb = ByteArray(0),
                    st = ByteArray(0),
                    wtSessionTicket = ByteArray(0),
                    wtSessionTicketKey = ByteArray(0),
                    randomKey = ByteArray(16),
                    sKey = ByteArray(0),
                    psKey = emptyMap(),
                ),
                guid = Random.nextBytes(16),
                androidId = Random.nextBytes(8).toHexString(),
                qimei = "",
                deviceName = "Lagrange-${Random.nextBytes(3).toHexString()}",
            )
        }

        fun fromJson(json: String): AndroidSessionStore = Json.decodeFromString(json)
    }

    fun toJson() = Json.encodeToString(this)
}