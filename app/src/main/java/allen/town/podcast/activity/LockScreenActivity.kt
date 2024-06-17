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
package allen.town.podcast.activity

import allen.town.podcast.R
import allen.town.podcast.databinding.ActivityLockScreenBinding
import allen.town.podcast.fragment.AudioPlayerFragment
import allen.town.podcast.playback.LibraryViewModel
import allen.town.podcast.playback.getSelectedAudioPlayerFragment
import allen.town.podcast.playback.onPaletteColorChanged
import android.app.KeyguardManager
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
import code.name.monkey.appthemehelper.util.VersionUtils
import allen.town.focus_common.extensions.setEdgeToEdgeOrImmersive
import allen.town.focus_common.extensions.setStatusBarColor
import allen.town.focus_common.extensions.setTaskDescriptionColorAuto
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrListener
import com.r0adkll.slidr.model.SlidrPosition

class LockScreenActivity : SimpleToolbarActivity() {
    private lateinit var binding: ActivityLockScreenBinding
    private var libraryViewModel: LibraryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreenInit()
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTaskDescriptionColorAuto()
        setEdgeToEdgeOrImmersive(R.id.status_bar, true)
        setStatusBarColor(Color.TRANSPARENT, R.id.status_bar)

        libraryViewModel = ViewModelProvider(this).get(
            LibraryViewModel::class.java
        )

        updateColor()

        val config = SlidrConfig.Builder().listener(object : SlidrListener {
            override fun onSlideStateChanged(state: Int) {
            }

            override fun onSlideChange(percent: Float) {
            }

            override fun onSlideOpened() {
            }

            override fun onSlideClosed(): Boolean {
                if (VersionUtils.hasOreo()) {
                    val keyguardManager =
                        getSystemService<KeyguardManager>()
                    keyguardManager?.requestDismissKeyguard(this@LockScreenActivity, null)
                }
                finish()
                return true
            }
        }).position(SlidrPosition.BOTTOM).build()

        Slidr.attach(this, config)

        if (supportFragmentManager.findFragmentByTag(AudioPlayerFragment.TAG) == null) {
            val transaction = getSupportFragmentManager().beginTransaction()
            transaction.replace(
                R.id.audioplayerFragment,
                getSelectedAudioPlayerFragment(),
                AudioPlayerFragment.TAG
            )
            transaction.commit()
        }

    }

    private var paletteColor = Color.WHITE
    private fun updateColor() {
        libraryViewModel!!.paletteColor.observe(this) { color: Int ->
            paletteColor = color
            onPaletteColorChanged(paletteColor)
        }
    }

    @Suppress("Deprecation")
    private fun lockScreenInit() {
        if (VersionUtils.hasOreoMR1()) {
            setShowWhenLocked(true)
            val keyguardManager = getSystemService<KeyguardManager>()
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            this.window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }
    }

}
