package org.ntqqrev.acidify.message.internal

import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.internal.proto.message.Elem
import org.ntqqrev.acidify.message.MessageScene

internal class MessageParsingContext(
    val scene: MessageScene,
    val elems: List<Elem>,
    val bot: AbstractBot,
) {
    var currentIndex = 0

    val remainingCount: Int
        get() = elems.size - currentIndex

    fun hasNext(): Boolean = currentIndex < elems.size

    fun peek(): Elem = elems[currentIndex]

    inline fun <T> tryPeekType(typeProvider: Elem.() -> T?): T? = peek().typeProvider()

    fun skip(count: Int = 1) {
        if (currentIndex + count > elems.size) {
            throw IndexOutOfBoundsException("Cannot skip $count elements from index $currentIndex, size is ${elems.size}")
        }
        currentIndex += count
    }

    fun consume() = skip(1)
}
