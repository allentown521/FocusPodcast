package allen.town.podcast.sync.model;

public class SyncServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public SyncServiceException(String message) {
        super(message);
    }

    public SyncServiceException(Throwable cause) {
        super(cause);
    }
}
