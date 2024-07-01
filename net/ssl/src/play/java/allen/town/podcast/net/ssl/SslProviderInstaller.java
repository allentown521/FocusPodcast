package allen.town.podcast.net.ssl;

import android.content.Context;

public class SslProviderInstaller {
    public static void install(Context context) {
//        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        //如果开启了那么 https://openlanguage.com/ez/podcast/learn-english/pj 这个订阅源在s21上无法订阅
        /*try {
            ProviderInstaller.installIfNeeded(context);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
            GoogleApiAvailability.getInstance().showErrorNotification(context, e.getConnectionStatusCode());
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }*/
    }
}
