package allen.town.podcast.util;

import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

/* loaded from: classes4.dex */
public class MovementCheck extends LinkMovementMethod {
    private static LinkMovementMethod sInstance;

    public static MovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new MovementCheck();
        }
        return sInstance;
    }

    @Override // android.text.method.LinkMovementMethod, android.text.method.ScrollingMovementMethod, android.text.method.BaseMovementMethod, android.text.method.MovementMethod
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        try {
            return super.onTouchEvent(textView, spannable, motionEvent);
        } catch (Exception unused) {
            return true;
        }
    }
}
