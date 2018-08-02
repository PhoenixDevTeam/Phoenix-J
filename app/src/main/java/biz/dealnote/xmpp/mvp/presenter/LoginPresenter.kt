package biz.dealnote.xmpp.mvp.presenter

import android.os.Bundle
import biz.dealnote.xmpp.App
import biz.dealnote.xmpp.Extra
import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.db.Accounts
import biz.dealnote.xmpp.model.Account
import biz.dealnote.xmpp.model.AccountContactPair
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.mvp.presenter.base.RequestSupportPresenter
import biz.dealnote.xmpp.mvp.view.ILoginView
import biz.dealnote.xmpp.service.request.Request
import biz.dealnote.xmpp.service.request.RequestFactory
import org.jxmpp.jid.impl.JidCreate

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
class LoginPresenter(savedInstanceState: Bundle?) : RequestSupportPresenter<ILoginView>(savedInstanceState) {

    private var mLogin: String? = null
    private var mPassword: String? = null

    fun fireLoginChanged(text: CharSequence) {
        mLogin = text.toString()
    }

    fun firePasswordChanged(text: CharSequence) {
        mPassword = text.toString()
    }

    fun fireLoginButtonClick() {
        val host: String
        val login: String
        val pass = mPassword

        try {
            val jid = JidCreate.entityBareFrom(mLogin)
            host = jid.domain.toString()
            login = jid.localpart.toString()
        } catch (e: Exception) {
            showError(view, R.string.invalid_user_login)
            return
        }

        val request = RequestFactory.getSignInRequest(login, pass, host, 5222)
        executeRequest(request)
    }

    override fun onRequestFinished(request: Request, resultData: Bundle) {
        super.onRequestFinished(request, resultData)
        if (request.requestType == RequestFactory.REQUEST_SIGN_IN) {
            val account : Account? = resultData.getParcelable(Extra.ACCOUNT)
            val contact = resultData.getParcelable<User>(Extra.CONTACT)
            account?.run {
                onAuthSuccess(this, contact)
            }
        }
    }

    private fun onAuthSuccess(account: Account, user: User?) {
        account.disabled = true
        Accounts.enableAccount(App.getInstance(), account.id, true)

        val pair = AccountContactPair(account)
        pair.setUser(user)

        view?.sendSuccess(pair)
    }
}