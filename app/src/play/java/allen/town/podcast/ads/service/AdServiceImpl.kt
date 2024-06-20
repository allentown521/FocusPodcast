package allen.town.podcast.ads.service

import allen.town.core.service.AdService
import allen.town.focus_common.BuildConfig
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.Timber
import allen.town.podcast.MyApp
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.wyjson.router.annotation.Service


@Service(remark = "/app/ad/ad_service")
class AdServiceImpl : AdService {
    override fun getOpenAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9257395921" else BasePreferenceUtil.admobOpenAdId
            ?: "ca-app-pub-6256483973048476/7814332954"
    }

    override fun getRewardedAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else BasePreferenceUtil.admobRewardAdId
            ?: "ca-app-pub-6256483973048476/5407661461"
    }

    override fun getBannerAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else BasePreferenceUtil.admobBannerAdId
            ?: "ca-app-pub-6256483973048476/7833947971"
    }

    override fun getInterstitialAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else BasePreferenceUtil.admobInterstitialAdId
            ?: "ca-app-pub-6256483973048476/5543392833"
    }


    override fun init() {
        MobileAds.initialize(
            MyApp.instance!!
        ) { Timber.d("onInitializationComplete") }
    }

}