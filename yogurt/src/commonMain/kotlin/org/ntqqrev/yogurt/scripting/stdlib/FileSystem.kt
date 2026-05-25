package org.ntqqrev.yogurt.scripting.stdlib

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.toJsObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.ntqqrev.ktfs.FileSystem
import org.ntqqrev.ktfs.withFs

internal suspend inline fun <R> withFsSuspend(crossinline block: FileSystem.() -> R): R = withContext(Dispatchers.IO) {
    withFs(block = block)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun QuickJs.defineStdlibFs() = define("fs") {
    asyncFunction("exists") { args ->
        args.requireSize(1..1, "exists(path: string)")
        val path = args.pathArg(0, "path")
        withFsSuspend { exists(path) }
    }

    asyncFunction("delete") { args ->
        args.requireSize(1..2, "delete(path: string, mustExist?: boolean)")
        val path = args.pathArg(0, "path")
        val mustExist = args.booleanArg(1, "mustExist", default = true)
        withFsSuspend { delete(path, mustExist) }
    }

    asyncFunction("createDirectories") { args ->
        args.requireSize(1..2, "createDirectories(path: string, mustCreate?: boolean)")
        val path = args.pathArg(0, "path")
        val mustCreate = args.booleanArg(1, "mustCreate", default = false)
        withFsSuspend { createDirectories(path, mustCreate) }
    }

    asyncFunction("atomicMove") { args ->
        args.requireSize(2..2, "atomicMove(source: string, destination: string)")
        val source = args.pathArg(0, "source")
        val destination = args.pathArg(1, "destination")
        withFsSuspend { atomicMove(source, destination) }
    }

    asyncFunction("metadataOrNull") { args ->
        args.requireSize(1..1, "metadataOrNull(path: string)")
        val path = args.pathArg(0, "path")
        withFsSuspend {
            metadataOrNull(path)?.let {
                mapOf(
                    "isRegularFile" to it.isRegularFile,
                    "isDirectory" to it.isDirectory,
                    "size" to it.size,
                ).toJsObject()
            }
        }
    }

    asyncFunction("resolve") { args ->
        args.requireSize(1..1, "resolve(path: string)")
        val path = args.pathArg(0, "path")
        withFsSuspend { resolve(path).toString() }
    }

    asyncFunction("list") { args ->
        args.requireSize(1..1, "list(directory: string)")
        val directory = args.pathArg(0, "directory")
        withFsSuspend { list(directory).map { it.toString() } }
    }

    asyncFunction("readText") { args ->
        args.requireSize(1..1, "readText(path: string)")
        val path = args.pathArg(0, "path")
        withFsSuspend { path.readText() }
    }

    asyncFunction("writeText") { args ->
        args.requireSize(2..4, "writeText(path: string, text: string, append?: boolean, createParentDirectories?: boolean)")
        val path = args.pathArg(0, "path")
        val text = args.stringArg(1, "text")
        val append = args.booleanArg(2, "append", default = false)
        val createParentDirectories = args.booleanArg(3, "createParentDirectories", default = false)
        withFsSuspend { path.writeText(text, append, createParentDirectories) }
    }

    asyncFunction("readBytes") { args ->
        args.requireSize(1..1, "readBytes(path: string)")
        val path = args.pathArg(0, "path")
        withFsSuspend { path.readBytes() }
    }

    asyncFunction("writeBytes") { args ->
        args.requireSize(2..4, "writeBytes(path: string, bytes: Int8Array | Uint8Array, append?: boolean, createParentDirectories?: boolean)")
        val path = args.pathArg(0, "path")
        val bytes = args[1].toByteArrayArg("bytes")
        val append = args.booleanArg(2, "append", default = false)
        val createParentDirectories = args.booleanArg(3, "createParentDirectories", default = false)
        withFsSuspend { path.writeBytes(bytes, append, createParentDirectories) }
    }
}
