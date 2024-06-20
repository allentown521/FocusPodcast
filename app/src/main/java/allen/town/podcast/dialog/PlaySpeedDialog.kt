package allen.town.podcast.dialog

import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.views.ItemOffsetDecoration
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.util.playback.PlaybackController
import allen.town.podcast.event.playback.SpeedChangedEvent
import allen.town.podcast.model.playback.MediaType
import allen.town.podcast.view.PlaybackSpeedSlider
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.util.Consumer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class PlaySpeedDialog : BottomSheetDialogFragment() {
    private var adapter: SpeedSelectionAdapter? = null
    private val speedFormat: DecimalFormat
    private var controller: PlaybackController? = null
    private val selectedSpeeds: MutableList<Float>
    private var speedSeekBar: PlaybackSpeedSlider? = null
    private var addCurrentSpeedChip: Chip? = null
    override fun onStart() {
        super.onStart()
        controller = object : PlaybackController(requireActivity()) {
            override fun loadMediaInfo() {
                updateSpeed(SpeedChangedEvent(controller!!.currentPlaybackSpeedMultiplier))
            }
        }
        controller!!.init()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        controller!!.release()
        controller = null
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateSpeed(event: SpeedChangedEvent) {
        speedSeekBar!!.updateSpeed(event.newSpeed)
        addCurrentSpeedChip!!.text = speedFormat.format(event.newSpeed.toDouble())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = View.inflate(context, R.layout.speed_select_dialog, null)
        speedSeekBar = root.findViewById(R.id.speed_seek_bar)
        speedSeekBar!!.setProgressChangedListener(Consumer { multiplier: Float? ->
            if (controller != null) {
                controller!!.setPlaybackSpeed(multiplier!!)
            }
        })
        val selectedSpeedsGrid = root.findViewById<RecyclerView>(R.id.selected_speeds_grid)
        selectedSpeedsGrid.layoutManager = GridLayoutManager(context, 4)
        selectedSpeedsGrid.addItemDecoration(ItemOffsetDecoration(requireContext(), 4))
        adapter = SpeedSelectionAdapter()
        adapter!!.setHasStableIds(true)
        selectedSpeedsGrid.adapter = adapter
        addCurrentSpeedChip = root.findViewById(R.id.add_current_speed_chip)
        addCurrentSpeedChip!!.setCloseIconVisible(true)
        addCurrentSpeedChip!!.setCloseIconResource(R.drawable.ic_add)
        addCurrentSpeedChip!!.setOnCloseIconClickListener(View.OnClickListener { v: View? -> addCurrentSpeed() })
        addCurrentSpeedChip!!.setOnClickListener(View.OnClickListener { v: View? -> addCurrentSpeed() })
        val speed = Prefs.getPlaybackSpeed(MediaType.AUDIO)
        updateSpeed(SpeedChangedEvent(speed))
        return root
    }

    private fun addCurrentSpeed() {
        val newSpeed = controller!!.currentPlaybackSpeedMultiplier
        if (selectedSpeeds.contains(newSpeed)) {
            showSnack(
                activity,
                getString(R.string.preset_already_exists, newSpeed),
                Toast.LENGTH_LONG
            )
        } else {
            selectedSpeeds.add(newSpeed)
            Collections.sort(selectedSpeeds)
            Prefs.playbackSpeedArray = selectedSpeeds
            adapter!!.notifyDataSetChanged()
        }
    }

    inner class SpeedSelectionAdapter : RecyclerView.Adapter<SpeedSelectionAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val entryView = inflater.inflate(R.layout.single_assist_chip, parent, false)
            val chip = entryView.findViewById<Chip>(R.id.chip)
            chip.textAlignment = View.TEXT_ALIGNMENT_CENTER
            return ViewHolder(entryView as Chip)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val speed = selectedSpeeds[position]
            holder.chip.text = speedFormat.format(speed.toDouble())
            holder.chip.setOnLongClickListener { v: View? ->
                selectedSpeeds.remove(speed)
                Prefs.playbackSpeedArray = selectedSpeeds
                notifyDataSetChanged()
                true
            }
            holder.chip.setOnClickListener { v: View? ->
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        if (controller != null) {
                            dismiss()
                            controller!!.setPlaybackSpeed(speed)
                        }
                    }, 200
                )
            }
        }

        override fun getItemCount(): Int {
            return selectedSpeeds.size
        }

        override fun getItemId(position: Int): Long {
            return selectedSpeeds[position].hashCode().toLong()
        }

        inner class ViewHolder internal constructor(var chip: Chip) : RecyclerView.ViewHolder(
            chip
        )
    }

    init {
        val format = DecimalFormatSymbols(Locale.US)
        format.decimalSeparator = '.'
        speedFormat = DecimalFormat("0.0", format)
        selectedSpeeds = ArrayList(Prefs.playbackSpeedArray)
    }
}