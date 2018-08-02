package biz.dealnote.xmpp.mvp.view

import biz.dealnote.mvp.core.IMvpView
import biz.dealnote.xmpp.model.User

interface IContactCardView: IMvpView, IErrorView {
    fun displayUser(user: User)
}