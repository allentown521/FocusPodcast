package allen.town.podcast.event.settings;

public class LoudnessChangedEvent {
    private final boolean enable;
    private final long feedId;

    public LoudnessChangedEvent(boolean enable, long feedId) {
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
