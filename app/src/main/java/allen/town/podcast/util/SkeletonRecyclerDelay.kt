package allen.town.podcast.util

import android.os.Looper
import android.os.Message
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.RecyclerView
import com.faltenreich.skeletonlayout.Skeleton

class SkeletonRecyclerDelay(val skeleton: Skeleton, val recyclerView: RecyclerView) :
    android.os.Handler(Looper.getMainLooper()) {
    var updated = false
    override fun handleMessage(msg: Message) {
        if (msg.what == MSG_SHOW) {
            recyclerView.visibility = VISIBLE
        } else if (msg.what == MSG_DISMISS) {
        } else {
            super.handleMessage(msg)
        }
    }

    fun showSkeleton() {
        skeleton.showSkeleton()
        recyclerView.visibility = GONE
        sendEmptyMessageDelayed(MSG_SHOW, 100)
    }

    fun showOriginal() {
        recyclerView.visibility = VISIBLE
        skeleton.showOriginal()
    }

    val DEFAULT_DELAY = 100

    val MSG_SHOW = 1
    val MSG_DISMISS = 2
}