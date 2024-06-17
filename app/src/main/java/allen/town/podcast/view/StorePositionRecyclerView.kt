package allen.town.podcast.view

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import allen.town.podcast.R
import allen.town.focus_common.util.Timber
import allen.town.podcast.view.StorePositionRecyclerView
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.AttributeSet

class StorePositionRecyclerView : RecyclerView {
    private var layoutManager: LinearLayoutManager? = null

    constructor(context: Context?) : super(context!!) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        setup()
    }

    private fun setup() {
        layoutManager = LinearLayoutManager(context)
        layoutManager!!.recycleChildrenOnDetach = true
        setLayoutManager(layoutManager)
        setHasFixedSize(true)
        //        addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
        clipToPadding = false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val horizontalSpacing =
            resources.getDimension(R.dimen.additional_horizontal_spacing).toInt()
        setPadding(horizontalSpacing, paddingTop, horizontalSpacing, paddingBottom)
    }

    fun saveScrollPosition(tag: String) {
        val firstItem = layoutManager!!.findFirstVisibleItemPosition()
        val firstItemView = layoutManager!!.findViewByPosition(firstItem)
        val topOffset: Float
        topOffset = firstItemView?.top?.toFloat() ?: 0f
        Timber.v("save for ${tag} $firstItem $topOffset ")
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
            .putInt(PREF_PREFIX_SCROLL_POSITION + tag, firstItem)
            .putInt(PREF_PREFIX_SCROLL_OFFSET + tag, topOffset.toInt())
            .apply()
    }

    fun restoreScrollPosition(tag: String) {
        val prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val position = prefs.getInt(PREF_PREFIX_SCROLL_POSITION + tag, 0)
        val offset = prefs.getInt(PREF_PREFIX_SCROLL_OFFSET + tag, 0)
        Timber.v("restore for ${tag} $position $offset ")
        if (position > 0 || offset > 0) {
            layoutManager!!.scrollToPositionWithOffset(position, offset)
        }
    }

    val isScrolledToBottom: Boolean
        get() {
            val visibleEpisodeCount = childCount
            val totalEpisodeCount = layoutManager!!.itemCount
            val firstVisibleEpisode = layoutManager!!.findFirstVisibleItemPosition()
            return totalEpisodeCount - visibleEpisodeCount <= firstVisibleEpisode + 3
        }

    companion object {
        private const val TAG = "StorePositionRecyclerView"
        private const val PREF_PREFIX_SCROLL_POSITION = "scroll_position_"
        private const val PREF_PREFIX_SCROLL_OFFSET = "scroll_offset_"
    }
}