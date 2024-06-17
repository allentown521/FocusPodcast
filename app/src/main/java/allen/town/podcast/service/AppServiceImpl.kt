package allen.town.podcast.service

import allen.town.core.service.AppService
import allen.town.podcast.MyApp
import android.content.Context
import com.wyjson.router.annotation.Service


@Service(remark = "/app/common/app_service")
class AppServiceImpl : AppService {
    override fun isForeground(): Boolean {
        return !MyApp.instance.isAppRunningBackground()
    }


    override fun init() {

    }


}