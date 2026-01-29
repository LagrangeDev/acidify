@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package org.ntqqrev.yogurt.codec

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import liblagrangecodec.audio_to_pcm
import liblagrangecodec.silk_decode
import liblagrangecodec.silk_encode
import kotlin.experimental.ExperimentalNativeApi

typealias AudioFunction = (
    audioData: CValuesRef<UByteVarOf<UByte>>?,
    dataLen: Int,
    callback: CPointer<CFunction<(
        CPointer<out CPointed>?,
        CPointer<UByteVarOf<UByte>>?,
        Int
    ) -> Unit>>?, userdata: CValuesRef<*>?
) -> Int

actual fun audioToPcm(input: ByteArray) = processAudio(input, ::audio_to_pcm)

actual fun silkDecode(input: ByteArray) = processAudio(input, ::silk_decode)

actual fun silkEncode(input: ByteArray) = processAudio(input, ::silk_encode)

private fun processAudio(input: ByteArray, func: AudioFunction): ByteArray = memScoped {
    val userData = Buffer()
    val userDataRef = StableRef.create(userData)
    val result = func.invoke(
        input.asUByteArray().toCValues(),
        input.size,
        staticCFunction { userData, p, len ->
            val buffer = userData!!.asStableRef<Buffer>().get()
            val byteArray = p!!.readBytes(len)
            buffer.write(byteArray)
        },
        userDataRef.asCPointer()
    )
    require(result == 0) { "audio processing failed with code $result" }
    userDataRef.dispose()
    return userData.readByteArray()
}