package allen.town.podcast.discovery

import allen.town.focus_common.util.Timber
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.discovery.CombinedSearcher
import io.reactivex.Single

object PodcastSearcherRegistry {
    @get:Synchronized
    var searchProviders: ArrayList<SearcherInfo>? = null
        get() {
            searchProviders = ArrayList()
            val searchList = Prefs.podcastSearchEngineList
            field!!.clear()
            field!!.add(SearcherInfo(CombinedSearcher(), 1.0f))
            for (searchEngine in searchList) {
                if(searchEngine.visible){
                    when (searchEngine.tag) {
                        ItunesPodcastSearcher.SEARCH_ENGINE_TAG -> {
                            Timber.d("add itunes search")
                            field!!.add(SearcherInfo(ItunesPodcastSearcher(), 1.0f))
                        }
                        FyydPodcastSearcher.SEARCH_ENGINE_TAG -> {
                            Timber.d("add fyyd search")
                            field!!.add(SearcherInfo(FyydPodcastSearcher(), 1.0f))
                        }
                        PodcastIndexPodcastSearcher.SEARCH_ENGINE_TAG -> {
                            Timber.d("add podcastIndex search")
                            field!!.add(SearcherInfo(PodcastIndexPodcastSearcher(), 1.0f))
                        }
                    }
                }


            }

            return field
        }
        private set

    //把搜索和查询url的分开，因为itunes始终需要查询url而其他的不需要
    @get:Synchronized
    var  lookUpUrlProviders: MutableList<SearcherInfo>? = null
        get() {
            if (field == null) {
                lookUpUrlProviders = ArrayList()
                field!!.add(SearcherInfo(CombinedSearcher(), 1.0f))
                field!!.add(SearcherInfo(FyydPodcastSearcher(), 1.0f))
                field!!.add(SearcherInfo(ItunesPodcastSearcher(), 1.0f))
                field!!.add(SearcherInfo(PodcastIndexPodcastSearcher(), 1.0f))
            }
            return field
        }
        private set
    /**
     * 目前看只有itunes需要，寻找到真实的feed rss url
     * @param url
     * @return
     */
    @JvmStatic
    fun lookupUrl(url: String): Single<String> {
        for (searchProviderInfo in lookUpUrlProviders!!) {
            if (searchProviderInfo.searcher.javaClass != CombinedSearcher::class.java
                && searchProviderInfo.searcher.urlNeedsLookup(url)
            ) {
                return searchProviderInfo.searcher.lookupUrl(url)
            }
        }
        return Single.just(url)
    }

    fun urlNeedsLookup(url: String): Boolean {
        for (searchProviderInfo in lookUpUrlProviders!!) {
            if (searchProviderInfo.searcher.javaClass != CombinedSearcher::class.java
                && searchProviderInfo.searcher.urlNeedsLookup(url)
            ) {
                return true
            }
        }
        return false
    }

    class SearcherInfo(val searcher: PodcastSearcher, val weight: Float)
}