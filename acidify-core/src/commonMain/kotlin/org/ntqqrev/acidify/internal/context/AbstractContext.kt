package org.ntqqrev.acidify.internal.context

import org.ntqqrev.acidify.internal.AbstractClient

internal abstract class AbstractContext(
    internal val client: AbstractClient,
) {
    protected val logger = client.loggerFactory.invoke(this)
    open suspend fun postOnline() {}
    open suspend fun preOffline() {}
}