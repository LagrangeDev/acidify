package org.ntqqrev.acidify.common.android

data class AndroidLegacyAppInfo(
    val fullVersion: String,
    val fekitVersion: String,
    val wtLoginSdkVersion: String,
) {
    object Bundled {
        val `9_1_60` = AndroidLegacyAppInfo(
            fullVersion = "9.1.60.24370",
            fekitVersion = "8.400.807",
            wtLoginSdkVersion = "6.0.0.2568",
        )

        val `9_1_70` = AndroidLegacyAppInfo(
            fullVersion = "9.1.70.25645",
            fekitVersion = "8.401.830",
            wtLoginSdkVersion = "6.0.0.2574",
        )

        val `9_2_0` = AndroidLegacyAppInfo(
            fullVersion = "9.2.0.28325",
            fekitVersion = "8.405.873",
            wtLoginSdkVersion = "6.0.0.2584",
        )

        val `9_2_20` = AndroidLegacyAppInfo(
            fullVersion = "9.2.20.30025",
            fekitVersion = "8.409.903",
            wtLoginSdkVersion = "6.0.0.2589",
        )

        val `9_2_80` = AndroidLegacyAppInfo(
            fullVersion = "9.2.80.35125",
            fekitVersion = "8.509.1016",
            wtLoginSdkVersion = "6.0.0.2591",
        )

        val current = `9_2_80`
    }
}