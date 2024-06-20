package allen.town.podcast.activity

import allen.town.focus_common.activity.DialogActivity
import allen.town.focus_common.adapter.BindableViewHolder
import allen.town.focus_common.adapter.ReactiveListAdapter
import allen.town.podcast.R
import allen.town.podcast.appshortcuts.SubscriptionActivityStarter
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.NavDrawerData.*
import allen.town.podcast.databinding.SubscriptionSelectionActivityBinding
import allen.town.podcast.model.feed.Feed
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller.create
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CreateSubscriptionShortcutActivity : DialogActivity() {
    private var disposable: Disposable? = null

    @Volatile
    private var listItems: List<Feed>? = null
    private var viewBinding: SubscriptionSelectionActivityBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = SubscriptionSelectionActivityBinding.inflate(
            layoutInflater
        )
        setContentView(viewBinding!!.root)
        setSupportActionBar(viewBinding!!.toolbar)
        setTitle(R.string.shortcut_select_subscription)
        viewBinding!!.transparentBackground.setOnClickListener { v: View? -> finish() }
        viewBinding!!.list.layoutManager = LinearLayoutManager(this)
        create(viewBinding!!.list)
        loadSubscriptions()
    }

    fun getFeedItems(items: List<DrawerItem>, result: MutableList<Feed>): List<Feed> {
        for (item in items) {
            if (item.type == DrawerItem.Type.TAG) {
                getFeedItems((item as TagDrawerItem).children, result)
            } else {
                val feed = (item as FeedDrawerItem).feed
                if (!result.contains(feed)) {
                    result.add(feed)
                }
            }
        }
        return result
    }

    private fun addShortcut(feed: Feed, bitmap: Bitmap) {
        val intent = SubscriptionActivityStarter.getIntentStartFromLauncher(this, feed)
        val icon = IconCompat.createWithAdaptiveBitmap(bitmap)
        val shortcut = ShortcutInfoCompat.Builder(this, System.currentTimeMillis().toString() + "")
            .setShortLabel(feed.title)
            .setLongLabel(feed.feedTitle)
            .setIntent(intent)
            .setIcon(icon)
            .build()
        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut))
        finish()
    }

    private fun loadSubscriptions() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        disposable = Observable.fromCallable {
            val data = DBReader.getNavDrawerData(true)
            getFeedItems(data.items, ArrayList())
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: List<Feed> ->
                    listItems = result
                    val titles = ArrayList<String>()
                    for (feed in result) {
                        titles.add(feed.title)
                    }
                    val listAdapter: FeedAdapter =
                        FeedAdapter(this@CreateSubscriptionShortcutActivity)
                    viewBinding!!.list.adapter = listAdapter
                    listAdapter.call(titles as List<String?>?)
                }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    inner class FeedAdapter(context: Context?) :
        ReactiveListAdapter<String?, FeedViewHolder?>(context) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
            return FeedViewHolder(
                layoutInflater.inflate(
                    R.layout.single_list_item, parent,
                    false
                )
            )
        }
    }

    inner class FeedViewHolder(view: View) : BindableViewHolder<String?>(view) {
        private val title: TextView
        override fun bindTo(s: String?) {
            title.text = s
            itemView.setOnClickListener { v ->
                if (Intent.ACTION_CREATE_SHORTCUT ==
                    intent.action
                ) {
                    SubscriptionActivityStarter.getBitmapFromUrl(
                        v.context,
                        listItems!![bindingAdapterPosition]
                    ) { feed: Feed, bitmap: Bitmap -> addShortcut(feed, bitmap) }
                }
            }
        }

        init {
            title = view.findViewById(R.id.title)
        }

    }

    companion object {
        private const val TAG = "SelectSubscription"
    }
}