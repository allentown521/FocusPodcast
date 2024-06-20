package allen.town.podcast.event.settings;

public class MonoChangedEvent {
    private final boolean enable;
    private final long feedId;

    public MonoChangedEvent(boolean enable, long feedId) {
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
