package org.ntqqrev.yogurt.fs

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.test.*

class FileSystemTest {
    private val fs = defaultFileSystem

    @Test
    fun cjkDirectoryAndTextRoundTrip() = withTemporaryRoot("目录读写") { root ->
        val topLevel = Path(root, "中文目录")
        val nested = Path(Path(topLevel, "二级_日本語"), "세번째_한글")
        val textFile = Path(nested, "数据-混合.txt")
        val content = """
            第一行：你好，世界
            第二行：日本語の文章
            第三行：한글 문장
        """.trimIndent()

        fs.createDirectories(nested)
        fs.sink(textFile).buffered().use { it.writeString(content) }

        assertTrue(fs.exists(topLevel))
        assertTrue(fs.exists(nested))
        assertTrue(fs.exists(textFile))

        val metadata = fs.metadataOrNull(textFile)
        assertNotNull(metadata)
        assertTrue(metadata.isRegularFile)
        assertFalse(metadata.isDirectory)
        assertEquals(content.encodeToByteArray().size.toLong(), metadata.size)

        val listedEntries = fs.list(topLevel)
        val listedNames = listedEntries.map(::semanticLeafName).toSet()
        assertEquals(setOf("二级_日本語"), listedNames)
        val listedPath = listedEntries.single()
        assertEquals(normalizedPath(Path(topLevel, "二级_日本語")), normalizedPath(listedPath))
        assertEquals(listOf("中文目录", "二级_日本語"), semanticTail(listedPath, 2))

        val readBack = fs.source(textFile).buffered().use { it.readString() }
        assertEquals(content, readBack)

        val resolved = fs.resolve(textFile)
        assertTrue(resolved.isAbsolute)
        assertEquals(listOf("二级_日本語", "세번째_한글", "数据-混合.txt"), semanticTail(resolved, 3))
    }

    @Test
    fun appendAndBinaryRoundTripOnCjkPaths() = withTemporaryRoot("追加与二进制") { root ->
        val dataDir = Path(root, "附件_资料")
        val textFile = Path(dataDir, "日志.txt")
        val binaryFile = Path(dataDir, "图像_样本.bin")
        val expectedText = "第一行\n第二行_追加\n第三行_終わり\n"
        val expectedBinary = byteArrayOf(0, 1, 2, 3, 127, (-1).toByte()) +
                "中文ABCかな한글".encodeToByteArray()

        fs.createDirectories(dataDir)

        fs.sink(textFile).buffered().use { it.writeString("第一行\n") }
        fs.sink(textFile, append = true).buffered().use { it.writeString("第二行_追加\n第三行_終わり\n") }
        fs.sink(binaryFile).buffered().use { it.write(expectedBinary) }

        val actualText = fs.source(textFile).buffered().use { it.readString() }
        val actualBinary = fs.source(binaryFile).buffered().use { it.readByteArray() }

        assertEquals(expectedText, actualText)
        assertContentEquals(expectedBinary, actualBinary)
    }

    @Test
    fun atomicMoveKeepsCjkNamesAndContent() = withTemporaryRoot("原子移动") { root ->
        val sourceDir = Path(root, "原目录")
        val destinationDir = Path(root, "目标目录")
        val sourceFile = Path(sourceDir, "待移动_文件.txt")
        val destinationFile = Path(destinationDir, "已移动_结果.txt")
        val content = "迁移内容_日本語_한글"

        fs.createDirectories(sourceDir)
        fs.createDirectories(destinationDir)
        fs.sink(sourceFile).buffered().use { it.writeString(content) }

        fs.atomicMove(sourceFile, destinationFile)

        assertFalse(fs.exists(sourceFile))
        assertTrue(fs.exists(destinationFile))
        assertEquals(content, fs.source(destinationFile).buffered().use { it.readString() })

        val destinationEntries = fs.list(destinationDir)
        val destinationListing = destinationEntries.map(::semanticLeafName).toSet()
        assertEquals(setOf(destinationFile.name), destinationListing)
        assertEquals(normalizedPath(destinationFile), normalizedPath(destinationEntries.single()))
    }

    private inline fun withTemporaryRoot(label: String, block: (Path) -> Unit) {
        val root = Path(
            SystemTemporaryDirectory,
            "ktfs-${Random.nextLong().toString(16)}-$label-中文_日本語_한글",
        )
        fs.createDirectories(root, mustCreate = true)
        try {
            block(root)
        } finally {
            deleteRecursively(root)
        }
    }

    private fun deleteRecursively(path: Path) {
        val metadata = fs.metadataOrNull(path) ?: return
        if (metadata.isDirectory) {
            fs.list(path).forEach(::deleteRecursively)
        }
        fs.delete(path, mustExist = true)
    }

    private fun semanticLeafName(path: Path): String {
        return semanticSegments(path).last()
    }

    private fun semanticTail(path: Path, count: Int): List<String> {
        return semanticSegments(path).takeLast(count)
    }

    private fun normalizedPath(path: Path): String {
        return path.toString().replace('\\', '/').removeSuffix("/.")
    }

    private fun semanticSegments(path: Path): List<String> {
        return normalizedPath(path)
            .split('/')
            .filter { it.isNotEmpty() && it != "." }
    }
}