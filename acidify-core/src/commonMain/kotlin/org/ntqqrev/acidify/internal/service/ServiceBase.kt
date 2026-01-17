package org.ntqqrev.acidify.internal.service

import org.ntqqrev.acidify.internal.IClient

internal abstract class Service<T, R>(val cmd: String) {
    abstract fun build(client: IClient, payload: T): ByteArray
    abstract fun parse(client: IClient, payload: ByteArray): R
}

internal abstract class NoInputService<R>(cmd: String) : Service<Unit, R>(cmd)

internal abstract class NoOutputService<T>(cmd: String) : Service<T, Unit>(cmd) {
    override fun parse(client: IClient, payload: ByteArray) = Unit
}