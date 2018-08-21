package biz.dealnote.xmpp.service

import io.reactivex.Completable
import io.reactivex.Single
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.Jid

interface IXmppRxApi {
    fun getVCard(acccount: Int, jid: String): Single<VCard>
    fun sendMessage(acccount: Int, message: Message): Completable
    fun sendPresence(acccount: Int, jid: Jid, type: Presence.Type): Completable
    fun addRosterEntry(acccount: Int, jid: BareJid, name: String, group: String? = null): Completable
}