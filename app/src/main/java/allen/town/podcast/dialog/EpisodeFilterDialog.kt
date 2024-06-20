package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_common.views.ItemOffsetDecoration
import allen.town.podcast.R
import allen.town.podcast.adapter.SimpleChipAdapter
import allen.town.podcast.databinding.EpisodeFilterDialogBinding
import allen.town.podcast.model.feed.FeedFilter
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.recyclerview.widget.GridLayoutManager

/**
 * Displays a dialog with a text box for filtering episodes and two radio buttons for exclusion/inclusion
 */
abstract class EpisodeFilterDialog(context: Context?, private val filter: FeedFilter) :
    AccentMaterialDialog(
        context!!, R.style.MaterialAlertDialogTheme
    ) {
    private lateinit var viewBinding: EpisodeFilterDialogBinding
    private lateinit var termList: MutableList<String>

    init {
        viewBinding = EpisodeFilterDialogBinding.inflate(LayoutInflater.from(context))
        setTitle(R.string.episode_filters_label)
        setView(viewBinding.getRoot())

        viewBinding.durationCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            viewBinding.episodeFilterDurationText.isEnabled = isChecked
        }
        if (filter.hasMinimalDurationFilter()) {
            viewBinding.durationCheckBox.setChecked(true)
            // Store minimal duration in seconds, show in minutes
            viewBinding.episodeFilterDurationText
                .setText(java.lang.String.valueOf(filter.minimalDurationFilter / 60))
        } else {
            viewBinding.episodeFilterDurationText.isEnabled = false
        }

        if (filter.excludeOnly()) {
            termList = filter.excludeFilter
            viewBinding.excludeRadio.setChecked(true)
        } else {
            termList = filter.includeFilter
            viewBinding.includeRadio.setChecked(true)
        }
        setupWordsList()
        setNegativeButton(R.string.cancel_label, null)
        setPositiveButton(R.string.confirm_label) { dialog: DialogInterface, which: Int ->
            onConfirmClick(
                dialog,
                which
            )
        }
    }

    open fun setupWordsList() {
        viewBinding.termsRecycler.setLayoutManager(GridLayoutManager(context, 2))
        viewBinding.termsRecycler.addItemDecoration(ItemOffsetDecoration(context, 4))
        val adapter: SimpleChipAdapter = object : SimpleChipAdapter(context) {
            override fun getChips(): List<String> {
                return termList
            }

            override fun onRemoveClicked(position: Int) {
                termList.removeAt(position)
                notifyDataSetChanged()
            }
        }
        viewBinding.termsRecycler.setAdapter(adapter)
        viewBinding.termsTextInput.setEndIconOnClickListener { v ->
            val newWord: String =
                viewBinding.termsTextInput.getEditText()?.getText().toString().replace("\"", "")
                    .trim()
            if (TextUtils.isEmpty(newWord) || termList.contains(newWord)) {
                return@setEndIconOnClickListener
            }
            termList.add(newWord)
            viewBinding.termsTextInput.getEditText()?.setText("")
            adapter.notifyDataSetChanged()
        }
    }

    protected abstract fun onConfirmed(filter: FeedFilter?)

    open fun onConfirmClick(dialog: DialogInterface, which: Int) {
        var minimalDuration = -1
        if (viewBinding.durationCheckBox.isChecked()) {
            try {
                // Store minimal duration in seconds
                minimalDuration =
                    viewBinding.episodeFilterDurationText.getText().toString().toInt() * 60
            } catch (e: NumberFormatException) {
                // Do not change anything on error
            }
        }
        var excludeFilter: String? = ""
        var includeFilter: String? = ""
        if (viewBinding.includeRadio.isChecked()) {
            includeFilter = toFilterString(termList)
        } else {
            excludeFilter = toFilterString(termList)
        }
        onConfirmed(FeedFilter(includeFilter, excludeFilter, minimalDuration))
    }

    open fun toFilterString(words: List<String?>?): String? {
        val result = StringBuilder()
        for (word in words!!) {
            result.append("\"").append(word).append("\" ")
        }
        return result.toString()
    }
}