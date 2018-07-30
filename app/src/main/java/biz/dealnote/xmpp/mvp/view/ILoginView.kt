package biz.dealnote.xmpp.mvp.view

import biz.dealnote.mvp.core.IMvpView
import biz.dealnote.xmpp.model.AccountContactPair

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
interface ILoginView : IMvpView, IErrorView, IToastView {
    fun displayLogin(text: CharSequence)
    fun displayPassword(text: CharSequence)
    fun sendSuccess(pair: AccountContactPair)
}