package org.ntqqrev.yogurt.util

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.event.QRCodeGeneratedEvent
import org.ntqqrev.qrmatrix.ErrorCorrectionLevel
import org.ntqqrev.qrmatrix.generateMatrix
import org.ntqqrev.yogurt.YogurtApp.t
import org.ntqqrev.yogurt.fs.withFs
import org.ntqqrev.yogurt.qrCodePath

object Palette {
    const val WHITE_WHITE = '\u2588'
    const val WHITE_BLACK = '\u2580'
    const val BLACK_WHITE = '\u2584'
    const val BLACK_BLACK = ' '
}

fun generateTerminalQRCode(data: String) = buildString {
    val matrix = generateMatrix(data, ErrorCorrectionLevel.LOW)
    val size = matrix.size

    for (row in 0 until size step 2) {
        for (col in 0 until size) {
            val paintUpper = matrix[col][row]
            val paintLower = if (row + 1 < size) matrix[col][row + 1] else false
            val char = when {
                paintUpper && paintLower -> Palette.WHITE_WHITE
                paintUpper && !paintLower -> Palette.WHITE_BLACK
                !paintUpper && paintLower -> Palette.BLACK_WHITE
                else -> Palette.BLACK_BLACK
            }
            append(char)
        }
        appendLine()
    }
}

fun Application.configureQRCodeDisplay() = launch {
    val bot = dependencies.resolve<AbstractBot>()
    bot.eventFlow.filterIsInstance<QRCodeGeneratedEvent>().collect {
        t.println("请用手机 QQ 扫描二维码：")
        t.println(generateTerminalQRCode(it.url))
        t.println(
            """
                或使用以下 URL 生成二维码并扫描：
                ${it.url}
            """.trimIndent()
        )
        withFs {
            qrCodePath.write(it.png)
            t.println("二维码文件已保存至 ${resolve(qrCodePath)}")
        }
    }
}