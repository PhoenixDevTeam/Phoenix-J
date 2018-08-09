package biz.dealnote.xmpp.mvp.presenter

import android.os.Bundle
import biz.dealnote.xmpp.Injection
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.mvp.presenter.base.RxSupportPresenter
import biz.dealnote.xmpp.mvp.view.IContactCardView
import biz.dealnote.xmpp.repo.IContactsRepository
import biz.dealnote.xmpp.util.fromIOToMain

class ContactCardPresenter(val contact: Contact, savedState: Bundle?) : RxSupportPresenter<IContactCardView>(savedState) {

    private val repository: IContactsRepository = Injection.proviceContactsRepository()

    init {
        actualizeData()
    }

    private fun actualizeData() {
        appendDisposable(repository.actualizeUserAndGet(contact.accountId.id, contact.jid)
                .fromIOToMain()
                .subscribe({ onActualDataReceived(it) }, { onActualDataFail(it) }))
    }

    private fun onActualDataFail(throwable: Throwable) {
        showError(view, throwable)
    }

    override fun onGuiCreated(view: IContactCardView) {
        super.onGuiCreated(view)
        view.displayUser(contact.user)
    }

    private fun onActualDataReceived(user: User) {
        contact.user = user
        view?.displayUser(user)
    }

    fun fireChatClick() {
        view?.openChat(contact.accountId, contact.jid)
    }
}