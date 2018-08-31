package biz.dealnote.xmpp.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.callback.AppStyleable;
import biz.dealnote.xmpp.callback.OnBackButtonCallback;
import biz.dealnote.xmpp.callback.OnPlaceOpenCallback;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.fragment.AccountFragment;
import biz.dealnote.xmpp.fragment.ChatFragment;
import biz.dealnote.xmpp.fragment.ContactCardFragment;
import biz.dealnote.xmpp.fragment.FilesCriteria;
import biz.dealnote.xmpp.fragment.IncomeFilesFragment;
import biz.dealnote.xmpp.fragment.MainTabsFragment;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.place.AppPlace;
import biz.dealnote.xmpp.place.AppPlaceProvider;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestAdapter;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.util.AppPerms;
import biz.dealnote.xmpp.util.Logger;
import biz.dealnote.xmpp.util.Objects;
import biz.dealnote.xmpp.util.Utils;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity implements OnPlaceOpenCallback, AppStyleable, AppPlaceProvider {

    //public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_OPEN_PLACE = "biz.dealnote.xmpp.activity.place";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long DOUBLE_BACK_PRESSED_TIMEOUT = 2000;
    private Toolbar mToolbar;

    private RequestAdapter mRequestAdapter;

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = this::resolveNavigationIcon;
    private long mLastBackPressedTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequestAdapter = new RequestAdapter() {
            @Override
            public void onRequestFinished(Request request, Bundle resultData) {
                if (request.getRequestType() == RequestFactory.REQUEST_CONNECT_TO_ACCOUNTS) {
                    ArrayList<Account> accounts = resultData.getParcelableArrayList(Extra.RESULT_LIST);
                    Log.d(TAG, "onAccountConnected: " + accounts);
                }
            }
        };

        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);

        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (!checkAccounts()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_START_MAIN_ACTIVITY_ON_SUCCESS, true);

            startActivity(intent);
            finish();
            return;
        }

        connectToActiveAccounts();

        if (savedInstanceState == null) {
            if (!handleIntent(getIntent())) {
                attachMainTabsFragment();
            }
        }

        resolveNavigationIcon();

        Injection.INSTANCE.getXmppConnectionManager()
                .observeKeepAlive()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {

                        if (resumed) {
                            Injection.INSTANCE.getXmppConnectionManager().keepAlive();
                        }

                        Logger.d("connectToActiveAccounts", "id: " + integer);
                    }
                });
    }

    private boolean resumed;

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
    }

    private void attachMainTabsFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, MainTabsFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    private void resolveNavigationIcon() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            mToolbar.setNavigationIcon(R.drawable.ic_back_24dp);
        } else {
            mToolbar.setNavigationIcon(null);
        }
    }

    public boolean handleIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (ACTION_OPEN_PLACE.equals(action)) {
            AppPlace place = intent.getParcelableExtra(Extra.PLACE);
            place.tryOpenWith(this);
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private boolean checkAccounts() {
        return Accounts.hasActiveAccount(this);
    }

    private void connectToActiveAccounts() {
        //Request request = RequestFactory.getConnectToAccountsRequest();
        //XmppRequestManager.from(this).execute(request, mRequestAdapter);

        Storages.getINSTANCE()
                .getAccounts()
                .getAllActive()
                .flatMapObservable(accounts -> Observable.fromIterable(accounts)
                        .flatMapSingle(account -> Injection.INSTANCE.getXmppConnectionManager().obtainConnected(account.id)))
                .toList()
                .subscribe(new Consumer<List<AbstractXMPPConnection>>() {
                    @Override
                    public void accept(List<AbstractXMPPConnection> connections) {
                        Logger.d("connectToActiveAccounts", "count: " + connections.size());
                    }
                });
    }

    @Override
    public void onContactCardOpen(Contact entry) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, ContactCardFragment.newInstance(entry))
                .addToBackStack("card")
                .commit();
    }

    @Override
    public void onAccountManagerOpen(AccountContactPair pair) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, AccountFragment.newInstance(pair))
                .addToBackStack("account_manager")
                .commit();
    }

    @Override
    public void showIncomeFiles(FilesCriteria criteria) {
        IncomeFilesFragment filesFragment = IncomeFilesFragment.newInstance(criteria);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, filesFragment)
                .addToBackStack("files")
                .commit();
    }

    @Override
    public void enableToolbarElevation(boolean enable) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mToolbar.getElevation() == 0) {
                mToolbar.setElevation(Utils.dpToPx(5F, this));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mToolbar.getElevation() != 0) {
                mToolbar.setElevation(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Fragment front = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (front instanceof OnBackButtonCallback) {
            if (!(((OnBackButtonCallback) front).onBackPressed())) {
                return;
            }
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            if (mLastBackPressedTime < 0 || mLastBackPressedTime + DOUBLE_BACK_PRESSED_TIMEOUT > System.currentTimeMillis()) {
                supportFinishAfterTransition();
                return;
            }

            this.mLastBackPressedTime = System.currentTimeMillis();
            Toast.makeText(this, getString(R.string.click_back_to_exit), Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        getSupportFragmentManager().removeOnBackStackChangedListener(mOnBackStackChangedListener);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppPerms.tryInterceptPermissionResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void openPlace(AppPlace place) {
        if (place.getType() == AppPlace.Type.CHAT) {
            int accountId = place.getArgs().getInt(Extra.ACCOUNT_ID);
            String destination = place.getArgs().getString(Extra.DESTINATION);
            Integer chatId = place.getArgs().containsKey(Extra.CHAT_ID) ? place.getArgs().getInt(Extra.CHAT_ID) : null;

            Objects.requireNonNull(destination);

            Fragment frontFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
            if (frontFragment instanceof ChatFragment) {
                ChatFragment chatFragment = (ChatFragment) frontFragment;
                if (chatFragment.compareChatAttributes(accountId, destination)) {
                    // если этот чат уже на экране - ничего не делаем
                    return;
                }
            }

            if (!isMainTabsFragmentAttached()) {
                // если на экране еще нет главного фрагмента, то надо его добавить
                attachMainTabsFragment();
            }

            ChatFragment chatFragment = ChatFragment.Companion.newInstance(accountId, destination, chatId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, chatFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private boolean isMainTabsFragmentAttached() {
        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof MainTabsFragment) {
                    return true;
                }
            }
        }

        return false;
    }
}
