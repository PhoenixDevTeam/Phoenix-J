package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.model.Msg
import io.reactivex.Single

interface IMessageRepository {
    /**
     * Save outgoing text message to message queue
     */
    fun saveMessage(accountId: Int, destination: String, text: String, type: Int): Single<Msg>

    fun startSendingQueue()
}