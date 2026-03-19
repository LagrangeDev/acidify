package org.ntqqrev.yogurt

import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.android.AndroidAppInfo
import org.ntqqrev.acidify.common.android.AndroidLegacyAppInfo

val bundledPCAppInfo = mapOf(
    "Linux/39038" to AppInfo.Bundled.Linux_39038,
    "Linux/46494" to AppInfo.Bundled.Linux_46494,
)

val bundledAndroidAppInfo = mapOf(
    "AndroidPhone/9.1.60" to AndroidAppInfo.Bundled.AndroidPhone_9_1_60,
    "AndroidPad/9.1.60" to AndroidAppInfo.Bundled.AndroidPad_9_1_60,
    "AndroidPhone/9.1.70" to AndroidAppInfo.Bundled.AndroidPhone_9_1_70,
    "AndroidPad/9.1.70" to AndroidAppInfo.Bundled.AndroidPad_9_1_70,
    "AndroidPhone/9.2.0" to AndroidAppInfo.Bundled.AndroidPhone_9_2_0,
    "AndroidPad/9.2.0" to AndroidAppInfo.Bundled.AndroidPad_9_2_0,
    "AndroidPhone/9.2.20" to AndroidAppInfo.Bundled.AndroidPhone_9_2_20,
    "AndroidPad/9.2.20" to AndroidAppInfo.Bundled.AndroidPad_9_2_20,
)

val bundledAndroidLegacyAppInfo = mapOf(
    "9.1.60" to AndroidLegacyAppInfo.Bundled.`9_1_60`,
    "9.1.70" to AndroidLegacyAppInfo.Bundled.`9_1_70`,
    "9.2.0" to AndroidLegacyAppInfo.Bundled.`9_2_0`,
    "9.2.20" to AndroidLegacyAppInfo.Bundled.`9_2_20`,
)