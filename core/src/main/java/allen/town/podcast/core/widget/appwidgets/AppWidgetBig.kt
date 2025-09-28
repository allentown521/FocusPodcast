/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package allen.town.podcast.core.widget.appwidgets

import allen.town.focus_common.util.RetroUtil
import allen.town.podcast.core.R
import allen.town.podcast.core.feed.util.ImageResourceUtils
import allen.town.podcast.core.widget.WidgetUpdater
import allen.town.podcast.core.widget.base.BaseAppWidget
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers

class AppWidgetBig : BaseAppWidget() {
    private var target: Target<Bitmap>? = null // for cancellation

    private val TAG = "AppWidgetBig"

    /**
     * Link up various button actions using [PendingIntent].
     */

    companion object {

        const val NAME = "AppWidgetBig"

        private var mInstance: AppWidgetBig? = null
        private var imageSize = 0
        private var cardRadius = 0f

        @JvmStatic
        val instance: AppWidgetBig
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetBig()
                }
                return mInstance!!
            }
    }

    override fun getLayout(): Int {
        return R.layout.app_widget_big
    }

    override fun getPauseBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_pause,
                MaterialValueHelper.getPrimaryTextColor(context, false)
            ), 1f
        )
    }

    override fun getPlayBitmap(context: Context): Bitmap {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_play,
                MaterialValueHelper.getPrimaryTextColor(context, false)
            ), 1f
        )
    }

    override fun getNextBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_skip,
                MaterialValueHelper.getPrimaryTextColor(context, false)
            ), 1f
        )
    }

    override fun getForwardBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_fast_forward,
                MaterialValueHelper.getPrimaryTextColor(context, false)
            ), 1f
        )
    }

    override fun getRewindBitmap(context: Context): Bitmap? {
        return createBitmap(
            RetroUtil.getTintedVectorDrawable(
                context,
                R.drawable.ic_widget_fast_rewind,
                MaterialValueHelper.getPrimaryTextColor(context, false)
            ), 1f
        )
    }

    override fun processRemoteViewIfNeeded(
        context: Context,
        remoteViews: RemoteViews,
        widgetState: WidgetUpdater.WidgetState,
        appWidgetIds: IntArray,
        isCreated: Boolean,
    ) {
        if (widgetState.media != null) {
            remoteViews.setImageViewResource(R.id.imgvCover, R.drawable.default_audio_art)

            if (imageSize == 0) {
                val p = RetroUtil.getScreenSize(context)
                imageSize = p.x.coerceAtMost(p.y)
            }
            // Load the album cover async and push the update on completion
            val appContext = context.applicationContext


            Completable.fromAction {
            }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (target != null) {
                        Glide.with(appContext).clear(target)
                    }

                    if (widgetState.media == null) {
                        Log.w(TAG, "widgetState.media is null")
                    }

                    if (widgetState.media == null) {
                        Log.w(TAG, "widgetState.media is null")
                        return@subscribe
                    }

                    //这里结合centerCrop就没有blur效果了
                    target = Glide.with(appContext).asBitmap()
                        .apply(RequestOptions.centerCropTransform())
                        .load(
                            if (widgetState.media.imageLocation.isNullOrEmpty()) ImageResourceUtils.getFallbackImageLocation(
                                widgetState.media
                            ) else widgetState.media.imageLocation
                        )
                        .into(object : SimpleTarget<Bitmap>(imageSize, imageSize) {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                update(resource)
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                super.onLoadFailed(errorDrawable)
                                Log.w(TAG, "onLoadFailed")
                                update(null)
                            }

                            private fun update(bitmap: Bitmap?) {
                                // Set correct drawable for pause state
                                if (bitmap == null) {
                                    remoteViews.setImageViewResource(
                                        R.id.imgvCover,
                                        R.drawable.default_audio_art
                                    )
                                } else {
                                    remoteViews.setImageViewBitmap(R.id.imgvCover, bitmap)
                                }


                                pushUpdate(context, appWidgetIds, remoteViews)
                            }
                        })
                }) { error: Throwable ->
                    Log.e(TAG, error.stackTraceToString())
                }
        } else {
            pushUpdate(context, appWidgetIds, remoteViews)
        }
    }
}
