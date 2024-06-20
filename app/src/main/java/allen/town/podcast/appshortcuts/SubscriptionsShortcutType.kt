package allen.town.podcast.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.os.Build
import code.name.monkey.appthemehelper.shortcut.AppShortcutIconGenerator
import code.name.monkey.appthemehelper.shortcut.BaseShortcutType
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.activity.MainActivity.Companion.EXTRA_FRAGMENT_TAG
import allen.town.podcast.fragment.SubFeedsFragment

@TargetApi(Build.VERSION_CODES.N_MR1)
class SubscriptionsShortcutType(context: Context) : BaseShortcutType(context) {
    override val shortcutInfo: ShortcutInfo
        get() = ShortcutInfo.Builder(
            context,
            id
        ).setShortLabel(context.getString(R.string.subscriptions_label)).setIcon(
            AppShortcutIconGenerator.generateThemedIcon(
                context,
                R.drawable.ic_folder_shortcut
            )
        ).setIntent(getPlaySongsIntent(MainActivity::class.java, EXTRA_FRAGMENT_TAG, SubFeedsFragment.TAG))
            .build()

    companion object {

        val id: String
            get() = ID_PREFIX + "subscriptions"
    }
}