package allen.town.podcast.statistics

import allen.town.podcast.R


class SubscriptionStatisticsFulltimeFragment : SubscriptionStatisticsBaseFragment() {
    override val timeFrom: Long
        get() = 0
    override val timeTo: Long
        get() = System.currentTimeMillis()
    override val headStrRes: Int
        get() = R.string.statistics_filter_all_time

}
