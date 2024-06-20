package allen.town.podcast.dialog

import allen.town.focus_common.adapter.BindableAdapter
import allen.town.focus_common.views.AccentMaterialDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.google.api.services.drive.model.File
import allen.town.podcast.R
import allen.town.podcast.activity.DriveBackupActivity

class RestoreGoogleDriveDialog(
    val fileList: List<File>,
    val onGoogleDriveBackupFileSelectedListener: DriveBackupActivity.OnGoogleDriveBackupFileSelectedListener
) : AppCompatDialogFragment() {

    var rootView: View? = null


    override fun onCreateDialog(bundle: Bundle?): Dialog {
        rootView = LayoutInflater.from(activity)
            .inflate(R.layout.restore_google_drive_dialog, null as ViewGroup?)
        val listView: ListView = rootView!!.findViewById(R.id.list)
        listView.adapter =
            FolderAdapter(requireContext(), fileList, onGoogleDriveBackupFileSelectedListener)
        listView.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                dismiss()
                onGoogleDriveBackupFileSelectedListener.onBackupFileSelected(
                    listView.adapter.getItem(
                        position
                    ) as File
                )
            }

        })

        val alertDialog = AccentMaterialDialog(
            requireContext(),
            R.style.MaterialAlertDialogTheme
        ).setView(rootView).create()
        return alertDialog
    }

    companion object {
        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            fileList: List<File>,
            onGoogleDriveBackupFileSelectedListener: DriveBackupActivity.OnGoogleDriveBackupFileSelectedListener
        ) {
            RestoreGoogleDriveDialog(fileList, onGoogleDriveBackupFileSelectedListener).show(
                fragmentManager,
                null as String?
            )
        }


    }

    class FolderAdapter(
        context: Context,
        private val fileList: List<File>,
        val onGoogleDriveBackupFileSelectedListener: DriveBackupActivity.OnGoogleDriveBackupFileSelectedListener
    ) : BindableAdapter<File>(context) {


        override fun getCount(): Int {
            return fileList.size
        }

        override fun getItem(i: Int): File {
            return fileList[i]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun bindView(t: File?, i: Int, view: View?) {
            var name = view!!.findViewById<TextView>(R.id.name)
            name.text = getItem(i).name

        }

        override fun newView(layoutInflater: LayoutInflater?, i: Int, viewGroup: ViewGroup?): View {
            return layoutInflater!!.inflate(R.layout.remote_backup_list_item, viewGroup, false)
        }

    }

}