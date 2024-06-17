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
package allen.town.podcast.adapter

import allen.town.focus_common.databinding.PreferenceDialogLibraryCategoriesListitemBinding
import allen.town.focus_common.model.CategoryInfo
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.SwipeAndDragHelper
import allen.town.focus_common.util.TopSnackbarUtil
import allen.town.podcast.R
import allen.town.podcast.fragment.*
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class CategoryInfoAdapter : RecyclerView.Adapter<CategoryInfoAdapter.ViewHolder>(),
    SwipeAndDragHelper.ActionCompletionContract {
    var categoryInfos: MutableList<CategoryInfo> =
        BasePreferenceUtil.libraryCategory.toMutableList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private val touchHelper: ItemTouchHelper
    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        touchHelper.attachToRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int {
        return categoryInfos.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryInfo = categoryInfos[position]
        holder.binding.checkbox.isChecked = categoryInfo.visible
        holder.binding.title.setText(
            when (categoryInfo.tag) {
                SubFeedsFragment.TAG -> {
                    R.string.subscriptions_label
                }
                EpisodesFragment.TAG -> {
                    R.string.episodes_label
                }
                PlaylistFragment.TAG -> {
                    R.string.playlist_label
                }
                FavoriteEpisodesFragment.TAG -> {
                    R.string.favorite_episodes_label
                }
                DownloadPagerFragment.TAG -> {
                    R.string.downloads_label
                }
                PlaybackHistoryFragment.TAG -> {
                    R.string.playback_history_label
                }
                DiscoverFragment.TAG -> {
                    R.string.discover
                }
                NavigationListAdapter.SUBSCRIPTION_LIST_TAG -> {
                    R.string.subscriptions_list_label
                }
                else -> {
                    R.string.episodes_label
                }
            }
        )
        holder.itemView.setOnClickListener {
            if (!(categoryInfo.visible && isLastCheckedCategory(categoryInfo))) {
                categoryInfo.visible = !categoryInfo.visible
                holder.binding.checkbox.isChecked = categoryInfo.visible
            } else {
                TopSnackbarUtil.showSnack(
                    holder.itemView.context,
                    allen.town.focus_common.R.string.you_have_to_select_at_least_one_category,
                    Toast.LENGTH_SHORT
                )
            }
        }
        holder.binding.dragView.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder)
            }
            false
        }
        //不能拖动排序就隐藏拖动图标
        if (!categoryInfo.dragAble) {
            holder.binding.dragView.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder {
        return ViewHolder(
            PreferenceDialogLibraryCategoriesListitemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onViewMoved(oldPosition: Int, newPosition: Int) {
        //如果目标是最后一行并且最后一行不能拖拽，那么最后一行不能排序
        if (newPosition == itemCount - 1 && !categoryInfos[newPosition].dragAble) {
            return
        }
        val categoryInfo = categoryInfos[oldPosition]
        categoryInfos.removeAt(oldPosition)
        categoryInfos.add(newPosition, categoryInfo)
        notifyItemMoved(oldPosition, newPosition)
    }

    private fun isLastCheckedCategory(categoryInfo: CategoryInfo): Boolean {
        if (categoryInfo.visible) {
            for (c in categoryInfos) {
                if (c !== categoryInfo && c.visible) {
                    return false
                }
            }
        }
        return true
    }

    class ViewHolder(val binding: PreferenceDialogLibraryCategoriesListitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
        }
    }

    init {
        val swipeAndDragHelper = SwipeAndDragHelper(this)
        touchHelper = ItemTouchHelper(swipeAndDragHelper)
    }
}