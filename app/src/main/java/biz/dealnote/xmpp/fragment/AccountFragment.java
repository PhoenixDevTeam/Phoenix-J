package biz.dealnote.xmpp.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.ActivityUtils;
import biz.dealnote.xmpp.activity.FileManagerActivity;
import biz.dealnote.xmpp.adapter.UserAttributesRecyclerAdapter;
import biz.dealnote.xmpp.callback.AppStyleable;
import biz.dealnote.xmpp.callback.OnBackButtonCallback;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.dialog.ChangePasswordDialog;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.model.UserAttribute;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.util.AppPerms;
import biz.dealnote.xmpp.util.PicassoAvatarHandler;
import biz.dealnote.xmpp.util.PicassoInstance;
import biz.dealnote.xmpp.view.InputTextDialog;

import static biz.dealnote.xmpp.util.Utils.nonEmpty;

public class AccountFragment extends AbsRequestSupportFragment implements UserAttributesRecyclerAdapter.ActionListener, OnBackButtonCallback {

    private static final String TAG = AccountFragment.class.getSimpleName();

    private static final String EXTRA_ACCOUNT_PAIR = "account_pair";
    private static final int REQUEST_SELECT_FILE = 12;
    private static final int REQUEST_CHANGE_PASSWORD = 13;
    private static final String SAVE_ATTRIBUTES = "save_attributes";
    private static final String SAVE_HAS_CHANGES = "save_has_changes";
    private AccountContactPair accountContactPair;
    private View root;
    private View headerView;
    private View footerView;
    private RecyclerView mRecyclerView;
    private ImageView avatar;
    private TextView avaterLetter;
    private ArrayList<UserAttribute> mAttributes;
    private UserAttributesRecyclerAdapter mAdapter;
    private boolean hasChanged;
    private ProgressDialog progressDialog;

