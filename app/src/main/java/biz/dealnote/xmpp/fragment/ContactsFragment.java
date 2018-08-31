package biz.dealnote.xmpp.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.mvp.core.IPresenterFactory;
import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.adapter.AccountsListAdapter;
import biz.dealnote.xmpp.adapter.ContactsAdapter;
import biz.dealnote.xmpp.callback.OnPlaceOpenCallback;
import biz.dealnote.xmpp.callback.PicassoPauseOnScrollListener;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.fragment.base.BaseMvpFragment;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.mvp.presenter.ContactsPresenter;
import biz.dealnote.xmpp.mvp.view.IContactsView;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.view.InputTextDialog;

public class ContactsFragment extends BaseMvpFragment<ContactsPresenter, IContactsView> implements ContactsAdapter.ClickListener, IContactsView {

    private static final String TAG = ContactsFragment.class.getSimpleName();

    private ContactsAdapter mAdapter;
    private TextView mEmptyText;

    public static ContactsFragment newInstance() {
        Bundle args = new Bundle();
        ContactsFragment fragment = new ContactsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        /*appendDisposable(Storages.Companion.getINSTANCE()
                .getAccounts()
                .observeDeletion()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(this::onAccountDelete));

        appendDisposable(Storages.Companion.getINSTANCE()
                .getUsers()
                .observeUpdates()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(this::handleContactUpdateEvent));*/
    }

    /*private void onAccountDelete(int id) {
        // если аакаунт был удален - удаляем все его контакты из списка
        Iterator<Contact> iterator = data.iterator();
        boolean changes = false;
        while (iterator.hasNext()) {
            if (iterator.next().accountId.getId() == id) {
                iterator.remove();
                changes = true;
            }
        }

        if (changes && isAdded() && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        resolveEmptyText();
    }*/

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mEmptyText = view.findViewById(R.id.empty);
        RecyclerView mRecyclerView = view.findViewById(R.id.list);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);

        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));

        mAdapter = new ContactsAdapter(Collections.emptyList(), getActivity());
        mAdapter.setClickListener(this);

        mRecyclerView.setAdapter(mAdapter);
        resolveEmptyText();
        return view;
    }

    /*@Override
    public void onRequestFinished(Request request, Bundle resultData) {
        if (request.getRequestType() == RequestFactory.REQUEST_ADD_CONTACT) {
            safeSnackbar(R.string.request_has_been_sent);
        }
    }*/

    /*private void safeSnackbar(int message) {
        if (isAdded() && root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }*/

    /*@Override
    public void onCustromError(Request request, String errorText, int code) {
        if (isAdded()) {
            Toast.makeText(getActivity(), errorText, Toast.LENGTH_LONG).show();
        }
    }

    private void restoreFromSaveInstanceState(Bundle outState) {
        data = outState.getParcelableArrayList(SAVE_DATA);
    }

    @Override
    public Loader<ArrayList<Contact>> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_CONTACTS) {
            return new RosterEntriesAsyncLoader(getActivity(), args);
        }

        return null;
    }*/

    /*@Override
    public void onLoadFinished(Loader<ArrayList<Contact>> loader, ArrayList<Contact> data) {
        this.data.clear();
        this.data.addAll(data);

        mAdapter.notifyDataSetChanged();
        Log.d(TAG, "onLoadFinished, data: " + data);

        resolveEmptyText();
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Contact>> loader) {

    }*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_contact_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                showSelectTargetAccountDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addToContactList(Account account, String jid) {
        Request request = RequestFactory.getAddContactRequest(account, jid);
        //getRequestManager().executeRequest(request);
    }

    private void showAddContactInputDialog(final Account account) {
        new InputTextDialog.Builder(getActivity())
                .setTitleRes(R.string.enter_nickname)
                .setAllowEmpty(false)
                .setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
                .setCallback(jid -> getPresenter().fireContactAddClick(account.id, jid))
                .show();
    }

    private void showSelectTargetAccountDialog() {
        ArrayList<AccountContactPair> data = Accounts.getAllPairs(getActivity());
        final AccountsListAdapter accountsListAdapter = new AccountsListAdapter(getActivity(), data);

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.select_target_account)
                .setAdapter(accountsListAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AccountContactPair selected = accountsListAdapter.getItem(which);
                        showAddContactInputDialog(selected.account);
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    @Override
    public void onClick(int position, Contact entry) {
        Log.d(TAG, "onClick, position: " + position + ", entry: " + entry);

        if (getActivity() instanceof OnPlaceOpenCallback) {
            ((OnPlaceOpenCallback) getActivity()).onContactCardOpen(entry);
        }
    }

    /*public void handleContactUpdateEvent(User user) {
        if (data == null) return;

        boolean changed = false;
        for (Contact entry : data) {
            if (user.equals(entry.user)) {
                entry.user = user;
                changed = true;
            }
        }

        if (changed && mAdapter != null) mAdapter.notifyDataSetChanged();
    }*/

    private void resolveEmptyText() {
        if (!isAdded()) return;
        mEmptyText.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public IPresenterFactory<ContactsPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new ContactsPresenter(saveInstanceState);
    }

    @Override
    public void displayContacts(@NotNull List<Contact> contacts) {
        mAdapter.setData(contacts);
        resolveEmptyText();
    }

    @Override
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
        resolveEmptyText();
    }
}