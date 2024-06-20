package allen.town.podcast.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import allen.town.focus_common.extensions.applyColor
import allen.town.focus_common.extensions.colorControlNormal
import allen.town.focus_common.extensions.isColorLight
import allen.town.podcast.event.CoverColorChangeEvent

/**
 * 有个bug，如果有章节，点击了菜单的更多，然后切换颜色不会变化，再点击更多恢复正常
 */
class FullAudioPlayerFragment : AudioPlayerFragment(false) {
    override fun coverColorUpdate(event: CoverColorChangeEvent) {
        super.coverColorUpdate(event)
        val thirdColor = if(event.color.backgroundColor.isColorLight) colorControlNormal() else  Color.WHITE
        ToolbarContentTintHelper.colorizeToolbar(toolbar, thirdColor, activity)

        TintHelper.setTintAuto(playPauseButton, event.color.primaryTextColor, true)
        TintHelper.setTintAuto(butPlay, event.color.backgroundColor, false)
        sbPosition.applyColor(event.color.primaryTextColor)

        lastPlaybackControlsColor = event.color.primaryTextColor
        lastDisabledPlaybackControlsColor = ColorUtil.withAlpha(event.color.primaryTextColor, 0.5f)

        butFF.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        butRev.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        butSkip.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        txtvRev.setTextColor(lastPlaybackControlsColor)
        txtvFF.setTextColor(lastPlaybackControlsColor)
        txtvPlaybackSpeed.setTextColor(lastPlaybackControlsColor)
        txtvPosition.setTextColor(lastDisabledPlaybackControlsColor)
        txtvLength.setTextColor(lastDisabledPlaybackControlsColor)

        event.coverViewHolder?.chapterTv?.setTextColor(thirdColor)
        event.coverViewHolder?.episodeTv?.setTextColor(thirdColor)
        event.coverViewHolder?.podcastTv?.setTextColor(thirdColor)
        event.coverViewHolder?.preChapterIv?.setColorFilter(thirdColor, PorterDuff.Mode.SRC_IN)
        event.coverViewHolder?.nextChapterIv?.setColorFilter(thirdColor, PorterDuff.Mode.SRC_IN)

        mask.backgroundTintList = ColorStateList.valueOf(event.color.backgroundColor)

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