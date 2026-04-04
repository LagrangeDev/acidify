package org.ntqqrev.yogurt.util

import io.ktor.utils.io.errors.PosixException

actual tailrec fun Throwable.isCausedByAddrInUse(): Boolean {
    if (this is PosixException.AddressAlreadyInUseException) {
        return true
    }
    return cause?.isCausedByAddrInUse() ?: false
}