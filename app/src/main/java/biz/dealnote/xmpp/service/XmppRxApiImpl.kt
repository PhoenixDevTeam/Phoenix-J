package biz.dealnote.xmpp.service

import biz.dealnote.xmpp.util.RxUtils.comletableFromRunnable
import biz.dealnote.xmpp.util.RxUtils.singleFromCallable
import io.reactivex.Completable
import io.reactivex.Single
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class XmppRxApiImpl(private val connectionManager: IXmppConnectionManager,
                    private val executor: ExecutorService) : IXmppRxApi {

    override fun addRosterEntry(acccount: Int, jid: BareJid, name: String, group: String?): Completable {
        return connectionManager.obtainConnected(acccount)
                .flatMapCompletable { connection ->
                    comletableFromRunnable(executor, Runnable {
                        val roster = Roster.getInstanceFor(connection)
                        roster.createEntry(jid, name, if (group != null) arrayOf(group) else null)
                    })
                }
    }

    override fun sendPresence(acccount: Int, jid: Jid, type: Presence.Type): Completable {
        return connectionManager.obtainConnected(acccount)
                .flatMapCompletable { connection ->
                    comletableFromRunnable(executor, Runnable {
                        connection.sendStanza(Presence(type).apply {
                            to = jid
                        })
                    })
                }
    }

    override fun getVCard(acccount: Int, jid: String): Single<VCard> {
        return connectionManager.obtainConnected(acccount)
                .flatMap { connection ->
                    singleFromCallable(executor, Callable {
                        val entityBareJid = JidCreate.entityBareFrom(jid)
                        return@Callable VCardManager.getInstanceFor(connection).loadVCard(entityBareJid)
                    })
                }
    }

    override fun sendMessage(acccount: Int, message: Message): Completable {
        return connectionManager.obtainConnected(acccount)
                .flatMapCompletable { connection ->
                    comletableFromRunnable(executor, Runnable {
                        connection.sendStanza(message)
                    })
                }
    }
}