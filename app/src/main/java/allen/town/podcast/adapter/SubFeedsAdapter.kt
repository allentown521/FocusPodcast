package allen.town.podcast.adapter

import allen.town.focus_common.util.MenuIconUtil.showContextMenuIcon
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.SubFeedsAdapter.SubscriptionViewHolder
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.storage.NavDrawerData.DrawerItem
import allen.town.podcast.core.storage.NavDrawerData.FeedDrawerItem
import allen.town.podcast.fragment.FeedItemlistFragment
import allen.town.podcast.fragment.SubFeedsFragment
import allen.town.podcast.model.feed.Feed
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.text.TextUtils
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import jp.shts.android.library.TriangleLabelView
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.*

/**
 * Adapter for subscriptions
 */
open class SubFeedsAdapter(mainActivity: MainActivity) :
    MultiSelectAdapter<SubscriptionViewHolder?>(mainActivity, R.menu.nav_feed_action_speeddial),
    OnCreateContextMenuListener {
    private val mainActivityRef: WeakReference<MainActivity>
    private var listItems: List<DrawerItem>
    var selectedItem: DrawerItem? = null
        private set
    var longPressedPosition = 0 // used to init actionMode
    fun getItem(position: Int): Any {
        return listItems[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val itemView = LayoutInflater.from(mainActivityRef.get())
            .inflate(R.layout.subscription_item, parent, false)
        val feedTitle = itemView.findViewById<TextView>(R.id.txtvTitle)
        val params = feedTitle.layoutParams as RelativeLayout.LayoutParams
        var topAndBottomItemId = R.id.imgvCover
        var belowItemId = 0
        if (viewType == COVER_WITH_TITLE) {
            topAndBottomItemId = 0
            belowItemId = R.id.imgvCover
            //            feedTitle.setBackgroundColor(feedTitle.getContext().getResources().getColor(R.color.feed_text_bg));
//            int padding = (int) convertDpToPixel(feedTitle.getContext(), 4);
//            feedTitle.setPadding(padding, padding, padding, padding);
        }
        //        params.addRule(RelativeLayout.BELOW, belowItemId);
//        params.addRule(RelativeLayout.ALIGN_TOP, topAndBottomItemId);
//        params.addRule(RelativeLayout.ALIGN_BOTTOM, topAndBottomItemId);
        feedTitle.layoutParams = params
        //        feedTitle.setSingleLine(viewType == COVER_WITH_TITLE);
        return SubscriptionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val drawerItem = listItems[position]
        val isFeed = drawerItem.type == DrawerItem.Type.FEED
        holder.bind(drawerItem)
        holder.itemView.setOnCreateContextMenuListener(this)
        if (inActionMode()) {
            if (isFeed) {
                holder.selectCheckbox.visibility = View.VISIBLE
                holder.selectView.visibility = View.VISIBLE
            }
            holder.selectCheckbox.isChecked = isSelected(position)
            holder.selectCheckbox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                setSelected(
                    holder.bindingAdapterPosition,
                    isChecked
                )
            }
            holder.imageView.alpha = 0.6f
            holder.count.visibility = View.GONE
        } else {
            holder.selectView.visibility = View.GONE
            holder.imageView.alpha = 1.0f
        }
        holder.itemView.setOnLongClickListener { v: View? ->
            if (!inActionMode()) {
                if (isFeed) {
                    longPressedPosition = holder.bindingAdapterPosition
                }
                selectedItem = getItem(holder.bindingAdapterPosition) as DrawerItem
            }
            false
        }
        holder.itemView.setOnTouchListener { v: View?, e: MotionEvent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                    && e.buttonState == MotionEvent.BUTTON_SECONDARY
                ) {
                    if (!inActionMode()) {
                        if (isFeed) {
                            longPressedPosition = holder.bindingAdapterPosition
                        }
                        selectedItem = getItem(holder.bindingAdapterPosition) as DrawerItem
                    }
                }
            }
            false
        }
        holder.itemView.setOnClickListener { v: View? ->
            if (isFeed) {
                if (inActionMode()) {
                    holder.selectCheckbox.isChecked = !isSelected(holder.bindingAdapterPosition)
                } else {
                    val fragment: Fragment = FeedItemlistFragment
                        .newInstance((drawerItem as FeedDrawerItem).feed.id)
                    mainActivityRef.get()!!.loadChildFragment(fragment)
                }
            } else if (!inActionMode()) {
                val fragment: Fragment = SubFeedsFragment.newInstance(drawerItem.title)
                mainActivityRef.get()!!.loadChildFragment(fragment)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun getItemId(position: Int): Long {
        return listItems[position].id
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        if (inActionMode() || selectedItem == null) {
            return
        }
        val inflater = mainActivityRef.get()!!.menuInflater
        if (selectedItem!!.type == DrawerItem.Type.FEED) {
            inflater.inflate(R.menu.nav_feed_context, menu)
            menu.findItem(R.id.multi_select).isVisible = true
            showContextMenuIcon(menu)
        } else {
            inflater.inflate(R.menu.nav_folder_context, menu)
        }
        menu.setHeaderTitle(selectedItem!!.title)
    }

    fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.multi_select) {
            startSelectMode(longPressedPosition)
            return true
        }
        return false
    }

    val selectedItems: List<Feed>
        get() {
            val items: MutableList<Feed> = ArrayList()
            for (i in 0 until itemCount) {
                if (isSelected(i)) {
                    val drawerItem = listItems[i]
                    if (drawerItem.type == DrawerItem.Type.FEED) {
                        val feed = (drawerItem as FeedDrawerItem).feed
                        items.add(feed)
                    }
                }
            }
            return items
        }

    fun setItems(listItems: List<DrawerItem>) {
        this.listItems = listItems
    }

    override fun setSelected(pos: Int, selected: Boolean) {
        val drawerItem = listItems[pos]
        if (drawerItem.type == DrawerItem.Type.FEED) {
            super.setSelected(pos, selected)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (Prefs.shouldShowSubscriptionTitle()) COVER_WITH_TITLE else 0
    }

    inner class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val feedTitle: TextView
        val imageView: ImageView
        val count: TriangleLabelView
        val selectView: FrameLayout
        val selectCheckbox: CheckBox
        fun bind(drawerItem: DrawerItem) {
            val drawable = AppCompatResources.getDrawable(
                selectView.context,
                R.drawable.ic_checkbox_background
            )
            selectView.background = drawable // Setting this in XML crashes API <= 21
            feedTitle.text = drawerItem.title
            imageView.contentDescription = drawerItem.title
            feedTitle.visibility = View.VISIBLE
            if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL) {
                count.corner = TriangleLabelView.Corner.TOP_LEFT
            }
            count.setTriangleBackgroundColor(accentColor(count.context))
            if (drawerItem.counter > 0) {
                count.primaryText = NumberFormat.getInstance().format(drawerItem.counter.toLong())
                count.visibility = View.VISIBLE
            } else {
                count.visibility = View.GONE
            }
            if (drawerItem.type == DrawerItem.Type.FEED) {
                val feed = (drawerItem as FeedDrawerItem).feed
                val textAndImageCombind = (feed.isLocalFeed
                        && feed.imageUrl != null && feed.imageUrl.startsWith(Feed.PREFIX_GENERATIVE_COVER))
                CoverLoader(mainActivityRef.get())
                    .withUri(feed.imageUrl)
                    .withPlaceholderView(feedTitle, textAndImageCombind)
                    .withCoverView(imageView)
                    .load()
            } else {
                CoverLoader(mainActivityRef.get())
                    .withResource(R.drawable.ic_tag)
                    .withPlaceholderView(feedTitle, true)
                    .withCoverView(imageView)
                    .load()
            }
        }

        init {
            feedTitle = itemView.findViewById(R.id.txtvTitle)
            imageView = itemView.findViewById(R.id.imgvCover)
            count = itemView.findViewById(R.id.triangleCountView)
            selectView = itemView.findViewById(R.id.selectView)
            selectCheckbox = itemView.findViewById(R.id.selectCheckBox)
        }
    }

    class GridDividerItemDecorator : ItemDecoration() {
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDraw(c, parent, state)
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            val context = parent.context
            val insetOffset = convertDpToPixel(context, 0f).toInt()
            outRect[insetOffset, insetOffset, insetOffset] = insetOffset
        }
    }

    companion object {
        private const val COVER_WITH_TITLE = 1
        fun convertDpToPixel(context: Context, dp: Float): Float {
            return dp * context.resources.displayMetrics.density
        }
    }

    init {
        mainActivityRef = WeakReference(mainActivity)
        listItems = ArrayList()
        setHasStableIds(true)
    }
}