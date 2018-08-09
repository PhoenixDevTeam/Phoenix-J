package biz.dealnote.xmpp.mvp.view

import biz.dealnote.mvp.core.IMvpView
import biz.dealnote.xmpp.model.AccountId
import biz.dealnote.xmpp.model.User

interface IContactCardView: IMvpView, IErrorView {
    fun displayUser(user: User)
    fun openChat(accountId: AccountId, jid: String)
}