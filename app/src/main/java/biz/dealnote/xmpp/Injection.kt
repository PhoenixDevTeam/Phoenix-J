package biz.dealnote.xmpp

import biz.dealnote.xmpp.db.Storages
import biz.dealnote.xmpp.repo.ContactsRepository
import biz.dealnote.xmpp.repo.IContactsRepository
import biz.dealnote.xmpp.repo.IMessageRepository
import biz.dealnote.xmpp.repo.MessageRepository
import biz.dealnote.xmpp.security.IOtrManager
import biz.dealnote.xmpp.security.OTRManager
import biz.dealnote.xmpp.service.*
import biz.dealnote.xmpp.transfer.FileTransferer
import biz.dealnote.xmpp.transfer.IFileTransferer
import biz.dealnote.xmpp.util.AppPrefs
import biz.dealnote.xmpp.util.IStanzaIdGenerator
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by admin on 24.04.2017.
 * phoenix-for-xmpp
 */
object Injection {

    private val xmppExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val fileTransferer: IFileTransferer by lazy { FileTransferer(App.getInstance(), Storages.INSTANCE.messages) }
    private val otrManager: IOtrManager by lazy { OTRManager(App.getInstance()) }
    private val connectionManager: IOldConnectionManager by lazy { OldConnectionManager(App.getInstance(), Storages.INSTANCE.accounts) }
    private val STORAGE: Storages by lazy { Storages(App.getInstance()) }

    fun proviceContactsRepository() = contactsRepository

    fun provideConnectionManager(): IOldConnectionManager = connectionManager

    fun provideMainThreadScheduler(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

    fun provideTransferer(): IFileTransferer {
        return fileTransferer
    }

    fun provideOtrManager(): IOtrManager {
        return otrManager
    }

    fun provideRepositories(): Storages {
        return Storages.INSTANCE
    }

    private val idGenerator: IStanzaIdGenerator by lazy {
        object : IStanzaIdGenerator {
            override fun next() = AppPrefs.generateMessageStanzaId(App.getInstance())
        }
    }

    val messageRepository: IMessageRepository by lazy { MessageRepository(XmppRxApiImpl(xmppConnectionManager, xmppExecutor), otrManager, Storages.INSTANCE, idGenerator, xmppConnectionManager) }

    val xmppConnectionManager: IXmppConnectionManager by lazy { XmppConnectionManager(App.getInstance(), Storages.INSTANCE.accounts) }

    private val contactsRepository: IContactsRepository by lazy {
        ContactsRepository(XmppRxApiImpl(xmppConnectionManager, xmppExecutor), STORAGE, messageRepository)
    }
}