package biz.dealnote.xmpp.service

import android.content.Context
import biz.dealnote.xmpp.Constants
import biz.dealnote.xmpp.model.Account
import biz.dealnote.xmpp.service.exception.ConnectionAlreadyRegisteredException
import de.duenndns.ssl.MemorizingTrustManager
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.filetransfer.FileTransferManager
import org.jivesoftware.smackx.filetransfer.FileTransferRequest
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Created by admin on 05.11.2016.
 * phoenix-for-xmpp
 */
internal class ConnectionManager(context: Context) : IConnectionManager {

    private val connectionsMap: MutableMap<Account, XMPPTCPConnection>
    private var sslContext: SSLContext? = null

    private val rosterAddingPublisher: PublishProcessor<AccountAction<Collection<RosterEntry>>> = PublishProcessor.create()
    private val rosterUpdatesPublisher: PublishProcessor<AccountAction<Collection<RosterEntry>>> = PublishProcessor.create()
    private val rosterDeletionPublisher: PublishProcessor<AccountAction<Collection<Jid>>> = PublishProcessor.create()
    private val rosterPresenseChangesPublisher: PublishProcessor<AccountAction<Presence>> = PublishProcessor.create()

    private val messagesAddingPublisher: PublishProcessor<AccountAction<Message>> = PublishProcessor.create()
    private val presensePublisher: PublishProcessor<AccountAction<Presence>> = PublishProcessor.create()
    private val fileTransferPublisher: PublishProcessor<AccountAction<FileTransferRequest>> = PublishProcessor.create()

    init {
        this.connectionsMap = ConcurrentHashMap()

        try {
            val trustManager = MemorizingTrustManager(context)
            this.sslContext = SSLContext.getInstance("TLS")
            this.sslContext!!.init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("Unable to create SSL Context")
        } catch (e: KeyManagementException) {
            throw IllegalStateException("Unable to create SSL Context")
        }
    }

    override fun registerConnectionFor(account: Account): AbstractXMPPConnection {
        if (findConnectionFor(account.getId()) != null) {
            throw ConnectionAlreadyRegisteredException()
        }

        val connection = createConnection(account)

        connectionsMap[account] = connection
        onConnectionsListChanged()
        return connection
    }

    private fun createConnection(account: Account): XMPPTCPConnection {
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
                .setCustomSSLContext(sslContext!!)
                .build()

        val connection = XMPPTCPConnection(conf)
        connection.packetReplyTimeout = timeout.toLong()

        val roster = Roster.getInstanceFor(connection)
        roster.addRosterListener(object : RosterListener {
            override fun entriesAdded(addresses: Collection<Jid>) {
                val entries = createRosterEntries(roster, addresses)
                rosterAddingPublisher.onNext(AccountAction(account, entries))
            }

            override fun entriesUpdated(addresses: Collection<Jid>) {
                val entries = createRosterEntries(roster, addresses)
                rosterUpdatesPublisher.onNext(AccountAction(account, entries))
            }

            override fun entriesDeleted(addresses: Collection<Jid>) {
                rosterDeletionPublisher.onNext(AccountAction(account, addresses))
            }

            override fun presenceChanged(presence: Presence) {
                rosterPresenseChangesPublisher.onNext(AccountAction(account, presence))
            }
        })

        connection.addAsyncStanzaListener({ messagesAddingPublisher.onNext(AccountAction(account, it as Message)) }, { stanza -> stanza is Message })
        connection.addAsyncStanzaListener({ presensePublisher.onNext(AccountAction(account, it as Presence)) }, { stanza -> stanza is Presence })

        val manager = ReconnectionManager.getInstanceFor(connection)
        manager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY)
        manager.enableAutomaticReconnection()

        val fileTransferManager = FileTransferManager.getInstanceFor(connection)
        fileTransferManager.addFileTransferListener { request -> fileTransferPublisher.onNext(AccountAction(account, request)) }

        return connection
    }

    override fun findConnectionFor(accountId: Int): AbstractXMPPConnection? {
        for ((key, value) in connectionsMap) {
            if (key.getId() == accountId) {
                return value
            }
        }
        return null
    }

    override fun findAccountById(accountId: Int): Account? {
        for ((key) in connectionsMap) {
            if (key.getId() == accountId) {
                return key
            }
        }
        return null
    }

    override fun unregisterFor(accountId: Int): Boolean {
        for ((key) in connectionsMap) {
            if (key.getId() == accountId) {
                connectionsMap.remove(key)
                return true
            }
        }
        return false
    }

    private fun onConnectionsListChanged() {

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

    override fun observeIncomeFileRequests(): Flowable<AccountAction<FileTransferRequest>> {
        return fileTransferPublisher
    }

    private fun createRosterEntries(roster: Roster, addresses: Collection<Jid>): Collection<RosterEntry> {
        val entries = ArrayList<RosterEntry>(addresses.size)
        for (jid in addresses) {
            entries.add(roster.getEntry(jid.asBareJid()))
        }

        return entries
    }
}