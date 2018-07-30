package biz.dealnote.xmpp.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.callback.OnBackButtonCallback;
import biz.dealnote.xmpp.fragment.FileManagerFragment;
import biz.dealnote.xmpp.fragment.PhotosFragments;

public class FileManagerActivity extends AppCompatActivity {

    private MyPagerAdapter mAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_ab_close);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(0);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        tabLayout.setTabTextColors(Color.WHITE, Color.WHITE);

        viewPager = (ViewPager) findViewById(R.id.viewpager);

        mAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mAdapter);

        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onBackPressed() {
        int currentTab = viewPager.getCurrentItem();

        for (int i = 0; i < mAdapter.fragments.size(); i++) {
            int key = mAdapter.fragments.keyAt(i);

            if (currentTab != key) {
                continue;
            }

            Fragment fragment = mAdapter.fragments.get(key);
            if (fragment instanceof OnBackButtonCallback) {
                if (!((OnBackButtonCallback) fragment).onBackPressed()) {
                    return;
                }
            }
        }

        super.onBackPressed();
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        SparseArray<Fragment> fragments = new SparseArray<>();

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new PhotosFragments();
                case 1:
                    return new FileManagerFragment();
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.photos);
                case 1:
                    return getString(R.string.file_explorer);
            }

            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            fragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            fragments.remove(position);
            super.destroyItem(container, position, object);
        }
    }
}
