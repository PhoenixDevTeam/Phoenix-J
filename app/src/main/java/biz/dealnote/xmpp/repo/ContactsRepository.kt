package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.service.request.IXmppRxApi
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import org.jivesoftware.smack.roster.RosterEntry

class ContactsRepository(private val api: IXmppRxApi, private val storage: IUsersStorage): IContactsRepository {

    override fun update(contacts: Collection<RosterEntry>): Single<Contact> {
        return Single.error(Exception())
    }

    override fun observeContacts(): Flowable<List<Contact>> = contactProcessor.onBackpressureBuffer()

    private val contactProcessor: PublishProcessor<List<Contact>> = PublishProcessor.create()

    override fun actualizeUser(account: Int, jid: String): Single<User> {
        return api.getVCard(account, jid)
                .flatMap {
                    return@flatMap storage.upsert(jid, it).andThen(storage.getByJid(jid))
                }
    }

    override fun getContacts(): Single<List<Contact>> {
        return storage.getContacts()
    }
}