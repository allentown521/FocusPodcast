package allen.town.podcast.adapter

import allen.town.focus_common.util.BasePreferenceUtil.libraryCategory
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.activity.SettingsActivity
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.storage.NavDrawerData.*
import allen.town.podcast.fragment.*
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.util.TypedValue
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.appthemehelper.ThemeStore.Companion.textColorPrimary
import code.name.monkey.appthemehelper.constants.ThemeConstants
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.joanzapata.iconify.Iconify
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.*

/**
 * BaseAdapter for the navigation drawer
 */
class NavigationListAdapter(private val itemAccess: ItemAccess, context: Activity) :
    RecyclerView.Adapter<NavigationListAdapter.Holder>(), OnSharedPreferenceChangeListener {
    val tags: MutableList<String> = ArrayList()
    private val activity: WeakReference<Activity>
    var showSubscriptionList = true
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (ThemeConstants.LIBRARY_CATEGORIES == key) {
            loadItems()
        }
    }

    private fun loadItems() {
        val categoryInfoList = libraryCategory
        val newTags: MutableList<String> = ArrayList()
        for ((tag, visible) in categoryInfoList) {
            if (visible) {
                newTags.add(tag)
            }
        }
        if (newTags.contains(SUBSCRIPTION_LIST_TAG)) {
            // we never want SUBSCRIPTION_LIST_TAG to be in 'tags'
            // since it doesn't actually correspond to a position in the list, but is
            // a placeholder that indicates if we should show the subscription list in the
            // nav drawer at all.
            showSubscriptionList = true
            newTags.remove(SUBSCRIPTION_LIST_TAG)
        } else {
            showSubscriptionList = false
        }
        tags.clear()
        tags.addAll(newTags)
        notifyDataSetChanged()
    }

    fun getLabel(tag: String?): String {
        val context = activity.get() ?: return ""
        return when (tag) {
            PlaylistFragment.TAG -> context.getString(R.string.playlist_label)
            EpisodesFragment.TAG -> context.getString(R.string.episodes_label)
            DownloadPagerFragment.TAG -> context.getString(R.string.downloads_label)
            PlaybackHistoryFragment.TAG -> context.getString(R.string.playback_history_label)
            SubFeedsFragment.TAG -> context.getString(R.string.subscriptions_label)
            DiscoverFragment.TAG -> context.getString(R.string.discover)
            FavoriteEpisodesFragment.TAG -> context.getString(R.string.favorite_episodes_label)
            else -> ""
        }
    }

    @DrawableRes
    private fun getDrawable(tag: String): Int {
        return when (tag) {
            PlaylistFragment.TAG -> R.drawable.ic_playlist
            EpisodesFragment.TAG -> R.drawable.ic_episodes
            DownloadPagerFragment.TAG -> R.drawable.ic_download
            PlaybackHistoryFragment.TAG -> R.drawable.ic_history
            SubFeedsFragment.TAG -> R.drawable.ic_my_subs
            DiscoverFragment.TAG -> R.drawable.ic_discover
            FavoriteEpisodesFragment.TAG -> R.drawable.ic_star
            else -> 0
        }
    }

    fun getFragmentTags(): List<String> {
        return Collections.unmodifiableList(tags)
    }

    override fun getItemCount(): Int {
        var baseCount = subscriptionOffset
        if (showSubscriptionList) {
            baseCount += itemAccess.count
        }
        return baseCount
    }

    override fun getItemId(position: Int): Long {
        val viewType = getItemViewType(position)
        return if (viewType == VIEW_TYPE_SUBSCRIPTION) {
            itemAccess.getItem(position - subscriptionOffset)!!.id
        } else if (viewType == VIEW_TYPE_NAV) {
            -Math.abs(tags[position].hashCode().toLong()) - 1 // Folder IDs are >0
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (0 <= position && position < tags.size) {
            VIEW_TYPE_NAV
        } else if (position < subscriptionOffset) {
            VIEW_TYPE_SECTION_DIVIDER
        } else {
            VIEW_TYPE_SUBSCRIPTION
        }
    }

    val subscriptionOffset: Int
        get() = if (tags.size > 0) tags.size + 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(activity.get())
        return if (viewType == VIEW_TYPE_NAV) {
            NavHolder(inflater.inflate(R.layout.nav_listitem, parent, false))
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            DividerHolder(inflater.inflate(R.layout.nav_section_item, parent, false))
        } else {
            FeedHolder(inflater.inflate(R.layout.nav_listitem, parent, false))
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val viewType = getItemViewType(position)
        holder.itemView.setOnCreateContextMenuListener(null)
        if (viewType == VIEW_TYPE_NAV) {
            bindNavView(getLabel(tags[position]), position, holder as NavHolder)
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            bindSectionDivider(holder as DividerHolder)
        } else {
            val itemPos = position - subscriptionOffset
            val item = itemAccess.getItem(itemPos)
            bindListItem(item!!, holder as FeedHolder)
            if (item!!.type == DrawerItem.Type.FEED) {
                bindFeedView(item as FeedDrawerItem, holder)
            } else {
                bindTagView(item as TagDrawerItem, holder)
            }
            holder.itemView.setOnCreateContextMenuListener(itemAccess)
        }
        if (viewType != VIEW_TYPE_SECTION_DIVIDER) {
            val typedValue = TypedValue()
            activity.get()!!.theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
            //只有这样才能实现想要的效果，我也不知道为啥，其他界面好像又不需要
            holder.itemView.setBackgroundResource(typedValue.resourceId)
            //            ((MaterialCardView) holder.itemView).setCardBackgroundColor(ThemeUtils.getColorFromAttr(activity.get(), R.attr.colorSurface));
            (holder.itemView as MaterialCardView).isChecked = itemAccess.isSelected(position)
            holder.itemView.setOnClickListener { v: View? -> itemAccess.onItemClick(position) }
            holder.itemView.setOnLongClickListener { v: View? -> itemAccess.onItemLongClick(position) }
            holder.itemView.setOnTouchListener { v: View?, e: MotionEvent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                        && e.buttonState == MotionEvent.BUTTON_SECONDARY
                    ) {
                        itemAccess.onItemLongClick(position)
                        return@setOnTouchListener false
                    }
                }
                false
            }
        }
    }

    private fun bindNavView(title: String, position: Int, holder: NavHolder) {
        val context = activity.get() ?: return
        holder.title.text = title

        // reset for re-use
        holder.count.visibility = View.GONE
        holder.count.setOnClickListener(null)
        holder.count.isClickable = false
        val tag = tags[position]
        if (tag == PlaylistFragment.TAG) {
            val queueSize = itemAccess.queueSize
            if (queueSize > 0) {
                holder.count.text = NumberFormat.getInstance().format(queueSize.toLong())
                holder.count.visibility = View.VISIBLE
            }
        } else if (tag == EpisodesFragment.TAG) {
            //new items 但是我去掉了
//            int unreadItems = itemAccess.getNumberOfNewItems();
            val sum = itemAccess.feedCounterSum
            if (sum > 0) {
                holder.count.text = NumberFormat.getInstance().format(sum.toLong())
                holder.count.visibility = View.VISIBLE
            }
        } else if (tag == SubFeedsFragment.TAG) {
            /*int sum = itemAccess.getFeedCounterSum();
            if (sum > 0) {
                holder.count.setText(NumberFormat.getInstance().format(sum));
                holder.count.setVisibility(View.VISIBLE);
            }*/
        } else if (tag == DownloadPagerFragment.TAG && Prefs.isEnableAutodownload) {
            val epCacheSize = Prefs.episodeCacheSize
            // don't count episodes that can be reclaimed
            val spaceUsed = (itemAccess.numberOfDownloadedItems
                    - itemAccess.reclaimableItems)
            if (epCacheSize > 0 && spaceUsed >= epCacheSize) {
                holder.count.text = "{md-disc-full 150%}"
                Iconify.addIcons(holder.count)
                holder.count.visibility = View.VISIBLE
                holder.count.setOnClickListener { v: View? ->
                    AccentMaterialDialog(
                        context,
                        R.style.MaterialAlertDialogTheme
                    )
                        .setTitle(R.string.episode_cache_full_title)
                        .setMessage(R.string.episode_cache_full_message)
                        .setPositiveButton(android.R.string.cancel, null)
                        .setNeutralButton(R.string.open_autodownload_settings) { dialog: DialogInterface?, which: Int ->
                            val intent = Intent(context, SettingsActivity::class.java)
                            intent.putExtra(SettingsActivity.OPEN_AUTO_DOWNLOAD_SETTINGS, true)
                            context.startActivity(intent)
                        }
                        .show()
                }
            }
        }
        holder.title.setTextColor(textColorPrimary(context))
        val drawable = AppCompatResources.getDrawable(context, getDrawable(tags[position]))
        if (itemAccess.isSelected(position)) {
            if (drawable != null) {
                drawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    accentColor(context), BlendModeCompat.SRC_IN
                )
            }
            holder.title.setTextColor(accentColor(context))
        }
        holder.image.setImageDrawable(drawable)
    }

    private fun bindSectionDivider(holder: DividerHolder) {
        val context = activity.get() ?: return
        if (Prefs.subscriptionsFilter.isEnabled && showSubscriptionList) {
            holder.itemView.isEnabled = true
            holder.feedsFilteredMsg.text = context.getString(R.string.subscriptions_are_filtered)
            holder.feedsFilteredIv.visibility = View.VISIBLE
            holder.feedsFilteredMsg.visibility = View.VISIBLE
        } else {
            //把根布局隐藏没用？我都没搞明白
            holder.itemView.isEnabled = false
            holder.feedsFilteredIv.visibility = View.GONE
            holder.feedsFilteredMsg.visibility = View.GONE
        }
    }

    private fun bindListItem(item: DrawerItem, holder: FeedHolder) {
        if (item.counter > 0) {
            holder.count.visibility = View.VISIBLE
            holder.count.text = NumberFormat.getInstance().format(item.counter.toLong())
        } else {
            holder.count.visibility = View.GONE
        }
        holder.title.text = item.title
        val padding =
            (activity.get()!!.resources.getDimension(R.dimen.thumbnail_length_navlist) / 2).toInt()
        holder.itemView.findViewById<View>(R.id.constraintLayout)
            .setPadding(item.layer * padding, 0, 0, 0)
    }

    private fun bindFeedView(drawerItem: FeedDrawerItem, holder: FeedHolder) {
        val feed = drawerItem.feed
        val context = activity.get() ?: return
        Glide.with(context)
            .load(feed.imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_podcast_background_round)
                    .error(R.drawable.ic_podcast_background_round)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .transforms(
                        CenterCrop(),
                        RoundedCorners((4 * context.resources.displayMetrics.density).toInt())
                    )
                    .dontAnimate()
            )
            .into(holder.image)
        if (feed.hasLastUpdateFailed()) {
//            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) holder.title.getLayoutParams();
//            p.addRule(RelativeLayout.LEFT_OF, R.id.itxtvFailure);
            holder.failure.visibility = View.VISIBLE
        } else {
//            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) holder.title.getLayoutParams();
//            p.addRule(RelativeLayout.LEFT_OF, R.id.txtvCount);
            holder.failure.visibility = View.GONE
        }
    }

    private fun bindTagView(tag: TagDrawerItem, holder: FeedHolder) {
        val context = activity.get() ?: return
        if (tag.isOpen) {
            holder.count.visibility = View.GONE
            holder.image.setImageResource(R.drawable.ic_round_keyboard_arrow_down_24)
        } else {
            holder.image.setImageResource(R.drawable.ic_round_keyboard_arrow_right_24)
        }
        Glide.with(context).clear(holder.image)
        holder.failure.visibility = View.GONE
    }

    open class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    internal class DividerHolder(itemView: View) : Holder(itemView) {
        val feedsFilteredMsg: TextView
        val feedsFilteredIv: View

        init {
            feedsFilteredMsg = itemView.findViewById(R.id.nav_feeds_filtered_message)
            feedsFilteredIv = itemView.findViewById(R.id.filter_iv)
        }
    }

    internal class NavHolder(itemView: View) : Holder(itemView) {
        val image: ImageView
        val title: TextView
        val count: TextView

        init {
            image = itemView.findViewById(R.id.imgvCover)
            title = itemView.findViewById(R.id.txtvTitle)
            count = itemView.findViewById(R.id.txtvCount)
        }
    }

    internal class FeedHolder(itemView: View) : Holder(itemView) {
        val image: ImageView
        val title: TextView
        val failure: ImageView
        val count: TextView

        init {
            image = itemView.findViewById(R.id.imgvCover)
            title = itemView.findViewById(R.id.txtvTitle)
            failure = itemView.findViewById(R.id.itxtvFailure)
            count = itemView.findViewById(R.id.txtvCount)
        }
    }

    interface ItemAccess : OnCreateContextMenuListener {
        val count: Int
        fun getItem(position: Int): DrawerItem?
        fun isSelected(position: Int): Boolean
        val queueSize: Int
        val numberOfNewItems: Int
        val numberOfDownloadedItems: Int
        val reclaimableItems: Int
        val feedCounterSum: Int
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int): Boolean
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?)
    }

    companion object {
        const val VIEW_TYPE_NAV = 0
        const val VIEW_TYPE_SECTION_DIVIDER = 1
        private const val VIEW_TYPE_SUBSCRIPTION = 2

        /**
         * a tag used as a placeholder to indicate if the subscription list should be displayed or not
         * This tag doesn't correspond to any specific activity.
         */
        const val SUBSCRIPTION_LIST_TAG = "Subscriptions_List"
    }

    init {
        activity = WeakReference(context)
        loadItems()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.registerOnSharedPreferenceChangeListener(this)
    }
}