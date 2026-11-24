package org.ntqqrev.yogurt.util

import org.ntqqrev.yogurt.YogurtApp.config

val isPC: Boolean
    get() = config.protocol.os in setOf("Windows", "Mac", "Linux")

val isAndroid: Boolean
    get() = config.protocol.os in setOf("AndroidPhone", "AndroidPad")

internal val isHarmony: Boolean
    get() = config.protocol.os == "Harmony"