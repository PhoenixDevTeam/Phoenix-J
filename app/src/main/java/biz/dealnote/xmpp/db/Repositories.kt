package biz.dealnote.xmpp.db

import android.content.Context
import android.content.ContextWrapper
import biz.dealnote.xmpp.App
import biz.dealnote.xmpp.db.impl.AccountsRepository
import biz.dealnote.xmpp.db.impl.ChatsRepository
import biz.dealnote.xmpp.db.impl.UsersStorage
import biz.dealnote.xmpp.db.impl.MessagesRepository
import biz.dealnote.xmpp.db.interfaces.IAccountsRepository
import biz.dealnote.xmpp.db.interfaces.IChatsRepository
import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.db.interfaces.IMessagesRepository

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
class Repositories constructor(base: Context) : ContextWrapper(base) {

    val accountsRepository: IAccountsRepository by lazy { AccountsRepository(this) }
    val messages: IMessagesRepository by lazy { MessagesRepository(this) }
    val chats: IChatsRepository by lazy { ChatsRepository(this) }
    val usersStorage: IUsersStorage = UsersStorage(this)

    companion object {
        @JvmStatic
        val instance: Repositories by lazy { Repositories(App.getInstance()) }
    }
}