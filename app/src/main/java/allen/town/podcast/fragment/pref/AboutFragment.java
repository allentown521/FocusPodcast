package allen.town.podcast.fragment.pref;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.List;

import allen.town.focus_common.util.Intents;
import allen.town.focus_common.util.PackageUtils;
import allen.town.focus_common.views.AccentMaterialDialog;
import allen.town.podcast.MyApp;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import code.name.monkey.appthemehelper.ThemeStore;
import allen.town.podcast.BuildConfig;
import allen.town.podcast.R;
import allen.town.podcast.core.util.IntentUtils;

public class AboutFragment extends AppCompatDialogFragment {
    @BindView(R.id.credits)
    TextView credits;
    @BindView(R.id.icon)
    ImageView icon;
    @BindView(R.id.privacy_policy)
    RelativeLayout policy;
    @BindView(R.id.version_text)
    TextView version;
    @BindView(R.id.check_upgrade)
    View checkUpradeView;
    @BindView(R.id.opensource)
    View opensourceView;
    @BindViews({R.id.twitter_image, R.id.rate_image, R.id.privacy_policy_image,R.id.more_apps_of_us_image,R.id.check_upgrade_image
            ,R.id.share_app_image,R.id.opensource_image})
    List<ImageView> styleButtons;

    @OnClick(R.id.share_app)
    public void shareMyApp() {
        Intents.shareText(getContext(), getString(R.string.share_to_friends_tip, PackageUtils.getAppName(getContext())) + " \n" +
                        (MyApp.getInstance().isAlipay() ? "https://www.pgyer.com/focuspodcast" : "https://play.google.com/store/apps/details?id=allen.town.focus.podcast"),
                "");
    }

    @OnClick(R.id.privacy_policy)
    public void showPrivacyPolicy() {
        IntentUtils.openInBrowser(getActivity(), "https://sites.google.com/view/focus-podcast-privacy-policy/");
    }

    @OnClick(R.id.opensource)
    public void showOpensource() {
        IntentUtils.openInBrowser(getActivity(), "https://github.com/allentown521/FocusPodcast/");
    }

    @OnClick(R.id.twitter_follow_me)
    public void followMe() {
        String url = "https://twitter.com/allentown521";
        Uri parse = Uri.parse(url);
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(parse);

        if (!Intents.startActivity(getActivity(), intent)) {
            IntentUtils.openInBrowser(getActivity(), url);
        }

    }

    @OnClick(R.id.rate_me)
    public void rateMe() {
        Uri parse = Uri.parse("market://details?id=allen.town.focus.podcast");
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(parse);
        Intents.startActivity(getContext(), intent);

    }

    @OnClick(R.id.more_apps_of_us)
    public void moreAppsOfUs() {
        Uri parse = Uri.parse("https://play.google.com/store/apps/dev?id=8458616364286916829");
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(parse);
        Intents.startActivity(getContext(), intent);

    }

    @OnClick(R.id.check_upgrade)
    public void checkUpgrade() {
        Uri parse = Uri.parse("https://www.pgyer.com/focuspodcast");
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(parse);
        Intents.startActivity(getContext(), intent);

    }

    @Override
    // android.support.v7.app.AppCompatDialogFragment, android.support.v4.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        View inflate = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_about, (ViewGroup) null);
        ButterKnife.bind(this, inflate);
        this.version.setText(getString(R.string.version, BuildConfig.VERSION_NAME));
        if(!MyApp.getInstance().isAlipay()){
            checkUpradeView.setVisibility(View.GONE);
        }
        if(!MyApp.getInstance().isDroid()){
            opensourceView.setVisibility(View.GONE);
        }
        butterknife.ViewCollections.run(this.styleButtons, (view, i) -> ((ImageView) view)
                .setColorFilter(ThemeStore.accentColor(getContext()), PorterDuff.Mode.SRC_IN));
        return new AccentMaterialDialog(getContext(),R.style.MaterialAlertDialogTheme).setView(inflate).create();
    }
}

