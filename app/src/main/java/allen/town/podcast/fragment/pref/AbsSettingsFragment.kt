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
package allen.town.podcast.fragment.pref

import allen.town.focus_common.activity.ToolbarBaseActivity
import allen.town.focus_common.common.prefs.supportv7.ATEPreferenceFragmentCompat
import allen.town.podcast.pref.LibraryPreference
import allen.town.podcast.pref.LibraryPreferenceDialog
import allen.town.podcast.pref.NowPlayingScreenPreference
import allen.town.podcast.pref.NowPlayingScreenPreferenceDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import dev.chrisbanes.insetter.applyInsetter

/**
 * @author Hemanth S (h4h13).
 */

abstract class AbsSettingsFragment : ATEPreferenceFragmentCompat() {


    protected fun setSummary(preference: Preference, value: Any?) {
        val stringValue = value.toString()
        if (preference is ListPreference) {
            val index = preference.findIndexOfValue(stringValue)
            preference.setSummary(if (index >= 0) preference.entries[index] else null)
        } else {
            preference.summary = stringValue
        }
    }

    abstract fun invalidateSettings()

    protected fun setSummary(preference: Preference?) {
        preference?.let {
            setSummary(
                it, PreferenceManager
                    .getDefaultSharedPreferences(it.context)
                    .getString(it.key, "")
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            listView.overScrollMode = View.OVER_SCROLL_NEVER
        }

        //https://github.com/zhanghai/AndroidFastScroll/issues/new,目前会同时出现暂时隐藏
//        ThemedFastScroller.create(listView)
        listView.isVerticalScrollBarEnabled = false
        listView.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }
        invalidateSettings()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is LibraryPreference -> {
                val fragment = LibraryPreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            is NowPlayingScreenPreference -> {
                val fragment = NowPlayingScreenPreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            is PodcastSearchEnginePreference -> {
                val fragment = PodcastSearchEnginePreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            else ->
                super.onDisplayPreferenceDialog(preference)
        }
    }

    fun restartActivity() {
        (activity as ToolbarBaseActivity).clearAllAppcompactActivities(true)
        //11以下会有黑屏，此方法可以避免
/*        private fun restart() {
            val savedInstanceState = Bundle().apply {
                onSaveInstanceState(this)
            }
            finish()
            val intent = Intent(this, this::class.java).putExtra(TAG, savedInstanceState)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }*/
    }
}
