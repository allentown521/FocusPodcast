package allen.town.podcast.core.util

import java.util.*

object LottieHelper {
    private val lottieAmiList: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("lottie_podcast-girl.json")
            add("lottie_listening-music1.json")
            add("lottie_music-man.json")
            add("lottie_relaxing-bath.json")
            add("20546-i-stay-at-home.json")
            add("52650-relax.json")
            add("69484-relax.json")
            add("17241-relaxing-time.json")
            add("41371-bicycles-wind-turbines.json")
        }
    }

    @JvmStatic
    fun getRandomLottieFileName():String{
        //随机播放一个动画
        val size: Int = lottieAmiList.size
        return lottieAmiList[Random().nextInt(size)]
    }
}