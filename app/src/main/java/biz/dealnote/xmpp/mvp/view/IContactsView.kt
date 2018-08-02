package biz.dealnote.xmpp.mvp.view

import biz.dealnote.mvp.core.IMvpView
import biz.dealnote.xmpp.model.Contact

interface IContactsView: IMvpView, IErrorView {
    fun displayContacts(contacts: List<Contact>)
}