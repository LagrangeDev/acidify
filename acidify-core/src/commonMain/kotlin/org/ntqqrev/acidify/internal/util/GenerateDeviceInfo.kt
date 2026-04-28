package org.ntqqrev.acidify.internal.util

import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.system.DeviceInfo

internal fun LagrangeClient.generateDeviceInfo() = DeviceInfo(
    devName = sessionStore.deviceName,
    devType = appInfo.kernel,
    osVer = when (appInfo.os) {
        "Windows" -> "Windows 10.0.19042"
        "Mac" -> "macOS 14.4.1"
        "Linux" -> "Ubuntu 22.04.4 LTS"
        else -> ""
    },
    vendorOsName = appInfo.vendorOs,
)

internal fun KuromeClient.generateDeviceInfo() = DeviceInfo(
    devName = sessionStore.deviceName,
    devType = appInfo.kernel,
    osVer = "Android 12.0",
    vendorOsName = appInfo.vendorOs,
)