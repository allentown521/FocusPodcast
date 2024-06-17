package allen.town.podcast.view;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import allen.town.focus_common.extensions.ActivityThemeExtensionsUtils;
import allen.town.podcast.R;
import allen.town.podcast.activity.MainActivity;

public class FeedItemListToolbarIconTintHelper implements AppBarLayout.OnOffsetChangedListener {
    private final Context context;
    private final CollapsingToolbarLayout collapsingToolbar;
    private final Toolbar toolbar;
    private boolean isTinted = false;

    public FeedItemListToolbarIconTintHelper(Context context, Toolbar toolbar, CollapsingToolbarLayout collapsingToolbar) {
        this.context = context;
        this.collapsingToolbar = collapsingToolbar;
        this.toolbar = toolbar;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        boolean tint  = (collapsingToolbar.getHeight() + offset) > (collapsingToolbar.getMinimumHeight());
        if (isTinted != tint) {
            isTinted = tint;
            updateTint();
        }
    }

    public void updateTint() {
        if (isTinted) {
            safeSetColorFilter(toolbar.getNavigationIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            safeSetColorFilter(toolbar.getOverflowIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            safeSetColorFilter(toolbar.getCollapseIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            Drawable refreshDrawable = ContextCompat.getDrawable(context,R.drawable.ic_refresh);
            refreshDrawable.setColorFilter(new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            toolbar.getMenu().findItem(R.id.refresh_item)
                    .setIcon(refreshDrawable);

            Drawable shareDrawable = ContextCompat.getDrawable(context,R.drawable.ic_share);
            shareDrawable.setColorFilter(new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            toolbar.getMenu().findItem(R.id.share_item)
                    .setIcon(shareDrawable);
            ActivityThemeExtensionsUtils.setLightStatusBar((MainActivity)context,false);
        } else {
//            doTint(context);
            safeSetColorFilter(toolbar.getNavigationIcon(), null);
            safeSetColorFilter(toolbar.getOverflowIcon(), null);
            safeSetColorFilter(toolbar.getCollapseIcon(), null);
            Drawable refreshDrawable = ContextCompat.getDrawable(context,R.drawable.ic_refresh);
            refreshDrawable.setColorFilter(null);
            toolbar.getMenu().findItem(R.id.refresh_item)
                    .setIcon(refreshDrawable);

            Drawable shareDrawable = ContextCompat.getDrawable(context,R.drawable.ic_share);
            shareDrawable.setColorFilter(null);
            toolbar.getMenu().findItem(R.id.share_item)
                    .setIcon(shareDrawable);
            ActivityThemeExtensionsUtils.setLightStatusBar((MainActivity)context,true);
        }
    }

    private void safeSetColorFilter(Drawable icon, PorterDuffColorFilter filter) {
        if (icon != null) {
            icon.setColorFilter(filter);
        }
    }

}
