package biz.dealnote.xmpp.mvp.view

import biz.dealnote.mvp.core.IMvpView
import biz.dealnote.xmpp.model.Chat

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
interface IChatsView : IMvpView {
    fun displayData(chats: List<Chat>)
    fun notifyDataChanged()
    fun setEmptyTextVisible(visible: Boolean)
    fun goToChat(accountId: Int, destination: String, chatId: Int?)
}