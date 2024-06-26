package allen.town.podcast.parser.feed;

import java.util.Map;

import allen.town.podcast.model.feed.Feed;

/**
 * Container for results returned by the Feed parser
 */
public class FeedHandlerResult {

    public final Feed feed;
    public final Map<String, String> alternateFeedUrls;
    public final String redirectUrl;

    public FeedHandlerResult(Feed feed, Map<String, String> alternateFeedUrls, String redirectUrl) {
        this.feed = feed;
        this.alternateFeedUrls = alternateFeedUrls;
        this.redirectUrl = redirectUrl;
    }
}
