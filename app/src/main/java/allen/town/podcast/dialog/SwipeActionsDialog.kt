package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.databinding.*
import allen.town.podcast.fragment.EpisodesFragment
import allen.town.podcast.fragment.FeedItemlistFragment
import allen.town.podcast.fragment.PlaylistFragment
import allen.town.podcast.fragment.swipeactions.SwipeAction
import allen.town.podcast.fragment.swipeactions.SwipeActions
import allen.town.focus_common.util.ThemeUtils.getColorFromAttr
import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.gridlayout.widget.GridLayout
import com.annimon.stream.Stream

class SwipeActionsDialog(private val context: Context, private val tag: String) {
    private var rightAction: SwipeAction? = null
    private var leftAction: SwipeAction? = null
    private var keys: List<SwipeAction>? = null
    fun show(prefsChanged: Callback) {
        val actions = SwipeActions.getPrefsWithDefaults(context, tag)
        leftAction = actions.left
        rightAction = actions.right
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        keys = SwipeActions.swipeActions
        var forFragment = ""
        when (tag) {
            EpisodesFragment.TAG -> forFragment = context.getString(R.string.episodes_label)
            FeedItemlistFragment.TAG -> forFragment = context.getString(R.string.feeds_label)
            PlaylistFragment.TAG -> {
                forFragment = context.getString(R.string.playlist_label)
                keys = Stream.of<SwipeAction>(keys!!)
                    .filter { a: SwipeAction -> a.id != SwipeAction.ADD_TO_QUEUE }
                    .toList()
            }
            else -> {}
        }
        if (tag != PlaylistFragment.TAG) {
            keys = Stream.of<SwipeAction>(keys!!)
                .filter { a: SwipeAction -> a.id != SwipeAction.REMOVE_FROM_QUEUE }
                .toList()
        }
        builder.setTitle(context.getString(R.string.swipeactions_label) + " - " + forFragment)
        val viewBinding = SwipeactionsDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
        builder.setView(viewBinding.root)
        viewBinding.enableSwitch.setOnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            viewBinding.actionLeftContainer.root.alpha = if (b) 1.0f else 0.4f
            viewBinding.actionRightContainer.root.alpha = if (b) 1.0f else 0.4f
        }
        viewBinding.enableSwitch.isChecked = SwipeActions.isSwipeActionEnabled(context, tag)
        setupSwipeDirectionView(viewBinding.actionLeftContainer, LEFT)
        setupSwipeDirectionView(viewBinding.actionRightContainer, RIGHT)
        builder.setPositiveButton(R.string.confirm_label) { dialog: DialogInterface?, which: Int ->
            savePrefs(tag, rightAction!!.getId(), leftAction!!.getId())
            saveActionsEnabledPrefs(viewBinding.enableSwitch.isChecked)
            prefsChanged.onCall()
        }
        builder.setNegativeButton(R.string.cancel_label, null)
        builder.create().show()
    }

    private fun setupSwipeDirectionView(view: SwipeactionsRowBinding, direction: Int) {
        val action = if (direction == LEFT) leftAction else rightAction
        view.swipeDirectionLabel.setText(if (direction == LEFT) R.string.swipe_left else R.string.swipe_right)
        view.swipeActionLabel.text = action!!.getTitle(context)
        view.changeButton.setOnClickListener { v: View? -> showPicker(view, direction) }
    }

    private fun showPicker(view: SwipeactionsRowBinding, direction: Int) {
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(if (direction == LEFT) R.string.swipe_left else R.string.swipe_right)
        val picker = SwipeactionsPickerBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
        builder.setView(picker.root)
        builder.setNegativeButton(R.string.cancel_label, null)
        val dialog = builder.show()
        for (i in keys!!.indices) {
            val action = keys!![i]
            val item = SwipeactionsPickerItemBinding.inflate(
                LayoutInflater.from(
                    context
                )
            )
            item.swipeActionLabel.text = action.getTitle(context)
            val icon =
                DrawableCompat.wrap(AppCompatResources.getDrawable(context, action.actionIcon)!!)
            icon.mutate()
            DrawableCompat.setTintMode(icon, PorterDuff.Mode.SRC_ATOP)
            if (direction == LEFT && leftAction === action || direction == RIGHT && rightAction === action) {
                DrawableCompat.setTint(
                    icon, getColorFromAttr(
                        context, action.actionColor
                    )
                )
                item.swipeActionLabel.setTextColor(
                    getColorFromAttr(
                        context, action.actionColor
                    )
                )
            } else {
                DrawableCompat.setTint(
                    icon, getColorFromAttr(
                        context, R.attr.colorControlNormal
                    )
                )
            }
            item.swipeIcon.setImageDrawable(icon)
            item.root.setOnClickListener { v: View? ->
                if (direction == LEFT) {
                    leftAction = keys!![i]
                } else {
                    rightAction = keys!![i]
                }
                setupSwipeDirectionView(view, direction)
                dialog.dismiss()
            }
            val param = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.BASELINE),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
            )
            param.width = 0
            picker.pickerGridLayout.addView(item.root, param)
        }
        picker.pickerGridLayout.columnCount = 2
        picker.pickerGridLayout.rowCount = (keys!!.size + 1) / 2
    }

    private fun populateMockEpisode(view: FeeditemlistItemBinding) {
        view.container.alpha = 0.3f
        view.dragHandle.visibility = View.GONE
        view.txtvTitle.text = "███████"
    }

    private fun savePrefs(tag: String, right: String, left: String) {
        val prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SwipeActions.KEY_PREFIX_SWIPEACTIONS + tag, "$right,$left").apply()
    }

    private fun saveActionsEnabledPrefs(enabled: Boolean) {
        val prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(SwipeActions.KEY_PREFIX_NO_ACTION + tag, enabled).apply()
    }

    interface Callback {
        fun onCall()
    }

    companion object {
        private const val LEFT = 1
        private const val RIGHT = 0
    }
}