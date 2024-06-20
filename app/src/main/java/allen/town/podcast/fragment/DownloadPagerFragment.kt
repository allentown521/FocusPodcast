package allen.town.podcast.fragment

import allen.town.podcast.R
import allen.town.podcast.activity.MainActivity
import allen.town.podcast.ui.common.PagedToolbarFragment
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

/**
 * Shows the CompletedDownloadsFragment and the RunningDownloadsFragment.
 */
class DownloadPagerFragment constructor() : PagedToolbarFragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var displayUpArrow: Boolean = false
    public override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val root: View = inflater.inflate(R.layout.collapsing_pager_fragment, container, false)
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.downloads_label)
        toolbar.inflateMenu(R.menu.downloads)
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW)
        }
        (getActivity() as MainActivity?)!!.setupToolbarToggle(toolbar, displayUpArrow)
        viewPager = root.findViewById(R.id.viewpager)
        viewPager.setAdapter(DownloadsPagerAdapter(this))
        viewPager.setOffscreenPageLimit(2)
        super.setupPagedToolbar(toolbar, viewPager)

        // Give the TabLayout the ViewPager
        tabLayout = root.findViewById(R.id.sliding_tabs)

//        tabLayout.setTabGravity(GRAVITY_CENTER);
        TabLayoutMediator(
            tabLayout,
            viewPager,
            TabConfigurationStrategy({ tab: TabLayout.Tab, position: Int ->
                when (position) {
                    POS_COMPLETED -> tab.setText(R.string.downloads_completed_label)
                    POS_LOG -> tab.setText(R.string.notification_channel_downloading)
                    else -> {}
                }
            })
        ).attach()

        // restore our last position
        val prefs: SharedPreferences =
            requireActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val lastPosition: Int = prefs.getInt(PREF_LAST_TAB_POSITION, 0)
        viewPager.setCurrentItem(lastPosition, false)
        return root
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow)
        super.onSaveInstanceState(outState)
    }

    public override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (getArguments() != null) {
            val tab: Int = requireArguments().getInt(ARG_SELECTED_TAB)
            viewPager.setCurrentItem(tab, false)
        }
    }

    public override fun onPause() {
        super.onPause()
        // save our tab selection
        val prefs: SharedPreferences =
            requireActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putInt(PREF_LAST_TAB_POSITION, tabLayout.getSelectedTabPosition())
        editor.apply()
    }

    class DownloadsPagerAdapter internal constructor(fragment: Fragment) :
        FragmentStateAdapter(fragment) {
        public override fun createFragment(position: Int): Fragment {
            when (position) {
                POS_COMPLETED -> return CompletedDownloadsFragment()
                POS_LOG -> return DownloadLogFragment()
                else -> return DownloadLogFragment()
            }
        }

        public override fun getItemCount(): Int {
            return TOTAL_COUNT
        }
    }

    companion object {
        @JvmField
        val TAG: String = "DownloadPagerFragment"
        @JvmField
        val ARG_SELECTED_TAB: String = "selected_tab"
        private val PREF_LAST_TAB_POSITION: String = "tab_position"
        private val KEY_UP_ARROW: String = "up_arrow"
        private val POS_COMPLETED: Int = 0
        @JvmField
        val POS_LOG: Int = 1
        private val TOTAL_COUNT: Int = 2
    }
}