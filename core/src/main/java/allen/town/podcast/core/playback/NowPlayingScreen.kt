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
package allen.town.podcast.core.playback

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import allen.town.podcast.core.R

enum class NowPlayingScreen constructor(
    @param:StringRes @field:StringRes
    val titleRes: Int,
    @param:DrawableRes @field:DrawableRes val drawableResId: Int,
    val id: Int,
    val defaultCoverTheme: AlbumCoverStyle?,
    val isCharge: Boolean = true
) {
    // Some Now playing themes look better with particular Album cover theme
    Full(R.string.full, R.drawable.np_full, 2, AlbumCoverStyle.Full),
    BlurCard(R.string.blur_card, R.drawable.np_blur_card, 9, AlbumCoverStyle.BlurCard),
    Adaptive(R.string.adaptive, R.drawable.np_adaptive, 10, AlbumCoverStyle.FullCard, false),
    Vinyl(R.string.vinyl, R.drawable.np_vinyl, 16, AlbumCoverStyle.Vinyl),
    Circle(R.string.circular, R.drawable.np_circle, 15, AlbumCoverStyle.Circle, false),
    Color(R.string.color, R.drawable.np_color, 5, AlbumCoverStyle.Normal),
    Blur(R.string.blur, R.drawable.np_blur, 4, AlbumCoverStyle.Normal, false),
    Normal(R.string.normal, R.drawable.np_normal, 0, AlbumCoverStyle.Normal, false),
}
