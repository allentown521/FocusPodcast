package allen.town.podcast.activity

import allen.town.focus_common.dialog.WebDAVSettingDialog
import allen.town.focus_common.util.*
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.views.AccentProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.content.FileProvider
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import code.name.monkey.appthemehelper.util.VersionUtils
import com.dropbox.core.android.Auth
import com.dropbox.core.json.JsonReadException
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.appbar.MaterialToolbar
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.microsoft.onedrivesdk.picker.IPicker
import com.microsoft.onedrivesdk.picker.IPickerResult
import com.microsoft.onedrivesdk.picker.LinkType
import com.microsoft.onedrivesdk.picker.Picker
import com.microsoft.onedrivesdk.saver.ISaver
import com.microsoft.onedrivesdk.saver.Saver
import com.microsoft.onedrivesdk.saver.SaverException
import allen.town.podcast.MyApp
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.dialog.RestoreDropboxDialog
import allen.town.podcast.dialog.RestoreGoogleDriveDialog
import allen.town.podcast.dropbox.DbxRequestConfigFactory
import allen.town.podcast.dropbox.DropboxClientFactory
import allen.town.podcast.googledrive.DriveServiceHelper
import allen.town.podcast.util.CustomDistribution
import com.google.android.material.snackbar.Snackbar
import com.paul623.wdsyncer.SyncConfig
import com.paul623.wdsyncer.SyncManager
import com.paul623.wdsyncer.api.OnSyncResultListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import org.zeroturnaround.zip.ZipUtil
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class DriveBackupActivity : SimpleToolbarActivity() {
    private lateinit var mPicker: IPicker

    /**
     * The OneDrive saver instance used by this activity
     */
    private lateinit var mSaver: ISaver
    private var mDriveServiceHelper: DriveServiceHelper? = null
    private val mOpenFileId: String? = null

    @JvmField
    @BindView(R.id.connectGoogleDriveBtn)
    var connectGoogleDriveBtn: Button? = null

    @OnClick(R.id.connectGoogleDriveBtn)
    fun connectGoogleDrive() {
        requestSignIn()
    }

    @JvmField
    @BindView(R.id.connectDropboxBtn)
    var connectDropboxBtn: Button? = null

    @OnClick(R.id.connectDropboxBtn)
    fun connectDropbox() {
        startOAuth2Authentication(
            this,
            "mg97zk12mma2xqk",
            Arrays.asList("account_info.read", "files.content.write", "files.content.read")
        )
    }

    private val USE_SLT = true //If USE_SLT is set to true, our Android example

    // will use our Short Lived Token.
    private val BACKUP_FILE_NAME = "FocusPodcast_backup.zip"
    private val RESOTRE_FILE_NAME = "FocusPodcast_restore.zip"
    private val BACKUP_FOLD = "FocusPodcast"
    private val DROP_BOX_BACKUP_FOLD = "/backup"

    private fun initDropbox() {
        if (USE_SLT) {
            val serailizedCredental: String? = Prefs.getDropboxCredential(this)
            if (serailizedCredental == null) {
                val credential = Auth.getDbxCredential()
                if (credential != null) {
                    Prefs.setDropboxCredential(this, credential.toString())
                    DropboxClientFactory.init(credential)
                }
            } else {
                try {
                    val credential = DbxCredential.Reader.readFully(serailizedCredental)
                    DropboxClientFactory.init(credential)
                } catch (e: JsonReadException) {
                    throw IllegalStateException("Credential data corrupted: " + e.message)
                }
            }
        } else {
            var accessToken: String? = Prefs.getDropboxAccessToken(this)
            if (accessToken == null) {
                accessToken = Auth.getOAuth2Token()
                if (accessToken != null) {
                    Prefs.setDropboxAccessToken(this, accessToken)
                    DropboxClientFactory.init(accessToken)
                }
            } else {
                DropboxClientFactory.init(accessToken)
            }
        }

        checkDropboxAccount()
    }

    fun checkDropboxAccount() {
        if (hasDropboxToken()) {
            connectDropboxBtn!!.setText(R.string.already_connect)
        }
    }

    fun startOAuth2Authentication(context: Context?, app_key: String?, scope: List<String?>?) {
        if (USE_SLT) {
            Auth.startOAuth2PKCE(
                context,
                app_key,
                DbxRequestConfigFactory.getRequestConfig(),
                scope
            )
        } else {
            Auth.startOAuth2Authentication(context, app_key)
        }
    }

    protected fun hasDropboxToken(): Boolean {
        return if (USE_SLT) {
            Prefs.getDropboxCredential(this) != null
        } else {
            val accessToken = Prefs.getDropboxAccessToken(this)
            accessToken != null
        }
    }


    private fun packData(): File {
        val backUpFile = File(getExternalFilesDir(null)!!.getPath() + File.separator + BACKUP_FILE_NAME)

        val zipParameters = ZipParameters()
        //默认会将根目录作为zip文件的一级目录
        zipParameters.isIncludeRootFolder = false
        zipParameters.excludeFileFilter = object : ExcludeFileFilter {
            override fun isExcluded(file: File): Boolean {
                return file.absolutePath.contains("cache", true)
                        || file.absolutePath.contains(".so", true)
                        || file.absolutePath.contains("lib", true)
                        || file.absolutePath.contains("jiagu", true)
                        || file.absolutePath.contains("no_backup", true)
            }

        }
        ZipFile(backUpFile).addFolder(File("/data/data/allen.town.focus.podcast/"), zipParameters)

        return backUpFile
    }

    @OnClick(R.id.dropbox_backup_ll)
    fun backupToDropbox() {
        if (MyApp.instance.checkSupporter(this)) {
            val progressDialog =
                AccentProgressDialog.show(this, getString(R.string.backup))
            Observable.just(0).subscribeOn(Schedulers.io()).subscribe({

                val localFile: File? = UriHelpers.getFileForUri(this, Uri.fromFile(packData()))

                if (localFile != null) {
                    Timber.d("begin to backup to dropbox")
                    val remoteFolderPath = DROP_BOX_BACKUP_FOLD

                    // Note - this is not ensuring the name is a valid dropbox file name
                    val remoteFileName =
                        SimpleDateFormat("yyyy-MM-dd-HH-mm").format(System.currentTimeMillis()) + "_" + localFile.name
                    try {
                        FileInputStream(localFile).use { inputStream ->
                            DropboxClientFactory.getClient().files()
                                .uploadBuilder("$remoteFolderPath/$remoteFileName")
                                .withMode(WriteMode.OVERWRITE)
                                .uploadAndFinish(inputStream)
                            TopSnackbarUtil.showSnack(
                                this@DriveBackupActivity,
                                R.string.already_backup_dropbox,
                                Toast.LENGTH_LONG
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "backupToDropbox")
                        TopSnackbarUtil.showSnack(
                            this@DriveBackupActivity,
                            R.string.backup_failed,
                            Toast.LENGTH_LONG
                        )
                    } finally {
                        progressDialog.dismiss()
                    }
                }

            }, {
                Timber.e(it, "backupToDropbox e")
                TopSnackbarUtil.showSnack(this@DriveBackupActivity, R.string.backup_failed, Toast.LENGTH_LONG)
                progressDialog.dismiss()
            })
        }
    }


    @OnClick(R.id.dropbox_restore_ll)
    fun restoreFromDropbox() {
        if (MyApp.instance.checkSupporter(this)) {
            val progressDialog =
                AccentProgressDialog.show(this, getString(R.string.restore))
            Observable.just(0).subscribeOn(Schedulers.io()).subscribe({

                val result =
                    DropboxClientFactory.getClient().files().listFolder(DROP_BOX_BACKUP_FOLD)
                progressDialog.dismiss()

                RestoreDropboxDialog.show(
                    supportFragmentManager,
                    result.entries,
                    object : RestoreDropboxDialog.DropboxCallback {
                        override fun onFolderClicked(folderMetadata: FolderMetadata?) {
                            TODO("Not yet implemented")
                        }

                        override fun onFileClicked(fileMetadata: FileMetadata) {
                            val progressDialog2 = AccentProgressDialog.show(
                                this@DriveBackupActivity,
                                getString(R.string.restore),
                            )

                            Observable.just(0).subscribeOn(Schedulers.io()).subscribe({
                                try {
                                    val restorePath =
                                        CustomDistribution.SDCARD_ROOT + File.separator + RESOTRE_FILE_NAME
                                    // Download the file.
                                    FileOutputStream(File(restorePath)).use { outputStream ->
                                        DropboxClientFactory.getClient().files().download(
                                            fileMetadata.getPathLower(),
                                            fileMetadata.getRev()
                                        )
                                            .download(outputStream)
                                    }

                                    moveDataToPrivateFolder(File(restorePath))

                                    //关闭当前进程
                                    Process.killProcess(Process.myPid())


                                } catch (e: Exception) {
                                    Timber.e(e, "backupToDropbox download")
                                    TopSnackbarUtil.showSnack(
                                        this@DriveBackupActivity,
                                        R.string.restore_failed,
                                        Toast.LENGTH_LONG
                                    )
                                } finally {
                                    progressDialog2.dismiss()
                                }
                            }, {
                                Timber.e(it, "backupToDropbox onFileClicked")
                                TopSnackbarUtil.showSnack(
                                    this@DriveBackupActivity,
                                    R.string.restore_failed,
                                    Toast.LENGTH_LONG
                                )
                                progressDialog2.dismiss()
                            })
                        }

                    })


            }, {
                Timber.e(it, "backupToDropbox e")
                TopSnackbarUtil.showSnack(this@DriveBackupActivity, R.string.restore_failed, Toast.LENGTH_LONG)
                progressDialog.dismiss()
            })
        }
    }

    @OnClick(R.id.google_drive_backup_ll)
    fun backupToGoogleDrive() {
        if (MyApp.instance.checkSupporter(this)) {
            backupToGoogleDriveImpl()
        }
    }


    @OnClick(R.id.google_drive_restore_ll)
    fun restoreFromGoogleDrive() {
        if (MyApp.instance.checkSupporter(this)) {
            restoreFromGoogleDriveImpl()
        }
    }


    private var oneDrivePickRequestCode = 0
    private var oneDriveSaveRequestCode = 0

    @OnClick(R.id.one_drive_backup_ll)
    fun backupToOneDrive() {
        if (!MyApp.instance.checkSupporter(this)) {
            return
        }
        // Create the picker instance
        mSaver = Saver.createSaver(ONEDRIVE_APP_ID)
        oneDriveSaveRequestCode = mSaver.getRequestCode()
        val progressDialog =
            AccentProgressDialog.show(this, getString(R.string.backup))
        Observable.just(0).subscribeOn(Schedulers.io())
            .map { integer: Int? -> packData().absolutePath }
            .observeOn(AndroidSchedulers.mainThread()).subscribe({

                var uri: Uri? = null
                if (!TextUtils.isEmpty(it)) {
                    val file = File(it)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        uri = FileProvider.getUriForFile(
                            this,
                            getString(R.string.provider_authority),
                            file
                        )
                    } else {
                        uri = Uri.fromFile(file)
                    }
                }

                if (VersionUtils.hasNougat()) {
                    //7.0以后onedrive 不支持sdk发起存储调用
                    val intentOD = Intent(Intent.ACTION_SEND)
                    intentOD.type = "text/*"
                    intentOD.setPackage("com.microsoft.skydrive")

                    intentOD.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    intentOD.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(Intent.createChooser(intentOD, "title"))
                } else {
                    // Start the saver
                    mSaver.startSaving(this, BACKUP_FILE_NAME, uri)
                }

                progressDialog.dismiss()
            }) {
                Timber.e(it, "backupToOneDrive")
                progressDialog.dismiss()
            }

    }

    @OnClick(R.id.one_drive_restore_ll)
    fun restoreFromOneDrive() {
        if (!MyApp.instance.checkSupporter(this)) {
            return
        }
        mPicker = Picker.createPicker(ONEDRIVE_APP_ID)
        oneDrivePickRequestCode = mPicker.getRequestCode()
        mPicker.startPicking(this, LinkType.DownloadLink)

    }

    @OnClick(R.id.connectWebDevBtn)
    fun configWebdev() {
        val webDAVSettingDialog = WebDAVSettingDialog.show(supportFragmentManager)
    }

    @OnClick(R.id.webdav_backup_ll)
    fun backupToWebdev() {
        if (!MyApp.instance.checkSupporter(this)) {
            return
        }
        val config = SyncConfig(this)
        config.serverUrl = BasePreferenceUtil.webDevUrl
        config.passWord = BasePreferenceUtil.webDevPass
        config.userAccount = BasePreferenceUtil.webDevUser
        val syncManager = SyncManager(this)

        val progressDialog =
            AccentProgressDialog.show(this, "", getString(R.string.backup), true, false)
        rx.Observable.just(0).subscribeOn(rx.schedulers.Schedulers.io())
            .map { integer: Int? -> packData().absolutePath }
            .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread()).subscribe({

                syncManager.uploadFile(
                    BACKUP_FILE_NAME,
                    BACKUP_FOLD,
                    File(it),
                    object : OnSyncResultListener {
                        override fun onSuccess(result: String) {
                            //成功
                            progressDialog.dismiss()
                            showSnack(
                                this@DriveBackupActivity,
                                R.string.already_backup_webdav,
                                Snackbar.LENGTH_LONG
                            )
                        }

                        override fun onError(errorMsg: String) {
                            //失败
                            progressDialog.dismiss()
                            showSnack(
                                this@DriveBackupActivity,
                                getString(R.string.backup_failed) + errorMsg,
                                Snackbar.LENGTH_LONG
                            )
                        }
                    })


            }) {
                Timber.e(it, "backupToWebdev")
                showSnack(
                    this@DriveBackupActivity,
                    R.string.backup_failed,
                    Snackbar.LENGTH_LONG
                )
                progressDialog.dismiss()
            }
    }

    @OnClick(R.id.webdav_restore_ll)
    fun restoreFromWebdev() {

        val config = SyncConfig(this)
        config.serverUrl = BasePreferenceUtil.webDevUrl
        config.passWord = BasePreferenceUtil.webDevPass
        config.userAccount = BasePreferenceUtil.webDevUser
        val syncManager = SyncManager(this)

        val progressDialog2 = AccentProgressDialog.show(
            this@DriveBackupActivity,
            "",
            getString(R.string.restore),
            true,
            false
        )

        progressDialog2.show()

        rx.Observable.just(0).subscribeOn(rx.schedulers.Schedulers.io()).subscribe({

            syncManager.downloadFile(
                BACKUP_FILE_NAME,
                BACKUP_FOLD,
                object : OnSyncResultListener {
                    override fun onSuccess(result: String) {
                        //成功
                        moveDataToPrivateFolder(
                            File(result)
                        )

                        //关闭当前进程
                        Process.killProcess(Process.myPid())

                        progressDialog2.dismiss()
                        showSnack(
                            this@DriveBackupActivity,
                            R.string.restore_success,
                            Snackbar.LENGTH_LONG
                        )
                    }

                    override fun onError(errorMsg: String) {
                        //失败
                        progressDialog2.dismiss()
                        showSnack(
                            this@DriveBackupActivity,
                            getString(R.string.restore_failed) + errorMsg,
                            Snackbar.LENGTH_LONG
                        )
                    }
                })

        }, {
            Timber.e(it, "backupToWebDAV failed")
            showSnack(
                this@DriveBackupActivity,
                getString(R.string.restore_failed) + it.message,
                Snackbar.LENGTH_LONG
            )
            progressDialog2.dismiss()
        })
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.activity_drive_backup)
        ButterKnife.bind(this)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { v -> finish() }
    }

    override fun onResume() {
        super.onResume()
        initDropbox()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> if (resultCode == RESULT_OK && resultData != null) {
                handleSignInResult(resultData)
            }
            REQUEST_CODE_OPEN_DOCUMENT -> if (resultCode == RESULT_OK && resultData != null) {
                val uri = resultData.data
                if (uri != null) {
                }
            }
            oneDrivePickRequestCode -> {
                // Get the results from the from the picker
                val result = mPicker.getPickerResult(requestCode, resultCode, resultData)
                    ?: return

                // Handle the case if nothing was picked
                handleOneDriveRestore(result)
            }
            oneDriveSaveRequestCode -> {
                handleOneDriveBackup(requestCode, resultCode, resultData)
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun handleOneDriveBackup(requestCode: Int, resultCode: Int, resultData: Intent?) {
        var result = false
        try {
            result = mSaver.handleSave(requestCode, resultCode, resultData)

        } catch (e: SaverException) {
            Timber.e(e, "handleOneDriveSave " + e.errorType)

        } finally {
//            progressDialog.dismiss()
            if (result) TopSnackbarUtil.showSnack(
                this@DriveBackupActivity,
                R.string.already_backup_one_drive,
                Toast.LENGTH_LONG
            )
            else TopSnackbarUtil.showSnack(this@DriveBackupActivity, R.string.backup_failed, Toast.LENGTH_LONG)
        }
    }

    /**
     * Updates the results table with details from an [IPickerResult]
     *
     * @param result The results of the picker
     */
    private fun handleOneDriveRestore(result: IPickerResult) {
        val downloadUrl = result.link
        downloadUrl?.run {
            val progressDialog = AccentProgressDialog.show(
                this@DriveBackupActivity,
                getString(R.string.restore),
            )
            Observable.just(0)
                .observeOn(Schedulers.io()).subscribe {
                    try {
                        val conn: HttpURLConnection =
                            URL(downloadUrl.toString()).openConnection() as HttpURLConnection
                        conn.setDoInput(true)
                        conn.connect()
                        val `is`: InputStream = conn.getInputStream()
                        val restorePath =
                            CustomDistribution.SDCARD_ROOT + File.separator + RESOTRE_FILE_NAME
                        if (FileUtils.copyFile(`is`, restorePath)) {
                            moveDataToPrivateFolder(File(restorePath))
                            TopSnackbarUtil.showSnack(
                                this@DriveBackupActivity,
                                R.string.restore_success,
                                Toast.LENGTH_LONG
                            )
                            //关闭当前进程
                            Process.killProcess(Process.myPid())
                        }
                    } catch (e1: Exception) {
                        Timber.e(e1, "handleOneDrivePickResult")
                        progressDialog.dismiss()
                        TopSnackbarUtil.showSnack(
                            this@DriveBackupActivity,
                            R.string.restore_failed,
                            Toast.LENGTH_LONG
                        )
                    }
                }

        }
    }

    /**
     * Starts a sign-in activity using [.REQUEST_CODE_SIGN_IN].
     */
    private fun requestSignIn() {
        Log.d(TAG, "Requesting sign-in")
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private var mDriveService: Drive? = null

    /**
     * Handles the `result` of a completed sign-in activity initiated from [ ][.requestSignIn].
     */
    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                Log.d(TAG, "Signed in as " + googleAccount.email)
                connectGoogleDriveBtn!!.setText(R.string.already_connect)
                // Use the authenticated account to sign in to the Drive service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, setOf(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                )
                    .setApplicationName("Drive API Migration")
                    .build()

                // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                // Its instantiation is required before handling any onClick actions.
                mDriveServiceHelper = DriveServiceHelper(googleDriveService)
                mDriveService = googleDriveService
            }
            .addOnFailureListener { exception: Exception? ->
                Log.e(
                    TAG,
                    "Unable to sign in.",
                    exception
                )
            }
    }

    interface OnGoogleDriveBackupFileSelectedListener {
        fun onBackupFileSelected(file: com.google.api.services.drive.model.File?)
    }

    interface OnLocalBackupFileSelectedListener {
        fun onLocalFileSelected(path: String)
    }

    private fun moveDataToPrivateFolder(file: File) {
        ZipUtil.unpack(
            file,
            File("/data/data/allen.town.focus.podcast")
        )
    }

    private fun restoreFromGoogleDriveImpl() {
        if (mDriveService != null) {
            Log.d(TAG, "restore from GoogleDriveImpl")
            val progressDialog =
                AccentProgressDialog.show(this, getString(R.string.restore))
            val finalFileList: MutableList<com.google.api.services.drive.model.File> = ArrayList()
            Observable.just(0).subscribeOn(Schedulers.io()).subscribe({ integer: Int? ->
                try {
                    val fileList = mDriveService!!.files().list().setSpaces("drive").execute().files
                    for (file in fileList) {
                        if (file.mimeType == "application/zip") {
                            finalFileList.add(file)
                        }
                    }
                    Timber.d("restore from google drive size " + finalFileList.size)
                } catch (e: IOException) {
                    Timber.e(e, "IOException")
                } finally {
                    progressDialog.dismiss()
                    if (finalFileList.size == 0) {
                        TopSnackbarUtil.showSnack(
                            this@DriveBackupActivity,
                            R.string.backup_not_found,
                            Toast.LENGTH_LONG
                        )
                    } else {
                        RestoreGoogleDriveDialog.show(
                            supportFragmentManager,
                            finalFileList,
                            object : OnGoogleDriveBackupFileSelectedListener {
                                override fun onBackupFileSelected(file: com.google.api.services.drive.model.File?) {
                                    if (file != null && file.id != null) {
                                        Log.d(TAG, "Saving " + file.id)
                                        val progressDialog2 = AccentProgressDialog.show(
                                            this@DriveBackupActivity,
                                            getString(R.string.restore),
                                        )
                                        Observable.just(0).subscribeOn(Schedulers.io())
                                            .subscribe({ integer: Int? ->
                                                try {
                                                    val restorePath =
                                                        CustomDistribution.SDCARD_ROOT + File.separator + RESOTRE_FILE_NAME
                                                    val `is` =
                                                        mDriveService!!.files()[file.id].executeMediaAsInputStream()
                                                    if (FileUtils.copyFile(`is`, restorePath)) {
                                                        moveDataToPrivateFolder(File(restorePath))
                                                        TopSnackbarUtil.showSnack(
                                                            this@DriveBackupActivity,
                                                            R.string.restore_success,
                                                            Toast.LENGTH_LONG
                                                        )
                                                        //关闭当前进程
                                                        Process.killProcess(Process.myPid())
                                                    }
                                                } catch (e: IOException) {
                                                    Timber.e(e, "restore failed")
                                                    TopSnackbarUtil.showSnack(
                                                        this@DriveBackupActivity,
                                                        R.string.restore_failed,
                                                        Toast.LENGTH_LONG
                                                    )
                                                } finally {
                                                    progressDialog2.dismiss()
                                                }
                                            }) { throwable: Throwable? ->
                                                Timber.e(throwable, "restore failed")
                                                TopSnackbarUtil.showSnack(
                                                    this@DriveBackupActivity,
                                                    R.string.restore_failed,
                                                    Toast.LENGTH_LONG
                                                )
                                                progressDialog2.dismiss()
                                            }
                                    }
                                }
                            })
                    }
                }
            }) { throwable: Throwable? -> progressDialog.dismiss() }
        }
    }

    /**
     * Queries the Drive REST API for files visible to this app and lists them in the content view.
     */
    private fun backupToGoogleDriveImpl() {
        if (mDriveService != null) {
            val progressDialog =
                AccentProgressDialog.show(this, getString(R.string.backup))
            Observable.just(0).subscribeOn(Schedulers.io()).subscribe({ integer: Int? ->
                Log.d(TAG, "Querying for files.")
                try {
                    val fileList = mDriveService!!.files().list().setSpaces("drive").execute().files
                    var i = 0
                    var num: Int?
                    var str2: String?
                    while (true) {
                        num = null
                        if (i >= fileList.size) {
                            str2 = null
                            break
                        }
                        val file2 = fileList[i]
                        if (file2.name == BACKUP_FOLD) {
                            str2 = file2.id
                            break
                        }
                        i++
                    }
                    if (str2 == null) {
                        val file3 = com.google.api.services.drive.model.File()
                        file3.name = BACKUP_FOLD
                        file3.mimeType = "application/vnd.google-apps.folder"
                        str2 = mDriveService!!.files().create(file3).setFields("id").execute().id
                    }
                    if (str2 != null) {
                        val eVar = FileContent("application/zip", packData())
                        val hashMap: HashMap<String, String> = HashMap()
                        hashMap["ipp"] = true.toString()
                        val mimeType =
                            com.google.api.services.drive.model.File().setParents(listOf(str2))
                                .setMimeType("application/zip")
                        val originalFilename =
                            mimeType.setOriginalFilename(System.currentTimeMillis().toString())
                        val date = Date()
                        val timeZone = TimeZone.getTimeZone("UTC")
                        val time = date.time
                        if (timeZone != null) {
                            num = timeZone.getOffset(date.time) / 60000
                        }
                        val execute = mDriveService!!.files().create(
                            originalFilename
                                .setCreatedTime(DateTime(false, time, num))
                                .setAppProperties(hashMap)
                                .setName(SimpleDateFormat("yyyy-MM-dd-HH-mm").format(System.currentTimeMillis()) + "_" + BACKUP_FILE_NAME),
                            eVar
                        )
                            .setFields("createdTime").execute()
                        TopSnackbarUtil.showSnack(
                            this@DriveBackupActivity,
                            R.string.already_backup_google_drive,
                            Toast.LENGTH_LONG
                        )
                    }
                } catch (e: IOException) {
                    Timber.e(e, "backup to remote exception")
                    TopSnackbarUtil.showSnack(
                        this@DriveBackupActivity,
                        R.string.backup_failed,
                        Toast.LENGTH_LONG
                    )
                } finally {
                    progressDialog.dismiss()
                }
            }) { throwable: Throwable? ->
                Timber.e(throwable, "backup to remote")
                TopSnackbarUtil.showSnack(this@DriveBackupActivity, R.string.backup_failed, Toast.LENGTH_LONG)
                progressDialog.dismiss()
            }
        }
    }

    companion object {
        private const val TAG = "DriveBackupActivity"

        /**
         * Registered Application id for OneDrive []//go.microsoft.com/fwlink/p/?LinkId=193157"">&quot;http://go.microsoft.com/fwlink/p/?LinkId=193157&quot;
         */
        private const val ONEDRIVE_APP_ID = "3bc7e2cf-28c4-4b43-bc4b-761a9a9bc0a2"
        private const val REQUEST_CODE_SIGN_IN = 1
        private const val REQUEST_CODE_OPEN_DOCUMENT = 2
    }
}