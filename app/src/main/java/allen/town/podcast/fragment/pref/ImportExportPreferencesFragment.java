package allen.town.podcast.fragment.pref;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import allen.town.focus_common.views.AccentProgressDialog;
import allen.town.podcast.MyApp;
import allen.town.podcast.R;
import allen.town.podcast.activity.ImportOPMLActivity;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.asynctask.DocumentFileExportWorker;
import allen.town.podcast.asynctask.ExportWorker;
import allen.town.podcast.core.export.ExportWriter;
import allen.town.podcast.core.export.opml.OpmlWriter;
import allen.town.podcast.core.storage.DatabaseExporter;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ImportExportPreferencesFragment extends AbsSettingsFragment {
    private static final String TAG = "ImportExportPreferencesFragment";
    private static final String PREF_OPML_EXPORT = "prefOpmlExport";
    private static final String PREF_OPML_IMPORT = "prefOpmlImport";
    private static final String PREF_DATABASE_IMPORT = "prefDatabaseImport";
    private static final String PREF_DATABASE_EXPORT = "prefDatabaseExport";
    private static final String DEFAULT_OPML_OUTPUT_NAME = "focusPodcast_feeds_%s.opml";
    //格式太严格有些app无法导入
    private static final String CONTENT_TYPE_OPML = "text/*";
    private static final String DEFAULT_HTML_OUTPUT_NAME = "focusPodcast_feeds_%s.html";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String DEFAULT_FAVORITES_OUTPUT_NAME = "focusPodcast_favorites-%s.html";
    private static final String DATABASE_EXPORT_FILENAME = "focusPodcastBackup-%s.db";
    private final ActivityResultLauncher<Intent> chooseOpmlExportPathLauncher =
            registerForActivityResult(new StartActivityForResult(), this::chooseOpmlExportPathResult);
    private final ActivityResultLauncher<Intent> restoreDatabaseLauncher =
            registerForActivityResult(new StartActivityForResult(), this::restoreDatabaseResult);
    private final ActivityResultLauncher<String> backupDatabaseLauncher =
            registerForActivityResult(new BackupDatabase(), this::backupDatabaseResult);
    private final ActivityResultLauncher<String> chooseOpmlImportPathLauncher =
            registerForActivityResult(new GetContent(), this::chooseOpmlImportPathResult);
    private Disposable disposable;
    private ProgressDialog progressDialog;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_import_export);
        setupStorageScreen();
        progressDialog = AccentProgressDialog.instance(getContext(),"",
                getContext().getString(R.string.please_wait),true,true,null);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.import_export_pref);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private String dateStampFilename(String fname) {
        return String.format(fname, new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
    }

    private void setupStorageScreen() {
        findPreference(PREF_OPML_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    openExportPathPicker(Export.OPML, chooseOpmlExportPathLauncher, new OpmlWriter());
                    return true;
                }
        );
        findPreference(PREF_OPML_IMPORT).setOnPreferenceClickListener(
                preference -> {
                    try {
                        chooseOpmlImportPathLauncher.launch("*/*");
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "No activity found. Should never happen...");
                    }
                    return true;
                });
        findPreference(PREF_DATABASE_IMPORT).setOnPreferenceClickListener(
                preference -> {
                    importDatabase();
                    return true;
                });
        findPreference(PREF_DATABASE_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    exportDatabase();
                    return true;
                });
    }

    private void exportWithWriter(ExportWriter exportWriter, Uri uri, Export exportType) {
        Context context = getActivity();
        progressDialog.show();
        if (uri == null) {
            Observable<File> observable = new ExportWorker(exportWriter, getContext()).exportObservable();
            disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output -> {
                        Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                                context.getString(R.string.provider_authority), output);
                        showExportSuccessDialog(output.toString(), fileUri, exportType);
                    }, this::showExportErrorDialog, progressDialog::dismiss);
        } else {
            DocumentFileExportWorker worker = new DocumentFileExportWorker(exportWriter, context, uri);
            disposable = worker.exportObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output ->
                            showExportSuccessDialog(output.getUri().toString(), output.getUri(), exportType),
                            this::showExportErrorDialog, progressDialog::dismiss);
        }
    }

    private void exportDatabase() {
        backupDatabaseLauncher.launch(dateStampFilename(DATABASE_EXPORT_FILENAME));
    }

    private void importDatabase() {
        // setup the alert builder
        AlertDialog.Builder builder = new AccentMaterialDialog(
                getActivity(),
                R.style.MaterialAlertDialogTheme
        );
        builder.setTitle(R.string.database_import_label);
        builder.setMessage(R.string.database_import_warning);

        // add a button
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            restoreDatabaseLauncher.launch(intent);
        });

        // create and show the alert dialog
        builder.show();
    }

    private void showDatabaseImportSuccessDialog() {
        AlertDialog.Builder builder =            new AccentMaterialDialog(
                    getContext(),
                    R.style.MaterialAlertDialogTheme
            );
        builder.setTitle(R.string.successful_import_label);
        builder.setMessage(R.string.import_ok);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> MyApp.forceRestart());
        builder.show();
    }

    private void showExportSuccessDialog(String path, Uri streamUri, Export exportType) {
        final AlertDialog.Builder alert =            new AccentMaterialDialog(
                    getContext(),
                    R.style.MaterialAlertDialogTheme
            );
        alert.setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_success_title);
