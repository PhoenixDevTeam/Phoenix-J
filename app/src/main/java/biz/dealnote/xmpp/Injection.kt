package biz.dealnote.xmpp

import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.repo.ContactsRepository
import biz.dealnote.xmpp.repo.IContactsRepository
import biz.dealnote.xmpp.security.IOtrManager
import biz.dealnote.xmpp.security.OTRManager
import biz.dealnote.xmpp.service.IOldConnectionManager
import biz.dealnote.xmpp.service.IXmppConnectionManager
import biz.dealnote.xmpp.service.OldConnectionManager
import biz.dealnote.xmpp.service.XmppConnectionManager
import biz.dealnote.xmpp.service.request.XmppRxApiImpl
import biz.dealnote.xmpp.transfer.FileTransferer
import biz.dealnote.xmpp.transfer.IFileTransferer
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

    private val fileTransferer: IFileTransferer by lazy { FileTransferer(App.getInstance(), Repositories.instance.messages) }
    private val otrManager: IOtrManager by lazy { OTRManager(App.getInstance()) }
    private val connectionManager: IOldConnectionManager by lazy { OldConnectionManager(App.getInstance(), Repositories.instance.accountsRepository) }
    private val contactsStorage: Repositories by lazy { Repositories(App.getInstance()) }
    private val contactsRepository: IContactsRepository by lazy { ContactsRepository(XmppRxApiImpl(connectionManager, xmppExecutor), contactsStorage.usersStorage) }

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

    fun provideRepositories(): Repositories {
        return Repositories.instance
    }

    val xmppConnectionManager: IXmppConnectionManager by lazy { XmppConnectionManager(App.getInstance(), Repositories.instance.accountsRepository) }
}