package allen.town.podcast.core.storage;

import allen.town.podcast.model.feed.Feed;

public class StatisticsItem {
    public final Feed feed;
    public final long time;

    /**
     * Respects speed, listening twice, ...
     */
    public final long timePlayed;

    /**
     * Number of episodes.
     */
    public final long episodes;

    /**
     * Episodes that are actually played.
     */
    public final long episodesStarted;

    /**
     * Simply sums up the size of download podcasts.
     */
    public final long totalDownloadSize;

    /**
     * Stores the number of episodes downloaded.
     */
    public final long episodesDownloadCount;

    public StatisticsItem(Feed feed, long time, long timePlayed,
                          long episodes, long episodesStarted,
                          long totalDownloadSize, long episodesDownloadCount) {
        this.feed = feed;
        this.time = time;
        this.timePlayed = timePlayed;
        this.episodes = episodes;
        this.episodesStarted = episodesStarted;
        this.totalDownloadSize = totalDownloadSize;
        this.episodesDownloadCount = episodesDownloadCount;
    }
}
