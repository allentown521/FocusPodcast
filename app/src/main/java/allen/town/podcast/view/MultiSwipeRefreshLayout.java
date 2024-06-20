package allen.town.podcast.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import code.name.monkey.appthemehelper.ThemeStore;

public class MultiSwipeRefreshLayout extends SwipeRefreshLayout {
    private boolean disableIntercept = false;
    private View[] swipeableChildren;

    public MultiSwipeRefreshLayout(Context context) {
        super(context);
    }

    public MultiSwipeRefreshLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        //设置为强调色
        setColorSchemeColors(ThemeStore.accentColor(context));
    }

    public void setSwipeableChildren(int... iArr) {
        if (iArr != null) {
            this.swipeableChildren = new View[iArr.length];
            int i = 0;
            while (i < iArr.length) {
                View findViewById = findViewById(iArr[i]);
                if (findViewById != null) {
                    this.swipeableChildren[i] = findViewById;
                    i++;
                } else {
                    throw new IllegalArgumentException("View not found " + iArr[i]);
                }
            }
        }
    }

    @Override // android.support.v4.widget.SwipeRefreshLayout
    public boolean canChildScrollUp() {
        View[] viewArr = this.swipeableChildren;
        if (viewArr != null) {
            for (View view : viewArr) {
                if (view.isShown() && ViewCompat.canScrollVertically(view, -1)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override // android.support.v4.view.NestedScrollingParent, android.support.v4.widget.SwipeRefreshLayout
    public void onNestedScroll(View view, int i, int i2, int i3, int i4) {
        if (!isEnabled() || isRefreshing()) {
            dispatchNestedScroll(i, i2, i3, i4, null);
        } else {
            super.onNestedScroll(view, i, i2, i3, i4);
        }
    }

    @Override // android.support.v4.widget.SwipeRefreshLayout
    public void requestDisallowInterceptTouchEvent(boolean z) {
        this.disableIntercept = z;
        super.requestDisallowInterceptTouchEvent(z);
    }

    @Override // android.support.v4.widget.SwipeRefreshLayout
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int actionMasked = MotionEventCompat.getActionMasked(motionEvent);
        if (actionMasked == 6 || actionMasked == 1 || actionMasked == 3) {
            this.disableIntercept = false;
        }
        if (!this.disableIntercept) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        return false;
    }
}
