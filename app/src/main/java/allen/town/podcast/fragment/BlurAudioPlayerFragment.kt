package allen.town.podcast.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import allen.town.focus_common.extensions.colorControlNormal
import allen.town.focus_common.extensions.isColorLight
import com.bumptech.glide.Glide
import allen.town.podcast.R
import allen.town.podcast.core.glide.GlideApp
import allen.town.podcast.core.glide.GlideRequest
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.event.CoverColorChangeEvent
import allen.town.podcast.glide.BlurTransformation
import allen.town.podcast.glide.crossfadeListener

/**
 * 有个bug，如果有章节，点击了菜单的更多，然后切换颜色不会变化，再点击更多恢复正常
 */
open class BlurAudioPlayerFragment : AudioPlayerFragment(false) {
    override fun coverColorUpdate(event: CoverColorChangeEvent) {
        super.coverColorUpdate(event)
        lastPlaybackControlsColor = if(event.color.backgroundColor.isColorLight) colorControlNormal() else  Color.WHITE
        lastDisabledPlaybackControlsColor = if(event.color.backgroundColor.isColorLight) colorControlNormal()
        else ContextCompat.getColor(requireContext(), R.color.md_grey_200)
        colorGradientBackground.visibility = GONE
        colorBackground.visibility = VISIBLE
        updateBlur(event.bitmap)
        ToolbarContentTintHelper.colorizeToolbar(toolbar, lastPlaybackControlsColor, activity)

        butFF.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        butRev.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        butSkip.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        txtvRev.setTextColor(lastPlaybackControlsColor)
        txtvFF.setTextColor(lastPlaybackControlsColor)
        txtvPlaybackSpeed.setTextColor(lastPlaybackControlsColor)
        txtvPosition.setTextColor(lastPlaybackControlsColor)
        txtvLength.setTextColor(lastPlaybackControlsColor)

        event.coverViewHolder?.chapterTv?.setTextColor(lastDisabledPlaybackControlsColor)
        event.coverViewHolder?.episodeTv?.setTextColor(lastPlaybackControlsColor)
        event.coverViewHolder?.podcastTv?.setTextColor(lastDisabledPlaybackControlsColor)
        event.coverViewHolder?.preChapterIv?.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        event.coverViewHolder?.nextChapterIv?.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)

        if (isFromScreenActivity) {
            swipeUpIv.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
            swipeUpTipTv.setTextColor(lastDisabledPlaybackControlsColor)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        ToolbarContentTintHelper.colorizeToolbar(toolbar, Color.WHITE, activity)
        initBlur()
    }
    private var lastRequest: GlideRequest<Drawable>? = null
    private fun initBlur() {
        GlideApp.with(this).load(ColorDrawable(Color.DKGRAY)).into(colorBackground)

    }



    private fun updateBlur(bitmap: Bitmap?) {
        // https://github.com/bumptech/glide/issues/527#issuecomment-148840717
        GlideApp.with(this)
            .load(bitmap)
            .transform(
                BlurTransformation.Builder(requireContext()).blurRadius(Prefs.blurAmount.toFloat())
                    .build()
            ).thumbnail(lastRequest)
            .error(Glide.with(this).load(ColorDrawable(Color.DKGRAY)).fitCenter())
            .also {
                lastRequest = it.clone()
                it.crossfadeListener()
                    .into(colorBackground)
            }
    }
}