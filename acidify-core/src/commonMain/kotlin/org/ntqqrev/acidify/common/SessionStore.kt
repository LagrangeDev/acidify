package org.ntqqrev.acidify.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlin.js.JsExport
import kotlin.js.JsStatic
import kotlin.jvm.JvmField
import kotlin.random.Random

/**
 * 存储 Bot 登录会话相关信息，如密钥等
 */
@JsExport
@Serializable
class SessionStore(
    @JvmField var uin: Long,
    @JvmField var uid: String,

    var a2: ByteArray,
    var d2: ByteArray,
    var d2Key: ByteArray,
    var tgtgt: ByteArray,
    var encryptedA1: ByteArray,
    var noPicSig: ByteArray,

    var qrSig: ByteArray,

    var guid: ByteArray,
    val deviceName: String,
) {
    @Transient
    internal var keySig: ByteArray? = null

    @Transient
    internal var exchangeKey: ByteArray? = null

    @Transient
    internal var unusualCookies: String? = null

    companion object {
        @JsStatic
        fun empty(): SessionStore {
            return SessionStore(
                uin = 0,
                uid = "",
                a2 = ByteArray(0),
                d2 = ByteArray(0),
                d2Key = ByteArray(16),
                tgtgt = ByteArray(0),
                encryptedA1 = ByteArray(0),
                noPicSig = ByteArray(0),
                qrSig = ByteArray(0),
                guid = Random.nextBytes(16),
                deviceName = "Lagrange-${Random.nextBytes(3).toHexString()}"
            )
        }

        @JsStatic
        fun fromJson(json: String): SessionStore = Json.decodeFromString(json)
    }

    fun clear() {
        a2 = ByteArray(0)
        d2 = ByteArray(0)
        d2Key = ByteArray(16)
        tgtgt = ByteArray(0)
        encryptedA1 = ByteArray(0)
        noPicSig = ByteArray(0)
        qrSig = ByteArray(0)
        keySig = null
        exchangeKey = null
        unusualCookies = null
    }

    fun refreshDeviceGuid() {
        guid = Random.nextBytes(16)
    }

    fun toJson() = Json.encodeToString(this)
}