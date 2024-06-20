package allen.town.podcast.discovery

import allen.town.podcast.core.service.download.PodcastHttpClient
import de.mfietz.fyydlin.FyydClient
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FyydPodcastSearcher : PodcastSearcher {
    private val client = FyydClient(PodcastHttpClient.getHttpClient())
    override fun search(query: String?): Single<List<PodcastSearchResult?>?>? {
        return Single.create(
            SingleOnSubscribe { subscriber: SingleEmitter<List<PodcastSearchResult?>?> ->
                val (_, _, _, data) = client.searchPodcasts(
                    query!!, 10
                )
                    .subscribeOn(Schedulers.io())
                    .blockingGet()
                val searchResults = ArrayList<PodcastSearchResult?>()
                if (!data.isEmpty()) {
                    for (searchHit in data) {
                        val podcast: PodcastSearchResult =
                            PodcastSearchResult.Companion.fromFyyd(searchHit)
                        searchResults.add(podcast)
                    }
                }
                subscriber.onSuccess(searchResults)
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun lookupUrl(url: String): Single<String> {
        return Single.just(url)
    }

    override fun urlNeedsLookup(url: String): Boolean {
        return false
    }

    override val name: String
        get() = "fyyd"
    companion object{
        const val SEARCH_ENGINE_TAG = "fyyd_podcast_search"
    }
}