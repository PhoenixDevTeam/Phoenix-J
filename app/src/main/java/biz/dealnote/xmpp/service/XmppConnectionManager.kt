package biz.dealnote.xmpp.service

import android.content.Context
import biz.dealnote.xmpp.Constants
import biz.dealnote.xmpp.db.interfaces.IAccountsRepository
import biz.dealnote.xmpp.model.Account
import de.duenndns.ssl.MemorizingTrustManager
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.*
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import java.lang.ref.WeakReference
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class XmppConnectionManager(private val context: Context,
                            private val accountsRepository: IAccountsRepository) : IXmppConnectionManager {

    private var sslContext: SSLContext

    private val rosterAddingPublisher: PublishProcessor<AccountAction<Collection<RosterEntry>>> = PublishProcessor.create()
    private val rosterUpdatesPublisher: PublishProcessor<AccountAction<Collection<RosterEntry>>> = PublishProcessor.create()
    private val rosterDeletionPublisher: PublishProcessor<AccountAction<Collection<Jid>>> = PublishProcessor.create()
    private val rosterPresenseChangesPublisher: PublishProcessor<AccountAction<Presence>> = PublishProcessor.create()
    private val messagesAddingPublisher: PublishProcessor<AccountAction<Message>> = PublishProcessor.create()
    private val presensePublisher: PublishProcessor<AccountAction<Presence>> = PublishProcessor.create()

    init {
        try {
            sslContext = SSLContext.getInstance("TLS")
                    .apply {
                        init(null, arrayOf<X509TrustManager>(MemorizingTrustManager(context)), SecureRandom())
                    }
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("Unable to create SSL Context")
        } catch (e: KeyManagementException) {
            throw IllegalStateException("Unable to create SSL Context")
        }
    }

    private fun notifyStanza(account: Account, stanza: Stanza) {
        when (stanza) {
            is Message -> messagesAddingPublisher.onNext(AccountAction(account, stanza))
            is Presence -> presensePublisher.onNext(AccountAction(account, stanza))
        }
    }

    private fun notifyRosterAdded(account: Account, entries: Collection<RosterEntry>) {
        rosterAddingPublisher.onNext(AccountAction(account, entries))
    }

    private fun notifyRosterUpdate(account: Account, entries: Collection<RosterEntry>) {
        rosterUpdatesPublisher.onNext(AccountAction(account, entries))
    }

    private fun notifyRosterDeleted(account: Account, jids: Collection<Jid>) {
        rosterDeletionPublisher.onNext(AccountAction(account, jids))
    }

    private fun notifyPresenceChanged(account: Account, presence: Presence) {
        rosterPresenseChangesPublisher.onNext(AccountAction(account, presence))
    }

    private fun onConnected(entry: Entry) {
        val copy: MutableSet<EmitterWrapper> = HashSet(emitters)
        for (emitter in copy) {
            if (entry.accountId == emitter.accountId) {
                emitter.emitter.onSuccess(entry.connection!!)
            }
        }
    }

    private val emitters: MutableSet<EmitterWrapper> = Collections.synchronizedSet(HashSet())
    private val entryMap: MutableMap<Int, Entry> = HashMap()

    private class EmitterWrapper(val emitter: SingleEmitter<AbstractXMPPConnection>, val accountId: Int)

    override fun obtainConnected(accountId: Int): Single<AbstractXMPPConnection> {
        return Single.create { emitter ->
            synchronized(entryMap) {
                val entry = entryMap.getOrPut(accountId) { Entry(accountId, sslContext, accountsRepository, WeakReference(this@XmppConnectionManager)) }

                if (entry.isConnected()) {
                    emitter.onSuccess(entry.connection!!)
                } else {
                    val wrapper = EmitterWrapper(emitter, accountId)
                    emitter.setCancellable { emitters.remove(wrapper) }
                    emitters.add(wrapper)
                    entry.connect()
                }
            }
        }
    }

    private class Entry(val accountId: Int,
                        val sslContext: SSLContext,
                        val accounts: IAccountsRepository,
                        val managerOld: WeakReference<XmppConnectionManager>) {

        val compositeDisposable = CompositeDisposable()
        var connection: XMPPTCPConnection? = null

        fun connect() {
            synchronized(this) {
                compositeDisposable.add(accounts.getById(accountId)
                        .subscribeOn(Schedulers.io())
                        .subscribe({ account -> onAccountFound(account) }, { _ -> onAccountNotFound() }))
            }
        }

        fun isConnected(): Boolean {
            synchronized(this) {
                return connection.let {
                    if (it == null) {
                        false
                    } else {
                        it.isConnected && it.isAuthenticated
                    }
                }
            }
        }

        fun onAccountNotFound() {

        }

        fun onAccountFound(account: Account) {
            synchronized(this) {
                if (connection != null) return

                connection = createXMPPTCPConnection(account)

                connection?.run {
                    try {
                        connect()
                    } catch (e: Exception) {

                    }
                }
            }
        }

        fun createXMPPTCPConnection(account: Account): XMPPTCPConnection {
            val domain = JidCreate.domainBareFrom(account.buildBareJid())
            val timeout = 30 * 1000

            val conf = XMPPTCPConnectionConfiguration.builder()
                    .setXmppDomain(domain)
                    .setPort(account.port)
                    .setUsernameAndPassword(account.login, account.password)
                    .setHost(account.host)
                    .setSendPresence(true)
                    .setDebuggerEnabled(true)
                    .setResource(Constants.APP_RESOURCE)
                    .setConnectTimeout(timeout)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                    .setCustomSSLContext(sslContext)
                    .build()

            return XMPPTCPConnection(conf).also { connection ->
                connection.packetReplyTimeout = timeout.toLong()

                val roster = Roster.getInstanceFor(connection)
                roster.addRosterListener(object : RosterListener {
                    override fun entriesDeleted(addresses: Collection<Jid>) {
                        managerOld.get()?.notifyRosterDeleted(account, addresses)
                    }

                    override fun presenceChanged(presence: Presence) {
                        managerOld.get()?.notifyPresenceChanged(account, presence)
                    }

                    override fun entriesUpdated(addresses: Collection<Jid>) {
                        managerOld.get()?.notifyRosterUpdate(account, addresses.map { roster.getEntry(it.asBareJid()) })
                    }

                    override fun entriesAdded(addresses: Collection<Jid>) {
                        managerOld.get()?.notifyRosterAdded(account, addresses.map { roster.getEntry(it.asBareJid()) })
                    }
                })

                connection.addAsyncStanzaListener({ packet -> managerOld.get()?.notifyStanza(account, packet) }, { it -> it is Message || it is Presence })

                connection.addConnectionListener(object : AbstractConnectionListener() {
                    override fun connected(connection: XMPPConnection) {
                        (connection as AbstractXMPPConnection).login()
                    }

                    override fun authenticated(connection: XMPPConnection, resumed: Boolean) {
                        managerOld.get()?.onConnected(this@Entry)
                    }
                })

                val manager = ReconnectionManager.getInstanceFor(connection)
                manager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY)
                manager.enableAutomaticReconnection()
            }
        }

        fun release() {
            connection?.disconnect()
            connection = null
            compositeDisposable.dispose()
        }
    }

    override fun observeRosterAdding(): Flowable<AccountAction<Collection<RosterEntry>>> {
        return rosterAddingPublisher
    }

    override fun observeRosterUpdates(): Flowable<AccountAction<Collection<RosterEntry>>> {
        return rosterUpdatesPublisher
    }

    override fun observeRosterDetetions(): Flowable<AccountAction<Collection<Jid>>> {
        return rosterDeletionPublisher
    }

    override fun observeRosterPresenses(): Flowable<AccountAction<Presence>> {
        return rosterPresenseChangesPublisher
    }

    override fun observeNewMessages(): Flowable<AccountAction<Message>> {
        return messagesAddingPublisher
    }

    override fun observePresenses(): Flowable<AccountAction<Presence>> {
        return presensePublisher
    }
}