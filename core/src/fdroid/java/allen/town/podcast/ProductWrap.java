package allen.town.podcast;

import android.content.Context;
import android.widget.Toast;

import allen.town.focus_common.http.bean.LeanUpgradeBean;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.podcast.core.R;
import allen.town.podcast.core.pref.Prefs;

public class ProductWrap {
    //google检查到更新提示即可
    public static void doCheck(Context context, LeanUpgradeBean leanUpgradeBean) {
        //每个版本的更新提示只有1次
        Prefs.setVersionCode(leanUpgradeBean.getVersion_code());
        TopSnackbarUtil.showSnack(context, R.string.upgrade_tip, Toast.LENGTH_LONG);
    }

    public static void setBaiduStat(Context context) {

    }
}
