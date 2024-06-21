/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package allen.town.podcast.util

import allen.town.focus_common.util.TopSnackbarUtil
import allen.town.focus_purchase.iap.util.GooglePayUtil.ALIPAY_REMOVE_AD
import allen.town.podcast.activity.PurchaseActivity
import allen.town.podcast.core.R
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast

object NavigationUtil {

    @JvmStatic
    fun openEqualizer(activity: Activity) {
        stockEqualizer(activity)
    }

    private fun stockEqualizer(activity: Activity) {
        try {
            val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            activity.startActivityForResult(effects, 0)
        } catch (notFound: ActivityNotFoundException) {
            TopSnackbarUtil.showSnack(activity, R.string.no_equalizer, Toast.LENGTH_LONG)
        }
    }

    @JvmStatic
    fun goToProVersion(context: Context, isAlipayRemoveAd: Boolean = false) {
        val intent = Intent(context, PurchaseActivity::class.java)
        if (isAlipayRemoveAd) {
            intent.putExtra(ALIPAY_REMOVE_AD, true)
        }
        context.startActivity(
            intent
        )
        (context as? Activity)?.overridePendingTransition(
            R.anim.retro_fragment_open_enter,
            R.anim.anim_activity_stay
        )
    }
}