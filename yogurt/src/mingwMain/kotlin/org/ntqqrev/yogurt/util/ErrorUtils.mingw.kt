package org.ntqqrev.yogurt.util

import io.ktor.utils.io.errors.PosixException

actual fun Throwable.isCausedByAddrInUse(): Boolean {
    if (this is PosixException.PosixErrnoException) {
        if (errno == 10048) { // WSAEADDRINUSE
            return true
        }
    }
    return cause?.isCausedByAddrInUse() ?: false
}