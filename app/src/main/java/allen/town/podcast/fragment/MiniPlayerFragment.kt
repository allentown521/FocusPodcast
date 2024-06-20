package allen.town.podcast.fragment

import allen.town.focus_common.util.RetroUtil
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import allen.town.focus_common.extensions.accentBackgroundColor
import allen.town.focus_common.extensions.accentColor
import allen.town.focus_common.extensions.applyAccentColor
import allen.town.focus_common.extensions.show
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.core.feed.util.ImageResourceUtils
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.event.playback.PlaybackPositionEvent
import allen.town.podcast.event.playback.PlaybackServiceEvent
import allen.town.podcast.model.playback.MediaType
import allen.town.podcast.model.playback.Playable
import allen.town.podcast.playback.base.PlayerStatus
import allen.town.podcast.view.PlayButton
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Fragment which is supposed to be displayed outside of the MediaplayerActivity.
 */
class MiniPlayerFragment : Fragment() {
    private var imgvCover: ImageView? = null
    private var txtvTitle: TextView? = null
    private var butPlay: PlayButton? = null
    private var feedName: TextView? = null
    private var progressBar: CircularProgressIndicator? = null
    private var controller: PlaybackController? = null
    private var disposable: Disposable? = null
    private var queueIv: AppCompatImageView? = null
    private var rewIv: AppCompatImageView? = null
    private var forwardIv: AppCompatImageView? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.external_player_fragment, container, false)
        imgvCover = root.findViewById(R.id.imgvCover)
        txtvTitle = root.findViewById(R.id.txtvTitle)
        butPlay = root.findViewById(R.id.butPlay)
        feedName = root.findViewById(R.id.txtvAuthor)
        queueIv = root.findViewById(R.id.queue_iv)
        rewIv = root.findViewById(R.id.butRev)
        forwardIv = root.findViewById(R.id.butFF)
        progressBar = root.findViewById(R.id.episodeProgress)
        progressBar!!.accentColor()
        butPlay!!.applyAccentColor()
        (root.rootView as? MaterialCardView)?.accentBackgroundColor()

        root.findViewById<View>(R.id.fragmentLayout).setOnClickListener { v: View? ->
            if (controller != null && controller!!.media != null) {
                if (controller!!.media.mediaType == MediaType.AUDIO) {
                    (activity as MainActivity?)!!.bottomSheet!!.setState(BottomSheetBehavior.STATE_EXPANDED)
                } else {
                    val intent =
                        PlaybackService.getPlayerActivityIntent(activity, controller!!.media)
                    startActivity(intent)
                }
            }
        }
        queueIv!!.setOnClickListener{
            (activity as MainActivity?)!!.loadChildFragment(PlaylistFragment())
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        butPlay!!.setOnClickListener { v: View? ->
            if (controller == null) {
                return@setOnClickListener
            }
            if (controller!!.media != null && controller!!.media.mediaType == MediaType.VIDEO && controller!!.status != PlayerStatus.PLAYING) {
                controller!!.playPause()
                requireContext().startActivity(
                    PlaybackService
                        .getPlayerActivityIntent(context, controller!!.media)
                )
            } else {
                controller!!.playPause()
            }
        }

        rewIv!!.setOnClickListener {
            if (controller != null) {
                val curr = controller!!.position
                controller!!.seekTo(curr - Prefs.rewindSecs * 1000)
            }
        }

        forwardIv!!.setOnClickListener {
            if (controller != null) {
                val curr = controller!!.position
                controller!!.seekTo(curr + Prefs.fastForwardSecs * 1000)
            }
        }

        if (RetroUtil.isTablet(context)) {
            forwardIv!!.show()
            rewIv!!.show()
        } else {
            forwardIv!!.visibility =
                if (Prefs.showExtraMiniButtons()) View.VISIBLE else View.GONE
            rewIv!!.visibility =
                if (Prefs.showExtraMiniButtons()) View.VISIBLE else View.GONE
        }

        loadMediaInfo()
    }

    private fun setupPlaybackController(): PlaybackController {
        return object : PlaybackController(requireActivity()) {
            override fun updatePlayButtonShowsPlay(showPlay: Boolean) {
                butPlay!!.setIsShowPlay(showPlay)
            }

            override fun loadMediaInfo() {
                this@MiniPlayerFragment.loadMediaInfo()
            }

            override fun onPlaybackEnd() {
                (activity as MainActivity?)!!.setPlayerVisible(false)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller = setupPlaybackController()
        controller!!.init()
        loadMediaInfo()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        if (controller != null) {
            controller!!.release()
            controller = null
        }
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPositionObserverUpdate(event: PlaybackPositionEvent?) {
        if (controller == null) {
            return
        } else if (controller!!.position == PlaybackService.INVALID_TIME
            || controller!!.duration == PlaybackService.INVALID_TIME
        ) {
            return
        }
        progressBar!!.progress =
            (controller!!.position.toDouble() / controller!!.duration * 100).toInt()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlaybackServiceChanged(event: PlaybackServiceEvent) {
        if (event.action == PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN) {
            (activity as MainActivity?)!!.setPlayerVisible(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    override fun onPause() {
        super.onPause()
        if (controller != null) {
            controller!!.pause()
        }
    }

    private fun loadMediaInfo() {
        if (controller == null) {
            Log.w(TAG, "loadMediaInfo was called while PlaybackController was null!")
            return
        }
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Maybe.fromCallable { controller!!.media }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { media: Playable? -> updateUi(media) },
                { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
            ) { (activity as MainActivity?)!!.setPlayerVisible(false) }
    }

    private fun updateUi(media: Playable?) {
        if (media == null) {
            return
        }
        (activity as MainActivity?)!!.setPlayerVisible(true)
        txtvTitle!!.text = media.episodeTitle
        feedName!!.text = media.feedTitle
        onPositionObserverUpdate(PlaybackPositionEvent(media.position, media.duration))
        val options = RequestOptions()
            .placeholder(R.drawable.ic_podcast_background_round)
            .error(R.drawable.ic_podcast_background_round)
            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
            .centerCrop()
            .dontAnimate()
        Glide.with(this)
            .load(ImageResourceUtils.getEpisodeListImageLocation(media))
            .error(
                Glide.with(this)
                    .load(ImageResourceUtils.getFallbackImageLocation(media))
                    .apply(options)
            )
            .apply(options)
            .into(imgvCover!!)
        if (controller != null && controller!!.isPlayingVideoLocally) {
            (activity as MainActivity?)!!.bottomSheet!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            butPlay!!.visibility = View.VISIBLE
        }
    }

    companion object {
        const val TAG = "MiniPlayerFragment"
    }
}