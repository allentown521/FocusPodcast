package allen.town.podcast.core.dialog;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import allen.town.focus_common.views.AccentMaterialDialog;
import allen.town.podcast.core.R;

/**
 * 确认dialog
 */
public class MessageDialog {

    private static final String TAG = MessageDialog.class.getSimpleName();

    private final Context context;
    private final String message;

    private int positiveText;


    public MessageDialog(Context context, String message) {
        this.context = context;
        this.message = message;
    }

    private void onCancelButtonPressed(DialogInterface dialog) {
        dialog.dismiss();
    }

    public void setPositiveText(int id) {
        this.positiveText = id;
    }


    public final AlertDialog createNewDialog() {
        AlertDialog.Builder builder = new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        );
        builder.setMessage(message);
        builder.setPositiveButton(positiveText != 0 ? positiveText : R.string.confirm_label,
                (dialog, which) -> dialog.dismiss());
        builder.setOnCancelListener(MessageDialog.this::onCancelButtonPressed);
        return builder.create();
    }

    public static void show(Context context, String message){
        new MessageDialog(context, message).createNewDialog().show();
    }
}
