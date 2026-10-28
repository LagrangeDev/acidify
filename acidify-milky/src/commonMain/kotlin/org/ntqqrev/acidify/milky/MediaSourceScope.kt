package org.ntqqrev.acidify.milky

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import org.ntqqrev.acidify.common.MediaSource

class MediaSourceScope(
    private val onDisposeFailure: (source: MediaSource, exception: Exception) -> Unit
) {
    // Segments may be transformed concurrently; null means the scope has been disposed.
    private val trackedSources = atomic<List<MediaSource>?>(emptyList())

    fun track(source: MediaSource) {
        trackedSources.loop { current ->
            if (current == null) {
                // Late arrivals would otherwise never be released.
                disposeSafely(source)
                return
            }
            if (trackedSources.compareAndSet(current, current + source)) return
        }
    }

    fun disposeAll() {
        trackedSources.getAndSet(null)?.forEach(::disposeSafely)
    }

    private fun disposeSafely(source: MediaSource) {
        try {
            source.dispose()
        } catch (e: Exception) {
            onDisposeFailure(source, e)
        }
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
