package allen.town.podcast.net.ssl;

import android.content.Context;


public class SslProviderInstaller {
    public static void install(Context context) {
        // Insert bundled conscrypt as highest security provider (overrides OS version).
//        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }
}
