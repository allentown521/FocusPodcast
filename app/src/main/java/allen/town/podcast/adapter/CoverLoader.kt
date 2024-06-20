package allen.town.podcast.adapter

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.glide.PaletteBitmap
import allen.town.podcast.core.pref.Prefs
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import java.lang.ref.WeakReference

class CoverLoader(private val activity: MainActivity?) {
    private var resource = 0
    private var uri: String? = null
    private var fallbackUri: String? = null
    private var txtvPlaceholder: TextView? = null
    private var imgvCover: ImageView? = null
    private var textAndImageCombined = false
    fun withUri(uri: String?): CoverLoader {
        this.uri = uri
        return this
    }

    fun withResource(resource: Int): CoverLoader {
        this.resource = resource
        return this
    }

    fun withFallbackUri(uri: String?): CoverLoader {
        fallbackUri = uri
        return this
    }

    fun withCoverView(coverView: ImageView?): CoverLoader {
        imgvCover = coverView
        return this
    }

    fun withPlaceholderView(placeholderView: TextView?): CoverLoader {
        txtvPlaceholder = placeholderView
        return this
    }

    /**
     * Set cover text and if it should be shown even if there is a cover image.
     *
     * @param placeholderView      Cover text.
     * @param textAndImageCombined Show cover text even if there is a cover image?
     */
    fun withPlaceholderView(placeholderView: TextView, textAndImageCombined: Boolean): CoverLoader {
        txtvPlaceholder = placeholderView
        this.textAndImageCombined = textAndImageCombined
        return this
    }

    fun load() {
        val coverTarget = CoverTarget(txtvPlaceholder, imgvCover, textAndImageCombined)
        if (resource != 0) {
            Glide.with(imgvCover!!).clear(coverTarget)
            imgvCover!!.setImageResource(resource)
            CoverTarget.setPlaceholderVisibility(txtvPlaceholder, textAndImageCombined, null)
            return
        }
        val options = RequestOptions()
            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY) //这里修改为centCrop那么底部标题的背景就固定是灰色的了，原因未知
            .fitCenter()
            .dontAnimate()
        var builder = Glide.with(imgvCover!!)
            .`as`(PaletteBitmap::class.java)
            .placeholder(R.color.light_gray)
            .load(uri)
            .apply(options)
        if (fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
            builder = builder.error(
                Glide.with(imgvCover!!)
                    .`as`(PaletteBitmap::class.java)
                    .load(fallbackUri)
                    .apply(options)
            )
        }
        builder.into(coverTarget)
    }

    internal class CoverTarget(
        txtvPlaceholder: TextView?,
        imgvCover: ImageView?,
        textAndImageCombined: Boolean
    ) : CustomViewTarget<ImageView?, PaletteBitmap?>(
        imgvCover!!
    ) {
        private val placeholder: WeakReference<TextView?>
        private val cover: WeakReference<ImageView?>
        private val textAndImageCombined: Boolean
        override fun onLoadFailed(errorDrawable: Drawable?) {
            setPlaceholderVisibility(placeholder.get(), true, null)
        }

        override fun onResourceReady(
            resource: PaletteBitmap,
            transition: Transition<in PaletteBitmap?>?
        ) {
            val ivCover = cover.get()
            ivCover!!.setImageBitmap(resource.bitmap)
            setPlaceholderVisibility(placeholder.get(), textAndImageCombined, resource.palette)
        }

        override fun onResourceCleared(placeholder: Drawable?) {
            val ivCover = cover.get()
            ivCover!!.setImageDrawable(placeholder)
            setPlaceholderVisibility(this.placeholder.get(), textAndImageCombined, null)
        }

        companion object {
            fun setPlaceholderVisibility(
                placeholder: TextView?,
                textAndImageCombined: Boolean,
                palette: Palette?
            ) {
                val showTitle = Prefs.shouldShowSubscriptionTitle()
                if (placeholder != null) {
                    if (textAndImageCombined || showTitle) {
                        placeholder.visibility = View.VISIBLE
                        val bgColor = placeholder.context.resources.getColor(R.color.feed_text_bg)
                        if (palette == null || !showTitle) {
//                        placeholder.setBackgroundColor(bgColor);
//                        placeholder.setTextColor(ThemeUtils.getColorFromAttr(placeholder.getContext(),
//                                android.R.attr.textColorPrimary));
                            return
                        }
                        val dominantColor = palette.getDominantColor(bgColor)
                        var textColor = placeholder.context.resources.getColor(R.color.white)
                        if (ColorUtils.calculateLuminance(dominantColor) > 0.5) {
                            textColor = placeholder.context.resources.getColor(R.color.black)
                        }
                        //                    placeholder.setTextColor(textColor);
//                    placeholder.setBackgroundColor(dominantColor);
                    } else {
                        placeholder.visibility = View.INVISIBLE
                    }
                }
            }
        }

        init {
            if (txtvPlaceholder != null) {
                txtvPlaceholder.visibility = View.VISIBLE
            }
            placeholder = WeakReference(txtvPlaceholder)
            cover = WeakReference(imgvCover)
            this.textAndImageCombined = textAndImageCombined
        }
    }
}