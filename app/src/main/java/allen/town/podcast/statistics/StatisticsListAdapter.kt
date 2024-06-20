package allen.town.podcast.statistics

import allen.town.podcast.R
import allen.town.podcast.core.glide.ApGlideSettings
import allen.town.podcast.core.storage.StatisticsItem
import allen.town.podcast.core.util.LottieHelper.getRandomLottieFileName
import android.content.Context
import android.util.Pair
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

/**
 * Parent Adapter for the playback and download statistics list.
 */
abstract class StatisticsListAdapter protected constructor(protected val context: Context?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var statisticsData: List<StatisticsItem>? = null
    protected var pieChartData: StatisticsData? = null
    override fun getItemCount(): Int {
        return statisticsData!!.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_FEED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.statistics_listitem_total, parent, false))
        } else StatisticsHolder(inflater.inflate(R.layout.statistics_listitem, parent, false))
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            val holder = h as HeaderHolder
            val pair = headerValue
            holder.totalTime1.text = pair.first
            holder.totalTime2.text = pair.second
            //随机播放一个动画
            holder.lottieAnimationView.setAnimation(getRandomLottieFileName())
        } else {
            val holder = h as StatisticsHolder
            val statsItem = statisticsData!![position - 1]
            Glide.with(context!!)
                .load(statsItem.feed.imageUrl)
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
            holder.title.text = statsItem.feed.title
            onBindFeedViewHolder(holder, statsItem)
        }
    }

    fun update(statistics: List<StatisticsItem>) {
        statisticsData = statistics
        pieChartData = generateChartData(statistics)
        notifyDataSetChanged()
    }

    internal class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var totalTime1: TextView
        var totalTime2: TextView
        var lottieAnimationView: LottieAnimationView

        init {
            totalTime1 = itemView.findViewById(R.id.total_time1)
            totalTime2 = itemView.findViewById(R.id.total_time2)
            lottieAnimationView = itemView.findViewById(R.id.lottie_play_item)
        }
    }

    class StatisticsHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var image: ImageView
        var title: TextView
        var value: TextView

        init {
            image = itemView.findViewById(R.id.imgvCover)
            title = itemView.findViewById(R.id.txtvTitle)
            value = itemView.findViewById(R.id.txtvValue)
        }
    }

    protected abstract val headerCaption: String
    protected abstract val headerValue: Pair<String, String>
    protected abstract fun generateChartData(statisticsData: List<StatisticsItem>): StatisticsData
    protected abstract fun onBindFeedViewHolder(holder: StatisticsHolder, item: StatisticsItem)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FEED = 1
    }
}