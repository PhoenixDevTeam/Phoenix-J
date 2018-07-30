package biz.dealnote.xmpp.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;

public class CreateAccountFragment extends AbsRequestSupportFragment implements View.OnClickListener {

    private EditText mHost;
    private EditText mLogin;
    private EditText mPassword;
    private EditText mPasswordAgain;
    private ProgressDialog progressDialog;

    public static CreateAccountFragment newInstance() {
        return new CreateAccountFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_register, container, false);
        mHost = (EditText) root.findViewById(R.id.host);
        mLogin = (EditText) root.findViewById(R.id.login);
        mPassword = (EditText) root.findViewById(R.id.password);
        mPasswordAgain = (EditText) root.findViewById(R.id.password_again);
        root.findViewById(R.id.button_sign_up).setOnClickListener(this);
        return root;
    }

    @Override
    public void onRestoreConnectionToRequest(Request request) {
        if (request.getRequestType() == RequestFactory.REQUEST_CREATE_ACCOUNT) {
            showProgressDialog();
        }
    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        if (request.getRequestType() == RequestFactory.REQUEST_CREATE_ACCOUNT) {
            cancelProgressDialog();

            String host = request.getString(Extra.HOST);
            int port = request.getInt(Extra.PORT);
            String login = request.getString(Extra.LOGIN);
            String password = request.getString(Extra.PASSWORD);

            Intent intent = new Intent();
            intent.putExtra(Extra.HOST, host);
            intent.putExtra(Extra.PORT, port);
            intent.putExtra(Extra.LOGIN, login);
            intent.putExtra(Extra.PASSWORD, password);

            if (getTargetFragment() != null) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }

            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onCustromError(Request request, String errorText, int code) {
        if (request.getRequestType() == RequestFactory.REQUEST_CREATE_ACCOUNT) {
            cancelProgressDialog();
        }

        if (isAdded()) {
            Toast.makeText(getActivity(), errorText, Toast.LENGTH_LONG).show();
        }
    }

    private void doSignUp() {
        String host = mHost.getText().toString().trim();
        String login = mLogin.getText().toString().trim();
        String password = mPassword.getText().toString().trim();
        String passwordAgain = mPasswordAgain.getText().toString().trim();

        mHost.setError(null);
        mLogin.setError(null);
        mPassword.setError(null);
        mPasswordAgain.setError(null);

        View focus = null;

        if (TextUtils.isEmpty(host)) {
            focus = mHost;
            mHost.setError(getString(R.string.fill_in_this_field));
        } else if (TextUtils.isEmpty(login)) {
            focus = mLogin;
            mLogin.setError(getString(R.string.fill_in_this_field));
        } else if (TextUtils.isEmpty(password)) {
            focus = mPassword;
            mPassword.setError(getString(R.string.fill_in_this_field));
        } else if (TextUtils.isEmpty(passwordAgain)) {
            focus = mPasswordAgain;
            mPasswordAgain.setError(getString(R.string.fill_in_this_field));
        } else if (!password.equals(passwordAgain)) {
            focus = mPasswordAgain;
            mPasswordAgain.setError(getString(R.string.passwords_do_not_match));
        } else {
            Request request = RequestFactory.getCreateAccountRequest(host, 5222, login, password);
            getRequestManager().executeRequest(request);
            showProgressDialog();
        }

        if (focus != null) {
            focus.requestFocus();
        }
    }

    private void cancelProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
            progressDialog = null;
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(R.string.please_wait);
            progressDialog.setMessage(getString(R.string.registration));
            progressDialog.setCancelable(false);
        }

        progressDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sign_up:
                doSignUp();
                break;
        }
    }
}
