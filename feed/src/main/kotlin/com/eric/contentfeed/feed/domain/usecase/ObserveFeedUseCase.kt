package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.data.local.ServiceCardCatalog
import com.eric.contentfeed.feed.domain.composition.FeedComposer
import com.eric.contentfeed.feed.domain.model.ArticleStreamStatus
import com.eric.contentfeed.feed.domain.model.FeedSnapshot
import com.eric.contentfeed.feed.repository.ArticleRepository
import com.eric.contentfeed.feed.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Combines Room's live article/placement/weather reads into one composed
 * [FeedSnapshot], recomposing on every emission. [FeedComposer] is a pure function —
 * called directly rather than injected, since it holds no state or dependencies of
 * its own; its algorithm is unit-tested in isolation via `FeedComposerTest`. */
class ObserveFeedUseCase(
    private val articleRepository: ArticleRepository,
    private val weatherRepository: WeatherRepository,
    private val serviceCardCatalog: ServiceCardCatalog,
) {
    operator fun invoke(): Flow<FeedSnapshot> {
        val itemsFlow =
            combine(
                articleRepository.observeArticles(),
                articleRepository.observePlacements(),
            ) { articles, placements ->
                FeedComposer.compose(articles, placements, serviceCardCatalog)
            }
        val articleStreamStatusFlow =
            combine(
                articleRepository.refreshStatus,
                articleRepository.appendStatus,
                articleRepository.isLastPage,
            ) { refresh, append, isLastPage -> ArticleStreamStatus(refresh, append, isLastPage) }

        return combine(
            itemsFlow,
            articleStreamStatusFlow,
            weatherRepository.observeWeather(),
            weatherRepository.status,
        ) { items, articleStreamStatus, weather, weatherStatus ->
            FeedSnapshot(
                weather = weather,
                weatherStatus = weatherStatus,
                items = items,
                articleStreamStatus = articleStreamStatus,
            )
        }
    }
}
