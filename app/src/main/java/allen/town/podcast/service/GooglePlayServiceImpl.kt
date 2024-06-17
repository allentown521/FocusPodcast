package allen.town.podcast.service

import allen.town.core.service.GooglePayService
import allen.town.podcast.BuildConfig
import android.content.Context
import com.wyjson.router.annotation.Service

/**
 * An exception not found during runtime is caused by different modules having the same grouping. For example, AModule defines @Route(path = “/module/a”)，
 * BModule also defines @Route(path = “/module/b”), which will cause this problem
 */
@Service(remark = "/app/pay/googleplay_service")
class GooglePlayServiceImpl : GooglePayService {
    override fun getMonthId(): String {
        return  "focus_reader_sub_monthly"
    }

    override fun getQuarterlyId(): String {
        return  "focus_reader_sub_quaterly"
    }

    override fun getYearlyId(): String {
        return  "focus_reader_sub_yearly"
    }

    override fun getWeeklyId(): String {
        return  "focus_reader_sub_weekly"
    }

    override fun getRemoveAdsId(): ArrayList<String> {
        return arrayListOf("focus_reader_remove_ads")
    }

    override fun getPublicKey(): String {
        return  BuildConfig.GOOGLE_PAY_PUBLIC_KEY
    }

    override fun init() {

    }

}