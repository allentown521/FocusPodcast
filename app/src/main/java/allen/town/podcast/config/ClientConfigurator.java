package allen.town.podcast.config;


import allen.town.podcast.BuildConfig;
import allen.town.podcast.core.ClientConfig;

/**
 * Configures the ClientConfig class of the core package.
 */
class ClientConfigurator {

    private ClientConfigurator() {
    }

    static {
        ClientConfig.USER_AGENT = "FocusPodcast_" + BuildConfig.VERSION_NAME;
        ClientConfig.applicationCallbacks = new ApplicationCallbacksImpl();
        ClientConfig.downloadServiceCallbacks = new DownloadServiceCallbacksImpl();
    }
}
