package allen.town.podcast.discovery

import allen.town.focus_common.util.Timber
import allen.town.podcast.MyApp.Companion.instance
import allen.town.podcast.core.util.DateFormatter
import allen.town.podcast.parser.feed.util.ItunesEpisodesDateUtils
import allen.town.podcast.sync.gpoddernet.model.GpodnetPodcast
import android.text.TextUtils
import de.mfietz.fyydlin.SearchHit
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicReference

open class PodcastSearchResult(
    /**
     * The name of the podcast
     */
    val title: String,
    /**
     * URL of the podcast image
     */
    val imageUrl: String?,
    /**
     * URL of the podcast feed
     */
    val feedUrl: String?,
    /**
     * artistName of the podcast feed
     */
    val author: String?,
    val description: String?,
    val itunesFeedId: String?,
    val publishDate: String?,
    duration: Long
) {
    var duration: Long = 0

    companion object {
        var TAG = "PodcastSearchResult"
        fun dummy(): PodcastSearchResult {
            return PodcastSearchResult("", "", "", "", "", "", "", 0)
        }

        /**
         * Constructs a Podcast instance from a iTunes search result
         *
         * @param json object holding the podcast information
         * @throws JSONException
         */
        fun fromItunes(json: JSONObject): PodcastSearchResult {
            val title = json.optString("collectionName", "")
            val imageUrl = json.optString("artworkUrl100", null)
            val feedUrl = json.optString("feedUrl", null)
            val author = json.optString("artistName", null)
            val publicationDate = DateFormatter.formatAbbrev(
                instance,
                Date(ItunesEpisodesDateUtils.getTime(json.optString("releaseDate", null)))
            )
            //releaseDate 格式一样
            //没有uuid和description
            return PodcastSearchResult(title, imageUrl, feedUrl, author, "", "", publicationDate, 0)
        }

        fun fromItunesEpisodes(json: JSONObject): PodcastSearchResult {
            val title = json.optString("collectionName", "")
            val episodeTitle = json.optString("trackName", "")
            val imageUrl = json.optString("artworkUrl160", "")
            val feedUrl = json.optString("feedUrl", "")
            val author = json.optString("artistName", "") //实际并没有
            val uuid = json.optString("episodeGuid", "")
            val dateStr = json.optString("releaseDate", null)
            //计算出来没有年付，6月10日这样
            val publicationDate =
                DateFormatter.formatAbbrev(instance, Date(ItunesEpisodesDateUtils.getTime(dateStr)))
            var description = json.optString("description")
            if (TextUtils.isEmpty(description)) {
                description = json.optString("shortDescription")
            }
            val itunesId = json.optString("collectionId", "")
            //size 没有
            var duration: Long = 0
            //存数据库就可以了，会当做audio处理
            val mimeType = json.optString("episodeContentType")
            val episodeUrl = json.optString("episodeUrl")
            try {
                duration = json.getLong("trackTimeMillis")
            } catch (unused2: Throwable) {
                duration = -1
            } finally {
                if (duration <= 0) {
                    duration = -1
                }
            }
            //contentAdvisoryRating（内容咨询评级） 为Explicit 代表是露骨内容，默认是Clean
/* 小写
 public enum PodcastTypeEnum {
            UNINITIALIZED,
            NONE,
            VIRTUAL,
            AUDIO,
            VIDEO,
            YOUTUBE,
            VIMEO,
            DAILYMOTION,
            LIVE_STREAM,
            SEARCH_BASED,
            TWITCH
        }*/return EpisodeSearchResult(
                title, imageUrl, feedUrl, author,
                description, itunesId, publicationDate, duration, episodeTitle,
                uuid, mimeType, episodeUrl, Date(ItunesEpisodesDateUtils.getTime(dateStr))
            )
        }

        /**
         * Constructs a Podcast instance from iTunes toplist entry
         *
         * @param json object holding the podcast information
         * @throws JSONException
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun fromItunesToplist(json: JSONObject): PodcastSearchResult {
            val title = json.getJSONObject("im:name").getString("label") //title 下的label太长了
            var summary: String? = null
            try {
                summary = json.getJSONObject("summary").getString("label")
            } catch (e: Exception) {
                // Some feeds have empty summary
            }
            var imageUrl: String? = null
            val images = json.getJSONArray("im:image")
            var i = 0
            while (imageUrl == null && i < images.length()) {
                val image = images.getJSONObject(i)
                val height = image.getJSONObject("attributes").getString("height")
                if (height.toInt() >= 100) {
                    imageUrl = image.getString("label")
                }
                i++
            }
            val itunesFeedId =
                json.getJSONObject("id").getJSONObject("attributes").getString("im:id")
            val feedUrl = AtomicReference("https://itunes.apple.com/lookup?id=$itunesFeedId")
            val publicationDate = DateFormatter.formatAbbrev(
                instance,
                Date(
                    ItunesEpisodesDateUtils.getTime(
                        json.getJSONObject("im:releaseDate").getString("label")
                    )
                )
            )
            var author: String? = null
            try {
                author = json.getJSONObject("im:artist").getString("label")
            } catch (e: Exception) {
                // Some feeds have empty artist
            }
            return PodcastSearchResult(
                title,
                imageUrl,
                feedUrl.get(),
                author,
                summary,
                itunesFeedId,
                publicationDate,
                0
            )
        }

        fun fromFyyd(searchHit: SearchHit): PodcastSearchResult {
            return PodcastSearchResult(
                searchHit.title,
                searchHit.thumbImageURL,
                searchHit.xmlUrl,
                searchHit.author, searchHit.description, "", DateFormatter.formatAbbrev(
                    instance,
                    searchHit.lastPubDate
                ), 0
            )
        }

        /**
         * 接口返回空，似乎没有用了？
         *
         * @param searchHit
         * @return
         */
        fun fromGpodder(searchHit: GpodnetPodcast): PodcastSearchResult {
            return PodcastSearchResult(
                searchHit.title,
                searchHit.logoUrl,
                searchHit.url,
                searchHit.author, searchHit.description, "", "", 0
            )
        }

        fun fromPodcastIndex(json: JSONObject): PodcastSearchResult {
            val title = json.optString("title", "")
            val imageUrl = json.optString("image", null)
            val feedUrl = json.optString("url", null)
            val author = json.optString("author", null)
            val description = json.optString("description", null)
            val lastUpdateTime = json.optString("lastUpdateTime", null)
            var date: Date? = null
            if (!TextUtils.isEmpty(lastUpdateTime)) {
                try {
                    date = Date(lastUpdateTime.toLong() * 1000)
                } catch (e: NumberFormatException) {
                    Timber.e(e, "fromPodcastIndex parse time failed")
                }
            }
            return PodcastSearchResult(
                title, imageUrl, feedUrl, author, description, "",
                DateFormatter.formatAbbrev(
                    instance,
                    date
                ), 0
            )
        }
    }

    init {
        this.duration = duration
    }
}