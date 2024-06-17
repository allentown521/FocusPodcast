package allen.town.podcast.discovery

import io.reactivex.Single

interface PodcastSearcher {
    fun search(query: String?): Single<List<PodcastSearchResult?>?>?

    /**
     * 查找真实的feed url
     * @param resultUrl
     *
     * @return
     */
    fun lookupUrl(resultUrl: String): Single<String>
    fun urlNeedsLookup(resultUrl: String): Boolean
    val name: String
}