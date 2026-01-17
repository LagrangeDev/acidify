package org.ntqqrev.acidify.internal.context

import org.ntqqrev.acidify.internal.IClient

internal abstract class AbstractContext(internal open val client: IClient) {
    open suspend fun postOnline() {}
    open suspend fun preOffline() {}
}