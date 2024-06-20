package allen.town.podcast.adapter

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.fragment.FeedItemlistFragment
import allen.town.podcast.model.feed.Feed
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.lang.ref.WeakReference

class FeedSearchResultAdapter(mainActivity: MainActivity) :
    RecyclerView.Adapter<FeedSearchResultAdapter.Holder>() {
    private val mainActivityRef: WeakReference<MainActivity>
    private val data: MutableList<Feed> = ArrayList()
    fun updateData(newData: List<Feed>?) {
        data.clear()
        data.addAll(newData!!)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val convertView = View.inflate(mainActivityRef.get(), R.layout.searchlist_item_feed, null)
        return Holder(convertView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val podcast = data[position]
        holder.imageView.contentDescription = podcast.title
        holder.imageView.setOnClickListener { v: View? ->
            mainActivityRef.get()!!
                .loadChildFragment(FeedItemlistFragment.newInstance(podcast.id))
        }
        Glide.with(mainActivityRef.get()!!)
            .load(podcast.imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_podcast_background_round)
                    .centerCrop()
                    .dontAnimate()
            )
            .into(holder.imageView)
    }

    override fun getItemId(position: Int): Long {
        return data[position].id
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView

        init {
            imageView = itemView.findViewById(R.id.discovery_cover)
        }
    }

    init {
        mainActivityRef = WeakReference(mainActivity)
    }
}