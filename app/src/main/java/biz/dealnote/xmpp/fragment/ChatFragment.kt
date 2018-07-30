package biz.dealnote.xmpp.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import biz.dealnote.mvp.core.IPresenterFactory
import biz.dealnote.xmpp.Constants
import biz.dealnote.xmpp.Extra
import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.activity.ActivityUtils
import biz.dealnote.xmpp.activity.FileManagerActivity
import biz.dealnote.xmpp.adapter.MessagesAdapter
import biz.dealnote.xmpp.callback.*
import biz.dealnote.xmpp.fragment.base.BasePresenterFragment
import biz.dealnote.xmpp.model.AppFile
import biz.dealnote.xmpp.model.AppMessage
import biz.dealnote.xmpp.mvp.presenter.ChatPresenter
import biz.dealnote.xmpp.mvp.view.IChatView
import biz.dealnote.xmpp.util.AvatarResorce
import biz.dealnote.xmpp.util.NotificationHelper
import biz.dealnote.xmpp.util.Utils
import biz.dealnote.xmpp.util.Utils.getDurationString
import biz.dealnote.xmpp.view.SimpleTextWatcher
import java.io.File

class ChatFragment : BasePresenterFragment<ChatPresenter, IChatView>(), OnBackButtonCallback, MessagesAdapter.ActionListener, ActionMode.Callback,
        MessagesAdapter.SubscriptionActionListener, IChatView, MessagesAdapter.AudioBindCallback {

    private var mAdapter: MessagesAdapter? = null
    private var mInputField: EditText? = null

    private var mActionMode: ActionMode? = null

    private var mButtonSend: ImageView? = null
    private var mButtonAttach: ImageView? = null
    private var mRecordTimeText: TextView? = null

    private var mOtrOptionItemLocked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_chat, container, false)

        val recyclerView = root.findViewById<View>(R.id.list) as RecyclerView

        mInputField = root.findViewById<View>(R.id.input_field) as EditText
        mInputField!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                presenter.fireInputTextChanged(charSequence)
            }
        })

        val manager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, true)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(requireActivity(), Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener(manager) {
            override fun onLoadMore() {
                presenter.fireScrollToEnd()
            }
        })

        mButtonSend = root.findViewById<View>(R.id.send_button) as ImageView
        root.findViewById<View>(R.id.send_button_root).setOnClickListener { _ -> presenter.fireSendButtonClick() }

        mButtonAttach = root.findViewById<View>(R.id.button_attach) as ImageView
        mButtonAttach!!.setOnClickListener { _ -> presenter.fireAttachOrStopRecordButtonClick() }

        mRecordTimeText = root.findViewById<View>(R.id.recorder_status_text) as TextView

        mAdapter = MessagesAdapter(emptyList(), AvatarResorce(), requireActivity())

        mAdapter!!.setActionListener(this)
        mAdapter!!.setSubscriptionActionListener(this)
        mAdapter!!.setAudioBindCallback(this)

        recyclerView.adapter = mAdapter
        return root
    }

    override fun onResume() {
        super.onResume()
        if (activity is AppStyleable) {
            (activity as AppStyleable).enableToolbarElevation(true)
        }

        ActivityUtils.supportToolbarFor(this)?.setTitle(R.string.chat)
        checkInputFile()
    }

    private fun checkInputFile() {
        val intent = activity?.intent ?: return
        val extras = intent.extras ?: return

        if (Intent.ACTION_SEND == intent.action) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val type = intent.type
                presenter?.fireInputStreamExist(uri, type)
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return presenter?.onBackPressed() ?: true
    }

    override fun onFileTransferAcceptClick(message: AppMessage) {
        presenter?.fireFileTransferAcceptClick(message)
    }

    override fun onFileTransferDeclineClick(message: AppMessage) {
        presenter?.fireFileTransferDeclineClick(message)
    }

    override fun onFileOpen(mid: Int, appFile: AppFile) {
        Utils.openFile(activity, appFile.uri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            val file = data!!.getStringExtra(FileManagerFragment.returnFileParameter)
            presenter?.fireFileSelected(File(file))
        }
    }

    override fun showDeleteMessagesConfirmation(messageIds: Set<Int>) {
        activity?.run {
            AlertDialog.Builder(this)
                    .setTitle(R.string.confirmation)
                    .setMessage(R.string.delete_messages_confiramtion)
                    .setPositiveButton(R.string.button_yes) { _, _ -> presenter.fireDeleteConfirmClick(messageIds) }
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()
        }
    }

    override fun onMessageLongClick(position: Int, message: AppMessage): Boolean {
        presenter?.fireMessageLongClick(position, message)
        return true
    }

    override fun onSubscriptionAcceptClick(message: AppMessage) {
        presenter?.fireSubscriptionAcceptClick(message)
    }

    override fun onSubscriptionDeclineClick(message: AppMessage) {
        presenter?.fireSubscriptionDeclineClick(message)
    }

    override fun onMessageClicked(position: Int, message: AppMessage) {
        presenter?.fireMessageClick(position, message)
    }

    override fun showReSentMessageDialog(message: AppMessage) {
        activity?.run {
            val items = arrayOf(getString(R.string.repeat))
            AlertDialog.Builder(this)
                    .setItems(items) { _, _ -> presenter?.fireMessageResentClick(message) }
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()
        }
    }

    override fun showActionMode(title: String) {
        if (mActionMode == null) {
            mActionMode = (activity as AppCompatActivity).startSupportActionMode(this)
        }

        mActionMode?.title = title
        mActionMode?.invalidate()
    }

    override fun finishActionMode() {
        mActionMode?.finish()
        mActionMode = null
    }

    override fun setupOptionMenu(otrLocked: Boolean) {
        mOtrOptionItemLocked = otrLocked
        activity?.invalidateOptionsMenu()
    }

    override fun goToIncomeFiles(destination: String) {
        if (activity is OnPlaceOpenCallback) {
            val criteria = FilesCriteria()
            criteria.setDestnation(destination)
            (activity as OnPlaceOpenCallback).showIncomeFiles(criteria)
        }
    }

    override fun bindAudioViewHolderById(holderId: Int, isCurrent: Boolean, isPaused: Boolean, duration: Int, position: Int) {
        mAdapter?.bindAudioControls(holderId, isCurrent, isPaused, duration, position)
    }

    override fun bindAllAudioViewHolders(currentEntityId: Int?, isPaused: Boolean, duration: Int, position: Int) {
        mAdapter?.bindAudioControls(currentEntityId, isPaused, duration, position)
    }

    fun compareChatAttributes(accountId: Int, destination: String): Boolean {
        val thisAccountId = arguments!!.getInt(Extra.ACCOUNT_ID)
        val thisDestination = arguments!!.getString(Extra.DESTINATION)
        return thisAccountId == accountId && destination == thisDestination
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_messages, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                presenter.fireActionModeDeleteClick()
                mode.finish()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        presenter?.fireActionModeDestroy()
        mActionMode = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_chat, menu)

        menu.findItem(R.id.action_otr_options).setIcon(if (mOtrOptionItemLocked)
            R.drawable.ic_locked
        else
            R.drawable.ic_unlocked)
    }

    override fun showOTRActionsMenu(canStart: Boolean, canRefresh: Boolean, canEnd: Boolean) {
        activity?.run {
            val view = findViewById<View>(R.id.action_otr_options)

            val popupMenu = PopupMenu(this, view)
            popupMenu.inflate(R.menu.popup_otr_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_otr_encrypt -> presenter.fireStartOtrClick()
                    R.id.action_otr_end -> presenter.fireEndOtrClick()
                    R.id.action_otr_refresh -> presenter.fireRefreshOtrClick()
                }

                true
            }

            val menu = popupMenu.menu
            menu.findItem(R.id.action_otr_encrypt).isEnabled = canStart
            menu.findItem(R.id.action_otr_end).isEnabled = canEnd
            menu.findItem(R.id.action_otr_refresh).isEnabled = canRefresh
            popupMenu.show()
        }
    }

    override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
        mAdapter?.notifyItemRangeInserted(positionStart, itemCount)
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(position: Int) {
        mAdapter?.notifyItemChanged(position)
    }

    override fun cancelNotificationsForDestination(destination: String) {
        NotificationHelper.cancelNotifyForDestination(requireActivity(), destination)
    }

    override fun setToolbarSubtitle(subtitle: String) {
        ActivityUtils.supportToolbarFor(this)?.run {
            this.subtitle = subtitle
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_show_files -> presenter?.fireIncomeFilesClick()
            R.id.action_otr_options -> presenter?.fireOtrOptionClick()
            else -> throw Exception()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatPresenter> {
        return IPresenterFactory {
            val accountId = arguments!!.getInt(Extra.ACCOUNT_ID)
            val destination = arguments!!.getString(Extra.DESTINATION)!!
            val chatId = if (arguments!!.containsKey(Extra.CHAT_ID)) arguments!!.getInt(Extra.CHAT_ID) else null
            ChatPresenter(accountId, destination, chatId, saveInstanceState)
        }
    }

    override fun setDraftMessageText(text: CharSequence?) {
        mInputField?.setText(text)
    }

    override fun setupInputMode(recordNow: Boolean) {
        mButtonAttach?.setImageResource(if (recordNow) R.drawable.ic_close_red_vector else R.drawable.ic_attach_red_vector)
        mInputField?.visibility = if (recordNow) View.GONE else View.VISIBLE
        mRecordTimeText?.visibility = if (recordNow) View.VISIBLE else View.GONE
    }

    override fun configSendButton(canSendMessage: Boolean, isRecordNow: Boolean) {
        mButtonSend?.setImageResource(if (canSendMessage) R.drawable.ic_send else if (isRecordNow) R.drawable.ic_check_vector else R.drawable.ic_microphone)
    }

    override fun displayRecordingDuration(time: Long) {
        mRecordTimeText?.run {
            val str = getDurationString((time / 1000).toInt())
            text = getString(R.string.recording_time, str)
        }
    }

    override fun requestRecordPermissions() {
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_RECORD_PERMISSIONS)
    }

    override fun requestWriteStoragePermission() {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSIONS)
    }

    override fun startAttachFileActivity() {
        val intent = Intent(activity, FileManagerActivity::class.java)
        intent.action = FileManagerFragment.INTENT_ACTION_SELECT_FILE
        activity?.startActivityForResult(intent, REQUEST_SELECT_FILE)
    }

    override fun showUploadStreamConfirmationDialog(uri: Uri, type: String?) {
        val alertDialog = AlertDialog.Builder(requireActivity())
                .setTitle(R.string.confirmation)
                .setMessage(R.string.send_file_in_this_chat_question)
                .setPositiveButton(R.string.button_yes) { _, _ -> presenter.fireInputStreamUploadConfirmed(uri, type) }
                .setNegativeButton(R.string.button_cancel, null)
                .show()

        alertDialog.setOnDismissListener { _ -> presenter.fireUploadInputStreamDialogDissmiss() }
    }

    override fun removeInputStreamExtra() {
        activity?.intent?.removeExtra(Intent.EXTRA_STREAM)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_PERMISSIONS && isPresenterPrepared) {
            presenter?.fireRecordPermissionsResolved()
        }

        if (requestCode == REQUEST_WRITE_PERMISSIONS && isPresenterPrepared) {
            presenter?.fireWritePermissionResolved()
        }
    }

    override fun onHolderCreate(holderId: Int, message: AppMessage) {
        presenter?.fireAudioHolderCreate(holderId, message)
    }

    override fun onPlayButtonClick(holderId: Int, message: AppMessage) {
        presenter?.fireAudioPlayButtonClick(holderId, message)
    }

    override fun onSeekbarMovedByUser(position: Int, message: AppMessage) {
        presenter?.fireAudioSeekBarMovedByUser(position, message)
    }

    override fun displayMessages(messages: List<AppMessage>, avatarResorce: AvatarResorce) {
        mAdapter?.setData(messages, avatarResorce)
    }

    companion object {
        private const val REQUEST_SELECT_FILE = 1534
        private const val REQUEST_RECORD_PERMISSIONS = 154
        private const val REQUEST_WRITE_PERMISSIONS = 156

        fun newInstance(accountId: Int, destination: String, chatId: Int?): ChatFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.DESTINATION, destination)
            if (chatId != null) {
                args.putInt(Extra.CHAT_ID, chatId)
            }

            val fragment = ChatFragment()
            fragment.arguments = args
            return fragment
        }
    }
}