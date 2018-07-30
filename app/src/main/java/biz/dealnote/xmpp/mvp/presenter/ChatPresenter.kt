package biz.dealnote.xmpp.mvp.presenter

import android.Manifest
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.PermissionChecker
import android.text.TextUtils
import biz.dealnote.mvp.reflect.OnGuiCreated
import biz.dealnote.xmpp.Constants
import biz.dealnote.xmpp.Injection
import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.loader.PhotoGalleryImageProvider
import biz.dealnote.xmpp.model.*
import biz.dealnote.xmpp.mvp.presenter.base.RequestSupportPresenter
import biz.dealnote.xmpp.mvp.view.IChatView
import biz.dealnote.xmpp.security.IOtrManager
import biz.dealnote.xmpp.security.OtrState
import biz.dealnote.xmpp.service.request.RequestFactory
import biz.dealnote.xmpp.util.*
import biz.dealnote.xmpp.util.Objects
import biz.dealnote.xmpp.util.RxUtils.ignore
import biz.dealnote.xmpp.util.Utils.*
import biz.dealnote.xmpp.util.recorder.AudioRecordException
import biz.dealnote.xmpp.util.recorder.AudioRecordWrapper
import biz.dealnote.xmpp.util.recorder.Recorder
import biz.dealnote.xmpp.util.recorder.VoicePlayer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by ruslan.kolbasa on 03.11.2016.
 * phoenix_for_xmpp
 */
