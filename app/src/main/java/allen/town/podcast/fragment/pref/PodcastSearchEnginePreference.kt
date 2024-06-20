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

package allen.town.podcast.fragment.pref

import allen.town.core.service.PayService
import allen.town.focus_common.R
import allen.town.focus_common.common.prefs.supportv7.ATEDialogPreference
import allen.town.focus_common.databinding.PreferenceDialogLibraryCategoriesBinding
import allen.town.focus_common.model.CategoryInfo
import allen.town.podcast.adapter.PodcastSearchChooseAdapter
import allen.town.podcast.core.pref.Prefs
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.extensions.materialDialog
import com.wyjson.router.GoRouter


class PodcastSearchEnginePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ATEDialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            ThemeStore.accentColor(context),
            SRC_IN
        )
    }
}

class PodcastSearchEnginePreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = PreferenceDialogLibraryCategoriesBinding.inflate(layoutInflater)

        val categoryAdapter = PodcastSearchChooseAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            categoryAdapter.attachToRecyclerView(this)
        }

        return materialDialog(R.string.library_categories)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.done) { _, _ -> updateCategories(categoryAdapter.categoryInfos) }
            .setView(binding.root)
            .create()
    }

    private fun updateCategories(categories: List<CategoryInfo>) {
        if(!GoRouter.getInstance().getService(PayService::class.java)!!.isPurchase(requireContext())){
            return
        }
        if (getSelected(categories) == 0) return
        Prefs.podcastSearchEngineList = categories
    }

    private fun getSelected(categories: List<CategoryInfo>): Int {
        var selected = 0
        for (categoryInfo in categories) {
            if(categoryInfo.visible)
                selected++
        }
        return selected
    }

    companion object {
        fun newInstance(): PodcastSearchEnginePreferenceDialog {
            return PodcastSearchEnginePreferenceDialog()
        }
    }
}