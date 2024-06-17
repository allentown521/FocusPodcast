package allen.town.podcast.fragment

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import allen.town.focus_common.extensions.applyColor
import allen.town.podcast.event.CoverColorChangeEvent

/**
 * 有个bug，如果有章节，点击了菜单的更多，然后切换颜色不会变化，再点击更多恢复正常
 */
class ColorAudioPlayerFragment : AudioPlayerFragment(false) {
    override fun coverColorUpdate(event: CoverColorChangeEvent) {
        super.coverColorUpdate(event)
        colorGradientBackground.visibility = GONE
        colorBackground.visibility = VISIBLE
        colorBackground.setBackgroundColor(event.color.backgroundColor)
        ToolbarContentTintHelper.colorizeToolbar(toolbar, event.color.secondaryTextColor, activity)

        TintHelper.setTintAuto(playPauseButton, event.color.primaryTextColor, true)
        TintHelper.setTintAuto(butPlay, event.color.backgroundColor, false)
        sbPosition.applyColor(event.color.primaryTextColor)

        lastPlaybackControlsColor = event.color.secondaryTextColor

        butFF.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        butRev.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        butSkip.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        txtvRev.setTextColor(lastPlaybackControlsColor)
        txtvFF.setTextColor(lastPlaybackControlsColor)
        txtvPlaybackSpeed.setTextColor(lastPlaybackControlsColor)
        txtvPosition.setTextColor(lastPlaybackControlsColor)
        txtvLength.setTextColor(lastPlaybackControlsColor)

        event.coverViewHolder?.chapterTv?.setTextColor(lastPlaybackControlsColor)
        event.coverViewHolder?.episodeTv?.setTextColor(lastPlaybackControlsColor)
        event.coverViewHolder?.podcastTv?.setTextColor(lastPlaybackControlsColor)
        event.coverViewHolder?.preChapterIv?.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        event.coverViewHolder?.nextChapterIv?.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        if (isFromScreenActivity) {
            swipeUpIv.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
            swipeUpTipTv.setTextColor(lastPlaybackControlsColor)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        ToolbarContentTintHelper.colorizeToolbar(toolbar, Color.WHITE, activity)
    }




}