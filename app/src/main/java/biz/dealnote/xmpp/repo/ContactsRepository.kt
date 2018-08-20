package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.Storages
import biz.dealnote.xmpp.db.entity.ContactEntity
import biz.dealnote.xmpp.db.entity.UserEntity
import biz.dealnote.xmpp.model.AccountId
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.Msg
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.service.IXmppRxApi
import biz.dealnote.xmpp.util.Optional
import biz.dealnote.xmpp.util.RxUtils
import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.XMPPError
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import java.util.concurrent.Executors

class ContactsRepository(private val api: IXmppRxApi,
                         private val storages: Storages,
                         private val messages: IMessageRepository) : IContactsRepository {

    override fun addContact(accountId: Int, jid: String): Completable {
        val bareJid = JidCreate.bareFrom(jid)
        return storages.accounts.getById(accountId)
                .flatMapCompletable {account ->
                    api.addRosterEntry(accountId, bareJid, jid)
                            .andThen(api.sendPresence(accountId, bareJid, Presence.Type.subscribe))
                            .andThen(messages.saveOurgoindPresenceMessage(accountId, Msg.TYPE_SUBSCRIBE, bareJid.toString(), account.buildBareJid()))
                            .andThen(api.sendPresence(accountId, bareJid, Presence.Type.subscribed))
                            .andThen(messages.saveOurgoindPresenceMessage(accountId, Msg.TYPE_SUBSCRIBED, bareJid.toString(), account.buildBareJid()))
                }
    }

    private val contactDeletiongProcessor: PublishProcessor<List<String>> = PublishProcessor.create()
    private val contactsAddProcessor: PublishProcessor<List<String>> = PublishProcessor.create()
    private val vcardProcessor: PublishProcessor<Collection<User>> = PublishProcessor.create()
    private val monoScheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun handleContactsAdded(account: Int, contacts: Collection<RosterEntry>): Completable {
        return storages.users.putContacts(account, contacts)
                .andThen(Single.just(contacts))
                .map { entries -> entries.map { it.jid.asBareJid().toString() } }
                .doOnSuccess { data ->
                    contactsAddProcessor.onNext(data)
                    startVcardsRefreshing()
                }
                .ignoreElement()
    }

    private fun startVcardsRefreshing() {
        val minUpdateTime = System.currentTimeMillis() - (12 * 60 * 60 * 1000) // 12 hours

        compositeDisposable.add(storages.users.getContacts()
                .map { entities ->
                    entities.filter { entity ->
                        entity.user.lastVcardUpdateTime.let { it == null || it < minUpdateTime }
                    }
                }
                .flatMapObservable {
                    Observable.fromIterable(it)
                }
                .flatMapSingle {
                    actualizeUser(it.accountId, it.jid)
                            .map { entity -> Optional.wrap(entity) }
                            .onErrorReturn { _ -> Optional.empty() }
                }
                .toList()
                .subscribeOn(monoScheduler)
                .subscribe(Consumer { it -> onVcardsRefreshed(it) }, RxUtils.ignore()))
    }

    private fun onVcardsRefreshed(completed: List<Optional<UserEntity?>>) {
        val users = completed
                .filter {
                    it.nonEmpty()
                }
                .map {
                    entity2Model(it.get()!!)
                }

        if (users.isNotEmpty()) {
            vcardProcessor.onNext(users)
        }
    }

    private fun actualizeUser(account: Int, jid: String): Single<UserEntity> {
        return getOptionalVcard(account, jid)
                .flatMap {
                    return@flatMap storages.users.upsert(jid, it.get())
                }
    }

    private fun getOptionalVcard(account: Int, jid: String): Single<Optional<VCard?>> {
        return api.getVCard(account, jid)
                .map { it -> Optional.wrap(it) }
                .onErrorResumeNext { error ->
                    if (error is XMPPException.XMPPErrorException &&
                            error.xmppError?.condition === XMPPError.Condition.item_not_found) {
                        Single.just(Optional.empty())
                    } else {
                        Single.error(error)
                    }
                }
    }

    override fun handleContactsDeleted(account: Int, jids: Collection<Jid>): Completable {
        val strinsJids = jids.map { it -> it.asBareJid().toString() }
        return storages.users.delete(account, strinsJids)
                .doOnComplete { contactDeletiongProcessor.onNext(strinsJids) }
    }

    override fun observeVcards(): Flowable<Collection<User>> = vcardProcessor.onBackpressureBuffer()

    override fun observeAddings(): Flowable<List<String>> = contactsAddProcessor.onBackpressureBuffer()

    override fun observeDeleting(): Flowable<List<String>> = contactDeletiongProcessor.onBackpressureBuffer()

    override fun actualizeUserAndGet(account: Int, jid: String): Single<User> {
        return actualizeUser(account, jid)
                .map { it ->
                    entity2Model(it)
                }
                .doOnSuccess {
                    vcardProcessor.onNext(listOf(it))
                }
    }

    private fun entity2Model(entity: UserEntity): User {
        return User().apply {
            id = entity.id
            jid = entity.jid
            firstName = entity.firstName
            lastName = entity.lastName
            middleName = entity.middleName
            prefix = entity.prefix
            suffix = entity.suffix
            emailWork = entity.emailWork
            emailHome = entity.emailHome
            organization = entity.organization
            organizationUnit = entity.organizationUnit
            photoMimeType = entity.photoMimeType
            photoHash = entity.photoHash
        }
    }

    private fun entity2Model(entity: ContactEntity): Contact {
        return Contact().apply {
            id = entity.id
            jid = entity.jid
            accountId = AccountId(entity.accountId, entity.accountJid)
            user = entity2Model(entity.user)
            flags = entity.flags
            availableToReceiveMessages = entity.availableToReceiveMessages
            away = entity.away
            presenceMode = entity.presenceMode
            presenceType = entity.presenceType
            presenceStatus = entity.presenceStatus
            type = entity.type
            nick = entity.nick
            priority = entity.priority
        }
    }

    override fun getContacts(): Single<List<Contact>> {
        return storages.users.getContacts()
                .map { list ->
                    list.map {
                        entity2Model(it)
                    }
                }
                .doOnSuccess {
                    startVcardsRefreshing()
                }
    }
}