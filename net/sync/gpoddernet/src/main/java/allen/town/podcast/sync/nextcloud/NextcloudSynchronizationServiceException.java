package allen.town.podcast.sync.nextcloud;

import allen.town.podcast.sync.model.SyncServiceException;

public class NextcloudSynchronizationServiceException extends SyncServiceException {
    public NextcloudSynchronizationServiceException(Throwable e) {
        super(e);
    }
}
