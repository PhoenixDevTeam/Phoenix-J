package biz.dealnote.xmpp.mvp.presenter

import android.os.Bundle
import biz.dealnote.xmpp.Injection
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.mvp.presenter.base.RxSupportPresenter
import biz.dealnote.xmpp.mvp.view.IContactsView
import biz.dealnote.xmpp.repo.IContactsRepository
import biz.dealnote.xmpp.util.Logger
import biz.dealnote.xmpp.util.fromIOToMain
import biz.dealnote.xmpp.util.subscribeIgnoreErrors
import biz.dealnote.xmpp.util.toMainThread
import io.reactivex.functions.Consumer

class ContactsPresenter(savedState: Bundle?) : RxSupportPresenter<IContactsView>(savedState) {

    private val repository: IContactsRepository = Injection.proviceContactsRepository()

    private var contacts: MutableList<Contact> = ArrayList()
        set(value) {
            field = value
            view?.displayContacts(value)
        }

    init {
        loadCachedUsers()

        appendDisposable(repository.observeAddings()
                .toMainThread()
                .subscribeIgnoreErrors(Consumer { loadCachedUsers() }))

        appendDisposable(repository.observeDeleting()
                .toMainThread()
                .subscribeIgnoreErrors(Consumer { it -> onContactRemoved(it) }))

        appendDisposable(repository.observeVcards()
                .toMainThread()
                .subscribeIgnoreErrors(Consumer { it -> onVcardsUpdated(it) }))
    }

    private fun onVcardsUpdated(users: Collection<User>) {
        Logger.d("onVcardsUpdated", "jids:${users.map { it.jid }}")
        loadCachedUsers()
    }

    private fun onContactRemoved(jids: List<String>) {
        if (contacts.removeAll { jids.contains(it.jid) }) view?.notifyDataSetChanged()
    }

    private fun loadCachedUsers() {
        appendDisposable(repository.getContacts()
                .fromIOToMain()
                .subscribeIgnoreErrors(Consumer { it -> onContactsReceived(it) }))
    }

    private fun onContactsReceived(contacts: List<Contact>) {
        this.contacts = contacts.toMutableList()
    }

    override fun onGuiCreated(view: IContactsView) {
        super.onGuiCreated(view)
        view.displayContacts(contacts)
    }
}