//        alert.setMessage(getContext().getString(R.string.export_success_sum, path));
        alert.setPositiveButton(R.string.share_label, (dialog, which) -> {
            new ShareCompat.IntentBuilder(getContext())
                    .setType(exportType.contentType)
                    .setSubject(getString(exportType.labelResId))
                    .addStream(streamUri)
                    .setChooserTitle(R.string.share_label)
                    .startChooser();
        });
        alert.create().show();
    }

    private void showExportErrorDialog(final Throwable error) {
        progressDialog.dismiss();
        final AlertDialog.Builder alert =            new AccentMaterialDialog(
                    getContext(),
                    R.style.MaterialAlertDialogTheme
            );
        alert.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_error_label);
        alert.setMessage(error.getMessage());
        alert.show();
    }

    private void chooseOpmlExportPathResult(final ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            return;
        }
        final Uri uri = result.getData().getData();
        exportWithWriter(new OpmlWriter(), uri, Export.OPML);
    }



    private void restoreDatabaseResult(final ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            return;
        }
        final Uri uri = result.getData().getData();
        progressDialog.show();
        disposable = Completable.fromAction(() -> DatabaseExporter.importBackup(uri, getContext()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    showDatabaseImportSuccessDialog();
                    progressDialog.dismiss();
                }, this::showExportErrorDialog);
    }

    private void backupDatabaseResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        progressDialog.show();
        disposable = Completable.fromAction(() -> DatabaseExporter.exportToDocument(uri, getContext()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    TopSnackbarUtil.showSnack(getActivity(), R.string.export_success_title, Toast.LENGTH_LONG);
                    progressDialog.dismiss();
                }, this::showExportErrorDialog);
    }

    private void chooseOpmlImportPathResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        final Intent intent = new Intent(getContext(), ImportOPMLActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }

    private void openExportPathPicker(Export exportType, ActivityResultLauncher<Intent> result, ExportWriter writer) {
        String title = dateStampFilename(exportType.outputNameTemplate);

        Intent intentPickAction = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(exportType.contentType)
                .putExtra(Intent.EXTRA_TITLE, title);

        // Creates an implicit intent to launch a file manager which lets
        // the user choose a specific directory to export to.
        try {
            result.launch(intentPickAction);
            return;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }

        // If we are using a SDK lower than API 21 or the implicit intent failed
        // fallback to the legacy export process
        exportWithWriter(writer, null, exportType);
    }

    @Override
    public void invalidateSettings() {

    }

    private static class BackupDatabase extends ActivityResultContracts.CreateDocument {
        @NonNull
        @Override
        public Intent createIntent(@NonNull final Context context, @NonNull final String input) {
            return super.createIntent(context, input)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/x-sqlite3");
        }
    }

    private enum Export {
        OPML(CONTENT_TYPE_OPML, DEFAULT_OPML_OUTPUT_NAME, R.string.opml_export_label);

        final String contentType;
        final String outputNameTemplate;
        @StringRes
        final int labelResId;

        Export(String contentType, String outputNameTemplate, int labelResId) {
            this.contentType = contentType;
            this.outputNameTemplate = outputNameTemplate;
            this.labelResId = labelResId;
        }
    }
}
