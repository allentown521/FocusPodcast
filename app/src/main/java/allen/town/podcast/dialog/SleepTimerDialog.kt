package allen.town.podcast.dialog

import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_common.views.ItemOffsetDecoration
import allen.town.podcast.R
import allen.town.podcast.core.pref.SleepTimerPreferences
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.core.util.Converter
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.event.playback.SleepTimerUpdatedEvent
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.google.android.material.chip.Chip
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SleepTimerDialog : DialogFragment() {
    private var controller: PlaybackController? = null
    private var timeSetup: LinearLayout? = null
    private var timeDisplay: LinearLayout? = null
    private var time: TextView? = null
    private var adapter: TimesAdapter? = null
    var spinnerContent = arrayOf(
        "5",
        "10",
        "15",
        "30",
        "45",
        "60",
        "120"
    )

    override fun onStart() {
        super.onStart()
        controller = object : PlaybackController(requireActivity()) {
            override fun loadMediaInfo() {}
        }
        controller!!.init()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        if (controller != null) {
            controller!!.release()
        }
        EventBus.getDefault().unregister(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = View.inflate(context, R.layout.time_dialog, null)
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            requireContext(),
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(R.string.sleep_timer_label)
        builder.setView(content)
        builder.setPositiveButton(R.string.close_label, null)
        timeSetup = content.findViewById(R.id.timeSetup)
        timeDisplay = content.findViewById(R.id.timeDisplay)
        timeDisplay!!.setVisibility(View.GONE)
        time = content.findViewById(R.id.time)
        val extendSleepFiveMinutesButton =
            content.findViewById<Button>(R.id.extendSleepFiveMinutesButton)
        extendSleepFiveMinutesButton.text = getString(R.string.extend_sleep_timer_label, 5)
        val extendSleepTenMinutesButton =
            content.findViewById<Button>(R.id.extendSleepTenMinutesButton)
        extendSleepTenMinutesButton.text = getString(R.string.extend_sleep_timer_label, 10)
        val extendSleepTwentyMinutesButton =
            content.findViewById<Button>(R.id.extendSleepTwentyMinutesButton)
        extendSleepTwentyMinutesButton.text = getString(R.string.extend_sleep_timer_label, 20)
        extendSleepFiveMinutesButton.setOnClickListener { v: View? ->
            if (controller != null) {
                controller!!.extendSleepTimer((5 * 1000 * 60).toLong())
            }
        }
        extendSleepTenMinutesButton.setOnClickListener { v: View? ->
            if (controller != null) {
                controller!!.extendSleepTimer((10 * 1000 * 60).toLong())
            }
        }
        extendSleepTwentyMinutesButton.setOnClickListener { v: View? ->
            if (controller != null) {
                controller!!.extendSleepTimer((20 * 1000 * 60).toLong())
            }
        }
        for (i in spinnerContent.indices) {
            if (spinnerContent[i] == SleepTimerPreferences.lastTimerValue()) {
                selectedIndex = i
            }
        }
        val recyclerView = content.findViewById<RecyclerView>(R.id.times_recycler_view)
        //https://github.com/BelooS/ChipsLayoutManager
        val chipsLayoutManager = ChipsLayoutManager.newBuilder(context).build()
        recyclerView.layoutManager = chipsLayoutManager
        recyclerView.addItemDecoration(ItemOffsetDecoration(requireContext(), 4))
        adapter = TimesAdapter()
        adapter!!.setHasStableIds(true)
        recyclerView.adapter = adapter
        val cbShakeToReset = content.findViewById<SwitchCompat>(R.id.cbShakeToReset)
        val cbVibrate = content.findViewById<SwitchCompat>(R.id.cbVibrate)
        val chAutoEnable = content.findViewById<SwitchCompat>(R.id.chAutoEnable)
        cbShakeToReset.isChecked = SleepTimerPreferences.shakeToReset()
        cbVibrate.isChecked = SleepTimerPreferences.vibrate()
        chAutoEnable.isChecked = SleepTimerPreferences.autoEnable()
        cbShakeToReset.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SleepTimerPreferences.setShakeToReset(
                isChecked
            )
        }
        cbVibrate.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SleepTimerPreferences.setVibrate(
                isChecked
            )
        }
        chAutoEnable.setOnCheckedChangeListener { compoundButton: CompoundButton?, isChecked: Boolean ->
            SleepTimerPreferences.setAutoEnable(
                isChecked
            )
        }
        val disableButton = content.findViewById<Button>(R.id.disableSleeptimerButton)
        disableButton.setOnClickListener { v: View? ->
            if (controller != null) {
                controller!!.disableSleepTimer()
            }
        }
        val setButton = content.findViewById<Button>(R.id.setSleeptimerButton)
        setButton.setOnClickListener { v: View? ->
            if (!PlaybackService.isRunning) {
                showSnack(activity, R.string.no_media_playing_label, Toast.LENGTH_LONG)
                return@setOnClickListener
            }
            try {
                val time = spinnerContent[selectedIndex].toLong()
                if (time == 0L) {
                    throw NumberFormatException("Timer must not be zero")
                }
                SleepTimerPreferences.setLastTimer(spinnerContent[selectedIndex])
                if (controller != null) {
                    controller!!.setSleepTimer(SleepTimerPreferences.timerMillis())
                }
                closeKeyboard(content)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                showSnack(activity, R.string.time_dialog_invalid_input, Toast.LENGTH_LONG)
            }
        }
        return builder.create()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun timerUpdated(event: SleepTimerUpdatedEvent) {
        timeDisplay!!.visibility =
            if (event.isOver || event.isCancelled) View.GONE else View.VISIBLE
        timeSetup!!.visibility =
            if (event.isOver || event.isCancelled) View.VISIBLE else View.GONE
        time!!.text =
            Converter.getDurationStringLong(event.timeLeft.toInt())
    }

    private fun closeKeyboard(content: View) {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(content.windowToken, 0)
    }

    var selectedIndex = 0

    internal inner class TimesAdapter : RecyclerView.Adapter<TimesAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val entryView = inflater.inflate(R.layout.single_tag_chip, parent, false)
            val chip = entryView.findViewById<Chip>(R.id.chip)
            return ViewHolder(entryView as Chip)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.chip.text =
                spinnerContent[holder.adapterPosition] + getString(R.string.time_minutes)
            holder.chip.isChecked = selectedIndex == holder.adapterPosition
            holder.chip.isCheckedIconVisible = holder.chip.isChecked
            holder.chip.setOnClickListener {
                selectedIndex = holder.adapterPosition
                adapter!!.notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return spinnerContent.size
        }

        override fun getItemId(position: Int): Long {
            return spinnerContent[position].hashCode().toLong()
        }

        inner class ViewHolder internal constructor(var chip: Chip) : RecyclerView.ViewHolder(
            chip
        )
    }
}