package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.Metadata

class RestoreDropboxDialog(
    val fileList: List<Metadata>?,
    val dropboxCallback: DropboxCallback
) : AppCompatDialogFragment() {

    var rootView: View? = null
    lateinit var alertDialog: Dialog


    override fun onCreateDialog(bundle: Bundle?): Dialog {
        rootView = LayoutInflater.from(activity)
            .inflate(R.layout.restore_dropbox_dialog, null as ViewGroup?)
        val listView: RecyclerView = rootView!!.findViewById(R.id.files_list)
        listView.setLayoutManager(LinearLayoutManager(activity))
        listView.adapter = FilesAdapter(fileList, dropboxCallback)

        alertDialog = AccentMaterialDialog(
            requireContext(),
            R.style.MaterialAlertDialogTheme
        ).setView(rootView).create()
        return alertDialog
    }

    companion object {
        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            fileList: List<Metadata>?,
            dropboxCallback: DropboxCallback
        ) {
            RestoreDropboxDialog(fileList, dropboxCallback).show(fragmentManager, null as String?)
        }


    }
    interface DropboxCallback {
        fun onFolderClicked(folderMetadata: FolderMetadata?)
        fun onFileClicked(fileMetadata: FileMetadata)
    }
    inner class FilesAdapter(val mFiles: List<Metadata>?, private val mDropboxCallback: DropboxCallback) :
        RecyclerView.Adapter<FilesAdapter.MetadataViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MetadataViewHolder {
            val context = viewGroup.context
            val view: View = LayoutInflater.from(context)
                .inflate(R.layout.files_item, viewGroup, false)
            return MetadataViewHolder(view)
        }

        override fun onBindViewHolder(metadataViewHolder: MetadataViewHolder, i: Int) {
            metadataViewHolder.bind(mFiles!![i])
        }

        override fun getItemId(position: Int): Long {
            return mFiles!![position].pathLower.hashCode().toLong()
        }

        override fun getItemCount(): Int {
            return if (mFiles == null) 0 else mFiles!!.size
        }

        inner class MetadataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            private val mTextView: TextView
            private var mItem: Metadata? = null
            override fun onClick(v: View) {
                if (mItem is FolderMetadata) {
                    mDropboxCallback.onFolderClicked(mItem as FolderMetadata?)
                } else if (mItem is FileMetadata) {
                    mDropboxCallback.onFileClicked(mItem as FileMetadata)
                    alertDialog.dismiss()
                }
            }

            fun bind(item: Metadata) {
                mItem = item
                mTextView.text = mItem!!.name
            }

            init {
                mTextView = itemView.findViewById<View>(R.id.text) as TextView
                itemView.setOnClickListener(this)
            }
        }
    }

}