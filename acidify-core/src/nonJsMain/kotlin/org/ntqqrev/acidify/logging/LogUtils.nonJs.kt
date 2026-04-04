package org.ntqqrev.acidify.logging

import kotlin.reflect.KClass

internal actual val KClass<*>.loggingTag: String?
    get() = qualifiedName