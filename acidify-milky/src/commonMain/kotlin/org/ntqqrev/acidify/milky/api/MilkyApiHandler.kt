package org.ntqqrev.acidify.milky.api

import org.ntqqrev.acidify.milky.MilkyContext

class MilkyApiHandler<T : Any, R : Any>(
    val path: String,
    val callHandler: suspend MilkyContext.(payload: T) -> R,
)