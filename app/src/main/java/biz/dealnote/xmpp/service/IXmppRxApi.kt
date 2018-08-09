package biz.dealnote.xmpp.service

import io.reactivex.Completable
import io.reactivex.Single
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.vcardtemp.packet.VCard

interface IXmppRxApi {
    fun getVCard(acccount: Int, jid: String): Single<VCard>
    fun sendMessage(acccount: Int, message: Message): Completable
}