package allen.town.podcast.statistics

class StatisticsData(values: FloatArray) {
    val sum: Float



    init {
        var valueSum = 0f
        for (datum in values) {
            valueSum += datum
        }
        sum = valueSum
    }
}