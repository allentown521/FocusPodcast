package allen.town.podcast.statistics

import allen.town.podcast.activity.MainActivity
import allen.town.podcast.activity.MainActivity.Companion.getIntentToOpenFeedWithId
import allen.town.podcast.core.storage.StatisticsItem
import allen.town.podcast.core.util.Converter
import android.util.Pair
import android.view.View
import androidx.fragment.app.Fragment

/**
 * Adapter for the playback statistics list.
 */
class PlaybackStatisticsListAdapter(private val fragment: Fragment) : StatisticsListAdapter(
    fragment.context
) {
    private var headStrRes = 0
    fun setTimeFilter(headStrRes: Int) {
        this.headStrRes = headStrRes
    }

    protected override val headerCaption: String
        protected get() = context!!.getString(headStrRes)
    protected override val headerValue: Pair<String, String>
        protected get() = Converter.shortLocalizedDuration(context, pieChartData!!.sum.toLong())

    override fun generateChartData(statisticsData: List<StatisticsItem>): StatisticsData {
        val dataValues = FloatArray(statisticsData.size)
        for (i in statisticsData.indices) {
            val item = statisticsData[i]
            dataValues[i] = item.timePlayed.toFloat()
        }
        return StatisticsData(dataValues)
    }

    override fun onBindFeedViewHolder(holder: StatisticsHolder, statsItem: StatisticsItem) {
        val time = statsItem.timePlayed
        val pair = Converter.shortLocalizedDuration(context, time)
        holder.value.text = pair.first.toString() + " " + pair.second
        holder.itemView.setOnClickListener { v: View? ->
            val intent = getIntentToOpenFeedWithId(holder.itemView.context, statsItem.feed.id)
            intent.putExtra(MainActivity.EXTRA_STARTED_FROM_SEARCH, true)
            fragment.requireActivity().startActivity(intent)
        }
    }
}