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
package code.name.monkey.retromusic.fragments.settings

import code.name.monkey.appthemehelper.constants.ThemeConstants.ACCENT_COLOR
import code.name.monkey.appthemehelper.constants.ThemeConstants.BLACK_THEME
import code.name.monkey.appthemehelper.constants.ThemeConstants.CUSTOM_LAUNCHER
import code.name.monkey.appthemehelper.constants.ThemeConstants.DESATURATED_COLOR
import code.name.monkey.appthemehelper.constants.ThemeConstants.GENERAL_THEME
import code.name.monkey.appthemehelper.constants.ThemeConstants.MATERIAL_YOU
import code.name.monkey.appthemehelper.constants.ThemeConstants.SHOULD_COLOR_APP_SHORTCUTS
import code.name.monkey.appthemehelper.constants.ThemeConstants.WALLPAPER_ACCENT
import allen.town.focus_common.theme.CustomLauncherIconMakerDialog
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.PackageUtils
import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import code.name.monkey.appthemehelper.ACCENT_COLORS
import code.name.monkey.appthemehelper.ACCENT_COLORS_SUB
import code.name.monkey.appthemehelper.ThemeStore
import allen.town.focus_common.common.prefs.supportv7.ATEColorPreference
import allen.town.focus_common.common.prefs.supportv7.ATESwitchPreference
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.appshortcuts.DynamicShortcutManager
import code.name.monkey.retromusic.extensions.materialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.google.android.material.color.DynamicColors
import allen.town.podcast.MyApp
import allen.town.podcast.R
import allen.town.podcast.activity.SettingsActivity
import allen.town.podcast.appshortcuts.ShortcutsDefaultList
import allen.town.podcast.fragment.pref.AbsSettingsFragment

/**
 * @author Hemanth S (h4h13).
 */

class ThemeSettingsFragment : AbsSettingsFragment() {
    @SuppressLint("CheckResult")
    override fun invalidateSettings() {
        val generalTheme: Preference? = findPreference(GENERAL_THEME)
        generalTheme?.let {
            setSummary(it)
            it.setOnPreferenceChangeListener { _, newValue ->
                val theme = newValue as String
                setSummary(it, newValue)
                ThemeStore.markChanged(requireContext())

                if (VersionUtils.hasNougatMR()) {
                    DynamicShortcutManager(
                        requireContext(),
                        ShortcutsDefaultList(requireContext()).defaultShortcuts
                    ).updateDynamicShortcuts()
                }
                restartActivity()
                true
            }
        }

        val accentColorPref: ATEColorPreference? = findPreference(ACCENT_COLOR)
        val accentColor = ThemeStore.accentColor(requireContext())
        accentColorPref?.setColor(accentColor, ColorUtil.darkenColor(accentColor))
        accentColorPref?.setOnPreferenceClickListener {
            materialDialog().show {
                colorChooser(
                    initialSelection = accentColor,
                    showAlphaSelector = false,
                    colors = ACCENT_COLORS,
                    subColors = ACCENT_COLORS_SUB, allowCustomArgb = true
                ) { _, color ->
                    if(!MyApp.instance.checkSupporter(requireContext())){
                        return@colorChooser
                    }
                    ThemeStore.editTheme(requireContext()).accentColor(color).commit()
                    if (VersionUtils.hasNougatMR())
                        DynamicShortcutManager(
                            requireContext(),
                            ShortcutsDefaultList(requireContext()).defaultShortcuts
                        ).updateDynamicShortcuts()
                    restartActivity()
                }
            }
            return@setOnPreferenceClickListener true
        }
        val blackTheme: ATESwitchPreference? = findPreference(BLACK_THEME)
        blackTheme?.setOnPreferenceChangeListener { _, value ->
            ThemeStore.markChanged(requireContext())
            if (VersionUtils.hasNougatMR()) {
                DynamicShortcutManager(
                    requireContext(),
                    ShortcutsDefaultList(requireContext()).defaultShortcuts
                ).updateDynamicShortcuts()
            }
            restartActivity()
            true
        }

        val desaturatedColor: ATESwitchPreference? = findPreference(DESATURATED_COLOR)
        desaturatedColor?.setOnPreferenceChangeListener { _, value ->
            val desaturated = value as Boolean
            ThemeStore.prefs(requireContext())
                .edit()
                .putBoolean(DESATURATED_COLOR, desaturated)
                .apply()
            BasePreferenceUtil.isDesaturatedColor = desaturated
            restartActivity()
            true
        }

        val colorAppShortcuts: TwoStatePreference? = findPreference(SHOULD_COLOR_APP_SHORTCUTS)
        if (!VersionUtils.hasNougatMR()) {
            colorAppShortcuts?.isVisible = false
        } else {
            colorAppShortcuts?.isChecked = BasePreferenceUtil.isColoredAppShortcuts
            colorAppShortcuts?.setOnPreferenceChangeListener { _, newValue ->
                BasePreferenceUtil.isColoredAppShortcuts = newValue as Boolean
                DynamicShortcutManager(
                    requireContext(),
                    ShortcutsDefaultList(requireContext()).defaultShortcuts
                ).updateDynamicShortcuts()
                true
            }
        }

        val materialYou: ATESwitchPreference? = findPreference(MATERIAL_YOU)
        materialYou?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                DynamicColors.applyToActivitiesIfAvailable(MyApp.instance as Application)
            }
            restartActivity()
            true
        }
        val wallpaperAccent: ATESwitchPreference? = findPreference(WALLPAPER_ACCENT)
        wallpaperAccent?.setOnPreferenceChangeListener { _, _ ->
            restartActivity()
            true
        }

        val customLauncher: Preference? = findPreference(CUSTOM_LAUNCHER)
        if (VersionUtils.hasOreo()) {
            customLauncher?.onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        CustomLauncherIconMakerDialog(
                            R.string.pref_custom_launcher_title,
                            PackageUtils.getPackageName(context), "allen.town.podcast.activity.SplashActivity"
                            ,R.color.white,R.drawable.ic_launcher_foreground, R.color.ic_launcher_background,
                            PackageUtils.getAppName(context)
                        ).show(
                            activity!!.supportFragmentManager,
                            null
                        )
                        return true
                    }

                }
        } else {
            customLauncher?.isVisible = false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_theme)
    }

    override fun onStart() {
        super.onStart()
        (activity as SettingsActivity).setTitle(R.string.pref_set_theme_title)
    }
}
