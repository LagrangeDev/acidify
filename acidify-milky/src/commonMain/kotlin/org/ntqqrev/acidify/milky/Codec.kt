package org.ntqqrev.acidify.milky

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.message.ImageFormat
import kotlin.time.Duration

interface Codec {
    suspend fun getImageInfo(input: ByteArray): ImageInfo
    suspend fun audioToPcm(input: ByteArray): ByteArray
    suspend fun silkEncode(input: ByteArray): ByteArray
    suspend fun calculatePcmDuration(
        input: ByteArray,
        bitDepth: Int = 16,
        channelCount: Int = 1,
        sampleRate: Int = 24000,
    ): Duration

    context(scope: MediaSourceScope)
    suspend fun getVideoInfo(videoSource: MediaSource): VideoInfo

    context(scope: MediaSourceScope)
    suspend fun getVideoFirstFrameJpg(videoSource: MediaSource): ByteArray
}

data class ImageInfo(
    val format: ImageFormat,
    val width: Int,
    val height: Int,
)

data class VideoInfo(
    val width: Int,
    val height: Int,
    val duration: Duration,
)