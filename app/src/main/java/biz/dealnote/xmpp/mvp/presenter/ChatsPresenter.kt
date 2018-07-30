package biz.dealnote.xmpp.mvp.presenter

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import biz.dealnote.mvp.reflect.OnGuiCreated
import biz.dealnote.xmpp.Extra
import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.model.Chat
import biz.dealnote.xmpp.model.ChatUpdateModel
import biz.dealnote.xmpp.mvp.presenter.base.RequestSupportPresenter
import biz.dealnote.xmpp.mvp.view.IChatsView
import biz.dealnote.xmpp.util.Utils
import biz.dealnote.xmpp.util.Utils.addElementToList
import biz.dealnote.xmpp.util.Utils.indexOf
import biz.dealnote.xmpp.util.toMainThread
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.Comparator

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
class ChatsPresenter(savedInstanceState: Bundle?) : RequestSupportPresenter<IChatsView>(savedInstanceState) {

    private val mChats: ArrayList<Chat> = ArrayList()

    init {
        loadAll()

        val repositories = Repositories.instance

        appendDisposable(repositories.chats.observeChatUpdate()
                .toMainThread()
                .subscribe(this::onChatUpdate))

        appendDisposable(repositories.chats.observeChatCreation()
                .toMainThread()
                .subscribe(this::onChatCreated))

        appendDisposable(repositories.chats.observeChatDeletion()
                .toMainThread()
                .subscribe(this::onChatDelete))

        appendDisposable(repositories.accountsRepository.observeDeletion()
                .toMainThread()
                .subscribe(this::onAccountDelete))
    }

    private fun onAccountDelete(id: Int) {
        var hasChanges = false
        val iterator = mChats.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().accountId == id) {
                iterator.remove()
                hasChanges = true
            }
        }

        if (hasChanges) {
            view?.notifyDataChanged()
            resolveEmptyTextVisibility()
        }
    }

    private fun onChatDelete(id: Int) {
        val index = indexOf(mChats, id)

        if (index != -1) {
            val chat = mChats[index]

            mChats.removeAt(index)

            view?.notifyDataChanged()
            resolveEmptyTextVisibility()

            if (chat.unreadCount > 0) {
                notifyAboutUnreadChatsCount()
            }
        }
    }

    private fun onChatCreated(chat: Chat) {
        addElementToList(chat, mChats, COMPARATOR)
        view?.notifyDataChanged()
        resolveEmptyTextVisibility()
        notifyAboutUnreadChatsCount()
    }

    private fun onChatUpdate(update: ChatUpdateModel) {
        val index = indexOf(mChats, update.chatId)

        if (index != -1) {
            applyUpdate(mChats[index], update)
        }

        update.hiddenUpdate?.run {
            if (isHidden && index != -1) {
                mChats.removeAt(index)
                view?.notifyDataChanged()
                return
            }
        }

        if (index != -1) {
            val chat = mChats[index]
            mChats.removeAt(index)
            addElementToList(chat, mChats, COMPARATOR)
            view?.notifyDataChanged()
        } else {
            findChatAndAdd(update.chatId)
        }

        resolveEmptyTextVisibility()
        notifyAboutUnreadChatsCount()
    }

    private fun findChatAndAdd(id: Int) {
        appendDisposable(Repositories.instance.chats
                .findById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { chat ->
                    addElementToList(chat, mChats, COMPARATOR)
                    view?.notifyDataChanged()
                    notifyAboutUnreadChatsCount()
                })
    }

    override fun onGuiCreated(viewHost: IChatsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mChats)
    }

    @OnGuiCreated
    private fun resolveEmptyTextVisibility() {
        view?.setEmptyTextVisible(Utils.isEmpty(mChats))
    }

    private fun loadAll() {
        appendDisposable(Repositories.instance.chats
                .getAll(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data -> onAllChatsLoadSuccess(data) }
        )
    }

    private fun onAllChatsLoadSuccess(chats: List<Chat>) {
        mChats.clear()
        mChats.addAll(chats)
        view?.notifyDataChanged()
        resolveEmptyTextVisibility()

        notifyAboutUnreadChatsCount()
    }

    fun fireChatClick(chat: Chat) {
        view?.goToChat(chat.accountId, chat.destination, chat.id)
    }

    fun fireChatLongClick(chat: Chat) {
        view?.displayChatOptionsDialog(chat)
    }

    private fun notifyAboutUnreadChatsCount() {
        var count = 0
        for (chat in mChats) {
            if (chat.unreadCount > 0) {
                count++
            }
        }

        val intent = Intent(WHAT_UNREAD_CHATS_COUNT)
        intent.putExtra(Extra.COUNT, count)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun applyUpdate(chat: Chat, update: ChatUpdateModel) {
        update.lastMessageUpdate?.run {
            chat.isLastMessageOut = lastMessageOut
            chat.lastMessageText = lastMessageText
            chat.lastMessageTime = lastMessageTime
            chat.lastMessageType = lastMessageType
        }

        update.unreadCountUpdate?.run {
            chat.unreadCount = count
        }

        update.hiddenUpdate?.run {
            chat.isHidden = isHidden
        }
    }

    companion object {
        private val COMPARATOR: Comparator<Chat> = Comparator { lhs, rhs ->
            if (rhs.lastMessageTime < lhs.lastMessageTime) -1 else if (rhs.lastMessageTime == lhs.lastMessageTime) 0 else 1
        }

        const val WHAT_UNREAD_CHATS_COUNT = "WHAT_UNREAD_CHATS_COUNT"
    }
}