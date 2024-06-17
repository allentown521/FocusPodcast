package allen.town.podcast.adapter

import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.util.StorageUtils
import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.util.Consumer
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ChooseStorageFolderAdapter(context: Context, selectionHandler: Consumer<String>) :
    RecyclerView.Adapter<ChooseStorageFolderAdapter.ViewHolder>() {
    private val selectionHandler: Consumer<String>
    private val currentPath: String?
    private val entries: List<StoragePath>
    private val freeSpaceString: String
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val entryView = inflater.inflate(R.layout.choose_data_folder_dialog_entry, parent, false)
        return ViewHolder(entryView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val storagePath = entries[position]
        val context = holder.root.context
        val freeSpace = Formatter.formatShortFileSize(context, storagePath.availableSpace)
        val totalSpace = Formatter.formatShortFileSize(context, storagePath.totalSpace)
        holder.path.text = storagePath.shortPath
        holder.size.text = String.format(freeSpaceString, freeSpace, totalSpace)
        holder.progressBar.progress = storagePath.usagePercentage
        val selectListener =
            View.OnClickListener { v: View? -> selectionHandler.accept(storagePath.fullPath) }
        holder.root.setOnClickListener(selectListener)
        holder.radioButton.setOnClickListener(selectListener)
        if (storagePath.fullPath == currentPath) {
            holder.radioButton.toggle()
        }
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    private fun getCurrentPath(): String? {
        val dataFolder = Prefs.getDataFolder(null)
        return dataFolder?.absolutePath
    }

    private fun getStorageEntries(context: Context): List<StoragePath> {
        val mediaDirs = context.getExternalFilesDirs(null)
        val entries: MutableList<StoragePath> = ArrayList(mediaDirs.size)
        for (dir in mediaDirs) {
            if (!isWritable(dir)) {
                continue
            }
            entries.add(StoragePath(dir.absolutePath))
        }
        if (entries.isEmpty() && isWritable(context.filesDir)) {
            entries.add(StoragePath(context.filesDir.absolutePath))
        }
        return entries
    }

    private fun isWritable(dir: File?): Boolean {
        return dir != null && dir.exists() && dir.canRead() && dir.canWrite()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View
        val path: TextView
        val size: TextView
        val radioButton: RadioButton
        val progressBar: ProgressBar

        init {
            root = itemView.findViewById(R.id.root)
            path = itemView.findViewById(R.id.path)
            size = itemView.findViewById(R.id.size)
            radioButton = itemView.findViewById(R.id.radio_button)
            progressBar = itemView.findViewById(R.id.used_space)
        }
    }

    internal class StoragePath(val fullPath: String) {
        val shortPath: String
            get() {
                val prefixIndex = fullPath.indexOf("Android")
                return if (prefixIndex > 0) fullPath.substring(0, prefixIndex) else fullPath
            }
        val availableSpace: Long
            get() = StorageUtils.getFreeSpaceAvailable(fullPath)
        val totalSpace: Long
            get() = StorageUtils.getTotalSpaceAvailable(fullPath)
        val usagePercentage: Int
            get() = 100 - (100 * availableSpace / totalSpace.toFloat()).toInt()
    }

    init {
        entries = getStorageEntries(context)
        currentPath = getCurrentPath()
        this.selectionHandler = selectionHandler
        freeSpaceString = context.getString(R.string.choose_data_directory_available_space)
    }
}