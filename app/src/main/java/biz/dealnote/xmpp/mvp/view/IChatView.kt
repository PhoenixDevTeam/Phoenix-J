package biz.dealnote.xmpp.mvp.view

import android.net.Uri

import biz.dealnote.mvp.core.IMvpView
import biz.dealnote.xmpp.model.AppMessage
import biz.dealnote.xmpp.util.AvatarResorce

/**
 * Created by ruslan.kolbasa on 03.11.2016.
 * phoenix_for_xmpp
 */
interface IChatView : IMvpView, IErrorView, IToastView {
    fun setDraftMessageText(text: CharSequence?)
    fun setupInputMode(recordNow: Boolean)
    fun configSendButton(canSendMessage: Boolean, isRecordNow: Boolean)
    fun displayRecordingDuration(time: Long)

    fun requestRecordPermissions()
    fun requestWriteStoragePermission()
    fun startAttachFileActivity()
    fun showUploadStreamConfirmationDialog(uri: Uri, type: String?)
    fun removeInputStreamExtra()

    fun showReSentMessageDialog(message: AppMessage)

    fun showOTRActionsMenu(canStart: Boolean, canRefresh: Boolean, canEnd: Boolean)

    fun displayMessages(messages: List<AppMessage>, avatarResorce: AvatarResorce)

    fun notifyItemRangeInserted(positionStart: Int, itemCount: Int)

    fun notifyItemRemoved(position: Int)

    fun notifyDataSetChanged()

    fun notifyItemChanged(position: Int)

    fun cancelNotificationsForDestination(destination: String)

    fun setToolbarSubtitle(subtitle: String)

    fun showDeleteMessagesConfirmation(messageIds: Set<Int>)

    fun showActionMode(title: String)

    fun finishActionMode()

    fun setupOptionMenu(otrLocked: Boolean)

    fun goToIncomeFiles(destination: String)

    fun bindAudioViewHolderById(holderId: Int, isCurrent: Boolean, isPaused: Boolean, duration: Int, position: Int)

    fun bindAllAudioViewHolders(currentEntityId: Int?, isPaused: Boolean, duration: Int, position: Int)
}