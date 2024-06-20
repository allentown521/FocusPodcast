package allen.town.podcast.discovery

import allen.town.podcast.core.feed.FeedUrlNotFoundException
import allen.town.podcast.core.service.download.PodcastHttpClient
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern

open class ItunesPodcastSearcher : PodcastSearcher {
    fun searchByApiUrl(query: String?, url: String?): Single<List<PodcastSearchResult?>?> {
        return Single.create(
            SingleOnSubscribe { subscriber: SingleEmitter<List<PodcastSearchResult?>?> ->
                val encodedQuery: String?
                encodedQuery = try {
                    URLEncoder.encode(query, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    // this won't ever be thrown
                    query
                }
                val formattedUrl = String.format(url!!, encodedQuery)
                val client = PodcastHttpClient.getHttpClient()
                val httpReq = Request.Builder()
                    .url(formattedUrl)
                val podcasts: MutableList<PodcastSearchResult?> = ArrayList()
                try {
                    val response = client.newCall(httpReq.build()).execute()
                    if (response.isSuccessful) {
                        val resultString = response.body!!.string()
                        val result = JSONObject(resultString)
                        val j = result.getJSONArray("results")
                        var podcast: PodcastSearchResult
                        for (i in 0 until j.length()) {
                            val podcastJson = j.getJSONObject(i)
                            podcast = if (this@ItunesPodcastSearcher is ItunesEpisodesSearcher) {
                                PodcastSearchResult.Companion.fromItunesEpisodes(podcastJson)
                            } else {
                                PodcastSearchResult.Companion.fromItunes(podcastJson)
                            }
                            if (podcast.feedUrl != null) {
                                podcasts.add(podcast)
                            }
                        }
                    } else {
                        subscriber.onError(IOException(response.toString()))
                    }
                } catch (e: IOException) {
                    subscriber.onError(e)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }
                subscriber.onSuccess(podcasts)
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun search(query: String?): Single<List<PodcastSearchResult?>?>? {
        return searchByApiUrl(query, ITUNES_API_URL)
    }

    override fun lookupUrl(url: String): Single<String> {
        val pattern = Pattern.compile(PATTERN_BY_ID)
        val matcher = pattern.matcher(url)
        val lookupUrl =
            if (matcher.find()) "https://itunes.apple.com/lookup?id=" + matcher.group(1) else url
        return Single.create { emitter: SingleEmitter<String> ->
            val client = PodcastHttpClient.getHttpClient()
            val httpReq = Request.Builder().url(lookupUrl)
            try {
                val response = client.newCall(httpReq.build()).execute()
                if (response.isSuccessful) {
                    val resultString = response.body!!.string()
                    val result = JSONObject(resultString)
                    val results = result.getJSONArray("results").getJSONObject(0)
                    val feedUrlName = "feedUrl"
                    if (!results.has(feedUrlName)) {
                        val artistName = results.getString("artistName")
                        val trackName = results.getString("trackName")
                        emitter.onError(FeedUrlNotFoundException(artistName, trackName))
                        return@create
                    }
                    val feedUrl = results.getString(feedUrlName)
                    emitter.onSuccess(feedUrl)
                } else {
                    emitter.onError(IOException(response.toString()))
                }
            } catch (e: IOException) {
                emitter.onError(e)
            } catch (e: JSONException) {
                emitter.onError(e)
            }
        }
    }

    override fun urlNeedsLookup(url: String): Boolean {
        return url.contains("itunes.apple.com") || url.matches(Regex(PATTERN_BY_ID))
    }

    override val name: String
        get() = "Apple"

    companion object {
        //搜播客
        private const val ITUNES_API_URL = "https://itunes.apple.com/search?media=podcast&term=%s"
        private const val PATTERN_BY_ID = ".*/podcasts\\.apple\\.com/.*/podcast/.*/id(\\d+).*"
        const val SEARCH_ENGINE_TAG = "itunes_podcast_search"
    }
}