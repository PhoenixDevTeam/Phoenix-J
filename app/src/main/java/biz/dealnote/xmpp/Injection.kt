package biz.dealnote.xmpp

import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.security.IOtrManager
import biz.dealnote.xmpp.security.OTRManager
import biz.dealnote.xmpp.transfer.FileTransferer
import biz.dealnote.xmpp.transfer.IFileTransferer
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Created by admin on 24.04.2017.
 * phoenix-for-xmpp
 */
object Injection {

    private val fileTransferer: IFileTransferer by lazy { FileTransferer(App.getInstance(), Repositories.instance.messages) }
    private val otrManager: IOtrManager by lazy { OTRManager(App.getInstance()) }

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
}