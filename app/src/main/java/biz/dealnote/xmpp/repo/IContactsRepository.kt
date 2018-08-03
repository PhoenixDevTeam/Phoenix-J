package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import org.jivesoftware.smack.roster.RosterEntry

interface IContactsRepository {
    fun actualizeUser(account: Int, jid: String): Single<User>

    fun getContacts(): Single<List<Contact>>

    fun observeAddings(): Flowable<List<String>>

    fun upsert(account: Int, contacts: Collection<RosterEntry>): Completable
}