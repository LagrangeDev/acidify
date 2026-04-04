package org.ntqqrev.acidify.milky.api

import org.ntqqrev.acidify.milky.MediaSourceScope
import org.ntqqrev.acidify.milky.MilkyContext

class MilkyApiHandler<T : Any, R : Any>(
    val path: String,
    val callHandler: suspend context(MediaSourceScope) MilkyContext.(payload: T) -> R,
)