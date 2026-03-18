package org.ntqqrev.yogurt.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.codec.VideoInfo
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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

    suspend fun getVideoInfo(videoData: ByteArray): VideoInfo = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.getVideoInfo(videoData)
    }

    suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.getVideoFirstFrameJpg(videoData)
    }
}

internal data class CommandResult(
    val exitCode: Int,
    val output: String,
)

internal expect fun runCliCommand(arguments: List<String>): CommandResult

object FFMpegCliCodec {
    private const val FFMPEG_PATH = "ffmpeg"

    private val durationRegex = Regex("""Duration:\s*(\d{2}):(\d{2}):(\d{2})\.(\d+)""")
    private val resolutionRegex = Regex("""\bVideo:.*?(\d{2,5})x(\d{2,5})\b""")

    fun getVideoInfo(videoData: ByteArray): VideoInfo {
        val inputPath = createTemporaryPath("video-info", ".bin")
        try {
            writeFile(inputPath, videoData)
            val result = runCliCommand(listOf(FFMPEG_PATH, "-i", inputPath.toString()))
            return parseVideoInfo(result.output)
        } finally {
            deleteIfExists(inputPath)
        }
    }

    fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray {
        val inputPath = createTemporaryPath("video-frame", ".bin")
        val outputPath = createTemporaryPath("video-frame", ".jpg")
        try {
            writeFile(inputPath, videoData)
            val result = runCliCommand(
                listOf(
                    FFMPEG_PATH,
                    "-loglevel",
                    "error",
                    "-y",
                    "-i",
                    inputPath.toString(),
                    "-frames:v",
                    "1",
                    outputPath.toString(),
                )
            )
            check(result.exitCode == 0 && SystemFileSystem.exists(outputPath)) {
                "ffmpeg 提取视频首帧失败：${result.output.trim()}"
            }
            return SystemFileSystem.source(outputPath).buffered().use {
                it.readByteArray()
            }
        } finally {
            deleteIfExists(inputPath)
            deleteIfExists(outputPath)
        }
    }

    private fun parseVideoInfo(output: String): VideoInfo {
        val durationMatch = durationRegex.find(output)
            ?: error("无法从 ffmpeg 输出中解析视频时长")
        val videoLine = output.lineSequence().firstOrNull { "Video:" in it }
            ?: error("无法从 ffmpeg 输出中解析视频流信息")
        val resolutionMatch = resolutionRegex.find(videoLine)
            ?: error("无法从 ffmpeg 输出中解析视频分辨率")

        val hours = durationMatch.groupValues[1].toLong()
        val minutes = durationMatch.groupValues[2].toLong()
        val seconds = durationMatch.groupValues[3].toLong()
        val millis = durationMatch.groupValues[4].padEnd(3, '0').take(3).toLong()

        return VideoInfo(
            width = resolutionMatch.groupValues[1].toInt(),
            height = resolutionMatch.groupValues[2].toInt(),
            duration = (((hours * 60 + minutes) * 60 + seconds) * 1000L)
                .plus(millis)
                .milliseconds,
        )
    }

    private fun createTemporaryPath(prefix: String, suffix: String): Path {
        val filename = buildString {
            append("yogurt-")
            append(prefix)
            append('-')
            append(Clock.System.now().toEpochMilliseconds())
            append('-')
            append(Random.nextInt().toUInt().toString(16))
            append(suffix)
        }
        return Path(SystemTemporaryDirectory.toString(), filename)
    }

    private fun writeFile(path: Path, data: ByteArray) {
        SystemFileSystem.sink(path).buffered().use { sink ->
            sink.write(data)
        }
    }

    private fun deleteIfExists(path: Path) {
        if (SystemFileSystem.exists(path)) {
            SystemFileSystem.delete(path)
        }
    }
}