package allen.town.podcast.glide

import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import allen.town.podcast.core.glide.GlideRequest

fun GlideRequest<Drawable>.crossfadeListener(): GlideRequest<Drawable> {
    return listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            return if (isFirstResource) {
                false // thumbnail was not shown, do as usual
            } else DrawableCrossFadeFactory.Builder()
                .setCrossFadeEnabled(true).build()
                .build(dataSource, isFirstResource)
                .transition(resource, target as Transition.ViewAdapter)
        }
    })
}