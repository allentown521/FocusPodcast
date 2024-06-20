package allen.town.podcast.service

import allen.town.core.service.PayService
import android.content.Context
import allen.town.podcast.MyApp
import com.wyjson.router.annotation.Service


@Service(remark = "/app/pay/pay_service")
class PayServiceImpl : PayService {


    override fun init() {

    }

    override fun isAdBlocker(): Boolean {
        return MyApp.instance.isAdBlockUser()
    }

    override fun isAliPay(): Boolean {
        return MyApp.instance.isAlipay
    }

    override fun isPurchase(context: Context?,gotoPro:Boolean): Boolean {
        return MyApp.instance.checkSupporter(context,gotoPro)
    }

    override fun setPurchase(purchase: Boolean) {
        MyApp.instance.setSubSupporter(purchase)
    }

    override fun setRemoveAdPurchase(purchase: Boolean) {
        MyApp.instance.setAdSupporter(purchase)
    }


    override fun purchaseDbName(): String {
        return "purchase.db"
    }

    override fun dbVersion(): Int {
        return 27
    }

}