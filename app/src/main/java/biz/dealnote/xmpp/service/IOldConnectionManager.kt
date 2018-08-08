package biz.dealnote.xmpp.service

import biz.dealnote.xmpp.model.Account
import io.reactivex.Flowable
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smackx.filetransfer.FileTransferRequest
import org.jxmpp.jid.Jid

/**
 * Created by admin on 05.11.2016.
 * phoenix-for-xmpp
 */
interface IOldConnectionManager {
    fun registerConnectionFor(account: Account): AbstractXMPPConnection

    fun findConnectionFor(accountId: Int): AbstractXMPPConnection?

    fun findAccountById(accountId: Int): Account?

    fun unregisterFor(accountId: Int): Boolean

    fun observeRosterAdding(): Flowable<AccountAction<Collection<RosterEntry>>>

    fun observeRosterUpdates(): Flowable<AccountAction<Collection<RosterEntry>>>

    fun observeRosterDetetions(): Flowable<AccountAction<Collection<Jid>>>

    fun observeRosterPresenses(): Flowable<AccountAction<Presence>>

    fun observeNewMessages(): Flowable<AccountAction<Message>>

    fun observePresenses(): Flowable<AccountAction<Presence>>

    fun observeIncomeFileRequests(): Flowable<AccountAction<FileTransferRequest>>
}