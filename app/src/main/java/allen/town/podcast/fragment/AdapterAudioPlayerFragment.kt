package allen.town.podcast.fragment

import android.os.Bundle
import android.view.View
import allen.town.podcast.event.CoverColorChangeEvent

/**
 * 有个bug，如果有章节，点击了菜单的更多，然后切换颜色不会变化，再点击更多恢复正常
 */
class AdapterAudioPlayerFragment : AudioPlayerFragment(false) {
    override fun coverColorUpdate(event: CoverColorChangeEvent) {
        super.coverColorUpdate(event)
//        colorGradientBackground.visibility = GONE
//        colorBackground.visibility = VISIBLE
//        colorBackground.setBackgroundColor(event.color.backgroundColor)
//        ToolbarContentTintHelper.colorizeToolbar(toolbar, event.color.secondaryTextColor, activity)


//        val colorFinal = if (UserPreferences.isAdapterColor()) {
//            event.color.primaryTextColor
//        } else {
//            ThemeStore.accentColor(requireContext())
//        }.ripAlpha()
        //加了这行有个问题必现，播放过程中去设置“自定义颜色”然后回来，播放按钮动画没有完全完成
//        TintHelper.setTintAuto(
//            butPlay,
//            getPrimaryTextColor(
//                requireContext(),
//                isColorLight(colorFinal)
//            ),
//            false
//        )
//        TintHelper.setTintAuto(playPauseButton, colorFinal, true)
//        sbPosition.applyColor(colorFinal)

//        butFF.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
//        butRev.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
//        butSkip.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
//        txtvRev.setTextColor(accentColor)
//        txtvFF.setTextColor(accentColor)
//        txtvPlaybackSpeed.setTextColor(accentColor)
//        txtvPosition.setTextColor(lastPlaybackControlsColor)
//        txtvLength.setTextColor(lastPlaybackControlsColor)

//        butRev.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
//        butFF.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        ToolbarContentTintHelper.colorizeToolbar(toolbar, Color.WHITE, activity)
    }



}