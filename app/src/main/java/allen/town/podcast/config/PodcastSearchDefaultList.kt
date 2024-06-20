package allen.town.podcast.config

import allen.town.focus_common.model.CategoryInfo
import allen.town.podcast.discovery.FyydPodcastSearcher
import allen.town.podcast.discovery.ItunesPodcastSearcher
import allen.town.podcast.discovery.PodcastIndexPodcastSearcher

object PodcastSearchDefaultList {
    val defaultList: List<CategoryInfo>
        get() = listOf(
            CategoryInfo(
                ItunesPodcastSearcher.SEARCH_ENGINE_TAG,
                true,
                true
            ),
            CategoryInfo(
                PodcastIndexPodcastSearcher.SEARCH_ENGINE_TAG,
                false,
                true
            ),
            CategoryInfo(
                FyydPodcastSearcher.SEARCH_ENGINE_TAG,
                false,
                true
            )

        )
}