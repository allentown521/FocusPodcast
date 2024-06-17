package allen.town.podcast

import allen.town.focus_common.http.bean.LeanUpgradeBean
import allen.town.focus_common.util.PackageUtils
import allen.town.focus_common.util.Timber
import allen.town.podcast.core.pref.Prefs
import android.app.Activity
import android.content.Context
import com.azhon.appupdate.listener.OnButtonClickListener
import com.azhon.appupdate.manager.DownloadManager
import com.baidu.mobstat.StatService
import java.util.*

object ProductWrap {
    //其他渠道弹窗提示
    @JvmStatic
    fun doCheck(context: Context?, leanUpgradeBean: LeanUpgradeBean) {
        DownloadManager.Builder(context as Activity)
            .apkName(PackageUtils.getAppName(context) + ".apk")
            .apkUrl(leanUpgradeBean.download_url!!)
            .smallIcon(R.mipmap.ic_launcher) //                                            .setShowNewerToast(true)
            .apkVersionCode(leanUpgradeBean.version_code)
            .apkVersionName(leanUpgradeBean.version_name!!)
            .apkSize(leanUpgradeBean.app_size!!)
            .forcedUpgrade(leanUpgradeBean.force_upgrade)
            .apkDescription(if ("zh" == Locale.getDefault().language) leanUpgradeBean.change_log_cn!! else leanUpgradeBean.change_log_en!!) //                .setApkMD5("DC501F04BBAA458C9DC33008EFED5E7F")
            .onButtonClickListener(object : OnButtonClickListener{
                override fun onButtonClick(id: Int) {
                    if (id == OnButtonClickListener.CANCEL) {
                        //点击取消说明用户不希望更新这个版本记录下这个新版本的版本号
                        Prefs.versionCode = leanUpgradeBean.version_code
                    }
                }

            })
            .build()
            .download()

    }

    fun setBaiduStat(context: Context?) {
        try {
//                只统计支付宝渠道的用户
            StatService.setAppKey("69772a8fe3")
            if (BuildConfig.DEBUG) {
                StatService.setDebugOn(true)
            }
            //初始化common库
            StatService.setAppChannel(context, BuildConfig.FLAVOR, true)
            StatService.start(context)
            Timber.d("init baidu")
        } catch (e: Exception) {
            Timber.e(e, "init baidu")
        }
    }
}