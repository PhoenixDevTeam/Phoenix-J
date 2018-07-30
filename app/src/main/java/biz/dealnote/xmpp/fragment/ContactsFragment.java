package biz.dealnote.xmpp.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.adapter.AccountsListAdapter;
import biz.dealnote.xmpp.adapter.RosterEntriesAdapter;
import biz.dealnote.xmpp.callback.OnPlaceOpenCallback;
import biz.dealnote.xmpp.callback.PicassoPauseOnScrollListener;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.db.columns.AccountsColumns;
import biz.dealnote.xmpp.loader.RosterEntriesAsyncLoader;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.AppRosterEntry;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.util.Utils;
import biz.dealnote.xmpp.view.InputTextDialog;

public class ContactsFragment extends AbsRequestSupportFragment implements LoaderManager.LoaderCallbacks<ArrayList<AppRosterEntry>>, RosterEntriesAdapter.ClickListener {

    private static final String TAG = ContactsFragment.class.getSimpleName();

    private static final int LOADER_CONTACTS = 2;
    private static final String SAVE_DATA = "save_data";
    private ArrayList<AppRosterEntry> data;
    private View root;
    private RecyclerView mRecyclerView;
    private RosterEntriesAdapter mAdapter;
    private TextView mEmptyText;

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            restoreFromSaveInstanceState(savedInstanceState);
        }

        appendDisposable(Repositories.Companion.getInstance()
                .getAccountsRepository()
                .observeDeletion()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(this::onAccountDelete));

        appendDisposable(Repositories.Companion.getInstance()
                .getContactsRepository()
                .observeUpdates()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(this::handleContactUpdateEvent));
    }

    private void onAccountDelete(int id) {
        // если аакаунт был удален - удаляем все его контакты из списка
        Iterator<AppRosterEntry> iterator = data.iterator();
        boolean changes = false;
        while (iterator.hasNext()) {
            if (iterator.next().account.id == id) {
                iterator.remove();
                changes = true;
            }
        }

        if (changes && isAdded() && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        resolveEmptyText();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_contacts, container, false);
        mEmptyText = (TextView) root.findViewById(R.id.empty);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.list);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);

        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(getActivity(), Constants.PICASSO_TAG));
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        boolean firstRun = false;
        if (data == null) {
            firstRun = true;
            data = new ArrayList<>();
        }

        mAdapter = new RosterEntriesAdapter(data, getActivity());
        mAdapter.setClickListener(this);

        mRecyclerView.setAdapter(mAdapter);

        if (firstRun) {
            getLoaderManager().initLoader(LOADER_CONTACTS, RosterEntriesAsyncLoader.createArgs(AccountsColumns.FULL_LOGIN), this);
        }

        resolveEmptyText();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_DATA, data);
    }

    @Override
    public void onRestoreConnectionToRequest(Request request) {

    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        if (request.getRequestType() == RequestFactory.REQUEST_ADD_CONTACT) {
            safeSnackbar(R.string.request_has_been_sent);
        }
    }

    private void safeSnackbar(int message) {
        if (isAdded() && root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCustromError(Request request, String errorText, int code) {
        if (isAdded()) {
            Toast.makeText(getActivity(), errorText, Toast.LENGTH_LONG).show();
        }
    }

    private void restoreFromSaveInstanceState(Bundle outState) {
        data = outState.getParcelableArrayList(SAVE_DATA);
    }

    @Override
    public Loader<ArrayList<AppRosterEntry>> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_CONTACTS) {
            return new RosterEntriesAsyncLoader(getActivity(), args);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppRosterEntry>> loader, ArrayList<AppRosterEntry> data) {
        this.data.clear();
        this.data.addAll(data);

        mAdapter.notifyDataSetChanged();
        Log.d(TAG, "onLoadFinished, data: " + data);

        resolveEmptyText();
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AppRosterEntry>> loader) {

    }

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
        getRequestManager().executeRequest(request);
    }

    private void showAddContactInputDialog(final Account account) {
        new InputTextDialog.Builder(getActivity())
                .setTitleRes(R.string.enter_nickname)
                .setAllowEmpty(false)
                .setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
                .setCallback(new InputTextDialog.Callback() {
                    @Override
                    public void onChanged(String newValue) {
                        addToContactList(account, newValue);
                    }
                })
                .show();
    }

    private void showSelectTargetAccountDialog() {
        ArrayList<AccountContactPair> data = Accounts.getAllPairs(getActivity());
        final AccountsListAdapter accountsListAdapter = new AccountsListAdapter(getActivity(), data);

        new AlertDialog.Builder(getActivity())
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
    public void onClick(int position, AppRosterEntry entry) {
        Log.d(TAG, "onClick, position: " + position + ", entry: " + entry);

        if (getActivity() instanceof OnPlaceOpenCallback) {
            ((OnPlaceOpenCallback) getActivity()).onContactCardOpen(entry);
        }
    }

    public void handleContactUpdateEvent(Contact contact) {
        if (data == null) return;

        boolean changed = false;
        for (AppRosterEntry entry : data) {
            if (contact.equals(entry.contact)) {
                entry.contact = contact;
                changed = true;
            }
        }

        if (changed && mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    private void resolveEmptyText() {
        if (!isAdded()) return;
        boolean visible = Utils.isEmpty(data);
        mEmptyText.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

}
