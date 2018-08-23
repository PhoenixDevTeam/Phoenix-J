package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.Messages
import biz.dealnote.xmpp.db.Storages
import biz.dealnote.xmpp.db.exception.RecordDoesNotExistException
import biz.dealnote.xmpp.db.interfaces.IMessagesStorage
import biz.dealnote.xmpp.model.MessageBuilder
import biz.dealnote.xmpp.model.MessageUpdate
import biz.dealnote.xmpp.model.Msg
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.security.IOtrManager
import biz.dealnote.xmpp.service.IXmppConnectionManager
import biz.dealnote.xmpp.service.IXmppRxApi
import biz.dealnote.xmpp.util.*
import biz.dealnote.xmpp.util.Optional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.impl.JidCreate
import java.util.*

class MessageRepository(private val api: IXmppRxApi,
                        private val otr: IOtrManager,
                        private val storages: Storages,
                        private val idGenerator: IStanzaIdGenerator,
                        connectionManager: IXmppConnectionManager) : IMessageRepository {

    override fun saveOutgoindPresenceMessage(accountId: Int, type: Int, destination: String, senderJid: String): Completable {
        return obtainSenderId(accountId)
                .flatMapCompletable { user ->
                    val builder = MessageBuilder(accountId)
                            .also {
                                it.type = type
                                it.destination = destination
                                it.senderJid = user.jid
                                it.senderId = user.id
                                it.uniqueServiceId = idGenerator.next()
                                it.chatId = chatIds[destination]
                                it.isOut = true
                                it.date = Unixtime.now()
                                it.status = Msg.STATUS_WAITING_FOR_REASON
                            }
                    return@flatMapCompletable messagesStorage.saveMessage(builder)
                            .doOnSuccess {
                                chatIds[destination] = it.chatId
                            }
                            .ignoreElement()
                }
    }

    private val compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable.add(connectionManager.observeNewMessages()
                .flatMapSingle {
                    val account = it.account
                    val message = it.data

                    if (account.buildBareJid() == message.from.asBareJid().toString()) {
                        return@flatMapSingle Single.just(Optional.empty<Msg>())
                    }

                    val body = message.body
                    val from = message.from.asBareJid().toString()

                    if (body.isNullOrEmpty()) {
                        return@flatMapSingle Single.just(Optional.empty<Msg>())
                    }

                    val encryptedBody = otr.handleInputMessage(account.id, from, body)
                    if (encryptedBody.isNullOrEmpty()) {
                        return@flatMapSingle Single.just(Optional.empty<Msg>())
                    }

                    val encrypted = encryptedBody != body
                    val type = Messages.getAppTypeFrom(message.type)

                    val builder = MessageBuilder(account.id)
                            .setDestination(from)
                            .setSenderJid(from)
                            .setType(type)
                            .setBody(encryptedBody)
                            .setDate(Unixtime.now())
                            .setOut(false)
                            .setReadState(false)
                            .setStatus(Msg.STATUS_SENT)
                            .setUniqueServiceId(message.stanzaId)
                            .setWasEncrypted(encrypted)

                    return@flatMapSingle messagesStorage.saveMessage(builder).map { msg -> Optional.wrap(msg) }
                }
                .subscribeOn(Schedulers.io())
                .filter { it.nonEmpty() }
                .map { it.get()!! }
                .subscribe(Consumer { onIncomingMessage(it) }, RxUtils.ignore()))

        compositeDisposable.add(connectionManager.observeRosterPresenses()
                .flatMapSingle {
                    val account = it.account
                    val presence = it.data

                    val type = Messages.appPresenseTypeFromApi(presence.type)
                            ?: return@flatMapSingle Single.just(Optional.empty<Msg>())

                    val from = presence.from.asBareJid().toString()
                    val builder = MessageBuilder(account.id)
                            .setType(type)
                            .setDate(Unixtime.now())
                            .setDestination(from)
                            .setSenderJid(from)
                            .setOut(false)

                    return@flatMapSingle messagesStorage.saveMessage(builder).map { msg -> Optional.wrap(msg) }
                }
                .subscribeOn(Schedulers.io())
                .filter { it.nonEmpty() }
                .map { it.get()!! }
                .subscribe(Consumer { onIncomingMessage(it) }, RxUtils.ignore()))
    }

    private fun onIncomingMessage(message: Msg) {

    }

    private val messagesProcessor: PublishProcessor<Msg> = PublishProcessor.create()

    override fun observeNewMessages(): Flowable<Msg> = messagesProcessor.onBackpressureBuffer()

    private val messagesStorage: IMessagesStorage get() = storages.messages

    private val chatIds: MutableMap<String, Int> = Collections.synchronizedMap(HashMap())

    override fun saveTextMessage(accountId: Int, destination: String, text: String, type: Int): Single<Msg> {
        return obtainSenderId(accountId)
                .flatMap { user ->
                    val builder = MessageBuilder(accountId)
                            .also {
                                it.body = text
                                it.type = type
                                it.destination = destination
                                it.senderJid = user.jid
                                it.senderId = user.id
                                it.uniqueServiceId = idGenerator.next()
                                it.chatId = chatIds[destination]
                                it.isOut = true
                                it.date = Unixtime.now()
                                it.status = Msg.STATUS_IN_QUEUE
                            }

                    return@flatMap messagesStorage.saveMessage(builder)
                            .doOnSuccess {
                                chatIds[destination] = it.chatId
                            }
                }
    }

    @Volatile
    private var sending = false

    override fun startSendingQueue() {
        if (sending) return

        val disposable = messagesStorage.firstWithStatus(Msg.STATUS_IN_QUEUE)
                .flatMap {
                    val message = it.get()
                            ?: return@flatMap Single.error<Msg>(RecordDoesNotExistException())

                    return@flatMap sendImpl(message).andThen(Single.just(message))
                }
                .fromIOToMain()
                .subscribe({ onSent(it) }, { onSendingFail(it) })
    }

    private fun onSent(message: Msg) {
        sending = false
        startSendingQueue()
    }

    private fun onSendingFail(t: Throwable) {
        sending = false

        if (t is RecordDoesNotExistException) {
            return
        }

        startSendingQueue()
    }

    private fun sendImpl(message: Msg): Completable {
        val dto = Message(JidCreate.from(message.destination), Messages.getTypeFrom(message.type))
        dto.body = message.body
        dto.stanzaId = message.stanzaId

        return changeStatus(message.id, Msg.STATUS_SENDING)
                .andThen(api.sendMessage(message.accountId, dto))
                .andThen(changeStatus(message.id, Msg.STATUS_SENT))
                .onErrorResumeNext {
                    messagesStorage.updateStatus(message.chatId, Msg.STATUS_IN_QUEUE, Msg.STATUS_ERROR)
                            .andThen(Completable.error(it))
                }
    }

    private fun changeStatus(id: Int, status: Int): Completable = messagesStorage.updateMessage(id, MessageUpdate.simpleStatusChange(status))

    private fun obtainSenderId(accountId: Int): Single<User> {
        return storages.accounts.getById(accountId)
                .flatMap { account ->
                    storages.users
                            .getByJid(account.buildBareJid())
                }
    }
}