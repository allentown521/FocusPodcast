package allen.town.podcast.statistics

import allen.town.focus_common.util.BaseDateUtils
import allen.town.podcast.R

class SubscriptionStatisticsYearFragment : SubscriptionStatisticsBaseFragment() {
    override val timeFrom: Long
        get() = BaseDateUtils.getDayBefore(366)
    override val timeTo: Long
        get() = BaseDateUtils.getDayBefore(1)
    override val headStrRes: Int
        get() = R.string.statistics_filter_past_year
}