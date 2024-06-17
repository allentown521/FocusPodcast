package allen.town.podcast.config;


import android.app.Application;

import allen.town.podcast.MyApp;
import allen.town.podcast.core.ApplicationCallbacks;

public class ApplicationCallbacksImpl implements ApplicationCallbacks {

    @Override
    public Application getApplicationInstance() {
        return MyApp.getInstance();
    }

}
