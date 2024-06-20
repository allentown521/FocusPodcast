package allen.town.podcast.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import allen.town.podcast.R

class FeedItemExtraInfoFragment
/**
 * Constructor
 */
    : BottomSheetDialogFragment() {

    private lateinit var bottomSheet:  ViewGroup
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabViewPagerLayout: View
    private var windowInsets: WindowInsetsCompat? = null
    private var selectedPosition = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        bottomSheet = dialog!!.findViewById(com.google.android.material.R.id.design_bottom_sheet) as ViewGroup
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
//        bottomSheetBehavior.skipCollapsed = true
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//        if (dialog != null) {
//            val bottomSheet: View = dialog!!.findViewById(com.google.android.material.R.id.design_bottom_sheet)
//            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
//        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.feed_into_extra_layout_bottom_sheet, container, false)
//        root.findViewById<View>(R.id.toolbar).visibility = View.GONE
        viewPager = root.findViewById(R.id.viewpager)
        viewPager.setOffscreenPageLimit(2)
        viewPager.setAdapter(PagerAdapter(this))

        //加了这行默认只有第一个item滑动正常,除非每个item的可滑动项的isNestedScrollingEnabled全部设置为false才行
        viewPager.children.find { it is RecyclerView }?.let {
            (it as RecyclerView).isNestedScrollingEnabled = false
        }

        if(selectedPosition > 0){
            viewPager.setCurrentItem(selectedPosition,false)
        }
        tabLayout = root.findViewById(R.id.sliding_tabs)
        TabLayoutMediator(
            tabLayout,
            viewPager,
            TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
                when (position) {
                    0 -> tab.setText(R.string.description_label)
                    1 -> tab.setText(R.string.chapters_label)
                    else -> {}
                }
            }).attach()

        return root
    }

    fun scrollToPage(page: Int):FeedItemExtraInfoFragment {
        selectedPosition = page
        return this
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    class PagerAdapter internal constructor(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                DESC_POS -> FeedItemDescriptionFragment()
                CHAPTERS_POS -> ChaptersFragment()
                else -> FeedItemDescriptionFragment()
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }

    companion object {
        private const val TAG = "FeedItemExtraInfoFragment"
        const val DESC_POS = 0
        const val CHAPTERS_POS = 1
    }
}