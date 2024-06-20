package allen.town.podcast.core.service.download.handler;

import allen.town.podcast.core.event.DownloadEvent;
import allen.town.podcast.core.service.download.Downloader;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostDownloaderTask implements Runnable {
    private List<Downloader> downloads;

    public PostDownloaderTask(List<Downloader> downloads) {
        this.downloads = downloads;
    }

    @Override
    public void run() {
        List<Downloader> runningDownloads = new ArrayList<>();
        for (Downloader downloader : downloads) {
            if (!downloader.cancelled) {
                runningDownloads.add(downloader);
            }
        }
        List<Downloader> list = Collections.unmodifiableList(runningDownloads);
        EventBus.getDefault().postSticky(DownloadEvent.refresh(list));
    }
}
