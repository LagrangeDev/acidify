package org.ntqqrev.acidify.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.JsExport
import kotlin.js.JsStatic

/**
 * Bot 登录所模拟的 QQ 客户端信息
 */
@JsExport
@Serializable
data class AppInfo(
    @SerialName("Os") val os: String,
    @SerialName("Kernel") val kernel: String,
    @SerialName("VendorOs") val vendorOs: String,
    @SerialName("CurrentVersion") val currentVersion: String,
    @SerialName("MiscBitmap") val miscBitmap: Int,
    @SerialName("PtVersion") val ptVersion: String,
    @SerialName("SsoVersion") val ssoVersion: Int,
    @SerialName("PackageName") val packageName: String,
    @SerialName("WtLoginSdk") val wtLoginSdk: String,
    @SerialName("AppId") val appId: Int,
    @SerialName("SubAppId") val subAppId: Int,
    @SerialName("AppClientVersion") val appClientVersion: Int,
    @SerialName("MainSigMap") val mainSigMap: Int,
    @SerialName("SubSigMap") val subSigMap: Int,
    @SerialName("NTLoginType") val ntLoginType: Int
) {
    companion object {
        private val jsonModule = Json { ignoreUnknownKeys = true }

        @JsStatic
        fun fromJson(json: String): AppInfo = jsonModule.decodeFromString(json)
    }

    fun toJson() = jsonModule.encodeToString(this)

    object Bundled {
        val Linux_39038 = AppInfo(
            os = "Linux",
            kernel = "Linux",
            vendorOs = "linux",
            currentVersion = "3.2.19-39038",
            miscBitmap = 32764,
            ptVersion = "2.0.0",
            ssoVersion = 19,
            packageName = "com.tencent.qq",
            wtLoginSdk = "nt.wtlogin.0.0.1",
            appId = 1600001615,
            subAppId = 537313942,
            appClientVersion = 39038,
            mainSigMap = 169742560,
            subSigMap = 0,
            ntLoginType = 1
        )

        val Linux_46494 = AppInfo(
            os = "Linux",
            kernel = "Linux",
            vendorOs = "linux",
            currentVersion = "3.2.26-46494",
            miscBitmap = 32764,
            ptVersion = "2.0.0",
            ssoVersion = 19,
            packageName = "com.tencent.qq",
            wtLoginSdk = "nt.wtlogin.0.0.1",
            appId = 1600001615,
            subAppId = 537345891,
            appClientVersion = 46494,
            mainSigMap = 169742560,
            subSigMap = 0,
            ntLoginType = 1,
        )

        val Linux = Linux_39038
    }
}