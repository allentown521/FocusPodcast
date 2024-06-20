package allen.town.podcast.event.playback;

public class TitleChangeEvent {
    private final int title;

    public TitleChangeEvent(int title) {
        this.title = title;
    }

    public int getTitle() {
        return title;
    }

}
