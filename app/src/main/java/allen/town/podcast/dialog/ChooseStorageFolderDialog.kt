package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import allen.town.podcast.adapter.ChooseStorageFolderAdapter
import android.content.Context
import android.view.View
import androidx.core.util.Consumer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

object ChooseStorageFolderDialog {
    @JvmStatic
    fun showDialog(context: Context?, handlerFunc: Consumer<String?>) {
        val content = View.inflate(context, R.layout.choose_data_folder_dialog, null)
        val dialog = AccentMaterialDialog(
            context!!,
            R.style.MaterialAlertDialogTheme
        )
            .setView(content)
            .setTitle(R.string.choose_data_directory)
            .setNegativeButton(R.string.cancel_label, null)
            .create()
        (content.findViewById<View>(R.id.recyclerView) as RecyclerView).layoutManager =
            LinearLayoutManager(context)
        val adapter = ChooseStorageFolderAdapter(context) { path: String? ->
            dialog.dismiss()
            handlerFunc.accept(path)
        }
        (content.findViewById<View>(R.id.recyclerView) as RecyclerView).adapter = adapter
        if (adapter.itemCount > 0) {
            dialog.show()
        } else {
            AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
            )
                .setTitle(R.string.error_label)
                .setMessage(R.string.external_storage_error_msg)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}