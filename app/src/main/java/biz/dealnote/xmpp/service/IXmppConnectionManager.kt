package biz.dealnote.xmpp.service

import io.reactivex.Flowable
import io.reactivex.Single
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.RosterEntry
import org.jxmpp.jid.Jid

interface IXmppConnectionManager {
    fun observeRosterAdding(): Flowable<AccountAction<Collection<RosterEntry>>>

    fun observeRosterUpdates(): Flowable<AccountAction<Collection<RosterEntry>>>

    fun observeRosterDetetions(): Flowable<AccountAction<Collection<Jid>>>

    fun observeRosterPresenses(): Flowable<AccountAction<Presence>>

    fun observeNewMessages(): Flowable<AccountAction<Message>>

    fun observePresenses(): Flowable<AccountAction<Presence>>

    fun obtainConnected(accountId: Int): Single<AbstractXMPPConnection>
}