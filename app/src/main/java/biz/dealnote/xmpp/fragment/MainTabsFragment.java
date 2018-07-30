package biz.dealnote.xmpp.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.ActivityUtils;
import biz.dealnote.xmpp.callback.AppStyleable;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AppRosterEntry;
import biz.dealnote.xmpp.mvp.presenter.ChatsPresenter;
import biz.dealnote.xmpp.service.request.RequestAdapter;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.service.request.XmppRequestManager;
import biz.dealnote.xmpp.util.AppPrefs;

import static biz.dealnote.xmpp.util.Utils.isEmpty;

public class MainTabsFragment extends Fragment {

    private static final int TAB_CHATS = 0;
    private static final int TAB_CONTACTS = 1;
    private static final int TAB_SETTINGS = 2;

    private static final RequestAdapter DUMMMY = new RequestAdapter();

    private View[] tabViews;
    private TabLayout mTabLayout;
    private ViewPager viewPager;

    public static MainTabsFragment newInstance() {
        return new MainTabsFragment();
    }

    private int[] counters;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : null;
            if(ChatsPresenter.WHAT_UNREAD_CHATS_COUNT.equals(action)){
                int count = intent.getIntExtra(Extra.COUNT, 0);
                counters[TAB_CHATS] = count;
                refreshTabViews();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            counters = savedInstanceState.getIntArray("counters");
        } else {
            counters = new int[3];
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver,
                new IntentFilter(ChatsPresenter.WHAT_UNREAD_CHATS_COUNT));
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray("counters", counters);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        mTabLayout = (TabLayout) root.findViewById(R.id.tablayout);

        viewPager = (ViewPager) root.findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(2);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                refreshTabViews();
                resolveActionBarText();
            }
        });

        tabViews = new View[3];
        tabViews[TAB_CHATS] = inflater.inflate(R.layout.tab_layout_main, mTabLayout, false);
        tabViews[TAB_CONTACTS] = inflater.inflate(R.layout.tab_layout_main, mTabLayout, false);
        tabViews[TAB_SETTINGS] = inflater.inflate(R.layout.tab_layout_main, mTabLayout, false);

        refreshTabViews();
        return root;
    }

    private void refreshTabViews(){
        if(!isAdded() || viewPager == null || tabViews == null) return;

        int tab = viewPager.getCurrentItem();

        setupTabView(TAB_CHATS, R.drawable.ic_chats, R.drawable.ic_chats_disable, tab == TAB_CHATS, counters[TAB_CHATS]);
        setupTabView(TAB_CONTACTS, R.drawable.ic_contacts, R.drawable.ic_contacts_disable, tab == TAB_CONTACTS, counters[TAB_CONTACTS]);
        setupTabView(TAB_SETTINGS, R.drawable.ic_settings, R.drawable.ic_settings_disable, tab == TAB_SETTINGS, counters[TAB_SETTINGS]);
    }

    private void setupTabView(int index, int iconRes, int iconResDisable, boolean enabled, int counterValue){
        tabViews[index].findViewById(R.id.counter_root).setVisibility(counterValue > 0 ? View.VISIBLE : View.GONE);

        ImageView icon = (ImageView) tabViews[index].findViewById(R.id.imageView);
        icon.setImageResource(enabled ? iconRes : iconResDisable);

        TextView counter = (TextView) tabViews[index].findViewById(R.id.counter);
        counter.setText(String.valueOf(counterValue));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MyPagerAdapter myPagerAdapter = new MyPagerAdapter(getChildFragmentManager());
        myPagerAdapter.addFragment(ChatsFragment.newInstance(), getString(R.string.chats));
        myPagerAdapter.addFragment(ContactsFragment.newInstance(), getString(R.string.contacts));
        myPagerAdapter.addFragment(SettingsFragment.newInstance(), getString(R.string.settings));

        viewPager.setAdapter(myPagerAdapter);

        mTabLayout.setupWithViewPager(viewPager);

        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(myPagerAdapter.getTabView(i));
            }
        }
    }

    private void resolveActionBarText() {
        if (!isAdded()) return;

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            switch (viewPager.getCurrentItem()) {
                case TAB_CHATS:
                    actionBar.setTitle(R.string.chats);
                    break;
                case TAB_CONTACTS:
                    actionBar.setTitle(R.string.contacts);
                    break;
                case TAB_SETTINGS:
                    actionBar.setTitle(R.string.settings);
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resolveActionBarText();
        refreshTabViews();

        if (getActivity() instanceof AppStyleable) {
            AppStyleable appStyleable = (AppStyleable) getActivity();
            appStyleable.enableToolbarElevation(false);
        }

        if (AppPrefs.checkOnline(getActivity())) {
            List<Account> accounts = Accounts.getAll(getActivity());
            if (!isEmpty(accounts)) {
                for (Account account : accounts) {
                    XmppRequestManager.from(getActivity())
                            .execute(RequestFactory.getSendPresenceRequest(account.id, account.buildBareJid(), null, AppRosterEntry.PRESENCE_TYPE_AVAILABLE), DUMMMY);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // notifying nested fragments (support library bug fix)
        // https://gist.github.com/artem-zinnatullin/6916740
        final FragmentManager childFragmentManager = getChildFragmentManager();
        if (childFragmentManager != null) {
            final List<Fragment> nestedFragments = childFragmentManager.getFragments();
            if (isEmpty(nestedFragments)) {
                return;
            }

            for (Fragment childFragment : nestedFragments) {
                //if (childFragment != null && !childFragment.isDetached() && !childFragment.isRemoving()) { // original, но я так не думаю
                if (childFragment != null) {
                    childFragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        View getTabView(int position) {
            return tabViews[position];
        }
    }
}
