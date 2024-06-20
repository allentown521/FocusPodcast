package allen.town.podcast.fragment

import allen.town.core.service.GooglePayService
import allen.town.focus.reader.data.db.table.GooglePlayInAppTable
import allen.town.focus_common.ad.RewardedAdManager
import allen.town.focus_common.ads.OnUserEarnedRewardListener
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.EntityDateUtils
import allen.town.focus_common.util.MenuIconUtil.showContextMenuIcon
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_purchase.iap.SupporterManager
import allen.town.focus_purchase.iap.SupporterManagerWrap
import allen.town.podcast.MyApp
import allen.town.podcast.MyApp.Companion.instance
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.activity.SettingsActivity
import allen.town.podcast.adapter.NavigationListAdapter
import allen.town.podcast.adapter.NavigationListAdapter.ItemAccess
import allen.town.podcast.appshortcuts.SubscriptionActivityStarter
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.NavDrawerData
import allen.town.podcast.core.storage.NavDrawerData.*
import allen.town.podcast.core.util.LottieHelper.getRandomLottieFileName
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.dialog.RemoveFeedDialog
import allen.town.podcast.dialog.RenameItemDialog
import allen.town.podcast.dialog.SubsFilterDialog
import allen.town.podcast.dialog.TagEditDialog
import allen.town.podcast.dialog.TagEditDialog.Companion.newInstance
import allen.town.podcast.event.FeedListUpdateEvent
import allen.town.podcast.event.QueueEvent
import allen.town.podcast.event.RemoveAdsPurchaseEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import allen.town.podcast.model.feed.Feed
import allen.town.podcast.util.NavigationUtil
import allen.town.podcast.util.NavigationUtil.goToProVersion
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.appthemehelper.util.VersionUtils
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.SimpleLottieValueCallback
import com.android.billingclient.api.SkuDetails
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wyjson.router.GoRouter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class NavigationDrawerFragment : Fragment(), OnSharedPreferenceChangeListener {
    private var navDrawerData: NavDrawerData? = null
    private var flatItemList: List<DrawerItem>? = null
    private var contextPressedItem: DrawerItem? = null
    private var navAdapter: NavigationListAdapter? = null
    private var disposable: Disposable? = null
    private var progressBar: ProgressBar? = null
    private var openFolders: MutableSet<String> = HashSet()
    private var lottieAnimationView: LottieAnimationView? = null
    private var lottieVip: LottieAnimationView? = null
    private lateinit var removeAdIv: LottieAnimationView
    private lateinit var viewVideoAdIv: LottieAnimationView
    private lateinit var supporterManager: SupporterManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.nav_list, container, false)
        val preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        supporterManager = SupporterManagerWrap.getSupporterManger(requireContext())

        openFolders =
            HashSet(preferences.getStringSet(PREF_OPEN_FOLDERS, HashSet())) // Must not modify
        progressBar = root.findViewById(R.id.progressBar)
        lottieVip = root.findViewById(R.id.already_vip_lottie)
        removeAdIv = root.findViewById(R.id.remove_ad_iv)
        viewVideoAdIv = root.findViewById(R.id.view_ad_video_iv)
        lottieVip!!.setOnClickListener(View.OnClickListener {
            (activity as MainActivity?)!!.closeDrawer {
                goToProVersion(requireActivity())
            }
        })
        val navList = root.findViewById<RecyclerView>(R.id.nav_list)
        navAdapter = NavigationListAdapter(
            itemAccess,
            requireActivity()
        )
        navAdapter!!.setHasStableIds(true)
        navList.adapter = navAdapter
        navList.layoutManager = LinearLayoutManager(context)
        root.findViewById<View>(R.id.nav_settings).setOnClickListener { v: View? ->
            (activity as MainActivity?)!!.closeDrawer {
                startActivity(Intent(activity, SettingsActivity::class.java))
                (activity as MainActivity?)!!.overridePendingTransition(
                    R.anim.retro_fragment_open_enter,
                    R.anim.anim_activity_stay
                )
            }
        }
        lottieAnimationView = root.findViewById(R.id.lottie_play_item)
        lottieAnimationView!!.setAnimation(getRandomLottieFileName())
        val lottieSettings = root.findViewById<LottieAnimationView>(R.id.lottie_settings)
        lottieVip!!.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            SimpleLottieValueCallback {
                PorterDuffColorFilter(
                    accentColor(
                        requireContext()
                    ), PorterDuff.Mode.SRC_ATOP
                )
            }
        )
        preferences.registerOnSharedPreferenceChangeListener(this)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
        requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = requireActivity().menuInflater
        menu.setHeaderTitle(contextPressedItem!!.title)
        if (contextPressedItem!!.type == DrawerItem.Type.FEED) {
            inflater.inflate(R.menu.nav_feed_context, menu)
            // episodes are not loaded, so we cannot check if the podcast has new or unplayed ones!
        } else {
            inflater.inflate(R.menu.nav_folder_context, menu)
        }
        showContextMenuIcon(menu)
        MenuItemUtils.setOnClickListeners(menu) { item: MenuItem -> onContextItemSelected(item) }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val pressedItem = contextPressedItem
        contextPressedItem = null
        if (pressedItem == null) {
            return false
        }
        return if (pressedItem.type == DrawerItem.Type.FEED) {
            onFeedContextMenuClicked((pressedItem as FeedDrawerItem).feed, item)
        } else {
            onTagContextMenuClicked(pressedItem, item)
        }
    }

    private fun onFeedContextMenuClicked(feed: Feed, item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.shortcut_item) {
            SubscriptionActivityStarter.createShortcut(context, feed)
            return true
        } else if (itemId == R.id.edit_tags) {
            newInstance(listOf(feed.preferences))
                .show(childFragmentManager, TagEditDialog.TAG)
            return true
        } else if (itemId == R.id.rename_item) {
            RenameItemDialog(requireActivity(), feed).show()
            return true
        } else if (itemId == R.id.remove_feed) {
            (activity as MainActivity?)!!.loadFragment(EpisodesFragment.TAG, null)
            RemoveFeedDialog.show(requireContext(), feed)
            return true
        }
        return super.onContextItemSelected(item)
    }

    private fun onTagContextMenuClicked(drawerItem: DrawerItem, item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.rename_folder_item) {
            RenameItemDialog(requireActivity(), drawerItem).show()
            return true
        }
        return super.onContextItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        loadData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFeedListChanged(event: FeedListUpdateEvent?) {
        loadData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onQueueChanged(event: QueueEvent) {
        // we are only interested in the number of queue items, not download status or position
        if (event.action == QueueEvent.Action.DELETED_MEDIA || event.action == QueueEvent.Action.SORTED || event.action == QueueEvent.Action.MOVED) {
            return
        }
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
        lottieVip!!.visibility =
            if (instance.checkSupporter(requireContext(), false) && !MyApp.instance.isDroid) View.VISIBLE else View.GONE
        setRemoveAdButton()
        setViewVideoAdButton()
    }

    private val itemAccess: ItemAccess = object : ItemAccess {
        override val count: Int
            get() = if (flatItemList != null) {
                flatItemList!!.size
            } else {
                0
            }

        override fun getItem(position: Int): DrawerItem? {
            return if (flatItemList != null && 0 <= position && position < flatItemList!!.size) {
                flatItemList!![position]
            } else {
                null
            }
        }

        override fun isSelected(position: Int): Boolean {
            val lastNavFragment = getLastNavFragment(requireContext())
            if (position < navAdapter!!.subscriptionOffset) {
                return navAdapter!!.getFragmentTags()[position] == lastNavFragment
            } else if (StringUtils.isNumeric(lastNavFragment)) { // last fragment was not a list, but a feed
                val feedId = lastNavFragment!!.toLong()
                if (navDrawerData != null) {
                    val itemToCheck = flatItemList!![position - navAdapter!!.subscriptionOffset]
                    if (itemToCheck.type == DrawerItem.Type.FEED) {
                        // When the same feed is displayed multiple times, it should be highlighted multiple times.
                        return (itemToCheck as FeedDrawerItem).feed.id == feedId
                    }
                }
            }
            return false
        }

        override val queueSize: Int
            get() = if (navDrawerData != null) navDrawerData!!.queueSize else 0
        override val numberOfNewItems: Int
            get() = if (navDrawerData != null) navDrawerData!!.numNewItems else 0
        override val numberOfDownloadedItems: Int
            get() = if (navDrawerData != null) navDrawerData!!.numDownloadedItems else 0
        override val reclaimableItems: Int
            get() = if (navDrawerData != null) navDrawerData!!.reclaimableSpace else 0
        override val feedCounterSum: Int
            get() {
                if (navDrawerData == null) {
                    return 0
                }
                var sum = 0
                for (counter in navDrawerData!!.feedCounters.values()) {
                    sum += counter
                }
                return sum
            }


        override fun onItemClick(position: Int) {
            val viewType = navAdapter!!.getItemViewType(position)
            if (viewType != NavigationListAdapter.VIEW_TYPE_SECTION_DIVIDER) {
                if (position < navAdapter!!.subscriptionOffset) {
                    val tag = navAdapter!!.getFragmentTags()[position]
                    (activity as MainActivity?)!!.loadFragment(tag, null)
                    (activity as MainActivity?)!!.bottomSheet!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
                } else {
                    val pos = position - navAdapter!!.subscriptionOffset
                    val clickedItem = flatItemList!![pos]
                    if (clickedItem.type == DrawerItem.Type.FEED) {
                        val feedId = (clickedItem as FeedDrawerItem).feed.id
                        (activity as MainActivity?)!!.loadFeedFragmentById(feedId, null)
                        (activity as MainActivity?)!!.bottomSheet!!
                            .setState(BottomSheetBehavior.STATE_COLLAPSED)
                    } else {
                        val folder = clickedItem as TagDrawerItem
                        if (openFolders.contains(folder.name)) {
                            openFolders.remove(folder.name)
                        } else {
                            openFolders.add(folder.name)
                        }
                        requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putStringSet(PREF_OPEN_FOLDERS, openFolders)
                            .apply()
                        disposable = Observable.fromCallable {
                            makeFlatDrawerData(
                                navDrawerData!!.items, 0
                            )
                        }
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { result: List<DrawerItem>? ->
                                    flatItemList = result
                                    navAdapter!!.notifyDataSetChanged()
                                }) { error: Throwable? ->
                                Log.e(
                                    TAG,
                                    Log.getStackTraceString(error)
                                )
                            }
                    }
                }
            } else if (Prefs.subscriptionsFilter.isEnabled
                && navAdapter!!.showSubscriptionList
            ) {
                SubsFilterDialog.showDialog(requireContext())
            }
        }

        override fun onItemLongClick(position: Int): Boolean {
            return if (position < navAdapter!!.getFragmentTags().size) {
                true
            } else {
                contextPressedItem = flatItemList!![position - navAdapter!!.subscriptionOffset]
                false
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            this@NavigationDrawerFragment.onCreateContextMenu(menu, v, menuInfo)
        }
    }

    private fun loadData() {
        disposable = Observable.fromCallable {
            val data = DBReader.getNavDrawerData(false)
            Pair(data, makeFlatDrawerData(data.items, 0))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Pair<NavDrawerData, List<DrawerItem>> ->
                    navDrawerData = result.first
                    flatItemList = result.second
                    navAdapter!!.notifyDataSetChanged()
                    progressBar!!.visibility =
                        View.GONE // Stays hidden once there is something in the list
                }) { error: Throwable? ->
                Log.e(TAG, Log.getStackTraceString(error))
                progressBar!!.visibility = View.GONE
            }
    }

    private fun makeFlatDrawerData(items: List<DrawerItem>, layer: Int): List<DrawerItem> {
        val flatItems: MutableList<DrawerItem> = ArrayList()
        for (item in items) {
            item.layer = layer
            flatItems.add(item)
            if (item.type == DrawerItem.Type.TAG) {
                val folder = item as TagDrawerItem
                folder.isOpen = openFolders.contains(folder.name)
                if (folder.isOpen) {
                    flatItems.addAll(makeFlatDrawerData(item.children, layer + 1))
                }
            }
        }
        return flatItems
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (PREF_LAST_FRAGMENT_TAG == key) {
            navAdapter!!.notifyDataSetChanged() // Update selection
        }
    }

    /**
     * 激励广告
     */
    private fun setViewVideoAdButton() {
        if ((instance.checkSupporter(requireContext(), false)
                    && !instance.temporarySupporter())
            || !BasePreferenceUtil.isRewardCanShowToday() || instance.isDroid
        ) {
            viewVideoAdIv.visibility = View.GONE
        } else {
            viewVideoAdIv.visibility = View.VISIBLE
            viewVideoAdIv.setOnClickListener {

                if (BasePreferenceUtil.firstToViewVideoAd) {
                    AccentMaterialDialog(
                        requireContext(),
                        R.style.MaterialAlertDialogTheme
                    )
                        .setTitle(R.string.rewarded_title)
                        .setMessage(R.string.rewarded_ad_one_hour_tip)
                        .setPositiveButton(android.R.string.cancel, null)
                        .setNeutralButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                            showRewardedAd()
                        }
                        .show()
                    BasePreferenceUtil.firstToViewVideoAd = false
                } else {
                    showRewardedAd()
                }


            }
        }
    }

    private fun showRewardedAd() {

        RewardedAdManager.showRewardedVideo(requireActivity(), object : OnUserEarnedRewardListener {
            override fun onUserEarnedReward() {
                BasePreferenceUtil.rewardAdValidTime = System.currentTimeMillis()
                setRemoveAdButton()
                setViewVideoAdButton()
                EventBus.getDefault().post(
                    RemoveAdsPurchaseEvent()
                )
            }

            override fun onClosed(isEarned: Boolean) {
                if (isEarned) {
                    TopSnackbarUtil.showSnack(
                        activity,
                        getString(
                            R.string.rewarded_locked, EntityDateUtils.timeStamp2Date(
                                BasePreferenceUtil
                                    .rewardAdValidTime, "yyyy-MM-dd HH:mm"
                            )
                        ),
                        Toast.LENGTH_LONG
                    )
                }
            }

        })

    }

    private fun setRemoveAdButton() {
        if (instance.isAdBlockUser()) {
            removeAdIv.visibility = View.GONE
        } else {
            removeAdIv.visibility = View.VISIBLE
            removeAdIv.setOnClickListener {
                (activity as MainActivity?)!!.closeDrawer {
                    if (instance.isAlipay) {
                        goToProVersion(requireContext(), true)
                    } else {
                        supporterManager.supporterInAppItem.subscribeOn(rx.schedulers.Schedulers.io())
                            .observeOn(
                                rx.android.schedulers.AndroidSchedulers.mainThread()
                            ).subscribe(
                                { skuDetails: List<SkuDetails> ->
                                    for (detail in skuDetails) {
                                        if (GoRouter.getInstance().getService(GooglePayService::class.java)!!.getRemoveAdsId().contains(detail.sku)) {
                                            supporterManager.becomeInAppSubSupporter(
                                                activity,
                                                detail,
                                                GooglePlayInAppTable.TYPE_REMOVE_ADS
                                            ).subscribeOn(rx.schedulers.Schedulers.io()).observeOn(
                                                rx.android.schedulers.AndroidSchedulers.mainThread()
                                            )
                                                .subscribe({ aBoolean: Boolean ->
                                                    if (aBoolean) {
                                                        TopSnackbarUtil.showSnack(
                                                            activity,
                                                            R.string.thanks_purchase,
                                                            Toast.LENGTH_LONG
                                                        )
                                                        setRemoveAdButton()
                                                        EventBus.getDefault().post(
                                                            RemoveAdsPurchaseEvent()
                                                        )
                                                    }
                                                }) { throwable: Throwable? ->
                                                    Timber.d(
                                                        throwable,
                                                        "There was an error while purchasing remove ads supporter item"
                                                    )
                                                }
                                        } else {
                                            Timber.e("unknown remove ads sku %s", detail.sku)
                                        }
                                    }
                                }) { throwable: Throwable? ->
                                Timber.e(
                                    throwable, "There was an error while retrieving " +
                                            "remove ads supporter sub item"
                                )
                            }
                    }
                }
            }
        }
    }

    companion object {
        @VisibleForTesting
        val PREF_LAST_FRAGMENT_TAG = "pref_last_fragment_tag"
        private const val PREF_OPEN_FOLDERS = "pref_opened_folders"

        @VisibleForTesting
        val PREF_NAME = "pref_navigation_drawer"
        const val TAG = "DrawerFragment"

        @JvmField
        val NAV_DRAWER_TAGS = arrayOf(
            PlaylistFragment.TAG,
            EpisodesFragment.TAG,
            SubFeedsFragment.TAG,
            FavoriteEpisodesFragment.TAG,
            DownloadPagerFragment.TAG,
            PlaybackHistoryFragment.TAG,
            DiscoverFragment.TAG,
            NavigationListAdapter.SUBSCRIPTION_LIST_TAG
        )

        /**
         * 保存最近一次打开的fragment
         *
         * @param context
         * @param tag
         */
        @JvmStatic
        fun saveLastNavFragment(context: Context, tag: String?) {
            Log.d(TAG, "set last nav fragment -> $tag")
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val edit = prefs.edit()
            if (tag != null) {
                edit.putString(PREF_LAST_FRAGMENT_TAG, tag)
            } else {
                edit.remove(PREF_LAST_FRAGMENT_TAG)
            }
            edit.apply()
        }

        @JvmStatic
        fun getLastNavFragment(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val lastFragment = prefs.getString(PREF_LAST_FRAGMENT_TAG, PlaylistFragment.TAG)
            Log.v(TAG, "get last nav fragment() -> $lastFragment")
            return lastFragment
        }
    }
}