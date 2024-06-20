package allen.town.podcast.fragment

import allen.town.focus_common.util.TopSnackbarUtil
import allen.town.focus_common.views.ItemCategoryDecoration
import allen.town.podcast.R
import allen.town.podcast.activity.RssSearchActivity
import allen.town.podcast.core.pref.Prefs.clearOnlinePodcastSearchHistory
import allen.town.podcast.core.pref.Prefs.onlinePodcastSearchHistory
import allen.town.podcast.discovery.PodcastSearcher
import allen.town.podcast.event.SearchOnlineEvent
import allen.town.podcast.fragment.onlinesearch.OnlineSearchEpisodesFragment
import allen.town.podcast.fragment.onlinesearch.OnlineSearchPodcastFragment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import code.name.monkey.appthemehelper.util.EditTextUtil
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import org.greenrobot.eventbus.EventBus

class OnlineSearchFragment
/**
 * Constructor
 */
constructor() : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var tabViewPagerLayout: View? = null
    private var suggestionLayout: View? = null
    private lateinit var clearHistoryIv: AppCompatImageView
    private lateinit var recyclerView: RecyclerView
    private var searchHistoryAdapter: SearchHistoryAdapter? = null
    private var searchKeywordHistory: MutableList<String?>? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root: View = inflater.inflate(R.layout.collapsing_pager_fragment, container, false)
        setupToolbar(root.findViewById(R.id.toolbar))
        viewPager = root.findViewById(R.id.viewpager)
        viewPager.setOffscreenPageLimit(2)
        viewPager.setAdapter(PagerAdapter(this))
        tabLayout = root.findViewById(R.id.sliding_tabs)
        TabLayoutMediator(
            tabLayout,
            viewPager,
            TabConfigurationStrategy({ tab: TabLayout.Tab, position: Int ->
                when (position) {
                    0 -> tab.setText(R.string.feeds_label)
                    1 -> tab.setText(R.string.episodes_label)
                    else -> {}
                }
            })
        ).attach()
        tabViewPagerLayout = root.findViewById(R.id.tab_viewpager_layout)
        suggestionLayout = root.findViewById(R.id.suggestion_layout)
        clearHistoryIv = root.findViewById(R.id.clear_history_iv)
        recyclerView = root.findViewById(R.id.recycler_view)
        searchKeywordHistory = onlinePodcastSearchHistory as MutableList<String?>?
        val chipsLayoutManager: ChipsLayoutManager = ChipsLayoutManager.newBuilder(getContext())
            .build()
        recyclerView.setLayoutManager(chipsLayoutManager)
        recyclerView.addItemDecoration(ItemCategoryDecoration((getContext())!!, 4))
        searchHistoryAdapter = SearchHistoryAdapter()
        searchHistoryAdapter!!.setHasStableIds(true)
        recyclerView.setAdapter(searchHistoryAdapter)
        showSearchLayout(false)
        clearHistoryIv.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                searchKeywordHistory!!.clear()
                clearOnlinePodcastSearchHistory()
                showSearchLayout(false)
            }
        })
        return root
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    class PagerAdapter internal constructor(fragment: Fragment) : FragmentStateAdapter(fragment) {
        public override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return OnlineSearchPodcastFragment.newInstance()
                1 -> return OnlineSearchEpisodesFragment.newInstance()
                else -> return OnlineSearchPodcastFragment.newInstance()
            }
        }

        public override fun getItemCount(): Int {
            return 2
        }
    }

    var sv: SearchView? = null
    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.setNavigationOnClickListener(View.OnClickListener({ v: View? -> getParentFragmentManager().popBackStack() }))
        toolbar.inflateMenu(R.menu.online_search)
        val searchItem: MenuItem = toolbar.getMenu().findItem(R.id.action_search)
        sv = searchItem.getActionView() as SearchView?
        EditTextUtil.setCursorDrawableForSearchView(sv)
        sv!!.setQueryHint(getString(R.string.search_podcast_hint))
        sv!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            public override fun onQueryTextSubmit(s: String): Boolean {
                sv!!.clearFocus()
                search(s)
                return true
            }

            public override fun onQueryTextChange(s: String): Boolean {
                if (TextUtils.isEmpty(s)) {
                    showSearchLayout(false)
                }
                return false
            }
        })
        sv!!.setOnQueryTextFocusChangeListener(OnFocusChangeListener({ view: View, hasFocus: Boolean ->
            if (hasFocus) {
                showInputMethod(view.findFocus())
            }
        }))
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            public override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            public override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                requireActivity().getSupportFragmentManager().popBackStack()
                return true
            }
        })
        searchItem.expandActionView()
        val clipboard: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData: ClipData? = clipboard.getPrimaryClip()
        if ((clipData != null) && (clipData.getItemCount() > 0) && (clipData.getItemAt(0)
                .getText() != null)
        ) {
            val clipboardContent: String = clipData.getItemAt(0).getText().toString()
            if (clipboardContent.trim({ it <= ' ' }).startsWith("http")) {
                TopSnackbarUtil.showSnack(context,R.string.feed_url_copied_tip,Toast.LENGTH_LONG)
                sv!!.setQuery(clipboardContent.trim({ it <= ' ' }), false)
            }
        }

        /*if (getArguments().getString(ARG_QUERY, null) != null) {
            sv.setQuery(getArguments().getString(ARG_QUERY, null), true);
        }*/
    }

    private fun addUrl(url: String) {
        val intent: Intent = Intent(getActivity(), RssSearchActivity::class.java)
        intent.putExtra(RssSearchActivity.ARG_FEEDURL, url)
        startActivity(intent)
        //添加完关闭此fragment
        parentFragmentManager.popBackStack()
    }

    private fun showSearchLayout(show: Boolean) {
        suggestionLayout?.visibility = if (show) View.GONE else View.VISIBLE
        tabViewPagerLayout?.visibility = if (show) View.VISIBLE else View.INVISIBLE
        if (searchHistoryAdapter != null) {
            searchHistoryAdapter!!.notifyDataSetChanged()
        }
    }

    private fun search(query: String) {
        //"https://feed.nashownotes.com/rss.xml "，从微信复制的带空格的用trim不够
        val finalQuery: String = query.trim({ it <= ' ' }).replace("\\s*".toRegex(), "")
        //如果是http开头的url那么打开解析订阅源界面
        if (finalQuery.matches(Regex("http[s]?://.*"))) {
            addUrl(finalQuery)
            return
        }
        if (!searchKeywordHistory!!.contains(finalQuery)) {
            searchKeywordHistory!!.add(finalQuery)
            onlinePodcastSearchHistory = searchKeywordHistory!!
        }
        showSearchLayout(true)
        EventBus.getDefault().post(SearchOnlineEvent(finalQuery))
    }

    private fun showInputMethod(view: View) {
        val imm: InputMethodManager? =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (imm != null) {
            imm.showSoftInput(view, 0)
        }
    }

    internal inner class SearchHistoryAdapter constructor() :
        RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {
        public override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(parent.getContext())
            val entryView: View = inflater.inflate(R.layout.single_assist_chip, parent, false)
            val chip: Chip = entryView.findViewById(R.id.chip)
            return ViewHolder(entryView as Chip)
        }

        public override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.chip.setText(searchKeywordHistory!!.get(holder.getAdapterPosition()))
            holder.chip.setCheckedIconVisible(holder.chip.isChecked())
            holder.chip.setOnClickListener(object : View.OnClickListener {
                public override fun onClick(v: View) {
                    sv!!.setQuery(searchKeywordHistory!!.get(holder.getAdapterPosition()), true)
                }
            })
        }

        public override fun getItemCount(): Int {
            return searchKeywordHistory!!.size
        }

        public override fun getItemId(position: Int): Long {
            return searchKeywordHistory!!.get(position).hashCode().toLong()
        }

        inner class ViewHolder internal constructor(var chip: Chip) : RecyclerView.ViewHolder(
            chip
        )
    }

    companion object {
        private val TAG: String = "OnlineSearchFragment"
        private val ARG_SEARCHER: String = "searcher"
        private val ARG_QUERY: String = "query"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            searchProvider: Class<out PodcastSearcher?>,
            query: String? = null
        ): OnlineSearchFragment {
            val fragment: OnlineSearchFragment = OnlineSearchFragment()
            val arguments: Bundle = Bundle()
            arguments.putString(ARG_SEARCHER, searchProvider.getName())
            arguments.putString(ARG_QUERY, query)
            fragment.setArguments(arguments)
            return fragment
        }
    }
}