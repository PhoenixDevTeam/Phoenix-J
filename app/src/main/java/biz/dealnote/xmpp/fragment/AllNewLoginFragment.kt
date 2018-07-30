package biz.dealnote.xmpp.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

import biz.dealnote.mvp.core.IPresenterFactory
import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.activity.LoginActivity
import biz.dealnote.xmpp.activity.MainActivity
import biz.dealnote.xmpp.fragment.base.BaseMvpFragment
import biz.dealnote.xmpp.model.AccountContactPair
import biz.dealnote.xmpp.mvp.presenter.LoginPresenter
import biz.dealnote.xmpp.mvp.view.ILoginView
import biz.dealnote.xmpp.view.SimpleTextWatcher

class AllNewLoginFragment : BaseMvpFragment<LoginPresenter, ILoginView>(), ILoginView {

    private var mLogin: EditText? = null
    private var mPassword: EditText? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_login_bird, container, false)

        mLogin = root.findViewById(R.id.login)
        mPassword = root.findViewById(R.id.password)

        mLogin!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                presenter?.fireLoginChanged(charSequence)
            }
        })

        mPassword!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                presenter?.firePasswordChanged(charSequence)
            }
        })

        root.findViewById<View>(R.id.button_login).setOnClickListener { _ -> presenter.fireLoginButtonClick() }
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LoginPresenter> {
        return IPresenterFactory{ LoginPresenter(saveInstanceState) }
    }

    override fun displayLogin(text: CharSequence) {
        mLogin?.setText(text)
    }

    override fun displayPassword(text: CharSequence) {
        mPassword?.setText(text)
    }

    override fun sendSuccess(pair: AccountContactPair) {
        activity?.run {
            val data = Intent()
            data.putExtra(LoginActivity.EXTRA_RESULT, pair)
            setResult(Activity.RESULT_OK, data)

            if (intent?.getBooleanExtra(LoginActivity.EXTRA_START_MAIN_ACTIVITY_ON_SUCCESS, false) == true) {
                startActivity(Intent(this, MainActivity::class.java))
            }

            finish()
        }
    }

    companion object {
        fun newInstance(): AllNewLoginFragment {
            return AllNewLoginFragment()
        }
    }
}