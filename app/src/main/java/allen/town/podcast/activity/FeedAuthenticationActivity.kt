package allen.town.podcast.activity

import allen.town.focus_common.activity.DialogActivity
import allen.town.podcast.R
import allen.town.podcast.core.service.download.DownloadRequest
import allen.town.podcast.core.service.download.DownloadService
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.dialog.AuthenticationDialog
import allen.town.podcast.model.feed.FeedMedia
import android.os.Bundle
import android.text.TextUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.Validate

/**
 * Shows a username and a password text field.
 * The activity MUST be started with the ARG_DOWNlOAD_REQUEST argument set to a non-null value.
 */
class FeedAuthenticationActivity : DialogActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Validate.isTrue(intent.hasExtra(ARG_DOWNLOAD_REQUEST), "Download request missing")
        val request = intent.getParcelableExtra<DownloadRequest>(ARG_DOWNLOAD_REQUEST)
        object : AuthenticationDialog(this, R.string.authentication_label, true, "", "") {
            override fun onConfirmed(username: String?, password: String?) {
                Completable.fromAction {
                    request!!.username = username
                    request.password = password
                    if (request.feedfileType == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                        val mediaId = request.feedfileId
                        val media = DBReader.getFeedMedia(mediaId)
                        if (media != null) {
                            val preferences = media.item!!.feed.preferences
                            if (TextUtils.isEmpty(preferences.password)
                                || TextUtils.isEmpty(preferences.username)
                            ) {
                                preferences.username = username
                                preferences.password = password
                                DBWriter.setFeedPreferences(preferences)
                            }
                        }
                    }
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        DownloadService.download(this@FeedAuthenticationActivity, false, request)
                        finish()
                    }
            }

            override fun onCancelled() {
                finish()
            }
        }.show()
    }

    companion object {
        /**
         * The download request object that contains information about the resource that requires a username and a password.
         */
        const val ARG_DOWNLOAD_REQUEST = "request"
    }
}