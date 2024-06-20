package allen.town.podcast.dialog

import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.podcast.R
import allen.town.podcast.core.dialog.ConfirmationDialog
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.model.feed.Feed
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RemoveFeedDialog {
    private const val TAG = "RemoveFeedDialog"
    @JvmStatic
    fun show(context: Context, feed: Feed, onFeedRemovedListener: OnFeedRemovedListener?) {
        val feeds = listOf(feed)
        val message = getMessageId(context, feeds)
        showDialog(context, feeds, message, onFeedRemovedListener)
    }

    @JvmStatic
    fun show(context: Context, feed: Feed) {
        val feeds = listOf(feed)
        val message = getMessageId(context, feeds)
        showDialog(context, feeds, message, null)
    }

    @JvmStatic
    fun show(context: Context, feeds: List<Feed>) {
        val message = getMessageId(context, feeds)
        showDialog(context, feeds, message, null)
    }

    private fun showDialog(
        context: Context,
        feeds: List<Feed>,
        message: String,
        onFeedRemovedListener: OnFeedRemovedListener?
    ) {
        val dialog: ConfirmationDialog =
            object : ConfirmationDialog(context, R.string.remove_feed_label, message) {
                override fun onConfirmButtonPressed(clickedDialog: DialogInterface) {
                    clickedDialog.dismiss()
                    showSnack(context, R.string.feed_remover_msg, Toast.LENGTH_SHORT)
                    Completable.fromAction {
                        for (feed in feeds) {
                            DBWriter.deleteFeed(context, feed.id).get()
                        }
                    }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {
                                onFeedRemovedListener?.onFeedRemoved()
                            }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
                }
            }
        dialog.createNewDialog().show()
    }

    private fun getMessageId(context: Context, feeds: List<Feed>): String {
        return if (feeds.size == 1) {
            if (feeds[0].isLocalFeed) {
                context.getString(R.string.feed_delete_confirmation_local_msg, feeds[0].title)
            } else {
                context.getString(R.string.feed_delete_confirmation_msg, feeds[0].title)
            }
        } else {
            context.getString(R.string.feed_delete_confirmation_msg_batch)
        }
    }

    interface OnFeedRemovedListener {
        fun onFeedRemoved()
    }
}