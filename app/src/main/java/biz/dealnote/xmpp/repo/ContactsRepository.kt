package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.service.request.IXmppRxApi
import io.reactivex.Single

class ContactsRepository(private val api: IXmppRxApi, private val storage: IUsersStorage): IContactsRepository {

    override fun actualizeUser(account: Int, jid: String): Single<User> {
        return api.getVCard(account, jid)
                .flatMap {
                    return@flatMap storage.upsert(jid, it).andThen(storage.getByJid(jid))
                }
    }
}