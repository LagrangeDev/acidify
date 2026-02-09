package org.ntqqrev.acidify.internal.service

internal enum class EncryptType(val underlying: Byte) {
    None(0),
    WithD2Key(1),
    WithEmptyKey(2),
}