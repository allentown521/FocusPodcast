package allen.town.podcast.core.util

import allen.town.focus_common.ui.customtabs.BrowserLauncher
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context

object IntentUtils {
    private const val TAG = "IntentUtils"

    /*
     *  Checks if there is at least one exported activity that can be performed for the intent
     */
    @JvmStatic
    fun isCallable(context: Context, intent: Intent?): Boolean {
        val list = context.packageManager.queryIntentActivities(
            intent!!,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        for (info in list) {
            if (info.activityInfo.exported) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun sendLocalBroadcast(context: Context, action: String?) {
        context.sendBroadcast(Intent(action).setPackage(context.packageName))
    }

    @JvmStatic
    fun openInBrowser(context: Context, url: String?) {
        BrowserLauncher.openUrl(context,url);
        /*try {
            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            showSnack(context, R.string.pref_no_browser_found, Toast.LENGTH_LONG)
            Log.e(TAG, Log.getStackTraceString(e))
        }*/
    }

}