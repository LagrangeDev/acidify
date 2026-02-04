package org.ntqqrev.yogurt.script.stdlib

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import kotlin.time.TimeMark
import kotlin.time.TimeSource

fun QuickJs.defineConsole() = define("console") {
    val consoleTimers = mutableMapOf<String, TimeMark>()
    val consoleCounts = mutableMapOf<String, Int>()
    var consoleGroupDepth = 0

    fun indent(): String = "  ".repeat(consoleGroupDepth.coerceAtLeast(0))

    fun formatArgs(args: Array<Any?>): String {
        if (args.isEmpty()) return ""
        return args.joinToString(" ") { it?.toString() ?: "null" }
    }

    fun printLine(prefix: String?, args: Array<Any?>) {
        val message = formatArgs(args)
        val space = if (prefix.isNullOrBlank() || message.isBlank()) "" else " "
        println("${indent()}${prefix.orEmpty()}$space$message")
    }

    fun tablePrint(data: Any?) {
        when (data) {
            null -> println("${indent()}null")
            is Map<*, *> -> {
                println("${indent()}Key\tValue")
                data.forEach { (key, value) ->
                    println("${indent()}${key}\t${value}")
                }
            }

            is Array<*> -> {
                tablePrint(data.asList())
            }

            is Iterable<*> -> {
                val items = data.toList()
                if (items.all { it is Map<*, *> } && items.isNotEmpty()) {
                    val keys = (items.first() as Map<*, *>).keys.toList()
                    println("${indent()}${keys.joinToString("\t")}")
                    items.forEach { item ->
                        val row = item as Map<*, *>
                        val values = keys.joinToString("\t") { key -> row[key].toString() }
                        println("${indent()}$values")
                    }
                } else {
                    println("${indent()}Index\tValue")
                    items.forEachIndexed { index, value ->
                        println("${indent()}$index\t${value}")
                    }
                }
            }

            else -> println("${indent()}${data}")
        }
    }

    function("log") { args ->
        printLine(null, args)
    }

    function("info") { args ->
        printLine("INFO", args)
    }

    function("warn") { args ->
        printLine("WARN", args)
    }

    function("error") { args ->
        printLine("ERROR", args)
    }

    function("debug") { args ->
        printLine("DEBUG", args)
    }

    function("assert") { args ->
        val condition = args.firstOrNull() as? Boolean ?: false
        if (!condition) {
            val messageArgs = if (args.size > 1) args.copyOfRange(1, args.size) else emptyArray()
            val message =
                if (messageArgs.isEmpty()) "Assertion failed" else "Assertion failed: ${formatArgs(messageArgs)}"
            printLine("ERROR", arrayOf(message))
        }
    }

    function("trace") { args ->
        val prefixArgs = if (args.isEmpty()) arrayOf<Any?>("Trace") else arrayOf<Any?>("Trace:", formatArgs(args))
        printLine("TRACE", prefixArgs)
    }

    function("group") { args ->
        if (args.isNotEmpty()) {
            printLine(null, args)
        }
        consoleGroupDepth += 1
    }

    function("groupEnd") { _ ->
        consoleGroupDepth = (consoleGroupDepth - 1).coerceAtLeast(0)
    }

    function("time") { args ->
        val label = args.firstOrNull()?.toString() ?: "default"
        consoleTimers[label] = TimeSource.Monotonic.markNow()
    }

    function("timeEnd") { args ->
        val label = args.firstOrNull()?.toString() ?: "default"
        val start = consoleTimers.remove(label)
        if (start == null) {
            printLine("WARN", arrayOf("No such label '$label' for timeEnd"))
        } else {
            val duration = start.elapsedNow()
            printLine(null, arrayOf("$label: ${duration.inWholeMilliseconds}ms"))
        }
    }

    function("count") { args ->
        val label = args.firstOrNull()?.toString() ?: "default"
        val next = (consoleCounts[label] ?: 0) + 1
        consoleCounts[label] = next
        printLine(null, arrayOf("$label: $next"))
    }

    function("table") { args ->
        tablePrint(args.firstOrNull())
    }
}