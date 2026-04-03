package org.ntqqrev.yogurt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.milky.Codec
import org.ntqqrev.acidify.milky.ImageInfo
import org.ntqqrev.acidify.milky.VideoInfo
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.fs.FileSystem
import org.ntqqrev.yogurt.fs.withFs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FFmpegCodec : Codec {
    val ffmpegMutex = Mutex()

    override suspend fun getImageInfo(input: ByteArray): ImageInfo {
        return org.ntqqrev.acidify.codec.getImageInfo(input).toAcidifyImageInfo()
    }

    override suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.audioToPcm(input)
    }

    override suspend fun silkEncode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkEncode(input)
    }

    override suspend fun calculatePcmDuration(
        input: ByteArray,
        bitDepth: Int,
        channelCount: Int,
        sampleRate: Int
    ): Duration {
        return org.ntqqrev.acidify.codec.calculatePcmDuration(input, bitDepth, channelCount, sampleRate)
    }

    override suspend fun getVideoInfo(videoData: ByteArray): VideoInfo = if (config.milky.ffmpegPath.isNotEmpty()) {
        withContext(Dispatchers.IO) {
            withTempFile(videoData, "video-input") { inputPath ->
                val result = executeCommand(
                    config.milky.ffmpegPath,
                    "-hide_banner",
                    "-nostdin",
                    "-i",
                    inputPath,
                    "-frames:v",
                    "1",
                    "-f",
                    "null",
                    "-"
                )

                parseVideoInfoFromFfmpegOutput(result.stderr)
                    ?: throw IllegalStateException(
                        "Failed to parse video info from ffmpeg output (code=${result.errorCode}): ${result.stderr}"
                    )
            }
        }
    } else {
        ffmpegMutex.withLock {
            org.ntqqrev.acidify.codec.getVideoInfo(videoData).toAcidifyVideoInfo()
        }
    }

    override suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray =
        if (config.milky.ffmpegPath.isNotEmpty()) {
        withContext(Dispatchers.IO) {
            withTempFile(videoData, "video-input") { inputPath ->
                val outputPath = createCodecTempFilePath("video-first-frame", ".jpg")
                try {
                    val result = executeCommand(
                        config.milky.ffmpegPath,
                        "-hide_banner",
                        "-nostdin",
                        "-y",
                        "-i",
                        inputPath,
                        "-an",
                        "-sn",
                        "-frames:v",
                        "1",
                        outputPath
                    )

                    if (result.errorCode != 0) {
                        throw IllegalStateException(
                            "ffmpeg failed to extract the first video frame (code=${result.errorCode}): ${result.stderr}"
                        )
                    }

                    Path(outputPath).readBytes()
                } finally {
                    deleteCodecTempFile(outputPath)
                }
            }
        }
        } else {
            ffmpegMutex.withLock {
                org.ntqqrev.acidify.codec.getVideoFirstFrameJpg(videoData)
            }
        }
}

private fun org.ntqqrev.acidify.codec.ImageInfo.toAcidifyImageInfo() = ImageInfo(
    format = when (format) {
        org.ntqqrev.acidify.codec.ImageFormat.PNG -> ImageFormat.PNG
        org.ntqqrev.acidify.codec.ImageFormat.GIF -> ImageFormat.GIF
        org.ntqqrev.acidify.codec.ImageFormat.JPEG -> ImageFormat.JPEG
        org.ntqqrev.acidify.codec.ImageFormat.BMP -> ImageFormat.BMP
        org.ntqqrev.acidify.codec.ImageFormat.WEBP -> ImageFormat.WEBP
        org.ntqqrev.acidify.codec.ImageFormat.TIFF -> ImageFormat.TIFF
    },
    width = width,
    height = height,
)

private fun org.ntqqrev.acidify.codec.VideoInfo.toAcidifyVideoInfo() = VideoInfo(
    width = width,
    height = height,
    duration = duration,
)

private inline fun <T> withTempFile(
    data: ByteArray,
    kind: String,
    block: FileSystem.(String) -> T,
): T = withFs {
    val inputPath = createCodecTempFilePath(kind)
    try {
        Path(inputPath).write(data)
        return block(inputPath)
    } finally {
        deleteCodecTempFile(inputPath)
    }
}

private fun createCodecTempFilePath(kind: String, extension: String = ".tmp"): String = withFs {
    val basePath = createCommandTempFilePath(kind)
    if (extension == ".tmp") {
        return basePath
    }

    val targetPath = basePath.removeSuffix(".tmp") + extension
    atomicMove(Path(basePath), Path(targetPath))
    return targetPath
}

private fun deleteCodecTempFile(path: String) = withFs {
    val file = Path(path)
    if (exists(file)) {
        delete(file, mustExist = false)
    }
}

private fun parseVideoInfoFromFfmpegOutput(output: String): VideoInfo? {
    val duration = durationRegex.find(output)?.let { match ->
        parseFfmpegDuration(match.groupValues[1])
    } ?: return null

    val videoLine = output.lineSequence().firstOrNull { "Video:" in it } ?: return null
    val resolution = resolutionRegex.find(videoLine) ?: return null

    return VideoInfo(
        width = resolution.groupValues[1].toInt(),
        height = resolution.groupValues[2].toInt(),
        duration = duration,
    )
}

private fun parseFfmpegDuration(raw: String): Duration {
    val parts = raw.split(":")
    require(parts.size == 3) { "Unsupported ffmpeg duration format: $raw" }

    val hoursPart = parts[0].toInt()
    val minutesPart = parts[1].toInt()
    val secondsPart = parts[2].toDouble()

    return hoursPart.hours + minutesPart.minutes + secondsPart.seconds
}

private val durationRegex = Regex("""Duration:\s*([0-9]{2}:[0-9]{2}:[0-9]{2}(?:\.[0-9]+)?)""")
private val resolutionRegex = Regex("""\b(\d{2,5})x(\d{2,5})\b""")