package biz.dealnote.xmpp.mvp.presenter

import android.os.Bundle
import biz.dealnote.xmpp.Injection
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.mvp.presenter.base.RxSupportPresenter
import biz.dealnote.xmpp.mvp.view.IContactsView
import biz.dealnote.xmpp.repo.IContactsRepository
import biz.dealnote.xmpp.util.fromIOToMain
import biz.dealnote.xmpp.util.subscribeIgnoreErrors
import biz.dealnote.xmpp.util.toMainThread
import io.reactivex.functions.Consumer

class ContactsPresenter(savedState: Bundle?) : RxSupportPresenter<IContactsView>(savedState) {

    private val repository: IContactsRepository = Injection.proviceContactsRepository()

    private var contacts: List<Contact> = ArrayList()
        set(value) {
            field = value
            view?.displayContacts(value)
        }

    init {
        loadCachedUsers()

        appendDisposable(repository.observeAddings()
                .toMainThread()
                .subscribeIgnoreErrors(Consumer { loadCachedUsers() }))
    }

    private fun loadCachedUsers() {
        appendDisposable(repository.getContacts()
                .fromIOToMain()
                .subscribeIgnoreErrors(Consumer { it -> onContactsReceived(it) }))
    }

    private fun onContactsReceived(contacts: List<Contact>) {
        this.contacts = contacts
    }

    override fun onGuiCreated(view: IContactsView) {
        super.onGuiCreated(view)
        view.displayContacts(contacts)
    }
}