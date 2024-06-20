package allen.town.podcast.util;

import android.view.View;
import android.widget.FrameLayout;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/* renamed from: fm.player.ui.utils.BalancedUIHelper */
/* loaded from: classes3.dex */
public class BalancedUIHelper {
    private static final String TAG = "BalancedUIHelper";

    public static Pair<ArrayList<ArrayList<View>>, Boolean> balanceViesSeriesInfo(ArrayList<View> arrayList, int i, int i2, int i3, boolean z) {
        ArrayList<ArrayList<View>> arrayList2 = new ArrayList<>();
        boolean z2 = false;
        int i4 = 1;
        while (!z2 && i4 <= i3) {
            arrayList2 = balanceViewsAllowReorder(arrayList, i, i4);
            if (longestRowWidth(arrayList2, i) < i2) {
                z2 = true;
            } else {
                i4++;
            }
        }
        if (!z2) {
            if (longestRowWidth(arrayList2, i) < i2 * 1.25f) {
                arrayList.remove(arrayList.size() - 1);
                return balanceViesSeriesInfo(arrayList, i, i2, i3, z);
            }
            z = false;
        }
        return new Pair<>(reorderRowsHamburgerStyle(arrayList2, i), Boolean.valueOf(z));
    }

    private static ArrayList<ArrayList<View>> balanceViewsAllowReorder(ArrayList<View> arrayList, int i, int i2) {
        ArrayList<ArrayList<View>> arrayList2 = new ArrayList<>(i2);
        for (int i3 = 0; i3 < i2; i3++) {
            arrayList2.add(new ArrayList<>());
        }
        Collections.sort(arrayList, new Comparator<View>() { // from class: fm.player.ui.utils.BalancedUIHelper.1
            public int compare(View view, View view2) {
                return Integer.compare(view2.getMeasuredWidth(), view.getMeasuredWidth());
            }
        });
        Iterator<View> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            View next = it2.next();
            int i4 = Integer.MAX_VALUE;
            int i5 = 0;
            for (int i6 = 0; i6 < i2; i6++) {
                int sumWidthOfAllViews = sumWidthOfAllViews(arrayList2.get(i6), i);
                if (sumWidthOfAllViews < i4) {
                    i5 = i6;
                    i4 = sumWidthOfAllViews;
                }
            }
            arrayList2.get(i5).add(next);
        }
        int size = arrayList2.size();
        for (int i7 = 0; i7 < size; i7++) {
            Collections.sort(arrayList2.get(i7), new Comparator<View>() { // from class: fm.player.ui.utils.BalancedUIHelper.2
                public int compare(View view, View view2) {
                    if (!(view.getTag() instanceof Integer) || !(view2.getTag() instanceof Integer)) {
                        return 0;
                    }
                    return Integer.compare(((Integer) view2.getTag()).intValue(), ((Integer) view.getTag()).intValue());
                }
            });
        }
        return arrayList2;
    }

    /**
     * 固定
     *
     * @param arrayList
     * @param row
     * @return
     */
    public static ArrayList<ArrayList<FrameLayout>> balanceViewsDiscoveryCarousel(ArrayList<FrameLayout> arrayList, int row) {
        ArrayList<ArrayList<FrameLayout>> arrayList2 = new ArrayList<>(row);
        for (int i3 = 0; i3 < row; i3++) {
            arrayList2.add(new ArrayList<>());
        }
        //每行的个数
        int columnRow = arrayList.size() / row;
        int count = 0;
        for (int j = 0; j < arrayList2.size(); j++) {
            for (int i = 0; i < columnRow; i++) {
                arrayList2.get(j).add(arrayList.get(count));
                count++;
                if (count == arrayList.size()) {
                    break;
                }
            }

        }
        return arrayList2;
    }

    /**
     * 随机
     *
     * @param arrayList
     * @param i
     * @param i2
     * @return
     */
    public static ArrayList<ArrayList<View>> balanceViewsDiscoveryCarousel(ArrayList<View> arrayList, int i, int i2) {
        return reorderRowsHamburgerStyle(balanceViewsAllowReorder(arrayList, i, i2), i);
    }

    public static int longestRowWidth(ArrayList<ArrayList<View>> arrayList, int i) {
        Iterator<ArrayList<View>> it2 = arrayList.iterator();
        int i2 = 0;
        while (it2.hasNext()) {
            i2 = Math.max(i2, sumWidthOfAllViews(it2.next(), i));
        }
        return i2;
    }

    private static ArrayList<ArrayList<View>> reorderRowsHamburgerStyle(ArrayList<ArrayList<View>> arrayList, final int i) {
        Collections.sort(arrayList, new Comparator<ArrayList<View>>() { // from class: fm.player.ui.utils.BalancedUIHelper.3
            public int compare(ArrayList<View> arrayList2, ArrayList<View> arrayList3) {
                return Integer.compare(BalancedUIHelper.sumWidthOfAllViews(arrayList2, i), BalancedUIHelper.sumWidthOfAllViews(arrayList3, i));
            }
        });
        int size = arrayList.size();
        int i2 = size % 2 == 0 ? 1 : 0;
        ArrayList<ArrayList<View>> arrayList2 = new ArrayList<>();
        for (int i3 = 0; i3 < size; i3++) {
            if (i3 % 2 == i2) {
                arrayList2.add(arrayList.get(i3));
            } else {
                arrayList2.add(0, arrayList.get(i3));
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int sumWidthOfAllViews(ArrayList<View> arrayList, int i) {
        Iterator<View> it2 = arrayList.iterator();
        int i2 = 0;
        while (it2.hasNext()) {
            i2 += it2.next().getMeasuredWidth() + i;
        }
        return arrayList.isEmpty() ? i2 - i : i2;
    }
}
