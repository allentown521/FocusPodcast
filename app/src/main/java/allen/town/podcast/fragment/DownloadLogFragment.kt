package allen.town.podcast.fragment

import allen.town.focus_common.util.DoubleClickBackToContentTopListener.IBackToContentTopView
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.adapter.DownloadLogAdapter
import allen.town.podcast.core.event.DownloadEvent
import allen.town.podcast.core.event.DownloadLogEvent
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.service.download.Downloader
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.download.AutoUpdateManager
import allen.town.podcast.core.util.menuhandler.MenuItemUtils
import allen.town.podcast.core.util.menuhandler.MenuItemUtils.UpdateRefreshMenuItemChecker
import allen.town.podcast.model.download.DownloadStatus
import allen.town.podcast.ui.common.PagedToolbarFragment
import allen.town.podcast.view.EmptyViewHandler
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Shows the download log
 */
class DownloadLogFragment : Fragment(), IBackToContentTopView {
    private var downloadLog: List<DownloadStatus> = ArrayList()
    private var runningDownloads: List<Downloader> = ArrayList()
    private var adapter: DownloadLogAdapter? = null
    private var disposable: Disposable? = null
    private var isUpdatingFeeds = false
    private lateinit var recyclerView: RecyclerView
    private var skeleton: Skeleton? = null
    var emptyView: EmptyViewHandler? = null
    override fun onStart() {
        super.onStart()
        loadDownloadLog()
    }

    override fun onStop() {
        super.onStop()
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.simple_list_fragment, container, false)
        recyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.setRecycledViewPool((activity as MainActivity?)!!.recycledViewPool)
        adapter = DownloadLogAdapter((activity as MainActivity?)!!)
        recyclerView.setAdapter(adapter)
        create(recyclerView)
        emptyView = EmptyViewHandler(activity)
        emptyView!!.setIcon(R.drawable.ic_download)
        emptyView!!.setTitle(R.string.no_log_downloads_head_label)
        emptyView!!.attachToRecyclerView(recyclerView)
        EventBus.getDefault().register(this)
        skeleton = recyclerView.applySkeleton(R.layout.item_small_recyclerview_skeleton, 15)
        skeleton!!.showSkeleton()
        return root
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    @Subscribe
    fun onDownloadLogChanged(event: DownloadLogEvent?) {
        loadDownloadLog()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.clear_logs_item).isVisible = !downloadLog.isEmpty()
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(
            menu,
            R.id.refresh_item,
            updateRefreshMenuItemChecker
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) {
            return true
        } else if (item.itemId == R.id.clear_logs_item) {
            DBWriter.clearDownloadLog()
            return true
        } else if (item.itemId == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext())
            return true
        }
        return false
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            (parentFragment as PagedToolbarFragment?)!!.invalidateOptionsMenuIfActive(this)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: DownloadEvent) {
        val update = event.update
        runningDownloads = update.downloaders
        adapter!!.setRunningDownloads(runningDownloads)
    }

    private val updateRefreshMenuItemChecker =
        UpdateRefreshMenuItemChecker { DownloadService.isRunning && DownloadService.isDownloadingFeeds() }

    private fun loadDownloadLog() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        emptyView!!.hide()
        disposable = Observable.fromCallable { DBReader.getDownloadLog() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result: List<DownloadStatus>? ->
                if (result != null) {
                    downloadLog = result
                    if (skeleton!!.isSkeleton()) {
                        skeleton!!.showOriginal()
                    }
                    adapter!!.setDownloadLog(downloadLog)
                    (parentFragment as PagedToolbarFragment?)!!.invalidateOptionsMenuIfActive(this)
                }
            }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    companion object {
        private const val TAG = "DownloadLogFragment"
    }

    override fun backToContentTop() {
        recyclerView.scrollToPosition(5)
        recyclerView.post { recyclerView.smoothScrollToPosition(0) }
    }
}