package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.service.request.IXmppRxApi
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import org.jivesoftware.smack.roster.RosterEntry

class ContactsRepository(private val api: IXmppRxApi, private val storage: IUsersStorage): IContactsRepository {

    override fun upsert(account: Int, contacts: Collection<RosterEntry>): Completable {
        return storage.putContacts(account, contacts)
                .andThen(Single.just(contacts))
                .map { entries -> entries.toList().map { it.jid.asBareJid().toString() } }
                .ignoreElement()
    }

    override fun observeAddings(): Flowable<List<String>> = contactsAddProcessor.onBackpressureBuffer()

    override fun observeAdding(): Flowable<Contact> = contactAddProcessor.onBackpressureBuffer()

    private val contactsAddProcessor: PublishProcessor<List<String>> = PublishProcessor.create()
    private val contactAddProcessor: PublishProcessor<Contact> = PublishProcessor.create()

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