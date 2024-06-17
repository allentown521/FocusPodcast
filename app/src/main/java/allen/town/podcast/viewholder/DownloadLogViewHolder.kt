package allen.town.podcast.viewholder

import allen.town.podcast.R
import allen.town.podcast.view.PlayPauseProgressButton
import android.content.Context
import android.os.Build
import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView

class DownloadLogViewHolder(context: Context?, parent: ViewGroup?) : RecyclerView.ViewHolder(
    LayoutInflater.from(context).inflate(R.layout.downloadlog_item, parent, false)
) {
    val secondaryActionButton: PlayPauseProgressButton
    val icon: AppCompatImageView
    val title: TextView
    val status: TextView
    val reason: TextView

    init {
        status = itemView.findViewById(R.id.status)
        icon = itemView.findViewById(R.id.txtvIcon)
        reason = itemView.findViewById(R.id.txtvReason)
        secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton)
        title = itemView.findViewById(R.id.txtvTitle)
        if (Build.VERSION.SDK_INT >= 23) {
            title.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_FULL
        }
        itemView.tag = this
    }
}