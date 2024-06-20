package allen.town.podcast.ads

import allen.town.focus_common.util.Timber
import allen.town.podcast.BuildConfig
import allen.town.podcast.MyApp
import java.util.*

object AdUtils {
    private val adRamdon = Random()

    @JvmStatic
    fun getBannerAdUnitId(): String {
        return  if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-6256483973048476/1337968256"
    }


    @JvmStatic
    fun showAdInArticle(): Boolean {
        //概率算法，每个值的概率都是1/5，即每5次浏览文章显示一次广告
        val showRandom = adRamdon.nextInt(5) == 0
        Timber.v("showAdInArticle $showRandom")
        return !MyApp.instance.isAdBlockUser() && showRandom
    }


}