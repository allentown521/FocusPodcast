package allen.town.podcast.discovery

import java.util.*

class EpisodeSearchResult(
    title: String,
    imageUrl: String?,
    feedUrl: String?,
    author: String?,
    description: String?,
    itunesFeedId: String?,
    publishDate: String?,
    duration: Long,
    val episodeTitle: String?,
    val episodeUuid: String?,
    val episodeMimeType: String?,
    val episodeUrl: String?,
    val publishDateTypeDate: Date
) : PodcastSearchResult(
    title,
    imageUrl,
    feedUrl,
    author,
    description,
    itunesFeedId,
    publishDate,
    duration
) {

    companion object {
        var TAG = "EpisodeSearchResult"
    }
}