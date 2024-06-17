package allen.town.podcast.activity

import allen.town.focus_common.extensions.*
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.LanguageContextWrapper
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.util.theme.ThemeManager
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.playback.cast.CastEnabledActivity
import java.util.*

abstract class SimpleToolbarActivity : CastEnabledActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        updateTheme()
//        hideStatusBar(R.id.status_bar)
        super.onCreate(savedInstanceState)
        setEdgeToEdgeOrImmersive(R.id.status_bar,false)
        toggleScreenOn()
        setLightNavigationBarAuto()
        setLightStatusBarAuto(surfaceColor())
        if (VersionUtils.hasQ()) {
            window.decorView.isForceDarkAllowed = false
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        exitFullscreen()
    }

    private fun updateTheme() {
        setTheme(Prefs.theme)
        AppCompatDelegate.setDefaultNightMode(ThemeManager.getNightMode(application))

        if (BasePreferenceUtil.circlePlayButton) {
            //开启后上下文菜单背景和图标显示异常所以关闭
            setTheme(R.style.CircleFABOverlay)
        }
    }

    override fun    attachBaseContext(newBase: Context?) {
        val code = BasePreferenceUtil.languageCode
        val locale = if (code == "auto") {
            // Get the device default locale
            ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
        } else {
            Locale.forLanguageTag(code)
        }
        super.attachBaseContext(LanguageContextWrapper.wrap(newBase, locale))
        //和Android APP Bundle有关，加载资源用的，对apk方式有没有影响
        installSplitCompat()
    }
}