class ChatPresenter(private val mAccountId: Int,
                    private val mDestination: String, chatId: Int?,
                    savedInstanceState: Bundle?) : RequestSupportPresenter<IChatView>(savedInstanceState) {

    private val otrManager: IOtrManager = Injection.provideOtrManager()

    private val mAudioRecordWrapper: AudioRecordWrapper = AudioRecordWrapper.Builder(applicationContext)
            .setFileExt("mp3")
            .build()

    private val mRecordTimeLookup: Lookup = Lookup(1000)
    private val mVoicePlayerLookup: Lookup
    private var mChatId: Int? = null
    private val mData: MutableList<AppMessage> = ArrayList()
    private val mAvatarResorce: AvatarResorce = AvatarResorce()
    private var mEndOfContent: Boolean = false
    private val mVoicePlayer: VoicePlayer = VoicePlayer()

    private var mLoadingNow: Boolean = false

    private var mDraftMessageText: String? = null

    private val isDraftMessageEmpty: Boolean
        get() = trimmedIsEmpty(mDraftMessageText)

    private val isRecordNow: Boolean
        get() = mAudioRecordWrapper.recorderStatus != Recorder.Status.NO_RECORD

    private var mAccount: Account? = null
    private var mMyContact: Contact? = null

    private val firstMessageId: Int?
        get() = if (isEmpty(mData)) null else mData[mData.size - 1].id

    private val markedMessagesIds: Set<Int>
        get() {
            val mids = HashSet<Int>()
            for (m in mData) {
                if (m.isSelected) {
                    mids.add(m.id)
                }
            }

            return mids
        }

    private val selectedMessagesCount: Int
        get() {
            var count = 0
            for (message in mData) {
                if (message.isSelected) {
                    count++
                }
            }

            return count
        }

    init {
        mRecordTimeLookup.callback = Lookup.Callback { this.resolveRecordingTimeView() }

        mVoicePlayerLookup = Lookup(1000)
        mVoicePlayerLookup.callback = Lookup.Callback { this.onVoicePlayerLookupIteration() }

        if (savedInstanceState != null) {
            mAccount = savedInstanceState.getParcelable(SAVE_ACCOUNT)
            mChatId = if (savedInstanceState.containsKey(SAVE_CHAT_ID)) savedInstanceState.getInt(SAVE_CHAT_ID) else null
        } else {
            mChatId = chatId
        }

        appendDisposable(Repositories.instance.messages
                .createAddMessageObservable()
                .filter { message -> message.accountId == mAccountId && message.destination == mDestination }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onNewMessageAdded))

        appendDisposable(Repositories.instance.messages
                .createMessageUpdateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMessageUpdate))

        appendDisposable(Repositories.instance.messages
                .createMessageDeleteObservable()
                .filter { integerSetPair -> chatId != null && Objects.equals(integerSetPair.first, chatId) }
                .map { it -> it.second }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMessagesRemoved))

        if (mAccount == null) {
            loadAccountInfo()
        }

        loadAtStart()

        if (chatId != null) {
            Repositories.instance.chats
                    .setUnreadCount(chatId, 0)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe()
        }

        appendDisposable(otrManager.observeStateChanges()
                .filter { event -> event.accountId == mAccountId && mDestination == event.bareJid }
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe { _ -> resolveOptionMenu() })
    }

    @OnGuiCreated
    private fun resolveOptionMenu() {
        view?.run {
            val otrLocked = otrManager.getSessionState(mAccountId, mDestination) == OtrState.ENCRYPTED
            setupOptionMenu(otrLocked)
        }
    }

    private fun onMessageUpdate(pair: Pair<Int, MessageUpdate>) {
        val messageId = pair.first
        val update = pair.second

        val index = indexOf(mData, messageId)
        if (index != -1) {
            val message = mData[index]
            if (applyUpdate(message, update)) {
                view?.notifyItemChanged(index)
            }
        }
    }

    private fun applyUpdate(message: AppMessage, update: MessageUpdate): Boolean {
        var hasChanges = false
        if (update.statusUpdate != null) {
            message.status = update.statusUpdate.status
            hasChanges = true
        }

        if (update.fileUriUpdate != null) {
            message.attachedFile.setUri(update.fileUriUpdate.uri)
            hasChanges = true
        }

        return hasChanges
    }

    private fun onMessagesRemoved(ids: Set<Int>) {
        for (id in ids) {
            for (i in mData.indices) {
                if (mData[i].id == id) {
                    mData.removeAt(i)
                    view?.notifyItemRemoved(i)
                }
            }
        }
    }

    private fun onNewMessageAdded(message: AppMessage) {
        if (mChatId == null) {
            mChatId = message.chatId
        }

        mData.add(0, message)
        view?.notifyDataSetChanged()
    }

    private fun changeLoadingNowState(loadingNow: Boolean) {
        mLoadingNow = loadingNow
    }

    private fun loadAtStart() {
        if (mLoadingNow) return

        val criteria = MessageCriteria()
                .setAccountId(mAccountId)
                .setChatId(mChatId)
                .setDestination(mDestination)
                .setCount(COUNT_PER_LOAD)
        load(criteria)
    }

    private fun load(criteria: MessageCriteria) {
        if (mLoadingNow) {
            return
        }

        changeLoadingNowState(true)
        appendDisposable(Repositories.instance.messages
                .load(criteria)
                .fromIOToMain()
                .subscribe { pair -> onMessagesLoaded(pair.first, pair.second) })
    }

    private fun onMessagesLoaded(messages: List<AppMessage>, entries: List<AvatarResorce.Entry>) {
        changeLoadingNowState(false)
        mEndOfContent = messages.size < COUNT_PER_LOAD

        if (messages.isEmpty()) {
            return
        }

        mAvatarResorce.putList(entries)

        val sizeBefore = mData.size
        mData.addAll(messages)

        view?.notifyItemRangeInserted(sizeBefore, messages.size)
    }

    override fun onGuiCreated(viewHost: IChatView) {
        super.onGuiCreated(viewHost)
        viewHost.displayMessages(mData, mAvatarResorce)
    }

    @OnGuiCreated
    private fun resolveToolbarSubtitle() {
        view?.setToolbarSubtitle(mDestination)
    }

    fun fireInputTextChanged(text: CharSequence) {
        mDraftMessageText = text.toString()
        resolveSendButton()
    }

    @OnGuiCreated
    private fun resolveInputMode() {
        view?.setupInputMode(isRecordNow)
    }

    @OnGuiCreated
    private fun resolveSendButton() {
        view?.run {
            val canSendText = !isRecordNow && !isDraftMessageEmpty
            configSendButton(canSendText, isRecordNow)
        }
    }

    fun fireSendButtonClick() {
        if (isRecordNow) {
            stopRecordingAndSendFile()
        } else {
            if (isDraftMessageEmpty) {
                if (!hasAudioRecordPermissions()) {
                    view?.requestRecordPermissions()
                    return
                }

                startRecordImpl()
            } else {
                sendMessageImpl()
            }
        }

        resolveSendButton()
    }

    private fun stopRecordingAndSendFile() {
        try {
            val file = mAudioRecordWrapper.stopRecordingAndReceiveFile()
            sendFile(file)
        } catch (e: AudioRecordException) {
            e.printStackTrace()
        }

        onRecordingStateChanged()
    }

    private fun startRecordImpl() {
        try {
            mAudioRecordWrapper.doRecord()
        } catch (e: AudioRecordException) {
            e.printStackTrace()
        }

        onRecordingStateChanged()
        resolveRecordingTimeView()
    }

    private fun onRecordingStateChanged() {
        resolveInputMode()
        resolveSendButton()
        syncRecordingLookupState()
    }

    private fun syncRecordingLookupState() {
        if (isRecordNow) {
            mRecordTimeLookup.start()
        } else {
            mRecordTimeLookup.stop()
        }
    }

    private fun sendMessageImpl() {
        if (!checkChatReady()) {
            return
        }

        if (isDraftMessageEmpty) {
            showShortToast(view, R.string.type_your_message)
            return
        }

        if (Constants.FORCE_OTR) {
            val otrStatus = otrManager.getSessionState(mAccountId, mDestination)
            if (otrStatus != OtrState.ENCRYPTED) {
                showLongToast(view, R.string.otr_session_does_not_active)
                showOtrMenuImpl()
                return
            }
        }

        val body = mDraftMessageText!!.trim { it <= ' ' }

        val type = AppMessage.TYPE_CHAT
        val stanzaId = AppPrefs.generateMessageStanzaId(applicationContext)

        val builder = MessageBuilder(mAccountId)
                .setBody(body)
                .setDestination(mDestination)
                .setSenderJid(mAccount!!.buildBareJid())
                .setDate(Unixtime.now())
                .setType(type)
                .setChatId(mChatId)
                .setUniqueServiceId(stanzaId)
                .setSenderId(if (mAccount == null) null else mMyContact!!.id)
                .setOut(true)

        saveNewOutMessageAndSent(builder)

        mDraftMessageText = null
        resolveDraftMessageText()
    }

    private fun checkChatReady(): Boolean {
        if (mAccount == null) {
            showShortToast(view, R.string.chat_is_not_ready_yet)
            return false
        }

        return true
    }

    private fun sendFile(file: File) {
        if (!checkChatReady()) {
            return
        }

        val appFile = AppFile(Uri.fromFile(file), file.name, file.length())

        val builder = MessageBuilder(mAccountId)
                .setAppFile(appFile)
                .setDestination(mDestination)
                .setSenderId(if (mMyContact == null) null else mMyContact!!.id)
                .setSenderJid(mAccount!!.buildBareJid())
                .setDate(Unixtime.now())
                .setType(AppMessage.TYPE_OUTGOING_FILE)
                .setChatId(mChatId)
                .setOut(true)
                .setStatus(AppMessage.STATUS_WAITING_FOR_REASON)
                .setReadState(false)

        saveOutgoingFileMessageAndSend(builder)
    }

    private fun saveOutgoingFileMessageAndSend(builder: MessageBuilder) {
        appendDisposable(Repositories.instance.messages
                .saveMessage(builder)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onOutgoingFileMessageSaved))
    }

    private fun saveChatId(chatId: Int) {
        if (mChatId == null) {
            mChatId = chatId
        }
    }

    private fun onOutgoingFileMessageSaved(message: AppMessage) {
        saveChatId(message.chatId)

        val file = message.attachedFile
        val mime = Utils.getMimeType(applicationContext, file.getUri().path!!)

        val request = RequestFactory.getSendFileRequest(mAccountId, message.id, mDestination, file.getUri(), file.getName(), mime)
        executeRequest(request)
    }

    private fun saveNewOutMessageAndSent(builder: MessageBuilder) {
        Repositories.instance.messages
                .saveMessage(builder)
                .fromIOToMain()
                .subscribe(Consumer { data -> sendMessageImpl(data) }, ignore())
    }

    private fun sendMessageImpl(message: AppMessage) {
        saveChatId(message.chatId)

        executeSendRequestAndPlayRingtone(message)
    }

    private fun executeSendRequestAndPlayRingtone(message: AppMessage) {
        val context = applicationContext

        val request = RequestFactory.getSendMessageRequest(message)
        executeRequest(request)

        val ringtoneUri = Uri.parse(NotificationHelper.getOutgoingRingtoneUri(context))
        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone.play()
    }

    @OnGuiCreated
    private fun resolveDraftMessageText() {
        view?.setDraftMessageText(mDraftMessageText)
    }

    override fun onDestroyed() {
        mAudioRecordWrapper.release()
        mRecordTimeLookup.stop()
        mRecordTimeLookup.callback = null

        mVoicePlayer.release()
        mVoicePlayerLookup.stop()
        mVoicePlayerLookup.callback = null
        super.onDestroyed()
    }

    fun fireAttachOrStopRecordButtonClick() {
        if (isRecordNow) {
            cancelRecording()
        } else {
            view?.startAttachFileActivity()
        }
    }

    private fun cancelRecording() {
        mAudioRecordWrapper.stopRecording()
        onRecordingStateChanged()
    }

    @OnGuiCreated
    private fun resolveRecordingTimeView() {
        if (isRecordNow) {
            view?.displayRecordingDuration(mAudioRecordWrapper.currentRecordDuration)
        }
    }

    private fun hasAudioRecordPermissions(): Boolean {
        if (!Utils.hasMarshmallow()) {
            return true
        }

        val context = applicationContext
        val hasRecordPermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        val hasWritePermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return hasRecordPermission == PackageManager.PERMISSION_GRANTED && hasWritePermission == PackageManager.PERMISSION_GRANTED
    }

    fun fireRecordPermissionsResolved() {
        if (hasAudioRecordPermissions()) {
            startRecordImpl()
        }
    }

    fun onBackPressed(): Boolean {
        if (isRecordNow) {
            cancelRecording()
            return false
        }

        return true
    }

    private fun loadMyInfo(): Single<Pair<Account, Contact>> {
        val accounts = Repositories.instance.accountsRepository
        val contacts = Repositories.instance.contactsRepository
        return accounts
                .findById(mAccountId)
                .flatMapSingle { account ->
                    contacts.getByJid(account.buildBareJid())
                            .map { contact -> Pair.create(account, contact) }
                }
    }

    private fun loadAccountInfo() {
        appendDisposable(loadMyInfo()
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe { pair -> onAccountInfoResolved(pair.first, pair.second) })
    }

    private fun onAccountInfoResolved(account: Account, contact: Contact) {
        this.mAccount = account
        this.mMyContact = contact
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_ACCOUNT, mAccount)
        if (mChatId != null) {
            outState.putInt(SAVE_CHAT_ID, mChatId!!)
        }
    }

    fun fireFileTransferAcceptClick(message: AppMessage) {
        if (!AppPerms.hasWriteStoragePermision(applicationContext)) {
            view?.requestWriteStoragePermission()
            return
        }

        val request = RequestFactory.getAcceptFileTranferRequest(message.id)
        executeRequest(request)
    }

    fun fireWritePermissionResolved() {
        if (AppPerms.hasWriteStoragePermision(applicationContext)) {
            showShortToast(view, R.string.permission_granted_text)
        } else {
            showShortToast(view, R.string.permission_is_not_granted_text)
        }
    }

    fun fireFileTransferDeclineClick(message: AppMessage) {
        val request = RequestFactory.getCancelFileTranferRequest(message.id)
        executeRequest(request)
    }

    fun fireFileSelected(file: File) {
        sendFile(file)
    }

    fun fireSubscriptionDeclineClick(message: AppMessage) {
        val request = RequestFactory.getDeclineSubscriptionRequest(mAccount,
                message.destination, message.id)
        executeRequest(request)
    }

    fun fireSubscriptionAcceptClick(message: AppMessage) {
        val request = RequestFactory.getAcceptSubscriptionRequest(mAccount, message.destination, message.id)
        executeRequest(request)
    }

    fun fireInputStreamExist(uri: Uri, type: String?) {
        view?.showUploadStreamConfirmationDialog(uri, type)
    }

    fun fireInputStreamUploadConfirmed(uri: Uri, mime: String?) {
        val localFilePath = PhotoGalleryImageProvider.getRealPathFromURI(applicationContext, uri)

        val fileName: String
        if (TextUtils.isEmpty(localFilePath)) {
            fileName = "file_" + uri.lastPathSegment + "." + Utils.getImageExt(mime)
        } else {
            val file = File(localFilePath!!)
            fileName = file.name
        }

        val appFile = AppFile(uri, fileName)

        val builder = MessageBuilder(mAccountId)
                .setAppFile(appFile)
                .setDestination(mDestination)
                .setSenderId(if (mMyContact == null) null else mMyContact!!.id)
                .setSenderJid(mAccount!!.buildBareJid())
                .setDate(Unixtime.now())
                .setType(AppMessage.TYPE_OUTGOING_FILE)
                .setChatId(mChatId)
                .setOut(true)
                .setStatus(AppMessage.STATUS_WAITING_FOR_REASON)
                .setReadState(false)

        saveOutgoingFileMessageAndSend(builder)
    }

    fun fireUploadInputStreamDialogDissmiss() {
        view?.removeInputStreamExtra()
    }

    fun fireStartOtrClick() {
        executeRequest(RequestFactory.getStartOTRRequest(mAccount, mDestination))
    }

    fun fireEndOtrClick() {
        executeRequest(RequestFactory.getEndOTRRequest(mAccount, mDestination))
    }

    fun fireRefreshOtrClick() {
        executeRequest(RequestFactory.getRefreshOTRRequest(mAccount, mDestination))
    }

    fun fireMessageResentClick(message: AppMessage) {
        sendMessageImpl(message)
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            loadMore()
        }
    }

    private fun loadMore() {
        if (mLoadingNow) return

        val criteria = MessageCriteria()
                .setCount(COUNT_PER_LOAD)
                .setDestination(mDestination)
                .setChatId(mChatId)
                .setAccountId(mAccountId)
                .setStartMessageId(firstMessageId)
                .setIgnoreContactIds(mAvatarResorce.createContainedIdsSet())

        load(criteria)
    }

    private fun canLoadMore(): Boolean {
        return !mEndOfContent && !mLoadingNow && safeCountOf(mData) > 0
    }

    fun fireDeleteConfirmClick(mids: Set<Int>) {
        if (mData.isEmpty() || Utils.isEmpty(mids)) {
            return
        }

        val start = System.currentTimeMillis()

        val chatDeleted = Repositories.instance
                .messages
                .deleteMessages(mChatId!!, mids)
                .blockingGet()

        if (removeIf(mData) { message -> mids.contains(message.id) } > 0) {
            view?.notifyDataSetChanged()
        }
    }

    private fun clearSelection() {
        for (message in mData) {
            message.isSelected = false
        }

        view?.notifyDataSetChanged()
    }

    @OnGuiCreated
    private fun resolveActionMode() {
        view?.run {
            val count = selectedMessagesCount
            if (count == 0) {
                finishActionMode()
            } else {
                showActionMode(count.toString())
            }
        }
    }

    fun fireMessageLongClick(position: Int, message: AppMessage) {
        message.isSelected = !message.isSelected
        resolveActionMode()

        view?.notifyItemChanged(position)
    }

    fun fireMessageClick(position: Int, message: AppMessage) {
        if (selectedMessagesCount > 0) {
            message.isSelected = !message.isSelected
            resolveActionMode()
            view?.notifyItemChanged(position)
            return
        }

        if (message.status == AppMessage.STATUS_ERROR) {
            view?.showReSentMessageDialog(message)
        }
    }

    fun fireActionModeDeleteClick() {
        view?.showDeleteMessagesConfirmation(markedMessagesIds)
    }

    fun fireActionModeDestroy() {
        clearSelection()
        view?.notifyDataSetChanged()
    }

    fun fireIncomeFilesClick() {
        view?.goToIncomeFiles(mDestination)
    }

    private fun showOtrMenuImpl() {
        @OtrState
        val currentOtrState = otrManager.getSessionState(mAccountId, mDestination)
        val canStart = currentOtrState == OtrState.PLAINTEXT || currentOtrState == OtrState.FINISHED
        val canEnd = currentOtrState == OtrState.ENCRYPTED
        val canRefresh = currentOtrState == OtrState.ENCRYPTED

        view?.showOTRActionsMenu(canStart, canRefresh, canEnd)
    }

    fun fireOtrOptionClick() {
        showOtrMenuImpl()
    }

    fun fireAudioHolderCreate(holderId: Int, message: AppMessage) {
        bindAudioHolderById(holderId, message)
    }

    private fun bindAudioHolderById(holderId: Int, message: AppMessage) {
        val isCurrent = mVoicePlayer.playingVoiceId != null && message.id == mVoicePlayer.playingVoiceId

        view?.bindAudioViewHolderById(holderId, isCurrent, !mVoicePlayer.isSupposedToPlay,
                mVoicePlayer.duration, mVoicePlayer.position)
    }

    private fun rebindAllAudioPlayHolders() {
        view?.bindAllAudioViewHolders(mVoicePlayer.playingVoiceId, !mVoicePlayer.isSupposedToPlay, mVoicePlayer.duration, mVoicePlayer.position)
    }

    fun fireAudioPlayButtonClick(holderId: Int, message: AppMessage) {
        try {
            val uri = message.attachedFile.getUri()

            if (mVoicePlayer.toggle(applicationContext, message.id, uri)) {
                rebindAllAudioPlayHolders()
            } else {
                bindAudioHolderById(holderId, message)
            }

            syncVoicePlayerLookupState()
        } catch (e: Exception) {
            e.printStackTrace()
            showError(view, e)
        }

    }

    fun fireAudioSeekBarMovedByUser(position: Int, message: AppMessage) {
        if (mVoicePlayer.playingVoiceId != null && mVoicePlayer.playingVoiceId == message.id) {
            mVoicePlayer.seekTo(position)
        }
    }

    private fun syncVoicePlayerLookupState() {
        val enabled = isGuiResumed && mVoicePlayer.isSupposedToPlay
        if (enabled) {
            mVoicePlayerLookup.start()
        } else {
            mVoicePlayerLookup.stop()
        }
    }

    private fun onVoicePlayerLookupIteration() {
        if (!mVoicePlayer.isSupposedToPlay) {
            syncVoicePlayerLookupState()
        }

        if (isGuiReady) {
            rebindAllAudioPlayHolders()
        }
    }

    override fun onGuiPaused() {
        super.onGuiPaused()
        syncVoicePlayerLookupState()
        syncRecordingLookupState()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        syncVoicePlayerLookupState()
        syncRecordingLookupState()

        rebindAllAudioPlayHolders()
    }

    companion object {
        private const val COUNT_PER_LOAD = 50
        private const val SAVE_ACCOUNT = "save_account"
        private const val SAVE_CHAT_ID = "save_chat_id"
    }
}