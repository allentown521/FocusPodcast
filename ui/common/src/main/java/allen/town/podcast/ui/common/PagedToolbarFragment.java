package allen.town.podcast.ui.common;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import allen.town.focus_common.util.DoubleClickBackToContentTopListener;

/**
 * Fragment with a ViewPager where the displayed items influence the top toolbar's menu.
 * All items share the same general menu items and are just allowed to show/hide them.
 */
public abstract class PagedToolbarFragment extends Fragment implements DoubleClickBackToContentTopListener.IBackToContentTopView {
    public Toolbar getToolbar() {
        return toolbar;
    }

    protected Toolbar toolbar;
    protected ViewPager2 viewPager;

    /**
     * Invalidate the toolbar menu if the current child fragment is visible.
     *
     * @param child The fragment to invalidate
     */
    public void invalidateOptionsMenuIfActive(@NonNull Fragment child) {
        Fragment visibleChild = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (visibleChild == child) {
            visibleChild.onPrepareOptionsMenu(toolbar.getMenu());
        }
    }

    @Override
    public void backToContentTop() {
        Fragment child = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (child instanceof DoubleClickBackToContentTopListener.IBackToContentTopView) {
            ((DoubleClickBackToContentTopListener.IBackToContentTopView) child).backToContentTop();
        }
    }

    protected void setupPagedToolbar(Toolbar toolbar, ViewPager2 viewPager) {
        this.toolbar = toolbar;
        this.viewPager = viewPager;
        toolbar.setOnClickListener(new DoubleClickBackToContentTopListener(PagedToolbarFragment.this));
        toolbar.setOnMenuItemClickListener(item -> {
            if (this.onOptionsItemSelected(item)) {
                return true;
            }
            Fragment child = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
            if (child != null) {
                return child.onOptionsItemSelected(item);
            }
            return false;
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment child = getChildFragmentManager().findFragmentByTag("f" + position);
                if (child != null) {
                    child.onPrepareOptionsMenu(toolbar.getMenu());
                }
            }
        });
    }
}
