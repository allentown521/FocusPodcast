package allen.town.podcast.config

import allen.town.focus_common.model.CategoryInfo
import allen.town.podcast.R
import allen.town.podcast.adapter.NavigationListAdapter
import allen.town.podcast.fragment.*

object CategoriesDefaultList {
    val defaultList: List<CategoryInfo>
        get() = listOf(
            CategoryInfo(SubFeedsFragment.TAG,true,true),
            CategoryInfo(EpisodesFragment.TAG,true,true),
            CategoryInfo(PlaylistFragment.TAG,true,true),
            CategoryInfo(FavoriteEpisodesFragment.TAG,true,true),
            CategoryInfo(DownloadPagerFragment.TAG,true,true),
            CategoryInfo(PlaybackHistoryFragment.TAG,false,true),
            CategoryInfo(DiscoverFragment.TAG,true,true),
            CategoryInfo(NavigationListAdapter.SUBSCRIPTION_LIST_TAG,true,false)
        )
}