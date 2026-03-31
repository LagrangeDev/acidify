package org.ntqqrev.yogurt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.ntqqrev.acidify.codec.VideoInfo
import org.ntqqrev.yogurt.YogurtApp.config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FFMpegCodec {
    val ffmpegMutex = Mutex()

    suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.audioToPcm(input)
    }

    suspend fun silkDecode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkDecode(input)
    }

    suspend fun silkEncode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkEncode(input)
    }

    suspend fun getVideoInfo(videoData: ByteArray): VideoInfo = if (config.milky.ffmpegPath.isNotEmpty()) {
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
            org.ntqqrev.acidify.codec.getVideoInfo(videoData)
        }
    }

    suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray = if (config.milky.ffmpegPath.isNotEmpty()) {
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

                    readByteArrayFromFilePath(outputPath)
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

private inline fun <T> withTempFile(
    data: ByteArray,
    kind: String,
    block: (String) -> T,
): T {
    val inputPath = createCodecTempFilePath(kind)
    try {
        SystemFileSystem.sink(Path(inputPath)).buffered().use { sink ->
            sink.write(data)
        }
        return block(inputPath)
    } finally {
        deleteCodecTempFile(inputPath)
    }
}

private fun createCodecTempFilePath(kind: String, extension: String = ".tmp"): String {
    val basePath = createCommandTempFilePath(kind)
    if (extension == ".tmp") {
        return basePath
    }

    val targetPath = basePath.removeSuffix(".tmp") + extension
    SystemFileSystem.atomicMove(Path(basePath), Path(targetPath))
    return targetPath
}

private fun deleteCodecTempFile(path: String) {
    val file = Path(path)
    if (SystemFileSystem.exists(file)) {
        SystemFileSystem.delete(file, mustExist = false)
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