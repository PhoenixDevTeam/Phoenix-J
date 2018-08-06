package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import org.jivesoftware.smack.roster.RosterEntry
import org.jxmpp.jid.Jid

interface IContactsRepository {
    fun actualizeUserAndGet(account: Int, jid: String): Single<User>

    fun getContacts(): Single<List<Contact>>

    fun observeAddings(): Flowable<List<String>>

    fun observeVcards(): Flowable<Collection<User>>

    fun observeDeleting(): Flowable<List<String>>

    fun handleContactsAdded(account: Int, contacts: Collection<RosterEntry>): Completable

    fun handleContactsDeleted(account: Int, jids: Collection<Jid>): Completable
}