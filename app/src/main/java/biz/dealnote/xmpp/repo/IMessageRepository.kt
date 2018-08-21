package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.model.Msg
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface IMessageRepository {
    /**
     * Save outgoing text message to message queue
     */
    fun saveTextMessage(accountId: Int, destination: String, text: String, type: Int): Single<Msg>

    fun startSendingQueue()

    fun observeNewMessages(): Flowable<Msg>

    fun saveOutgoindPresenceMessage(accountId: Int, type: Int, destination: String, senderJid: String): Completable
}