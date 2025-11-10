package org.ntqqrev.acidify.logging

import kotlin.reflect.KClass

actual val KClass<*>.loggingTag: String?
    get() = simpleName