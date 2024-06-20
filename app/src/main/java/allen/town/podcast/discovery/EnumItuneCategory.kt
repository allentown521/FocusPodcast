package allen.town.podcast.discovery

import allen.town.podcast.R

enum class EnumItuneCategory(val code: Int, val strId: Int) {
    Podcast_All(0, R.string.all), Podcast_Art(1301, R.string.arts), Podcast_Business(
        1321,
        R.string.business
    ),
    Podcast_Comedy(1303, R.string.comedy), Podcast_Education(
        1304,
        R.string.education
    ),
    Podcast_Fiction(1483, R.string.fiction), Podcast_Government(
        1511,
        R.string.government
    ),
    Podcast_Health(1512, R.string.health_fitness), Podcast_History(
        1487,
        R.string.genre_type_hisotry
    ),
    Podcast_KidsFamily(1305, R.string.kids), Podcast_Leisure(1502, R.string.leisure), Podcast_Music(
        1310,
        R.string.music
    ),
    Podcast_News(1489, R.string.news), Podcast_Religion(
        1314,
        R.string.region
    ),
    Podcast_Science(1533, R.string.science), Podcast_Society(
        1324,
        R.string.society
    ),
    Podcast_Sports(1545, R.string.sports), Podcast_Technology(1318, R.string.tech), Podcast_Film(
        1309,
        R.string.tv
    ),
    Podcast_TrueCrime(1488, R.string.true_crime);

}