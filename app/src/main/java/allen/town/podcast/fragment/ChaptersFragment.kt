package allen.town.podcast.fragment

import allen.town.podcast.R
import allen.town.podcast.adapter.ChaptersListAdapter
import allen.town.podcast.core.util.ChapterUtils
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.model.feed.Chapter
import allen.town.podcast.model.playback.Playable
import allen.town.podcast.playback.base.PlayerStatus
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChaptersFragment constructor() : Fragment() {
    private var adapter: ChaptersListAdapter? = null
    private var controller: PlaybackController? = null
    private var disposable: Disposable? = null
    private var focusedChapter: Int = -1
    private var media: Playable? = null
    private var layoutManager: LinearLayoutManager? = null
    private var progressBar: ProgressBar? = null
    private var skeleton: Skeleton? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.chapters_list_fragment, null, false)
        val recyclerView: RecyclerView = root.findViewById(R.id.recyclerView)
        progressBar = root.findViewById(R.id.progLoading)
        layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        create(recyclerView)
        adapter = ChaptersListAdapter(requireContext(),object : ChaptersListAdapter.Callback{
            override fun onPlayChapterButtonClicked(position: Int) {
                if (controller!!.status != PlayerStatus.PLAYING) {
                    controller!!.playPause()
                }
                val chapter: Chapter = adapter!!.getItem(position)
                controller!!.seekTo(chapter.start.toInt())
                updateChapterSelection(position, true)
            }

        })
        recyclerView.adapter = adapter
        //这里换成 item_small_recyclerview_skeleton 只显示3条，原因未知
        skeleton = recyclerView.applySkeleton(R.layout.simplechapter_item, 15)
        skeleton!!.showSkeleton()

        return root
    }

    override fun onStart() {
        super.onStart()
        controller = object : PlaybackController((getActivity())!!) {
            override fun loadMediaInfo() {
                this@ChaptersFragment.loadMediaInfo()
            }

            override fun onPositionObserverUpdate() {
                adapter!!.notifyDataSetChanged()
            }
        }
        controller!!.init()
        EventBus.getDefault().register(this)
        loadMediaInfo()
    }

    override fun onStop() {
        super.onStop()
        if (disposable != null) {
            disposable!!.dispose()
        }
        controller!!.release()
        controller = null
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: PlaybackPositionEvent) {
        updateChapterSelection(getCurrentChapter(media), false)
        adapter!!.notifyTimeChanged(event.getPosition().toLong())
    }

    private fun getCurrentChapter(media: Playable?): Int {
        if (controller == null) {
            return -1
        }
        return ChapterUtils.getCurrentChapterIndex(media, controller!!.getPosition())
    }

    private fun loadMediaInfo() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Maybe.create { emitter: MaybeEmitter<Any> ->
            val media: Playable? = controller!!.media
            if (media != null) {
                ChapterUtils.loadChapters(media, context)
                emitter.onSuccess(media)
            } else {
                emitter.onComplete()
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { media: Any -> onMediaChanged(media as Playable) },
                { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
            )
    }

    private fun onMediaChanged(media: Playable) {
        this.media = media
        focusedChapter = -1
        if (adapter == null) {
            return
        }
        if (media.chapters != null && media.chapters.size <= 0) {
            progressBar!!.visibility = View.GONE
            skeleton!!.showOriginal()
        } else {
            progressBar!!.visibility = View.GONE
            skeleton!!.showOriginal()
        }
        adapter!!.setMedia(media)
        val positionOfCurrentChapter: Int = getCurrentChapter(media)
        updateChapterSelection(positionOfCurrentChapter, true)
    }

    private fun updateChapterSelection(position: Int, scrollTo: Boolean) {
        if (adapter == null) {
            return
        }
        if (position != -1 && focusedChapter != position) {
            focusedChapter = position
            adapter!!.notifyChapterChanged(focusedChapter)
            if (scrollTo && ((layoutManager!!.findFirstCompletelyVisibleItemPosition() >= position
                        || layoutManager!!.findLastCompletelyVisibleItemPosition() <= position))
            ) {
                layoutManager!!.scrollToPositionWithOffset(position, 100)
            }
        }
    }

    companion object {
        const val TAG: String = "ChaptersFragment"
    }
}