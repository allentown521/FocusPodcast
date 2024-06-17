package allen.town.podcast.ads.service

import allen.town.core.service.AdService
import allen.town.podcast.BuildConfig
import allen.town.podcast.MyApp
import com.wyjson.router.annotation.Service
import com.zh.pocket.PocketSdk


@Service(remark = "/app/ad/ad_service")
class AdServiceImpl : AdService {
    override fun getOpenAdUnitId(): String {
        return "55917"
    }

    override fun getRewardedAdUnitId(): String {
        return "55953"
    }

    override fun getBannerAdUnitId(): String {
        return "55915"
    }

    override fun getInterstitialAdUnitId(): String {
        return "56267"
    }

    override fun init() {
        PocketSdk.initSDK(MyApp.instance, BuildConfig.FLAVOR, BuildConfig.POCKET_AD_APP_ID)
//        TTAdSdk.getAdManager().themeStatus = 1
    }

}