    public static AccountFragment newInstance(@NonNull AccountContactPair pair) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ACCOUNT_PAIR, pair);
        AccountFragment fragment = new AccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mAttributes = savedInstanceState.getParcelableArrayList(SAVE_ATTRIBUTES);
            hasChanged = savedInstanceState.getBoolean(SAVE_HAS_CHANGES);
        }

        accountContactPair = getArguments().getParcelable(EXTRA_ACCOUNT_PAIR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_account, container, false);

        mRecyclerView = (RecyclerView) root.findViewById(R.id.list);
        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);

        headerView = inflater.inflate(R.layout.header_account_avatar, mRecyclerView, false);
        avatar = (ImageView) headerView.findViewById(R.id.avatar);
        avaterLetter = (TextView) headerView.findViewById(R.id.avatar_letter);
        headerView.findViewById(R.id.change_avatar_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChangedAvatarClicked();
            }
        });

        footerView = inflater.inflate(R.layout.footer_save, mRecyclerView, false);
        footerView.findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges(false);
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAttributes == null) {
            mAttributes = createAttributes(accountContactPair.user);
        }

        mAdapter = new UserAttributesRecyclerAdapter(getActivity(), mAttributes);
        mAdapter.addHeader(headerView);
        mAdapter.setActionListener(this);
        mRecyclerView.setAdapter(mAdapter);

        resolveViews(Boolean.TRUE);
        resolveFooter();
    }

    private ArrayList<UserAttribute> createAttributes(User user) {
        ArrayList<UserAttribute> attributes = new ArrayList<>();
        attributes.add(new UserAttribute(UserAttribute.FIRST_NAME, R.string.first_name, user == null ? null : user.getFirstName()));
        attributes.add(new UserAttribute(UserAttribute.LAST_NAME, R.string.last_name, user == null ? null : user.getLastName()));
        return attributes;
    }

    private void onChangedAvatarClicked() {
        if (!AppPerms.hasReadStroragePermission(getActivity())) {
            AppPerms.requestReadStoragePermission(getActivity());
            return;
        }

        Intent intent = new Intent(getActivity(), FileManagerActivity.class);
        intent.setAction(FileManagerFragment.INTENT_ACTION_SELECT_FILE);
        getActivity().startActivityForResult(intent, REQUEST_SELECT_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            updateAvatar(data.getStringExtra(FileManagerFragment.returnFileParameter));
        }

        if (requestCode == REQUEST_CHANGE_PASSWORD && resultCode == Activity.RESULT_OK) {
            safeSnackbar(R.string.saved);
        }
    }

    private void safeSnackbar(int text) {
        if (isAdded() && root != null) {
            Snackbar.make(root, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void updateAvatar(String file) {
        Uri uri = Uri.fromFile(new File(file));
        Log.d(TAG, "updateAvatar, uri: " + uri);

        Request request = RequestFactory.getChangeAvatarRequest(accountContactPair.account.id, accountContactPair.account.buildBareJid(), uri);
        getRequestManager().executeRequest(request);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_ATTRIBUTES, mAttributes);
        outState.putBoolean(SAVE_HAS_CHANGES, hasChanged);
    }

    private void resolveViews(boolean needRefreshAvatar) {
        if (!isAdded()) return;

        Account account = accountContactPair.account;
        User user = accountContactPair.user;

        boolean hasAvatar = user != null && nonEmpty(user.getPhotoHash());

        avatar.setVisibility(hasAvatar ? View.VISIBLE : View.GONE);
        avaterLetter.setText(account.buildBareJid().substring(0, 1));

        if (hasAvatar && needRefreshAvatar) {
            PicassoInstance.get()
                    .load(PicassoAvatarHandler.generateUri(user.getPhotoHash()))
                    .centerCrop()
                    .resize(500, 500)
                    .into(avatar);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.account);
            actionBar.setSubtitle(accountContactPair.account.buildBareJid());
        }

        if (getActivity() instanceof AppStyleable) {
            ((AppStyleable) getActivity()).enableToolbarElevation(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showAccountDeletingDialog();
                break;
            case R.id.action_change_password:
                ChangePasswordDialog dialog = ChangePasswordDialog.newInstance(accountContactPair.account);
                dialog.setTargetFragment(this, REQUEST_CHANGE_PASSWORD);
                dialog.show(getFragmentManager(), "change_password");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAccountDeletingDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_deleting_title)
                .setMessage(R.string.account_deleting_summary)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        delete();
                    }
                })
                .show();
    }

    private void delete() {
        Request request = RequestFactory.getDeleteAccountRequest(accountContactPair.account.id);
        getRequestManager().executeRequest(request);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_account, menu);
    }

    @Override
    public void onRestoreConnectionToRequest(Request request) {

    }

    private void updatedDataFromDb() {
        new UpdateMyDataTask() {
            @Override
            protected void onPostExecute(User user) {
                accountContactPair.user = user;
                resolveViews(true);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        if (request.getRequestType() == RequestFactory.REQUEST_DELETE_ACCOUNT) {
            if (isAdded() && root != null) {
                Snackbar.make(root, R.string.deleted, Snackbar.LENGTH_LONG).show();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }

        if (request.getRequestType() == RequestFactory.REQUEST_CHANGE_AVATAR) {
            updatedDataFromDb();
        }

        if (request.getRequestType() == RequestFactory.REQUEST_EDIT_VCARD) {
            cancelProgressDialog();

            hasChanged = false;
            updatedDataFromDb();

            if (request.getBoolean("exit")) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onCustromError(Request request, String errorText, int code) {
        if (isAdded()) {
            Toast.makeText(getActivity(), errorText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(final UserAttribute attribute) {
        switch (attribute.type) {
            case UserAttribute.FIRST_NAME:
            case UserAttribute.LAST_NAME:
                new InputTextDialog.Builder(getActivity())
                        .setTitleRes(attribute.title)
                        .setValue(attribute.value)
                        .setAllowEmpty(true)
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .setCallback(new InputTextDialog.Callback() {
                            @Override
                            public void onChanged(String newValue) {
                                attribute.value = newValue;
                                mAdapter.notifyDataSetChanged();
                                fireChanges();
                            }
                        })
                        .show();
                break;
        }
    }

    private void saveChanges(boolean exitOnFinish) {
        int accountId = accountContactPair.account.id;
        String jid = accountContactPair.account.buildBareJid();
        String firstName = findAttaributeValueByKey(UserAttribute.FIRST_NAME);
        String lastName = findAttaributeValueByKey(UserAttribute.LAST_NAME);

        Request request = RequestFactory.getEditVcardRequest(accountId, jid, firstName, lastName);
        request.put("exit", exitOnFinish);
        getRequestManager().executeRequest(request);

        showProgressDialog();
    }

    @Override
    public boolean onBackPressed() {
        if (hasChanged) {
            ActivityUtils.showSaveChangesWarning(getActivity(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveChanges(true);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });

            return false;
        }

        return true;
    }

    private String findAttaributeValueByKey(int key) {
        for (UserAttribute attribute : mAttributes) {
            if (attribute.type == key) {
                return attribute.value;
            }
        }

        return null;
    }

    private void resolveFooter() {
        if (hasChanged && mAdapter.getFootersCount() == 0) {
            mAdapter.addFooter(footerView);
        }
    }

    private void fireChanges() {
        hasChanged = true;
        resolveFooter();
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
            progressDialog.setMessage(getString(R.string.saving_changes));
            progressDialog.setCancelable(false);
        }

        progressDialog.show();
    }

    private class UpdateMyDataTask extends AsyncTask<Void, Void, User> {

        @Override
        protected User doInBackground(Void... params) {
            String bareJid = accountContactPair.account.buildBareJid();
            return Storages.Companion.getINSTANCE()
                    .getUsers()
                    .findByJid(bareJid)
                    .blockingGet()
                    .get();
        }
    }
}
