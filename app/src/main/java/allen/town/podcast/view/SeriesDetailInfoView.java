package allen.town.podcast.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.faltenreich.skeletonlayout.SkeletonLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;

import allen.town.focus_common.util.ImageUtils;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.util.Util;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import code.name.monkey.appthemehelper.ThemeStore;
import code.name.monkey.appthemehelper.util.ATHUtil;
import allen.town.podcast.R;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedFunding;
import allen.town.podcast.util.AnimationsUtils;
import allen.town.podcast.util.MovementCheck;


public class SeriesDetailInfoView extends FrameLayout {
    private static final int ANIMATION_DURATION_EXPAND_COLLAPSE = 250;
    private static final int DELAY_UPDATE_TRANSITION_RUNNING = 20;
    public static final int INFO_EXPANDO_BTN_STYLE_ICON = 0;
    public static final int INFO_EXPANDO_BTN_STYLE_PILL = 1;
    private static final String TAG = "SeriesDetailInfoView";
    private int mCollapsedHeight;

    @BindView(R.id.series_info_view_container)
    public FrameLayout mContainer;
    @BindView(R.id.content_placeholder)
    public SkeletonLayout mContentPlaceholder;
    private String mDescription;
    private String mDescriptionHtmlString;
    @BindView(R.id.description)
    public TextView mDescriptionView;

    @BindView(R.id.txtvPodcastUrl)
    public TextView mtxtvPodcastUrlView;

    @BindView(R.id.txtvFeedUrl)
    public TextView mtxtvFeedUrlView;

    @BindView(R.id.lblSupport)
    public TextView mtxtvlblSupportView;

    @BindView(R.id.txtvFundingUrl)
    public TextView mtxtvFundingUrlView;

    private boolean mEpisodesLoaded;
    @BindView(R.id.fade_overlay)
    public View mFadeOverlay;
    @BindView(R.id.fade_overlay_container)
    public View mFadeOverlayContainer;
    private boolean mIsAllDataLoaded;
    private boolean mIsChromeCustomTabsSupported;
    private boolean mIsCollapsed;
    private boolean mIsSubscribed;
    private boolean mIsTagsViewCollapsed;
    private boolean mInfoDataLoaded = false;
    @BindView(R.id.podchaser_rating_and_links_container)
    public LinearLayout mPodchaserRatingAndLinksContainer;
    public FragmentActivity mActivity;
    private int mScreenWidth;
    private boolean isLocalFeed = false;

    @BindView(R.id.copy_feed_url_item)
    public View mCopyFeedUrlIv;
    @BindView(R.id.copy_podcast_url_item)
    public View mCopyPodcastUrlIv;

    @BindView(R.id.feed_url_l)
    public View mFeedUrlL;
    @BindView(R.id.podcast_url_l)
    public View mPodcastUrlL;

    public SeriesDetailInfoView(@NonNull Context context) {
        super(context);
    }

