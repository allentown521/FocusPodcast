package allen.town.podcast.discovery

import io.reactivex.Single

class ItunesEpisodesSearcher : ItunesPodcastSearcher() {
    override fun search(query: String?): Single<List<PodcastSearchResult?>?>? {
        return searchByApiUrl(query, ITUNES_EPISODS_API_URL)
    }

    companion object {
        //搜单集
        private const val ITUNES_EPISODS_API_URL =
            "https://itunes.apple.com/search?entity=podcastEpisode&media=podcast&limit=100&term=%s"
    }
}