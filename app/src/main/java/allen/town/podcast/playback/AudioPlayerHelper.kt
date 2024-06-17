@file:JvmName("AudioPlayerHelper")

package allen.town.podcast.playback

import androidx.appcompat.app.AppCompatActivity
import allen.town.focus_common.extensions.isColorLight
import allen.town.focus_common.extensions.setLightNavigationBar
import allen.town.focus_common.extensions.setLightStatusBar
import allen.town.focus_common.extensions.setNavigationBarColor
import allen.town.podcast.core.playback.NowPlayingScreen
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.fragment.*

fun AppCompatActivity.getSelectedAudioPlayerFragment(): AudioPlayerFragment {
    val audioPlayerFragment: AudioPlayerFragment =
        when (Prefs.nowPlayingScreen) {
            NowPlayingScreen.Blur -> BlurAudioPlayerFragment()
            NowPlayingScreen.Color -> ColorAudioPlayerFragment()
            NowPlayingScreen.Circle -> CircleAudioPlayerFragment()
            NowPlayingScreen.Vinyl -> VinylAudioPlayerFragment()
            NowPlayingScreen.Adaptive -> AdapterAudioPlayerFragment()
            NowPlayingScreen.BlurCard -> BlurCardAudioPlayerFragment()
            NowPlayingScreen.Full -> FullAudioPlayerFragment()
            else -> AudioPlayerFragment(false)
        }
    return audioPlayerFragment
}

fun AppCompatActivity.onPaletteColorChanged(paletteColor: Int) {
    val isColorLight = paletteColor.isColorLight
    val nowPlayingScreen = Prefs.nowPlayingScreen
    if (Prefs.isAdapterColor && (nowPlayingScreen === NowPlayingScreen.Normal
                || nowPlayingScreen === NowPlayingScreen.Adaptive
                || nowPlayingScreen === NowPlayingScreen.Circle)
    ) {
        setLightNavigationBar(true)
        setLightStatusBar(isColorLight)
    } else if (nowPlayingScreen === NowPlayingScreen.Blur
        || nowPlayingScreen === NowPlayingScreen.Color
        || nowPlayingScreen === NowPlayingScreen.Vinyl
        || nowPlayingScreen === NowPlayingScreen.BlurCard
        || nowPlayingScreen === NowPlayingScreen.Full
    ) {
        setNavigationBarColor(paletteColor)
        //                ActivityThemeExtensionsUtils.setLightNavigationBar(this,isColorLight);
        setLightStatusBar(isColorLight)
    }
}