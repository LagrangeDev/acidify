@file:OptIn(ExperimentalContracts::class)

package org.ntqqrev.acidify.internal.util

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.internal.LagrangeClient
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal fun AbstractClient.ensureLagrange() {
    contract {
        returns() implies (this@ensureLagrange is LagrangeClient)
    }
    if (this !is LagrangeClient) {
        throw IllegalStateException("This operation is only supported for LagrangeClient.")
    }
}

internal fun AbstractClient.ensureKurome() {
    contract {
        returns() implies (this@ensureKurome is KuromeClient)
    }
    if (this !is KuromeClient) {
        throw IllegalStateException("This operation is only supported for KuromeClient.")
    }
}