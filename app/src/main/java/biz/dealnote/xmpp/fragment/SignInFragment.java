package biz.dealnote.xmpp.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.LoginActivity;
import biz.dealnote.xmpp.activity.MainActivity;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;

public class SignInFragment extends AbsRequestSupportFragment implements View.OnClickListener {

    public static final String TAG = SignInFragment.class.getSimpleName();

    public static final String EXTRA_TARGET_DATA = "target_data";
    private static final int REQUEST_CREATE_ACCOUNT = 1;
    private View root;
    private EditText loginField;
    private EditText passwordField;
    private EditText hostField;

    public static SignInFragment newInstance() {
        Bundle args = new Bundle();
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onRestoreConnectionToRequest(Request request) {

    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        Account account = resultData.getParcelable(Extra.ACCOUNT);
        Contact contact = resultData.getParcelable(Extra.CONTACT);

        if (account != null) {
            onAuthSuccess(account, contact);
        }
    }

    private void onAuthSuccess(Account account, Contact contact) {
        if (!isAdded()) return;

        account.disabled = Boolean.FALSE;
        Accounts.enableAccount(getActivity(), account.id, Boolean.TRUE);

        if (getArguments().getBoolean(LoginActivity.EXTRA_START_MAIN_ACTIVITY_ON_SUCCESS)) {
            startActivity(new Intent(getActivity(), MainActivity.class));
        }

        AccountContactPair pair = new AccountContactPair(account);
        pair.setContact(contact);

        Intent data = new Intent();
        data.putExtra(LoginActivity.EXTRA_RESULT, pair);

        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

    @Override
    public void onCustromError(Request request, String errorText, int code) {
        if (isAdded() && root != null) {
            Snackbar.make(root, errorText, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_signin, container, false);

        root.findViewById(R.id.signin_button).setOnClickListener(this);
        root.findViewById(R.id.button_sign_up).setOnClickListener(this);

        loginField = (EditText) root.findViewById(R.id.login);
        passwordField = (EditText) root.findViewById(R.id.password);
        hostField = (EditText) root.findViewById(R.id.host);

        TextInputLayout passwordFieldContainer = (TextInputLayout) root.findViewById(R.id.password_container);

        passwordFieldContainer.setTypeface(Typeface.DEFAULT);

        //passwordField.setTypeface(Typeface.DEFAULT_BOLD);
        passwordField.setTransformationMethod(new PasswordTransformationMethod());
        return root;
    }

    private void signup() {
        if (getActivity() instanceof Callback) {
            ((Callback) getActivity()).onRegisterClick(this, REQUEST_CREATE_ACCOUNT);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signin_button:
                String login = loginField.getText().toString();
                String password = passwordField.getText().toString();
                String host = hostField.getText().toString();

                Request request = RequestFactory.getSignInRequest(login, password, host, 5222);
                getRequestManager().executeRequest(request);

                break;
            case R.id.button_sign_up:
                signup();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getArguments().containsKey(EXTRA_TARGET_DATA)){
            Bundle target = getArguments().getBundle(EXTRA_TARGET_DATA);
            if (target != null) {
                loginField.setText(target.getString(Extra.LOGIN));
                passwordField.setText(target.getString(Extra.PASSWORD));
                hostField.setText(target.getString(Extra.HOST));
            }

            getArguments().remove(EXTRA_TARGET_DATA);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult, requestCode: " + requestCode + ", resultCode: " + resultCode + ", extras: " + (data == null ? "null" : data.getExtras()));

        if(requestCode == REQUEST_CREATE_ACCOUNT && resultCode == Activity.RESULT_OK && data != null){
            // Получаем результат регистрации и сохраняем в аргументы фрагмента.
            // Эти данные будут отображены в соответсвующих полях только в методе onResume,
            // так как на данный момент фрагмент находится не на верхушке стэка и его вьювы уничтожены
            getArguments().putBundle(EXTRA_TARGET_DATA, data.getExtras());
        }
    }

    public interface Callback {
        void onRegisterClick(Fragment target, int requestCode);
    }
}
