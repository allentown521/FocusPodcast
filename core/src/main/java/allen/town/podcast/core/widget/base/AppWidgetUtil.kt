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
package allen.town.podcast.core.widget.base

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import allen.town.podcast.core.R

object AppWidgetUtil {


    /**
     * Check against [AppWidgetManager] if there are any instances of this widget.
     */
    private fun hasInstances(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val mAppWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context, javaClass
            )
        )
        return mAppWidgetIds.isNotEmpty()
    }


    fun getAlbumArtDrawable(context: Context, bitmap: Bitmap?): Drawable {
        return if (bitmap == null) {
            ContextCompat.getDrawable(context, R.drawable.default_audio_art)!!
        } else {
            BitmapDrawable(context.resources, bitmap)
        }
    }



        fun createRoundedBitmap(
            drawable: Drawable?,
            width: Int,
            height: Int,
            tl: Float,
            tr: Float,
            bl: Float,
            br: Float
        ): Bitmap? {
            if (drawable == null) {
                return null
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(c)

            val rounded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(rounded)
            val paint = Paint()
            paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            paint.isAntiAlias = true
            canvas.drawPath(
                composeRoundedRectPath(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()), tl, tr, bl, br
                ), paint
            )

            return rounded
        }

        fun createBitmap(drawable: Drawable, sizeMultiplier: Float): Bitmap {
            val bitmap = Bitmap.createBitmap(
                (drawable.intrinsicWidth * sizeMultiplier).toInt(),
                (drawable.intrinsicHeight * sizeMultiplier).toInt(),
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, c.width, c.height)
            drawable.draw(c)
            return bitmap
        }

         fun composeRoundedRectPath(
            rect: RectF,
            tl: Float,
            tr: Float,
            bl: Float,
            br: Float
        ): Path {
            val path = Path()
            path.moveTo(rect.left + tl, rect.top)
            path.lineTo(rect.right - tr, rect.top)
            path.quadTo(rect.right, rect.top, rect.right, rect.top + tr)
            path.lineTo(rect.right, rect.bottom - br)
            path.quadTo(rect.right, rect.bottom, rect.right - br, rect.bottom)
            path.lineTo(rect.left + bl, rect.bottom)
            path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - bl)
            path.lineTo(rect.left, rect.top + tl)
            path.quadTo(rect.left, rect.top, rect.left + tl, rect.top)
            path.close()

            return path
    }
}
