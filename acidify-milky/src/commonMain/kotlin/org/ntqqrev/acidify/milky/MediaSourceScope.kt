package org.ntqqrev.acidify.milky

import org.ntqqrev.acidify.common.MediaSource

class MediaSourceScope(
    private val onDisposeFailure: (source: MediaSource, exception: Exception) -> Unit
) {
    private val trackedSources = mutableListOf<MediaSource>()

    fun track(source: MediaSource) {
        trackedSources.add(source)
    }

    fun disposeAll() {
        trackedSources.forEach {
            try {
                it.dispose()
            } catch (e: Exception) {
                onDisposeFailure(it, e)
            }
        }
        trackedSources.clear()
    }
}

inline fun <R> mediaSourceScoped(
    noinline onDisposeFailure: (source: MediaSource, exception: Exception) -> Unit = { _, _ -> },
    block: context(MediaSourceScope) () -> R
): R {
    val scope = MediaSourceScope(onDisposeFailure)
    try {
        return context(scope) {
            block()
        }
    } finally {
        scope.disposeAll()
    }
}

context(scope: MediaSourceScope)
inline fun <R : MediaSource> tracked(block: () -> R): R {
    val source = block()
    scope.track(source)
    return source
}