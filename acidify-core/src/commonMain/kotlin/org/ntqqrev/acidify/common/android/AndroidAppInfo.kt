package org.ntqqrev.acidify.common.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.JsExport

/**
 * Bot 登录所模拟的 Android QQ 客户端信息
 */
@JsExport
@Serializable
class AndroidAppInfo(
    @SerialName("Os") val os: String,
    @SerialName("Kernel") val kernel: String = "",
    @SerialName("VendorOs") val vendorOs: String,
    @SerialName("Qua") val qua: String,
    @SerialName("CurrentVersion") val currentVersion: String,
    @SerialName("PtVersion") val ptVersion: String,
    @SerialName("SsoVersion") val ssoVersion: Int,
    @SerialName("PackageName") val packageName: String,
    @SerialName("ApkSignatureMd5") val apkSignatureMd5: ByteArray,
    @SerialName("SdkInfo") val sdkInfo: WtLoginSdkInfo,
    @SerialName("AppId") val appId: Int,
    @SerialName("SubAppId") val subAppId: Int,
    @SerialName("AppClientVersion") val appClientVersion: Int,
) {
    @Serializable
    class WtLoginSdkInfo(
        @SerialName("SdkBuildTime") val sdkBuildTime: Long,
        @SerialName("SdkVersion") val sdkVersion: String,
        @SerialName("MiscBitMap") val miscBitMap: Long,
        @SerialName("SubSigMap") val subSigMap: Long,
        @SerialName("MainSigMap") val mainSigMap: Long,
    )

    object Sig {
        const val WLOGIN_A5: Long = 1 shl 1
        const val WLOGIN_RESERVED: Long = 1 shl 4
        const val WLOGIN_STWEB: Long = 1 shl 5
        const val WLOGIN_A2: Long = 1 shl 6
        const val WLOGIN_ST: Long = 1 shl 7
        const val WLOGIN_LSKEY: Long = 1 shl 9
        const val WLOGIN_SKEY: Long = 1 shl 12
        const val WLOGIN_SIG64: Long = 1 shl 13
        const val WLOGIN_OPENKEY: Long = 1 shl 14
        const val WLOGIN_TOKEN: Long = 1 shl 15
        const val WLOGIN_VKEY: Long = 1 shl 17
        const val WLOGIN_D2: Long = 1 shl 18
        const val WLOGIN_SID: Long = 1 shl 19
        const val WLOGIN_PSKEY: Long = 1 shl 20
        const val WLOGIN_AQSIG: Long = 1 shl 21
        const val WLOGIN_LHSIG: Long = 1 shl 22
        const val WLOGIN_PAYTOKEN: Long = 1 shl 23
        const val WLOGIN_PF: Long = 1 shl 24
        const val WLOGIN_DA2: Long = 1 shl 25
        const val WLOGIN_QRPUSH: Long = 1 shl 26
        const val WLOGIN_PT4Token: Long = 1 shl 27
    }

    companion object {
        private val jsonModule = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): AndroidAppInfo = jsonModule.decodeFromString(json)
    }

    fun toJson() = jsonModule.encodeToString(this)

    object Bundled {
        val AndroidPhone_9_1_60 = AndroidAppInfo(
            os = "Android",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.1.60_9388_YYB_D",
            currentVersion = "9.1.60.045f5d19",
            ptVersion = "9.1.60",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1740483688,
                sdkVersion = "6.0.0.2568",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537275636,
            appClientVersion = 0
        )

        val AndroidPad_9_1_60 = AndroidAppInfo(
            os = "ANDROID",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.1.60_9388_YYB_D",
            currentVersion = "9.1.60.045f5d19",
            ptVersion = "9.1.60",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1740483688,
                sdkVersion = "6.0.0.2568",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537275675,
            appClientVersion = 0
        )

        val AndroidPhone_9_1_70 = AndroidAppInfo(
            os = "Android",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.1.70_9898_YYB_D",
            currentVersion = "9.1.70.88c475c0",
            ptVersion = "9.1.70",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1745224715,
                sdkVersion = "6.0.0.2574",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537285947,
            appClientVersion = 0
        )

        val AndroidPad_9_1_70 = AndroidAppInfo(
            os = "ANDROID",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.1.70_9898_YYB_D",
            currentVersion = "9.1.70.88c475c0",
            ptVersion = "9.1.70",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1745224715,
                sdkVersion = "6.0.0.2574",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537285986,
            appClientVersion = 0
        )

        val AndroidPhone_9_2_0 = AndroidAppInfo(
            os = "Android",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.2.0_10970_YYB_D",
            currentVersion = "9.2.0.0a244d9a",
            ptVersion = "9.2.0",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1751448568,
                sdkVersion = "6.0.0.2584",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537303052,
            appClientVersion = 0
        )

        val AndroidPad_9_2_0 = AndroidAppInfo(
            os = "ANDROID",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.2.0_10970_YYB_D",
            currentVersion = "9.2.0.0a244d9a",
            ptVersion = "9.2.0",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1751448568,
                sdkVersion = "6.0.0.2584",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537303091,
            appClientVersion = 0
        )

        val AndroidPhone_9_2_20 = AndroidAppInfo(
            os = "Android",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.2.20_11650_YYB_D",
            currentVersion = "9.2.20.777b5929",
            ptVersion = "9.2.20",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1757058014,
                sdkVersion = "6.0.0.2589",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537315786,
            appClientVersion = 0
        )

        val AndroidPad_9_2_20 = AndroidAppInfo(
            os = "ANDROID",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.2.20_11650_YYB_D",
            currentVersion = "9.2.20.777b5929",
            ptVersion = "9.2.20",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1757058014,
                sdkVersion = "6.0.0.2589",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537315825,
            appClientVersion = 0
        )

        val AndroidPhone_9_2_80 = AndroidAppInfo(
            os = "Android",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.2.80_13690_YYB_D",
            currentVersion = "9.2.80.90316290",
            ptVersion = "9.2.80",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1757058014,
                sdkVersion = "6.0.0.2589",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537348293,
            appClientVersion = 0
        )

        val AndroidPad_9_2_80 = AndroidAppInfo(
            os = "ANDROID",
            vendorOs = "android",
            qua = "V1_AND_SQ_9.2.80_13690_YYB_D",
            currentVersion = "9.2.80.90316290",
            ptVersion = "9.2.80",
            ssoVersion = 22,
            packageName = "com.tencent.mobileqq",
            apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
            sdkInfo = WtLoginSdkInfo(
                sdkBuildTime = 1757058014,
                sdkVersion = "6.0.0.2589",
                miscBitMap = 150470524,
                subSigMap = 66560,
                mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                        or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                        or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                        or 65536L
            ),
            appId = 16,
            subAppId = 537348332,
            appClientVersion = 0
        )

        val AndroidPhone = AndroidPhone_9_2_80

        val AndroidPad = AndroidPad_9_2_80
    }
}