package org.ntqqrev.acidify.internal.service

import org.ntqqrev.acidify.internal.AbstractClient

internal abstract class Service<T, R>(val cmd: String) {
    open val ssoRequestType = RequestType.D2Auth
    open val ssoEncryptType = EncryptType.WithD2Key

    abstract fun build(client: AbstractClient, payload: T): ByteArray
    abstract fun parse(client: AbstractClient, payload: ByteArray): R
}

internal abstract class NoInputService<R>(cmd: String) : Service<Unit, R>(cmd)

internal abstract class NoOutputService<T>(cmd: String) : Service<T, Unit>(cmd) {
    override fun parse(client: AbstractClient, payload: ByteArray) = Unit
}