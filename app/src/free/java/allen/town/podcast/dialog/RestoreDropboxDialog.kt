package allen.town.podcast.dialog

import androidx.appcompat.app.AppCompatDialogFragment

class RestoreDropboxDialog(
    val fileList: List<Metadata>?,
    val dropboxCallback: DropboxCallback
) : AppCompatDialogFragment() {

    interface DropboxCallback {
        fun onFolderClicked()
        fun onFileClicked()
    }
}