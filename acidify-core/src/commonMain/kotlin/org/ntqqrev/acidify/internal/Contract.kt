package org.ntqqrev.acidify.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Marks a service or operation as only supported for LagrangeClient.
 */
@OptIn(ExperimentalContracts::class)
internal fun IClient.ensureIsLagrange() {
    contract {
        returns() implies (this@ensureIsLagrange is LagrangeClient)
    }
    if (this !is LagrangeClient) {
        throw IllegalStateException("This operation is only supported for LagrangeClient")
    }
}