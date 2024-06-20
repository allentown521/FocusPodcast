package allen.town.podcast.fragment.pref

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import code.name.monkey.appthemehelper.ThemeStore
import com.google.android.material.button.MaterialButton
import allen.town.podcast.R
import allen.town.podcast.util.NavigationUtil

class BuyPreference : Preference, View.OnClickListener {
    private val hint: String? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        layoutResource = R.layout.buy_layout
        parseAttrs(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        layoutResource = R.layout.buy_layout
        parseAttrs(attrs)
    }

    constructor(context: Context?) : super(context!!) {
        layoutResource = R.layout.buy_layout
        parseAttrs(null)
    }

    private fun parseAttrs(attrs: AttributeSet?) {

    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        holder.findViewById(R.id.buyProContainer).setOnClickListener(this)
        holder.itemView.setOnClickListener(this)
        holder.itemView.setBackgroundColor(0x0)
        holder.findViewById(R.id.buyProContainer).setOnClickListener(this)

        ThemeStore.accentColor(context).let {
            (holder.findViewById(R.id.buyPremium) as MaterialButton).setTextColor(it)
            (holder.findViewById(R.id.diamondIcon) as AppCompatImageView).imageTintList =
                ColorStateList.valueOf(it)
        }
    }

    override fun onClick(view: View) {
        NavigationUtil.goToProVersion(context)
    }
    /**
     * Returns the search configuration object for this preference
     * @return The search configuration
     */
}