    public SeriesDetailInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SeriesDetailInfoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SeriesDetailInfoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override // android.view.View
    public void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this, this);
        this.mCollapsedHeight = Util.dp2Px(getContext(), 144);
        afterViews();
    }

    public boolean isAllDataLoaded() {
        return this.mIsAllDataLoaded;
    }

    public boolean isCollapsed() {
        return this.mIsCollapsed;
    }

    public void setEpisodesLoaded(boolean z) {
        if (z != this.mEpisodesLoaded) {
            this.mEpisodesLoaded = z;
            checkAllLoaded("setEpisodesLoaded");
        }
    }

    private Feed mFeed;

    public void setData(Feed feed, FragmentActivity fragmentActivity) {
        mFeed = feed;

            mContentPlaceholder.setVisibility(VISIBLE);
            mContentPlaceholder.showSkeleton();

            this.mDescription = feed.getDescription();
            this.mIsSubscribed = feed.isSubscribed();
            String str10 = null;
            Spanned fromHtml = !TextUtils.isEmpty(feed.getDescription()) ? Html.fromHtml(feed.getDescription()) : null;
            if (!TextUtils.isEmpty(feed.getDescription()) && fromHtml != null) {
                str10 = fromHtml.toString().trim();
            }
            this.mDescriptionHtmlString = str10;
            this.mActivity = fragmentActivity;
            setDescription(!this.mIsCollapsed);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            fragmentActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            this.mScreenWidth = displayMetrics.widthPixels;
            this.mInfoDataLoaded = true;
            checkAllLoaded("setData2");

            if (TextUtils.isEmpty(feed.getLink()) || !Patterns.WEB_URL.matcher(feed.getLink()).matches()) {
                mPodcastUrlL.setVisibility(GONE);
            }

            if (TextUtils.isEmpty(feed.getDownload_url()) || !Patterns.WEB_URL.matcher(feed.getDownload_url()).matches()) {
                mFeedUrlL.setVisibility(GONE);
            }
            mtxtvPodcastUrlView.setText(feed.getLink());
            mtxtvFeedUrlView.setText(feed.getDownload_url());

            mCopyFeedUrlIv.setOnClickListener(new CopyListener(feed.getDownload_url()));
            mCopyPodcastUrlIv.setOnClickListener(new CopyListener(feed.getLink()));

            this.mtxtvFeedUrlView.setMovementMethod(MovementCheck.getInstance());
            this.mtxtvFeedUrlView.setLinkTextColor(ThemeStore.accentColor(getContext()));

            this.mtxtvPodcastUrlView.setMovementMethod(MovementCheck.getInstance());
            this.mtxtvPodcastUrlView.setLinkTextColor(ThemeStore.accentColor(getContext()));

            this.mtxtvFundingUrlView.setMovementMethod(MovementCheck.getInstance());
            this.mtxtvFundingUrlView.setLinkTextColor(ThemeStore.accentColor(getContext()));

            if (feed.getPaymentLinks() == null || feed.getPaymentLinks().size() == 0) {
                mtxtvlblSupportView.setVisibility(View.GONE);
                mtxtvFundingUrlView.setVisibility(View.GONE);
            } else {
                mtxtvlblSupportView.setVisibility(View.VISIBLE);
                mtxtvFundingUrlView.setVisibility(View.VISIBLE);
                ArrayList<FeedFunding> fundingList = feed.getPaymentLinks();
                StringBuilder str = new StringBuilder();
                HashSet<String> seen = new HashSet<String>();
                //When multiple funding tags reference the same URL, display the one with longer title，antennapod有此修改我没改感觉没啥必要
                for (FeedFunding funding : fundingList) {
                    if (seen.contains(funding.url)) {
                        continue;
                    }
                    seen.add(funding.url);
                    str.append(funding.content.isEmpty()
                            ? getContext().getResources().getString(R.string.support_podcast)
                            : funding.content).append(" - ").append(funding.url);
                    str.append("\n");
                }
                str = new StringBuilder(StringUtils.trim(str.toString()));
                mtxtvFundingUrlView.setText(str.toString());
            }


    }

    private void checkAllLoaded(String str) {
        boolean z = this.mInfoDataLoaded;
        boolean z4 = this.mEpisodesLoaded;
        if (z && z4) {
            this.mContentPlaceholder.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(this.mDescriptionView.getText())) {
                this.mDescriptionView.setVisibility(View.VISIBLE);
            }
            this.mPodchaserRatingAndLinksContainer.setVisibility(View.VISIBLE);
            if (this.mIsCollapsed && this.mFadeOverlayContainer.getVisibility() != View.VISIBLE) {
                this.mFadeOverlayContainer.setVisibility(View.VISIBLE);
            }
            this.mIsAllDataLoaded = true;
        }
    }

    public void setDescription(boolean z) {
        this.mDescriptionView.setMovementMethod(z ? MovementCheck.getInstance() : null);
        this.mDescriptionView.setAutoLinkMask(z ? 1 : 0);
        if (!TextUtils.isEmpty(this.mDescriptionHtmlString)) {
            this.mDescriptionView.setText(this.mDescriptionHtmlString);
            this.mDescriptionView.setLinkTextColor(ThemeStore.accentColor(getContext()));
            return;
        }
        this.mDescriptionView.setVisibility(View.GONE);
    }

    public int getCollapsedHeight() {
        return this.mCollapsedHeight;
    }

    public int getViewHeight(boolean z) {
        if (this.mIsCollapsed) {
            return this.mCollapsedHeight;
        }
        int measuredHeight = getMeasuredHeight();
        if (!z) {
            return measuredHeight;
        }
        this.mContainer.measure(View.MeasureSpec.makeMeasureSpec(this.mScreenWidth, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return this.mContainer.getMeasuredHeight() > measuredHeight ? this.mContainer.getMeasuredHeight() : measuredHeight;
    }

    public void clearDescriptionSelection() {
        TextView textView = this.mDescriptionView;
        if (textView != null) {
            try {
                textView.clearFocus();
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    public void animateFadeOverlayExpandButton(boolean z, int i) {
        if (this.mIsCollapsed) {
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.0f, z ? 0.0f : 1.0f, z ? 1.0f : 0.0f, 1, 0.0f, 1, 1.0f);
            scaleAnimation.setDuration(i);
            this.mFadeOverlayContainer.startAnimation(scaleAnimation);
        }
    }

    private void afterViews() {
        mFadeOverlay.setBackground(ImageUtils.getColoredDrawable(getContext(), R.drawable.gradient_white, ATHUtil.resolveColor(getContext(), R.attr.colorSurface)));
        this.mDescriptionView.setVisibility(View.GONE);
        mPodchaserRatingAndLinksContainer.setVisibility(View.GONE);

    }

    public void expandCollapseContent(boolean z, boolean z2) {
        int i = GONE;
        if (z2) {
            if (z) {
                int i3 = this.mScreenWidth;
                if (i3 > 0) {
                    this.mContainer.measure(View.MeasureSpec.makeMeasureSpec(i3, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                } else {
                    this.mContainer.measure(0, 0);
                }
                int measuredHeight = this.mContainer.getMeasuredHeight();
                ValueAnimator ofInt = ValueAnimator.ofInt(this.mCollapsedHeight, measuredHeight);
                ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: fm.player.ui.customviews.SeriesDetailInfoView.5
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        SeriesDetailInfoView.this.getLayoutParams().height = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                        SeriesDetailInfoView.this.requestLayout();
                    }
                });
                ofInt.addListener(new Animator.AnimatorListener() { // from class: fm.player.ui.customviews.SeriesDetailInfoView.6
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        SeriesDetailInfoView.this.getLayoutParams().height = -2;
                        SeriesDetailInfoView.this.requestLayout();
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animator) {
                        SeriesDetailInfoView.this.setDescription(true);
                    }
                });
                ofInt.setDuration(250L);
                ofInt.setInterpolator(new AccelerateDecelerateInterpolator());
                ofInt.start();
            } else {
                ValueAnimator ofInt2 = ValueAnimator.ofInt(this.mContainer.getMeasuredHeight(), this.mCollapsedHeight);
                ofInt2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: fm.player.ui.customviews.SeriesDetailInfoView.7
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        SeriesDetailInfoView.this.getLayoutParams().height = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                        SeriesDetailInfoView.this.requestLayout();
                    }
                });
                ofInt2.addListener(new Animator.AnimatorListener() { // from class: fm.player.ui.customviews.SeriesDetailInfoView.8
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animator) {
                        SeriesDetailInfoView.this.setDescription(false);
                    }
                });
                ofInt2.setDuration(250L);
                ofInt2.setInterpolator(new AccelerateDecelerateInterpolator());
                ofInt2.start();
            }
            if (z) {
                AnimationsUtils.fadeOut(this.mFadeOverlayContainer, 8, 250, 0);
            } else {
                AnimationsUtils.fadeIn(this.mFadeOverlayContainer, 250);
            }
        } else {
            getLayoutParams().height = z ? -2 : this.mCollapsedHeight;
            requestLayout();
            View view = this.mFadeOverlayContainer;
            if (!z && this.mIsAllDataLoaded) {
                i = VISIBLE;
            }
            view.setVisibility(i);
            setDescription(z);
        }
        this.mIsCollapsed = !z;
        this.mIsTagsViewCollapsed = !z;
    }

    @OnClick({R.id.description_container, R.id.description, R.id.fade_overlay_container})
    public void toggleCollapseMode() {
        if (!this.mIsSubscribed || this.mIsCollapsed) {
            expandCollapseContent(this.mIsCollapsed, true);
        }
    }


    class CopyListener implements View.OnClickListener {

        private String copyStr;

        public CopyListener(String str) {
            copyStr = str;
        }

        @Override
        public void onClick(View v) {
            if (copyStr != null) {
                ClipData clipData = ClipData.newPlainText(copyStr, copyStr);
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(clipData);
                if (Build.VERSION.SDK_INT <= 32) {
                    TopSnackbarUtil.showSnack(mActivity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT);
                }
            }
        }
    }
}
