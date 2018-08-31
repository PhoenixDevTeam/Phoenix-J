package biz.dealnote.xmpp.dialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;

public class ChangePasswordDialog extends AbsRequestSupportDialogFragment {

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etNewPasswordAgain;
    private ProgressDialog progressDialog;

    private Account account;

    public static ChangePasswordDialog newInstance(@NonNull Account account) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Extra.ACCOUNT, account);
        ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog();
        changePasswordDialog.setArguments(bundle);
        return changePasswordDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.account = getArguments().getParcelable(Extra.ACCOUNT);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.change_password);

        View root = inflater.inflate(R.layout.dialog_change_password, container, false);
        etOldPassword = root.findViewById(R.id.dialog_change_password_old);
        etNewPassword = root.findViewById(R.id.dialog_change_password_new);
        etNewPasswordAgain = root.findViewById(R.id.dialog_change_password_new_repeat);

        Button bConfirm = root.findViewById(R.id.dialog_change_password_confirm);
        bConfirm.setOnClickListener(v -> confirm());

        return root;
    }

    private void confirm() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String newPasswordRepeat = etNewPasswordAgain.getText().toString();

        etOldPassword.setError(null);
        etNewPassword.setError(null);
        etNewPasswordAgain.setError(null);

        View focus = null;

        if (TextUtils.isEmpty(oldPassword)) {
            focus = etOldPassword;
            etOldPassword.setError(getString(R.string.fill_in_this_field));
        } else if (TextUtils.isEmpty(newPassword)) {
            focus = etNewPassword;
            etNewPassword.setError(getString(R.string.fill_in_this_field));
        } else if (TextUtils.isEmpty(newPasswordRepeat)) {
            focus = etNewPasswordAgain;
            etNewPasswordAgain.setError(getString(R.string.fill_in_this_field));
        } else if (!newPassword.equals(newPasswordRepeat)) {
            focus = etNewPasswordAgain;
            etNewPasswordAgain.setError(getString(R.string.passwords_must_match));
        } else if (!oldPassword.equals(account.password)) {
            focus = etOldPassword;
            etOldPassword.setError(getString(R.string.invalid_old_password));
        } else {
            try {
                Request request = RequestFactory.getChangePasswordRequest(account.id, newPassword);
                getRequestManager().executeRequest(request);
                showProgressDialog();
            } catch (IllegalArgumentException e) {
                focus = etNewPassword;
                etNewPassword.setError(e.getMessage());
            }
        }

        if (focus != null) {
            focus.requestFocus();
        }
    }

    private void cancelProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(R.string.saving_title);
            progressDialog.setMessage(getString(R.string.apply_changes));
            progressDialog.setCancelable(false);
        }

        progressDialog.show();
    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        if (request.getRequestType() == RequestFactory.REQUEST_CHANGE_PASSWORD) {
            cancelProgressDialog();
            if (getTargetFragment() != null) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            }

            dismiss();
        }
    }

    @Override
    public void onCustromError(Request request, String errorText, int code) {
        if (isAdded()) {
            Toast.makeText(getActivity(), errorText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRestoreConnectionToRequest(Request request) {
        if (request.getRequestType() == RequestFactory.REQUEST_CHANGE_PASSWORD) {
            showProgressDialog();
        }
    }
}
