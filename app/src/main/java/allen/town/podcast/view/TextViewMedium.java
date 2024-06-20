package allen.town.podcast.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/* renamed from: fm.player.ui.customviews.TextViewMedium */
/* loaded from: classes2.dex */
public class TextViewMedium extends AppCompatTextView {
    public TextViewMedium(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (!isInEditMode()) {
            Typeface appFontMedium = null;
            if (appFontMedium == null) {
                setTypeface(null, Typeface.NORMAL);
            } else {
                setTypeface(appFontMedium);
            }
        }
    }

    public TextViewMedium(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TextViewMedium(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }
}
