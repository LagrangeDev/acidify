package org.ntqqrev.acidify.internal.packet

enum class EncryptType(val underlying: Byte) {
    None(0),
    WithD2Key(1),
    WithEmptyKey(2),
}