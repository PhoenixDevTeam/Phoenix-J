package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.db.exception.RecordDoesNotExistException
import biz.dealnote.xmpp.model.MessageBuilder
import biz.dealnote.xmpp.model.MessageUpdate
import biz.dealnote.xmpp.model.Msg
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.security.IOtrManager
import biz.dealnote.xmpp.service.IXmppRxApi
import biz.dealnote.xmpp.util.IStanzaIdGenerator
import biz.dealnote.xmpp.util.Unixtime
import biz.dealnote.xmpp.util.fromIOToMain
import io.reactivex.Completable
import io.reactivex.Single
import org.jivesoftware.smack.packet.Message
import java.util.*
import kotlin.collections.HashMap


class MessageRepository(private val api: IXmppRxApi,
                        private val otr: IOtrManager,
                        private val storages: Repositories,
                        private val idGenerator: IStanzaIdGenerator) : IMessageRepository {

    private val chatIds: MutableMap<String, Int> = Collections.synchronizedMap(HashMap())

    override fun saveMessage(accountId: Int, destination: String, text: String, type: Int): Single<Msg> {
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

                    return@flatMap storages.messages.saveMessage(builder)
                            .doOnSuccess {
                                chatIds[destination] = it.chatId
                            }
                }
    }

    @Volatile
    private var sending = false

    override fun startSendingQueue() {
        if (sending) return

        val disposable = storages.messages.firstWithStatus(Msg.STATUS_IN_QUEUE)
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
        val dto = Message()
        dto.body = message.body
        dto.stanzaId = message.stanzaId

        return changeStatus(message.id, Msg.STATUS_SENDING)
                .andThen(api.sendMessage(message.accountId, dto))
                .andThen(changeStatus(message.id, Msg.STATUS_SENT))
                .onErrorResumeNext {
                    storages.messages.updateStatus(message.chatId, Msg.STATUS_IN_QUEUE, Msg.STATUS_ERROR)
                            .andThen(Completable.error(it))
                }
    }

    private fun changeStatus(id: Int, status: Int): Completable = storages.messages.updateMessage(id, MessageUpdate.simpleStatusChange(status))

    private fun obtainSenderId(accountId: Int): Single<User> {
        return storages.accountsRepository.getById(accountId)
                .flatMap { account ->
                    storages.usersStorage
                            .getByJid(account.buildBareJid())
                }
    }
}