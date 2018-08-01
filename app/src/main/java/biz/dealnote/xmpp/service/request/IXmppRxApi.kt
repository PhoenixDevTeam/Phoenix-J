package biz.dealnote.xmpp.service.request

import io.reactivex.Single
import org.jivesoftware.smackx.vcardtemp.packet.VCard

interface IXmppRxApi {
    fun getVCard(acccount: Int, jid: String): Single<VCard>
}