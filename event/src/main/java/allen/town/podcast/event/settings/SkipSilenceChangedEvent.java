package allen.town.podcast.event.settings;

public class SkipSilenceChangedEvent {
    private final boolean enable;
    private final long feedId;

    public SkipSilenceChangedEvent(boolean enable, long feedId) {
        this.enable = enable;
        this.feedId = feedId;
    }

    public boolean isEnable() {
        return enable;
    }

    public long getFeedId() {
        return feedId;
    }
}
