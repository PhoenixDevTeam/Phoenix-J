package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.entity.ContactEntity
import biz.dealnote.xmpp.db.entity.UserEntity
import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.model.AccountId
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.service.request.IXmppRxApi
import biz.dealnote.xmpp.util.RxUtils
import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.roster.RosterEntry
import org.jxmpp.jid.Jid
import java.util.concurrent.Executors

class ContactsRepository(private val api: IXmppRxApi, private val storage: IUsersStorage) : IContactsRepository {

    private val contactDeletiongProcessor: PublishProcessor<List<String>> = PublishProcessor.create()
    private val contactsAddProcessor: PublishProcessor<List<String>> = PublishProcessor.create()
    private val monoScheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun handleContactsAdded(account: Int, contacts: Collection<RosterEntry>): Completable {
        return storage.putContacts(account, contacts)
                .andThen(Single.just(contacts))
                .map { entries -> entries.toList().map { it.jid.asBareJid().toString() } }
                .doOnSuccess {
                    data -> contactsAddProcessor.onNext(data)
                    startVcardsRefreshing()
                }
                .ignoreElement()
    }

    private fun startVcardsRefreshing() {
        val minUpdateTime = System.currentTimeMillis() - (12 * 60 * 60 * 1000)

        compositeDisposable.add(storage.getContacts()
                .map {
                    it.filter {
                        it.user.lastVcardUpdateTime.let { it == null || it < minUpdateTime }
                    }
                }
                .flatMapObservable {
                    Observable.fromIterable(it)
                }
                .flatMapCompletable {
                    actualizeUser(it.accountId, it.jid).ignoreElement().onErrorComplete()
                }
                .subscribeOn(monoScheduler)
                .subscribe(RxUtils.dummy(), RxUtils.ignore()))
    }

    private fun actualizeUser(account: Int, jid: String): Single<UserEntity> {
        return api.getVCard(account, jid)
                .flatMap {
                    return@flatMap storage.upsert(jid, it)
                }
    }

    override fun handleContactsDeleted(account: Int, jids: Collection<Jid>): Completable {
        val strinsJids = jids.map { it -> it.asBareJid().toString() }
        return storage.delete(account, strinsJids)
                .doOnComplete { contactDeletiongProcessor.onNext(strinsJids) }
    }

    override fun observeAddings(): Flowable<List<String>> = contactsAddProcessor.onBackpressureBuffer()

    override fun observeDeleting(): Flowable<List<String>> = contactDeletiongProcessor.onBackpressureBuffer()

    override fun actualizeUserAndGet(account: Int, jid: String): Single<User> {
        return actualizeUser(account, jid).map { it ->
            entity2Model(it)
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
        return storage.getContacts()
                .map {
                    it.map {
                        entity2Model(it)
                    }
                }
                .doOnSuccess {
                    startVcardsRefreshing()
                }
    }
}