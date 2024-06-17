package allen.town.podcast.adapter

import allen.town.podcast.R
import allen.town.podcast.adapter.ChaptersListAdapter.ChapterHolder
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.util.Converter
import allen.town.podcast.model.feed.Chapter
import allen.town.podcast.model.feed.EmbeddedChapterImage
import allen.town.podcast.model.playback.Playable
import allen.town.podcast.ui.common.CircularProgressBar
import allen.town.focus_common.util.ThemeUtils.getColorFromAttr
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView

class ChaptersListAdapter(private val context: Context, private val callback: Callback?) :
    RecyclerView.Adapter<ChapterHolder>() {
    private var media: Playable? = null
    private var currentChapterIndex = -1
    private var currentChapterPosition: Long = -1
    private var hasImages = false
    fun setMedia(media: Playable) {
        this.media = media
        hasImages = false
        if (media.chapters != null) {
            for (chapter in media.chapters) {
                if (!TextUtils.isEmpty(chapter.imageUrl)) {
                    hasImages = true
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ChapterHolder, position: Int) {
        val sc = getItem(position)
        if (sc == null) {
            holder.title.text = "Error"
            return
        }
        holder.title.text = sc.title
        holder.start.text = Converter.getDurationStringLong(
            sc
                .start.toInt()
        )
        val duration: Long
        duration = if (position + 1 < media!!.chapters.size) {
            media!!.chapters[position + 1].start - sc.start
        } else {
            media!!.duration - sc.start
        }
        holder.duration.text = context.getString(
            R.string.chapter_duration,
            Converter.getDurationStringLocalized(context, duration.toInt().toLong())
        )
        if (TextUtils.isEmpty(sc.link)) {
            holder.link.visibility = View.GONE
        } else {
//            holder.link.setVisibility(View.VISIBLE);
//            holder.link.setText(sc.getLink());
//            holder.link.setOnClickListener(v -> IntentUtils.openInBrowser(context, sc.getLink()));
        }
        holder.itemView.setOnClickListener { v: View? ->
            callback?.onPlayChapterButtonClicked(
                position
            )
        }

        //只有这样才能实现想要的效果，我也不知道为啥
        holder.itemView.setBackgroundColor(
            getColorFromAttr(
                context, R.attr.colorSurface
            )
        )
        if (position == currentChapterIndex) {
//            int playingBackGroundColor = ThemeUtils.getColorFromAttr(context, R.attr.currently_playing_background);
//            holder.itemView.setBackgroundColor(playingBackGroundColor);
//            ((MaterialCardView) holder.itemView).setCardBackgroundColor(ThemeUtils.getColorFromAttr(activity.get(), R.attr.colorSurface));
            (holder.itemView as MaterialCardView).isChecked = true
            var progress = (currentChapterPosition - sc.start).toFloat() / duration
            progress = Math.max(progress, CircularProgressBar.MINIMUM_PERCENTAGE)
            progress = Math.min(progress, CircularProgressBar.MAXIMUM_PERCENTAGE)
            holder.lottieAnimationView.visibility = View.VISIBLE
        } else {
//            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            (holder.itemView as MaterialCardView).isChecked = false
            holder.lottieAnimationView.visibility = View.GONE
        }

//        if (hasImages) {
//            holder.image.setVisibility(View.VISIBLE);
//            if (TextUtils.isEmpty(sc.getImageUrl())) {
//                Glide.with(context).clear(holder.image);
//            } else {
        Glide.with(context)
            .load(EmbeddedChapterImage.getModelFor(media, position))
            .apply(
                RequestOptions()
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .placeholder(R.drawable.ic_podcast_background_round)
                    .error(R.drawable.ic_podcast_background_round)
                    .dontAnimate()
                    .transforms(
                        CenterCrop(),
                        RoundedCorners((4 * context.resources.displayMetrics.density).toInt())
                    )
            )
            .into(holder.image)
        //            }
//        } else {
//            holder.image.setVisibility(View.GONE);
//        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder {
        val inflater = LayoutInflater.from(context)
        return ChapterHolder(inflater.inflate(R.layout.simplechapter_item, parent, false))
    }

    override fun getItemCount(): Int {
        return if (media == null || media!!.chapters == null) {
            0
        } else media!!.chapters.size
    }

    class ChapterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView
        val start: TextView
        val link: TextView
        val duration: TextView
        val image: ImageView
        val lottieAnimationView: LottieAnimationView

        init {
            title = itemView.findViewById(R.id.txtvTitle)
            start = itemView.findViewById(R.id.txtvStart)
            link = itemView.findViewById(R.id.txtvLink)
            image = itemView.findViewById(R.id.imgvCover)
            duration = itemView.findViewById(R.id.tv_item_size)
            lottieAnimationView = itemView.findViewById(R.id.playing_lottie)
        }
    }

    fun notifyChapterChanged(newChapterIndex: Int) {
        currentChapterIndex = newChapterIndex
        currentChapterPosition = getItem(newChapterIndex).start
        notifyDataSetChanged()
    }

    fun notifyTimeChanged(timeMs: Long) {
        currentChapterPosition = timeMs
        // Passing an argument prevents flickering.
        // See EpisodeItemListAdapter.notifyItemChangedCompat.
        notifyItemChanged(currentChapterIndex, "foo")
    }

    fun getItem(position: Int): Chapter {
        return media!!.chapters[position]
    }

    interface Callback {
        fun onPlayChapterButtonClicked(position: Int)
    }
}