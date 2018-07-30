package biz.dealnote.xmpp.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.fragment.AllNewLoginFragment;
import biz.dealnote.xmpp.fragment.CreateAccountFragment;
import biz.dealnote.xmpp.fragment.SignInFragment;

public class LoginActivity extends AppCompatActivity implements SignInFragment.Callback {

    public static final String EXTRA_START_MAIN_ACTIVITY_ON_SUCCESS = "start_main_activity";
    public static final String EXTRA_RESULT = "result";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signin);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (savedInstanceState == null) {
            /*SignInFragment fragment = SignInFragment.newInstance();
            fragment.setArguments(getIntent().getExtras());*/

            Fragment fragment = AllNewLoginFragment.Companion.newInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, fragment)
                    .commit();
        }
    }

    @Override
    public void onRegisterClick(Fragment target, int requestCode) {
        CreateAccountFragment accountFragment = CreateAccountFragment.newInstance();
        accountFragment.setTargetFragment(target, requestCode);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, accountFragment)
                .addToBackStack("sign_up")
                .commit();
    }
}
