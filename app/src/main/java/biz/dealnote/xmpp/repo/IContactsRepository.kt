package biz.dealnote.xmpp.repo

import biz.dealnote.xmpp.model.User
import io.reactivex.Single

interface IContactsRepository {
    fun actualizeUser(account: Int, jid: String): Single<User>
}