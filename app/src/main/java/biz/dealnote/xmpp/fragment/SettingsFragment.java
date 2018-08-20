package biz.dealnote.xmpp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.LoginActivity;
import biz.dealnote.xmpp.adapter.SettingsAdapter;
import biz.dealnote.xmpp.callback.OnPlaceOpenCallback;
import biz.dealnote.xmpp.callback.PicassoPauseOnScrollListener;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.dialog.NotificationSettingsDialog;
import biz.dealnote.xmpp.fragment.base.BaseFragment;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.settings.AbsSettings;
import biz.dealnote.xmpp.settings.AccountSettings;
import biz.dealnote.xmpp.settings.NotificationSettings;
import biz.dealnote.xmpp.settings.SimpleSetting;
import biz.dealnote.xmpp.util.NotificationHelper;

public class SettingsFragment extends BaseFragment implements SettingsAdapter.ActionListener {

    private static final String SAVE_DATA = "save_data";
    private static final int REQUEST_ADD_ACCOUNT = 13;
    private static final int REQUEST_CHANGE_NOTIFICATION_SETTINGS = 14;
    private static final AbsSettings.Section ACCOUNTS = new AbsSettings.Section(0, R.string.accounts);
    private static final AbsSettings.Section NOTIFICATIONS = new AbsSettings.Section(1, R.string.notifications);
    private static final AbsSettings.Section OTHER = new AbsSettings.Section(2, R.string.other);

    private static final int KEY_INCOME_FILES = 191;

    private RecyclerView mRecyclerView;
    private ArrayList<AbsSettings> data;
    private SettingsAdapter mAdapter;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            restoreFromSavedInstanceState(savedInstanceState);
        }

        appendDisposable(Storages.Companion.getINSTANCE()
                .getAccounts()
                .observePasswordChanges()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(pair -> onPasswordChange(pair.getFirst(), pair.getSecond())));

        appendDisposable(Storages.Companion.getINSTANCE()
                .getAccounts()
                .observeDeletion()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(this::onAccountDelete));

        appendDisposable(Storages.Companion.getINSTANCE()
                .getUsers()
                .observeUpdates()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(this::handleContactUpdateEvent));
    }

    private void onAccountDelete(int id) {
        Iterator<AbsSettings> iterator = data.iterator();

        boolean hasChanges = false;
        int accountCount = 0;

        while (iterator.hasNext()) {
            AbsSettings absSettings = iterator.next();
            if (!(absSettings instanceof AccountSettings)) {
                continue;
            }

            AccountSettings accountSettings = (AccountSettings) absSettings;
            if (accountSettings.accountContactPair.account.id == id) {
                iterator.remove();
                hasChanges = true;
            } else {
                accountCount++;
            }
        }

        if (hasChanges) {
            mAdapter.notifyDataSetChanged();
        }

        if (accountCount == 0) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_START_MAIN_ACTIVITY_ON_SUCCESS, true);
            startActivity(intent);

            getActivity().finish();
        }
    }

    private void onPasswordChange(int id, String pass) {
        for (AbsSettings settings : data) {
            if (settings instanceof AccountSettings && ((AccountSettings) settings).accountContactPair.account.id == id) {
                ((AccountSettings) settings).accountContactPair.account.password = pass;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        mRecyclerView = (RecyclerView) root.findViewById(R.id.list);
        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(getActivity(), Constants.PICASSO_TAG));

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (data == null) {
            data = buildSettingsList();
        }

        mAdapter = new SettingsAdapter(getActivity(), data);
        mAdapter.setActionListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<AbsSettings> buildSettingsList() {
        ArrayList<AbsSettings> list = new ArrayList<>();

        List<AccountContactPair> pairs = Accounts.getAllPairs(getActivity());
        for (AccountContactPair pair : pairs) {
            list.add(new AccountSettings(ACCOUNTS, pair));
        }

        NotificationSettings income = new NotificationSettings(NOTIFICATIONS, NotificationHelper.KEY_INCOMING_MESSAGES,
                R.string.income_messages, NotificationHelper.load(getActivity(), NotificationHelper.KEY_INCOMING_MESSAGES)
        );

        NotificationSettings newSubscription = new NotificationSettings(NOTIFICATIONS, NotificationHelper.KEY_NEW_SUBSCRIPTIONS,
                R.string.new_subscriptions, NotificationHelper.load(getActivity(), NotificationHelper.KEY_NEW_SUBSCRIPTIONS)
        );

        NotificationSettings incomeFiles = new NotificationSettings(NOTIFICATIONS, NotificationHelper.KEY_INCOMING_FILES,
                R.string.income_files, NotificationHelper.load(getActivity(), NotificationHelper.KEY_INCOMING_FILES)
        );

        list.add(income);
        list.add(newSubscription);
        list.add(incomeFiles);

        list.add(new SimpleSetting(OTHER, KEY_INCOME_FILES, R.string.income_files));

        return list;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_DATA, data);
    }

    private void restoreFromSavedInstanceState(@NonNull Bundle state) {
        data = state.getParcelableArrayList(SAVE_DATA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if (requestCode == REQUEST_ADD_ACCOUNT && resultCode == LoginActivity.RESULT_OK) {
            AccountContactPair pair = intentData.getParcelableExtra(LoginActivity.EXTRA_RESULT);
            data.add(0, new AccountSettings(ACCOUNTS, pair));
            mAdapter.notifyDataSetChanged();
        }

        if (requestCode == REQUEST_CHANGE_NOTIFICATION_SETTINGS) {
            refreshNotificationItems();
        }
    }

    private void refreshNotificationItems() {
        for (AbsSettings settings : data) {
            if (settings instanceof NotificationSettings) {
                NotificationSettings notificationSettings = (NotificationSettings) settings;
                notificationSettings.value = NotificationHelper.load(getActivity(), notificationSettings.key);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_account:
                onAddNewAccountClick();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onAddNewAccountClick() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_START_MAIN_ACTIVITY_ON_SUCCESS, Boolean.FALSE);
        getActivity().startActivityForResult(intent, REQUEST_ADD_ACCOUNT);
    }

    @Override
    public void onAccountClick(AccountSettings settings) {
        if (getActivity() instanceof OnPlaceOpenCallback) {
            ((OnPlaceOpenCallback) getActivity()).onAccountManagerOpen(settings.accountContactPair);
        }
    }

    @Override
    public void onNotificationClick(NotificationSettings settings) {
        NotificationSettingsDialog dialog = NotificationSettingsDialog.newInstance(settings.key);
        dialog.setTargetFragment(this, REQUEST_CHANGE_NOTIFICATION_SETTINGS);
        dialog.show(getFragmentManager(), "notification_settings");
    }


    @Override
    public void onSimpleOptionClick(SimpleSetting setting) {
        if (setting.key == KEY_INCOME_FILES && getActivity() instanceof OnPlaceOpenCallback) {
            ((OnPlaceOpenCallback) getActivity()).showIncomeFiles(null);
        }
    }

    public void handleContactUpdateEvent(User user) {
        for (AbsSettings settings : data) {
            if (!(settings instanceof AccountSettings)) continue;

            AccountSettings accountSettings = (AccountSettings) settings;
            if (user.equals(accountSettings.accountContactPair.user)) {
                accountSettings.accountContactPair.user = user;
                if (mAdapter != null) mAdapter.notifyDataSetChanged();
            }
        }
    }
}
