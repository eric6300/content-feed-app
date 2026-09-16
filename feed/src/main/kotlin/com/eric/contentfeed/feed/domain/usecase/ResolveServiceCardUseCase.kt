package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.data.local.ServiceCardCatalog
import com.eric.contentfeed.feed.domain.model.ServiceCard

/** Resolves a deterministic bundled service card for a navigation detail key. */
class ResolveServiceCardUseCase(
    private val catalog: ServiceCardCatalog,
) {
    operator fun invoke(poolIndex: Int): ServiceCard? = catalog.cardForSlot(poolIndex)
}
