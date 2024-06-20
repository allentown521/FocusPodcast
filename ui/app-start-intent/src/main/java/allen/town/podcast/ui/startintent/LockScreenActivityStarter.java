package allen.town.podcast.ui.startintent;

import android.content.Context;
import android.content.Intent;

/**
 * Launches the main activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class LockScreenActivityStarter {
    public static final String INTENT = "allen.town.focus.podcast.LockScreenActivity";

    private final Intent intent;
    private final Context context;

    public LockScreenActivityStarter(Context context) {
        this.context = context;
        intent = new Intent(INTENT);
        intent.setPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    public Intent getIntent() {
        return intent;
    }


    public void start() {
        context.startActivity(getIntent());
    }

}
