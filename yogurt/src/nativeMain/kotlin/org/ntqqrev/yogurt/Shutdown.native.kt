package org.ntqqrev.yogurt

import platform.posix._exit

actual fun halt(status: Int) {
    _exit(status)
}