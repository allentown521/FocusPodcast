package allen.town.podcast.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class ReaderListView extends ListView {
    public ReaderListView(Context context) {
        super(context);
    }
    public ReaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ReaderListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    /**
     * 重写该方法，达到使ListView适应ScrollView的效果
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //测量的大小由一个32位的数字表示，前两位表示测量模式，后30位表示大小，这里需要右移两位才能拿到测量的大小
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}