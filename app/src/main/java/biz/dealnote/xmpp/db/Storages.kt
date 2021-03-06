package biz.dealnote.xmpp.db

import android.content.Context
import android.content.ContextWrapper
import biz.dealnote.xmpp.App
import biz.dealnote.xmpp.db.impl.AccountsRepository
import biz.dealnote.xmpp.db.impl.ChatsRepository
import biz.dealnote.xmpp.db.impl.MessagesStorage
import biz.dealnote.xmpp.db.impl.UsersStorage
import biz.dealnote.xmpp.db.interfaces.IAccountsRepository
import biz.dealnote.xmpp.db.interfaces.IChatsRepository
import biz.dealnote.xmpp.db.interfaces.IMessagesStorage
import biz.dealnote.xmpp.db.interfaces.IUsersStorage

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
class Storages constructor(base: Context) : ContextWrapper(base) {

    val accounts: IAccountsRepository by lazy { AccountsRepository(this) }
    val messages: IMessagesStorage by lazy { MessagesStorage(this) }
    val chats: IChatsRepository by lazy { ChatsRepository(this) }
    val users: IUsersStorage = UsersStorage(this)

    companion object {
        @JvmStatic
        val INSTANCE: Storages by lazy { Storages(App.getInstance()) }
    }
}